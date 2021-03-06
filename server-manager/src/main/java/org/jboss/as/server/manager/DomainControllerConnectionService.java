/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.server.manager;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jboss.as.services.net.NetworkInterfaceBinding;
import org.jboss.logging.Logger;
import org.jboss.msc.inject.InjectionException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Domain controller client service.  This will connect to a domain controller to manage inter-process communication.
 * Once the connection is established this will register itself with the domain controller and start listening for
 * commands from the domain controller.
 *
 * @author John E. Bailey
 */
public class DomainControllerConnectionService implements Service<Void> {
    static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("domain", "controller", "client");
    private static final Logger log = Logger.getLogger("org.jboss.as.server.manager");
    private static final long POLLING_INTERVAL = 10000L;

    private final InjectedValue<InetAddress> domainControllerAddress = new InjectedValue<InetAddress>();
    private final InjectedValue<Integer> domainControllerPort = new InjectedValue<Integer>();
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private ExecutorService executor; // TODO: inject
    private Socket socket;
    private InputStream socketIn;
    private OutputStream socketOut;

    final ServerManager serverManager;

    public DomainControllerConnectionService(final ServerManager serverManager) {
        this.serverManager = serverManager;
    }

    /**
     * Start the service.  Creates and executes a {@link ConnectTask} to establish a connection to the domain controller.
     *
     * @param context The start context
     * @throws StartException
     */
    public synchronized void start(final StartContext context) throws StartException {
        if(started.compareAndSet(false, true)) {
            executor = Executors.newFixedThreadPool(2);
            executor.execute(new ConnectTask());
        }
    }

    /**
     * Stop the service.  This will shut down the executor and close the socket.
     *
     * @param context The stop context.
     */
    public synchronized void stop(final StopContext context) {
        if(started.compareAndSet(true, false)) {
            try {
                if (executor != null) {
                    executor.shutdown();
                }
                unregister();
            } finally {
                closeSocket();
            }
        }
    }

    /**
     * No value for this service.
     *
     * @return {@code null}
     */
    public synchronized Void getValue() throws IllegalStateException {
        return null;
    }

    public Injector<NetworkInterfaceBinding> getDomainControllerInterface() {
        return new Injector<NetworkInterfaceBinding>() {
            @Override
            public void inject(NetworkInterfaceBinding value) throws InjectionException {
                domainControllerAddress.inject(value.getAddress());
            }

            @Override
            public void uninject() {
                domainControllerAddress.uninject();
            }
        };
    }

    public Injector<InetAddress> getDomainControllerAddressInjector() {
        return domainControllerAddress;
    }

    public Injector<Integer> getDomainControllerPortInjector() {
        return domainControllerPort;
    }

    /**
     * Register from the domain controller.
     */
    void register() {
        try {
            DomainControllerClientProtocol.OutgoingCommand.REGISTER.execute(this, DomainControllerConnectionService.this.socketOut);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to register from domain controller", t); // TODO: better exception
        }
    }

    /**
     * Unregister from the domain controller.
     */
    void unregister() {
        try {
            DomainControllerClientProtocol.OutgoingCommand.UNREGISTER.execute(this, DomainControllerConnectionService.this.socketOut);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to unregister from domain controller", t); // TODO: better exception
        }
    }

    /**
     * Task that attempts to establish a connection to the domain controller.  It will poll
     * until it can establish the connection.  Once connected it will register with with the domain controller to let it
     * know this server manger exists.  Once registered it will start listening for commands from the domain controller.
     */
    private class ConnectTask implements Runnable {
        public void run() {
            if(connected.compareAndSet(false, true)) {
                for (; ;) {
                    try {
                        InetAddress dcAddress = domainControllerAddress.getValue();
                        if (dcAddress.isAnyLocalAddress() || dcAddress.isSiteLocalAddress()) {
                            dcAddress = InetAddress.getLocalHost();
                        }
                        DomainControllerConnectionService.this.socket = new Socket(dcAddress, domainControllerPort.getValue());
                        DomainControllerConnectionService.this.socketIn = new BufferedInputStream(socket.getInputStream());
                        DomainControllerConnectionService.this.socketOut = new BufferedOutputStream(socket.getOutputStream());
                        log.infof("Connected to domain controller [%s:%d]", dcAddress.getHostAddress(), domainControllerPort.getValue());
                        executor.execute(new ListenerTask());
                        serverManager.setDomainControllerConnection(new RemoteDomainControllerConnection(DomainControllerConnectionService.this));
                        break;
                    } catch (IOException e) {
                        log.info("Unable to connect to domain controller.  Will retry.");
                        log.trace("Unable to connect to domain controller.", e);
                    }
                    try {
                        Thread.sleep(POLLING_INTERVAL);
                    } catch (InterruptedException e) {
                        throw new RuntimeException("Domain controller connection polling was interrupted.", e);
                    }
                }
            }
        }
    }


    /**
     * Task that listens for commands from the domain controller.  When it receives the command it will determine the
     * correct {@link org.jboss.as.server.manager.DomainControllerClientProtocol.IncomingCommand} and execute it.
     */
    private class ListenerTask implements Runnable {
        @Override
        public void run() {
            boolean reconnect = false;
            try {
                for (; ;) {
                    if (!socket.isConnected()) {
                        reconnect = true;
                        break;
                    }
                    if(!DomainControllerClientProtocol.IncomingCommand.processNext(DomainControllerConnectionService.this, DomainControllerConnectionService.this.socketIn)) {
                        reconnect = true;
                        break;
                    }
                }
            } catch (Throwable t) {
                throw new RuntimeException(t); // TODO:  Better exceptions
            } finally {
                closeSocket();
                if(reconnect && started.get()) {
                    executor.execute(new ConnectTask());
                }
            }
        }
    }

    private void closeSocket() {
        if(connected.compareAndSet(true, false)) {
            if(socket == null) return;
            try {
                socket.shutdownOutput();
            } catch (IOException ignored) {
            }
            try {
                socket.shutdownInput();
            } catch (IOException ignored) {
            }
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }
}
