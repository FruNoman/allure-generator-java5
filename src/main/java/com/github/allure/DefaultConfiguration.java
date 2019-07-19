/*
 *  Copyright 2019 Qameta Software OÜ
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.github.allure;

import io.qameta.allure.Aggregator;
import io.qameta.allure.Extension;
import io.qameta.allure.Reader;
import io.qameta.allure.core.Configuration;
import io.qameta.allure.core.Plugin;

import java.util.Collections;
import java.util.List;
import java8.util.Optional;
import java8.util.stream.Collectors;
import java8.util.stream.StreamSupport;

/**
 * Default implementation of {@link Configuration}.
 *
 * @since 2.0
 */
public class DefaultConfiguration implements Configuration {

    private final List<Extension> extensions;

    private final List<Plugin> plugins;

    public DefaultConfiguration(final List<Extension> extensions,
                                final List<Plugin> plugins) {
        this.extensions = extensions;
        this.plugins = plugins;
    }

    @Override
    public List<Plugin> getPlugins() {
        return Collections.unmodifiableList(plugins);
    }

    @Override
    public List<Aggregator> getAggregators() {
        return StreamSupport.stream(extensions)
                .filter(Aggregator.class::isInstance)
                .map(Aggregator.class::cast)
                .collect(Collectors.toList());
    }

    @Override
    public List<Reader> getReaders() {
        return StreamSupport.stream(extensions)
                .filter(Reader.class::isInstance)
                .map(Reader.class::cast)
                .collect(Collectors.toList());
    }

    @Override
    public <T> T  getContext(final Class<T> contextType) {
        return StreamSupport.stream(extensions)
                .filter(contextType::isInstance)
                .map(contextType::cast)
                .findFirst().get();
    }

    @Override
    public <T> T requireContext(Class<T> aClass, Exception e) {
        // TODO: fix context
        return null;
    }
}
