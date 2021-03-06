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

import com.hotels.styx.api.HttpInterceptor;
import com.hotels.styx.api.HttpRequest;
import com.hotels.styx.api.HttpResponse;
import com.hotels.styx.common.http.handler.BaseHttpHandler;

import static com.hotels.styx.api.HttpHeaderNames.CONTENT_TYPE;
import static com.hotels.styx.api.HttpHeaderValues.PLAIN_TEXT;
import static com.hotels.styx.api.HttpResponse.response;
import static com.hotels.styx.api.HttpResponseStatus.OK;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * A simple handler that can confirm that the admin interface is available. Returns a non-caching plain-text response
 * with a body consisting of the string "pong".
 */
public class PingHandler extends BaseHttpHandler {
    @Override
    protected HttpResponse doHandle(HttpRequest request, HttpInterceptor.Context context) {
        return response(OK)
                .disableCaching()
                .addHeader(CONTENT_TYPE, PLAIN_TEXT)
                .body("pong", UTF_8)
                .build();
    }
}
