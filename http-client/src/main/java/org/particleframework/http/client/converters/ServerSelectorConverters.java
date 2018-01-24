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
package org.particleframework.http.client.converters;

import org.particleframework.core.convert.ConversionService;
import org.particleframework.core.convert.TypeConverterRegistrar;
import org.particleframework.http.client.ServerSelector;

import javax.inject.Singleton;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.function.Function;

/**
 * Converters from URL to {@link ServerSelector} interface
 *
 * @author graemerocher
 * @since 1.0
 */
@Singleton
public class ServerSelectorConverters implements TypeConverterRegistrar {
    @Override
    public void register(ConversionService<?> conversionService) {
        conversionService.addConverter(URL.class, ServerSelector.class, (Function<URL, ServerSelector>) url -> discriminator -> url);
        conversionService.addConverter(String.class, ServerSelector.class, (Function<String, ServerSelector>) url -> discriminator -> {
            try {
                return new URL(url);
            } catch (MalformedURLException e) {
                return null;
            }
        });
    }
}
