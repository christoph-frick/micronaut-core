/*
 * Copyright 2017 original authors
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
package org.particleframework.http.server.netty.async;

import io.netty.channel.ChannelHandlerContext;
import org.particleframework.core.async.subscriber.CompletionAwareSubscriber;
import org.reactivestreams.Subscription;

/**
 * A subscriber that subscribes to a single result with special handling
 * for the {@link ChannelHandlerContext}
 *
 * @param <T> The type of data being published
 *
 * @author James Kleeh
 * @since 1.0
 */
public abstract class ContextCompletionAwareSubscriber<T> extends CompletionAwareSubscriber<T> {

    private final ChannelHandlerContext context;
    private Subscription s;
    private Object message;

    public ContextCompletionAwareSubscriber(ChannelHandlerContext context) {
        this.context = context;
    }

    @Override
    protected void doOnSubscribe(Subscription subscription) {
        this.s = subscription;
        this.s.request(1);
    }

    @Override
    protected void doOnNext(T message) {
        this.message = message;
    }

    @Override
    protected void doOnError(Throwable t) {
        context.pipeline().fireExceptionCaught(t);
    }

    @Override
    protected void doOnComplete() {
        onComplete((T)message);
    }

    abstract protected void onComplete(T message);
}
