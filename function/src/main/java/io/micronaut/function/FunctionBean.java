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

package io.micronaut.function;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import io.micronaut.context.annotation.Executable;

import javax.inject.Singleton;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * <p>An annotation applied to classes that should be exposed as functions. The class itself must implement an interface
 * from <tt>java.util.function</tt> such as {@link java.util.function.Consumer} to be exposed as a function.</p>
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@Documented
@Retention(RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Singleton
@Executable
public @interface FunctionBean {

    /**
     * @return An optional ID of the function which may or may not be used depending on the target platform
     */
    String value() default "";
}
