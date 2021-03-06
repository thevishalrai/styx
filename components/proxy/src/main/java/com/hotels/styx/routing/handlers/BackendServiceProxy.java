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
package com.hotels.styx.routing.handlers;

import com.hotels.styx.Environment;
import com.hotels.styx.NettyExecutor;
import com.hotels.styx.api.Eventual;
import com.hotels.styx.api.HttpInterceptor;
import com.hotels.styx.api.LiveHttpRequest;
import com.hotels.styx.api.LiveHttpResponse;
import com.hotels.styx.api.extension.service.BackendService;
import com.hotels.styx.api.extension.service.spi.Registry;
import com.hotels.styx.infrastructure.configuration.yaml.JsonNodeConfig;
import com.hotels.styx.proxy.BackendServiceClientFactory;
import com.hotels.styx.proxy.BackendServicesRouter;
import com.hotels.styx.proxy.RouteHandlerAdapter;
import com.hotels.styx.proxy.StyxBackendServiceClientFactory;
import com.hotels.styx.routing.RoutingObject;
import com.hotels.styx.routing.config.RoutingObjectFactory;
import com.hotels.styx.routing.config.StyxObjectDefinition;

import java.util.List;
import java.util.Map;

import static com.hotels.styx.routing.config.RoutingSupport.append;
import static com.hotels.styx.routing.config.RoutingSupport.missingAttributeError;
import static java.lang.String.format;
import static java.lang.String.join;

/**
 * A HTTP routingObject that proxies requests to backend services based on the path prefix.
 *
 * @deprecated  Will be removed in Styx 1.1 release. Use a combination of HostProxy and LoadBalancingGroup,
 *              and PathPrefixRouter to achieve the same functionality.
 */
@Deprecated
public class BackendServiceProxy implements RoutingObject {

    private final RouteHandlerAdapter handler;

    private BackendServiceProxy(
            BackendServiceClientFactory serviceClientFactory,
            Registry<BackendService> registry,
            Environment environment,
            NettyExecutor executor) {
        BackendServicesRouter router = new BackendServicesRouter(serviceClientFactory, environment, executor);
        registry.addListener(router);
        handler = new RouteHandlerAdapter(router);
    }

    @Override
    public Eventual<LiveHttpResponse> handle(LiveHttpRequest request, HttpInterceptor.Context context) {
        return handler.handle(request, context);
    }

    /**
     * Builds a BackendServiceProxy from yaml routing configuration.
     */
    public static class Factory implements RoutingObjectFactory {
        private final BackendServiceClientFactory serviceClientFactory;
        private final Map<String, Registry<BackendService>> backendRegistries;
        private final Environment environment;

        private static StyxBackendServiceClientFactory serviceClientFactory(Environment environment) {
            return new StyxBackendServiceClientFactory(environment);
        }

        // Visible for testing
        Factory(Environment environment, BackendServiceClientFactory serviceClientFactory, Map<String, Registry<BackendService>> backendRegistries) {
            this.serviceClientFactory = serviceClientFactory;
            this.backendRegistries = backendRegistries;
            this.environment = environment;
        }

        public Factory(Environment environment, Map<String, Registry<BackendService>> backendRegistries) {
            this.backendRegistries = backendRegistries;
            this.serviceClientFactory = serviceClientFactory(environment);
            this.environment = environment;
        }

        @Override
        public RoutingObject build(List<String> fullName, Context context, StyxObjectDefinition configBlock) {
            JsonNodeConfig config = new JsonNodeConfig(configBlock.config());
            String provider = config.get("backendProvider")
                    .orElseThrow(() -> missingAttributeError(configBlock, join(".", fullName), "backendProvider"));

            Registry<BackendService> registry = backendRegistries.get(provider);
            if (registry == null) {
                throw new IllegalArgumentException(
                        format("No such backend service provider exists, attribute='%s', name='%s'",
                                join(".", append(fullName, "backendProvider")), provider));
            }

            return new BackendServiceProxy(serviceClientFactory, registry, environment, NettyExecutor.create("BackendServiceProxy", 0));
        }
    }

}
