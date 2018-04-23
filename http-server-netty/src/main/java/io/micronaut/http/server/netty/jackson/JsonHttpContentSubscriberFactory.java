/*
 * Copyright 2017-2018 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.micronaut.http.server.netty.jackson;

import com.fasterxml.jackson.core.JsonFactory;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.server.HttpServerConfiguration;
import io.micronaut.http.server.netty.HttpContentProcessor;
import io.micronaut.http.server.netty.HttpContentSubscriberFactory;
import io.micronaut.http.server.netty.NettyHttpRequest;

import javax.inject.Singleton;
import java.util.Optional;

/**
 * Builds the {@link org.reactivestreams.Subscriber} for JSON requests.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@Consumes(MediaType.APPLICATION_JSON)
@Singleton
public class JsonHttpContentSubscriberFactory implements HttpContentSubscriberFactory {

    private final HttpServerConfiguration httpServerConfiguration;
    private final Optional<JsonFactory> jsonFactory;

    /**
     * @param httpServerConfiguration The Http server configuration
     * @param jsonFactory             The json factory
     */
    public JsonHttpContentSubscriberFactory(HttpServerConfiguration httpServerConfiguration, Optional<JsonFactory> jsonFactory) {
        this.httpServerConfiguration = httpServerConfiguration;
        this.jsonFactory = jsonFactory;
    }

    @Override
    public HttpContentProcessor build(NettyHttpRequest request) {
        return new JsonContentProcessor(request, httpServerConfiguration, jsonFactory);
    }
}
