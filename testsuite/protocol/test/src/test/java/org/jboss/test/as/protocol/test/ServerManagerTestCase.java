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
package org.jboss.test.as.protocol.test;

import org.jboss.test.as.protocol.test.base.ServerManagerTest;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * The real code is in org.jboss.test.as.protocol.test.module.ServerManagerTestModule
 * which is loaded using the jboss-modules classloader
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
@RunWith(LoggingTestRunner.class)
public class ServerManagerTestCase extends AbstractProtocolTest<ServerManagerTest> implements ServerManagerTest{

	public ServerManagerTestCase() {
	    super(ServerManagerTest.class);
    }

	@Test
	public void testStartServerManagerNoConfig() throws Exception {
		getTestInstance().testStartServerManagerNoConfig();
	}

	@Test
	public void testStartStopServerManager() throws Exception {
		getTestInstance().testStartStopServerManager();
	}

	@Test
    public void testServerStartFailedAndGetsRespawned() throws Exception {
		getTestInstance().testServerStartFailedAndGetsRespawned();
    }

	@Test
    public void testServerStartFailedAndRespawnExpires() throws Exception {
		getTestInstance().testServerStartFailedAndRespawnExpires();
    }

	@Test
    public void testServerCrashedAfterStartGetsRespawned() throws Exception {
		getTestInstance().testServerCrashedAfterStartGetsRespawned();
    }

	@Test
	public void testServersGetReconnectMessageFollowingRestartedServerManager_StartedDoesNotGetStarted() throws Exception {
	    getTestInstance().testServersGetReconnectMessageFollowingRestartedServerManager_StartedDoesNotGetStarted();
	}

	@Test
    public void testServersGetReconnectMessageFollowingRestartedServerManager_AvailableGetsStarted() throws Exception {
        getTestInstance().testServersGetReconnectMessageFollowingRestartedServerManager_AvailableGetsStarted();
    }

	@Test
    public void testServersGetReconnectMessageFollowingRestartedServerManager_BootingGetsStarted() throws Exception {
        getTestInstance().testServersGetReconnectMessageFollowingRestartedServerManager_BootingGetsStarted();
    }

	@Test
    public void testServersGetReconnectMessageFollowingRestartedServerManager_FailedGetsStarted() throws Exception {
        getTestInstance().testServersGetReconnectMessageFollowingRestartedServerManager_FailedGetsStarted();
    }

	@Test
    public void testServersGetReconnectMessageFollowingRestartedServerManager_StartingDoesNotGetStarted() throws Exception {
        getTestInstance().testServersGetReconnectMessageFollowingRestartedServerManager_StartingDoesNotGetStarted();
    }

	@Test
    public void testServersGetReconnectMessageFollowingRestartedServerManager_StoppedDoesNotGetStarted() throws Exception {
        getTestInstance().testServersGetReconnectMessageFollowingRestartedServerManager_StoppedDoesNotGetStarted();
    }

	@Test
    public void testServersGetReconnectMessageFollowingRestartedServerManager_StoppingDoesNotGetStarted() throws Exception {
        getTestInstance().testServersGetReconnectMessageFollowingRestartedServerManager_StoppingDoesNotGetStarted();
    }

    @Test
    public void testServerGetsReconnectedFollowingBrokenConnection() throws Exception {
        getTestInstance().testServerGetsReconnectedFollowingBrokenConnection();
    }
}