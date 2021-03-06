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

/**
 *
 */
package org.jboss.as.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.as.model.ServerModel;
import org.jboss.as.server.manager.ServerManagerProtocolCommand;
import org.jboss.as.server.manager.ServerManagerProtocolUtils;
import org.jboss.as.server.manager.ServerState;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartException;


/**
 * An actual JBoss Application Server instance.
 *
 * @author Brian Stansberry
 * @author John E. Bailey
 */
public class Server extends AbstractServer {

    private ServerCommunicationHandler serverCommunicationHandler;
    private ServerCommunicationHandler processManagerCommunicationHandler;
    private AtomicBoolean stopping = new AtomicBoolean();

    private final MessageHandler messageHandler = new MessageHandler(this);

    private volatile ServerState state;

    public Server(ServerEnvironment environment) {
        super(environment);
        state = ServerState.BOOTING;
    }

    public void start() {
        launchCommunicationHandlers();
        sendMessage(ServerManagerProtocolCommand.SERVER_AVAILABLE);
        state = ServerState.AVAILABLE;
        log.info("Server Available to start");
    }

    public void start(ServerModel config) throws ServerStartException {
        try {
            state = ServerState.STARTING;
            super.start(config);
        } catch(ServerStartException e) {
            state = ServerState.FAILED;
            sendMessage(ServerManagerProtocolCommand.SERVER_START_FAILED);
            throw e;
        }
    }

    public void stop() {
        if (stopping.getAndSet(true))
            return;
        state = ServerState.STOPPING;
        super.stop();
        sendMessage(ServerManagerProtocolCommand.SERVER_STOPPED);
        state = ServerState.STOPPED;
        log.info("Server stopped");
        serverCommunicationHandler.shutdown();
        processManagerCommunicationHandler.shutdown();
    }

    public void reconnectToServerManager(String host, String port) {
        InetAddress addr;
        try {
            addr = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            log.errorf("Could not parse %s into a host", host);
            return;
        }
        Integer portNumber;
        try {
            portNumber = Integer.valueOf(port);
        } catch (NumberFormatException e) {
            log.errorf("Could not parse %s into a port", port);
            return;
        }
        this.serverCommunicationHandler = new DirectServerCommunicationHandler(getEnvironment().getProcessName(), addr, portNumber, messageHandler);
        this.serverCommunicationHandler.start();
        sendMessage(ServerManagerProtocolCommand.SERVER_RECONNECT_STATUS, state);
    }

    @Override
    ServerStartupListener.Callback createListenerCallback() {
        return new ServerStartupListener.Callback() {
            public void run(Map<ServiceName, StartException> serviceFailures, long elapsedTime, int totalServices, int onDemandServices, int startedServices) {
                if(serviceFailures.isEmpty()) {
                    log.infof("JBoss AS started in %dms. - Services [Total: %d, On-demand: %d. Started: %d]", elapsedTime, totalServices, onDemandServices, startedServices);
                    state = ServerState.STARTED;
                    sendMessage(ServerManagerProtocolCommand.SERVER_STARTED);
                } else {
                    state = ServerState.FAILED;
                    sendMessage(ServerManagerProtocolCommand.SERVER_START_FAILED);
                    final StringBuilder buff = new StringBuilder(String.format("JBoss AS server start failed. Attempted to start %d services in %dms", totalServices, elapsedTime));
                    buff.append("\nThe following services failed to start:\n");
                    for(Map.Entry<ServiceName, StartException> entry : serviceFailures.entrySet()) {
                        buff.append(String.format("\t%s => %s\n", entry.getKey(), entry.getValue().getMessage()));
                    }
                    log.error(buff.toString());
                }
            }
        };
    }


    private void launchCommunicationHandlers() {
        ServerEnvironment env = getEnvironment();
        this.processManagerCommunicationHandler = new ProcessManagerServerCommunicationHandler(env.getProcessName(), env.getProcessManagerAddress(), env.getProcessManagerPort(), messageHandler);
        this.processManagerCommunicationHandler.start();
        this.serverCommunicationHandler = new DirectServerCommunicationHandler(env.getProcessName(), env.getServerManagerAddress(), env.getServerManagerPort(), messageHandler);
        this.serverCommunicationHandler.start();
    }

    private void sendMessage(ServerManagerProtocolCommand command) {
        try {
            byte[] bytes = command.createCommandBytes(null);
            serverCommunicationHandler.sendMessage(bytes);
        } catch (IOException e) {
            log.error("Failed to send message to Server Manager [" + command + "]", e);
        }
    }

    private void sendMessage(ServerManagerProtocolCommand command, Object data) {
        try {
            serverCommunicationHandler.sendMessage(ServerManagerProtocolUtils.createCommandBytes(command, data));
        } catch (IOException e) {
            log.error("Failed to send message to Server Manager [" + command + ":" + data + "]", e);
        }
    }
}
