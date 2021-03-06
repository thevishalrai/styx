/*
  Copyright (C) 2013-2021 Expedia Inc.

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
package com.hotels.styx.client.loadbalancing.strategies;

import com.hotels.styx.api.Environment;
import com.hotels.styx.api.HttpHandler;
import com.hotels.styx.api.Id;
import com.hotels.styx.api.configuration.Configuration;
import com.hotels.styx.api.extension.ActiveOrigins;
import com.hotels.styx.api.extension.Origin;
import com.hotels.styx.api.extension.OriginsSnapshot;
import com.hotels.styx.api.extension.RemoteHost;
import com.hotels.styx.api.extension.loadbalancing.spi.LoadBalancer;
import com.hotels.styx.api.extension.loadbalancing.spi.LoadBalancingMetric;
import com.hotels.styx.api.extension.loadbalancing.spi.LoadBalancingMetricSupplier;
import com.hotels.styx.client.connectionpool.stubs.StubConnectionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static com.hotels.styx.api.Id.id;
import static com.hotels.styx.api.extension.Origin.newOriginBuilder;
import static com.hotels.styx.api.extension.RemoteHost.remoteHost;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RoundRobinStrategyTest {
    private static final StubConnectionFactory connectionFactory = new StubConnectionFactory();
    private static final RemoteHost HOST_1 = remoteHostFor("localhost", 1);
    private static final RemoteHost HOST_2 = remoteHostFor("localhost", 2);
    private static final RemoteHost HOST_3 = remoteHostFor("localhost", 3);
    private static final RemoteHost HOST_4 = remoteHostFor("localhost", 4);
    private static final RemoteHost HOST_5 = remoteHostFor("localhost", 5);
    private static final RemoteHost HOST_6 = remoteHostFor("localhost", 6);
    private Id APP_ID = id("app");
    private Environment environment;
    private Configuration configuration;

    private static RemoteHost remoteHostFor(String host, int port) {
        Origin origin = newOriginBuilder(host, port).build();

        LoadBalancingMetricSupplier metric = mock(LoadBalancingMetricSupplier.class);
        when(metric.loadBalancingMetric()).thenReturn(new LoadBalancingMetric(45));

        return remoteHost(origin, mock(HttpHandler.class), metric);
    }

    private LoadBalancer strategy;

    @BeforeEach
    public void setUp() {
        environment = mock(Environment.class);
        configuration = mock(Configuration.class);
        strategy = new RoundRobinStrategy.Factory().create(
                environment,
                configuration,
            new ActiveOrigins() {
                @Override
                public Iterable<RemoteHost> snapshot() {
                    return asList(HOST_1, HOST_2, HOST_3);
                }

                @Override
                public String getApplicationId() {
                    return null;
                }

                @Override
                public List<Origin> origins() {
                    return null;
                }
            });
    }

    @Test
    public void cyclesThroughOrigins() {
        assertThat(strategy.choose(null), is(Optional.of(HOST_1)));
        assertThat(strategy.choose(null), is(Optional.of(HOST_2)));
        assertThat(strategy.choose(null), is(Optional.of(HOST_3)));
        assertThat(strategy.choose(null), is(Optional.of(HOST_1)));
        assertThat(strategy.choose(null), is(Optional.of(HOST_2)));
    }

    @Test
    public void returnsEmptyIfNoActiveOrigins() {
        strategy = new RoundRobinStrategy.Factory().create(environment, configuration, new ActiveOrigins() {
            @Override
            public Iterable<RemoteHost> snapshot() {
                return List.of();
            }

            @Override
            public String getApplicationId() {
                return null;
            }

            @Override
            public List<Origin> origins() {
                return null;
            }
        });
        assertThat(strategy.choose(null), is(Optional.empty()));
    }

    @Test
    public void refreshesAtOriginsChange() {
        ActiveOrigins activeOrigins = mock(ActiveOrigins.class);
        when(activeOrigins.snapshot()).thenReturn(asList(HOST_1, HOST_2, HOST_3));
        strategy = new RoundRobinStrategy.Factory().create(environment, configuration, activeOrigins);

        assertThat(strategy.choose(null), is(Optional.of(HOST_1)));
        assertThat(strategy.choose(null), is(Optional.of(HOST_2)));
        assertThat(strategy.choose(null), is(Optional.of(HOST_3)));
        assertThat(strategy.choose(null), is(Optional.of(HOST_1)));
        assertThat(strategy.choose(null), is(Optional.of(HOST_2)));

        when(activeOrigins.snapshot()).thenReturn(asList(HOST_2, HOST_4, HOST_6));
        strategy.originsChanged(new OriginsSnapshot(
                APP_ID,
                asList(HOST_2, HOST_4, HOST_6),
                asList(HOST_1, HOST_3),
                asList(HOST_5)
                ));

        assertThat(strategy.choose(null), is(Optional.of(HOST_6)));
        assertThat(strategy.choose(null), is(Optional.of(HOST_2)));
        assertThat(strategy.choose(null), is(Optional.of(HOST_4)));
        assertThat(strategy.choose(null), is(Optional.of(HOST_6)));
        assertThat(strategy.choose(null), is(Optional.of(HOST_2)));
        assertThat(strategy.choose(null), is(Optional.of(HOST_4)));
    }

}