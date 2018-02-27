/*
 * Copyright 2018 original authors
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
package org.particleframework.function.client.aws;

import com.amazonaws.services.lambda.AWSLambdaAsync;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
import org.particleframework.context.annotation.Requires;
import org.particleframework.core.async.publisher.Publishers;
import org.particleframework.core.convert.ConversionService;
import org.particleframework.core.io.buffer.ByteBufferFactory;
import org.particleframework.core.type.Argument;
import org.particleframework.function.client.FunctionDefinition;
import org.particleframework.function.client.FunctionInvoker;
import org.particleframework.function.client.FunctionInvokerChooser;
import org.particleframework.function.client.exceptions.FunctionExecutionException;
import org.particleframework.jackson.codec.JsonMediaTypeCodec;
import org.particleframework.scheduling.executor.IOExecutorServiceConfig;

import javax.inject.Named;
import javax.inject.Singleton;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * A {@link FunctionInvoker} for invoking functions on AWS
 *
 * @author graemerocher
 * @since 1.0
 */
@Requires(beans = AWSLambdaAsync.class)
@Singleton
public class AWSLambdaFunctionExecutor<I, O> implements FunctionInvoker<I, O>, FunctionInvokerChooser {

    private final AWSLambdaAsync asyncClient;
    private final ByteBufferFactory byteBufferFactory;
    private final JsonMediaTypeCodec jsonMediaTypeCodec;
    private final ExecutorService ioExecutor;

    protected AWSLambdaFunctionExecutor(
            AWSLambdaAsync asyncClient,
            ByteBufferFactory byteBufferFactory,
            JsonMediaTypeCodec jsonMediaTypeCodec,
            @Named(IOExecutorServiceConfig.NAME) ExecutorService ioExecutor) {
        this.asyncClient = asyncClient;
        this.byteBufferFactory = byteBufferFactory;
        this.jsonMediaTypeCodec = jsonMediaTypeCodec;
        this.ioExecutor = ioExecutor;
    }

    @Override
    public O invoke(FunctionDefinition definition, I input, Argument<O> outputType) {
        if (!(definition instanceof AWSInvokeRequestDefinition)) {
            throw new IllegalArgumentException("Function definition must be a AWSInvokeRequestDefinition");
        }
        InvokeRequest invokeRequest = ((AWSInvokeRequestDefinition) definition).getInvokeRequest().clone();
        boolean isReactiveType = Publishers.isPublisher(outputType.getType());
        if (isReactiveType) {
            Flowable<Object> invokeFlowable = Flowable.just(invokeRequest)
                    .flatMap(req -> {
                        encodeInput(input, invokeRequest);

                        Future<InvokeResult> future = asyncClient.invokeAsync(req);
                        return Flowable.fromFuture(future, Schedulers.from(ioExecutor));
                    })
                    .map(invokeResult -> decodeResult(definition,(Argument<O>) outputType.getFirstTypeVariable().orElse(Argument.OBJECT_ARGUMENT), invokeResult));

            invokeFlowable = invokeFlowable.onErrorResumeNext(throwable -> {
                return Flowable.error(new FunctionExecutionException("Error executing AWS Lambda [" + definition.getName() + "]: " + throwable.getMessage(), throwable));
            });

            return ConversionService.SHARED.convert(invokeFlowable, outputType).orElseThrow(() -> new IllegalArgumentException("Unsupported Reactive type: " + outputType));

        } else {
            encodeInput(input, invokeRequest);
            InvokeResult invokeResult = asyncClient.invoke(invokeRequest);
            try {
                return (O) decodeResult(definition, outputType, invokeResult);
            } catch (Exception e) {
                throw new FunctionExecutionException("Error executing AWS Lambda [" + definition.getName() + "]: " + e.getMessage(), e);
            }
        }
    }

    private Object decodeResult(FunctionDefinition definition, Argument<O> outputType, InvokeResult invokeResult) {
        Integer statusCode = invokeResult.getStatusCode();
        if (statusCode >= 300) {
            throw new FunctionExecutionException("Error executing AWS Lambda [" + definition.getName() + "]: " + invokeResult.getFunctionError());
        }
        org.particleframework.core.io.buffer.ByteBuffer byteBuffer = byteBufferFactory.copiedBuffer(invokeResult.getPayload());
        return jsonMediaTypeCodec.decode(
                outputType,
                byteBuffer);
    }

    private void encodeInput(I input, InvokeRequest invokeRequest) {
        if (input != null) {
            ByteBuffer byteBuffer = jsonMediaTypeCodec.encode(input, byteBufferFactory).asNioBuffer();
            invokeRequest.setPayload(byteBuffer);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <I1, O2> Optional<FunctionInvoker<I1, O2>> choose(FunctionDefinition definition) {
        if (definition instanceof AWSInvokeRequestDefinition) {
            return Optional.of((FunctionInvoker) this);
        }
        return Optional.empty();
    }
}
