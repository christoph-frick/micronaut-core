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
package io.micronaut.retry.intercept;

import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.convert.value.ConvertibleValues;
import io.micronaut.core.type.Argument;
import io.micronaut.retry.RetryState;
import io.micronaut.retry.RetryStateBuilder;
import io.micronaut.retry.annotation.Retryable;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

/**
 * Builds a {@link RetryState} from {@link AnnotationMetadata}
 *
 * @author graemerocher
 * @since 1.0
 */
class AnnotationRetryStateBuilder implements RetryStateBuilder {

    private final AnnotationMetadata annotationMetadata;
    private static final String ATTEMPTS = "attempts";
    private static final String MULTIPLIER = "multiplier";
    private static final String DELAY = "delay";
    private static final String MAX_DELAY = "maxDelay";
    private static final String INCLUDES = "value";
    private static final String EXCLUDES = "excludes";

    AnnotationRetryStateBuilder(AnnotationMetadata annotationMetadata) {
        this.annotationMetadata = annotationMetadata;
    }

    @Override
    public RetryState build() {
        ConvertibleValues<?> retry = annotationMetadata.getValues(Retryable.class);
        int attempts = retry.get(ATTEMPTS, Integer.class).orElse(3);
        Duration delay = retry.get(DELAY, Duration.class).orElse(Duration.ofSeconds(1));
        Set<Class<? extends Throwable>> includes = resolveIncludes(retry, INCLUDES);
        Set<Class<? extends Throwable>> excludes = resolveIncludes(retry, EXCLUDES);

        return new SimpleRetry(
            attempts,
            retry.get(MULTIPLIER, Double.class).orElse(0d),
            delay,
            retry.get(MAX_DELAY, Duration.class).orElse(null),
            includes,
            excludes
        );
    }

    @SuppressWarnings("unchecked")
    private Set<Class<? extends Throwable>> resolveIncludes(ConvertibleValues<?> retry, String includes) {
        return retry
            .get(includes, Argument.of(Set.class, Argument.of(Class.class, Throwable.class)))
            .orElse(Collections.emptySet());
    }
}
