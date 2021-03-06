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
package com.hotels.styx.admin.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotels.styx.api.Eventual;
import com.hotels.styx.api.HttpInterceptor;
import com.hotels.styx.api.HttpRequest;
import com.hotels.styx.api.HttpResponse;
import com.hotels.styx.api.WebServiceHandler;
import com.hotels.styx.api.configuration.Configuration;
import com.hotels.styx.common.http.handler.StaticBodyHttpHandler;

import java.util.Map;

import static com.hotels.styx.admin.support.Json.PRETTY_PRINTER;
import static com.hotels.styx.api.HttpHeaderValues.PLAIN_TEXT;

/**
 * Returns a response consisting of the configuration variables.
 */
public class StyxConfigurationHandler implements WebServiceHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final StaticBodyHttpHandler styxConfigHandler;
    private final StaticBodyHttpHandler prettyStyxConfigHandler;

    /**
     * Constructs an instance that will construct a static body from a given configuration.
     *
     * @param configuration configuration
     */
    public StyxConfigurationHandler(Configuration configuration) {
            styxConfigHandler = new StaticBodyHttpHandler(PLAIN_TEXT, body(configuration));
            prettyStyxConfigHandler = new StaticBodyHttpHandler(PLAIN_TEXT, prettify(configuration));
    }

    @Override
    public Eventual<HttpResponse> handle(HttpRequest request, HttpInterceptor.Context context) {
        return configHandler(request.queryParam("pretty").isPresent())
                .handle(request, context)
                .map(StyxConfigurationHandler::disableCaching);
    }

    private StaticBodyHttpHandler configHandler(boolean pretty) {
        return pretty ? prettyStyxConfigHandler : styxConfigHandler;
    }

    private static HttpResponse disableCaching(HttpResponse response) {
        return response.newBuilder()
                .disableCaching()
                .build();
    }

    private static String body(Configuration styxConfig) {
        return styxConfig + "\n";
    }

    private String prettify(Configuration configuration) {
        try {
            return objectMapper
                    .writer(PRETTY_PRINTER)
                    .writeValueAsString(objectMapper.readValue(body(configuration), Map.class));
        } catch (RuntimeException | Error cause) {
            throw cause;
        } catch (Exception cause) {
            throw new RuntimeException(cause);
        }
    }
}
