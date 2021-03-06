/*
  Copyright (C) 2013-2022 Expedia Inc.

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */
package com.hotels.styx.client.connectionpool;

import com.hotels.styx.api.Clock;
import com.hotels.styx.client.Connection;
import com.hotels.styx.client.connectionpool.stubs.StubConnectionFactory;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ExpiringConnectionTest {
    @Test
    public void shouldExpireConnection() {
        Connection trackedConnection = new StubConnectionFactory.StubConnection(null);

        ExpiringConnection connectionTracker = new ExpiringConnection(trackedConnection, 2, new OneSecondPerTickClock());

        assertThat(connectionTracker.isConnected(), is(true));
        assertThat(connectionTracker.isConnected(), is(false));
    }

    /**
     * Dummy ticker that ticks one second every time a owner stop watch is checked.
     */
    private static class OneSecondPerTickClock implements Clock {
        private long counter = 0;

        @Override
        public long tickMillis() {
            counter += 1000;
            return counter;
        }
    }
}
