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
package com.github.allure.trend;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.Aggregator;
import io.qameta.allure.CompositeAggregator;
import io.qameta.allure.Constants;
import io.qameta.allure.Reader;
import io.qameta.allure.context.JacksonContext;
import io.qameta.allure.core.Configuration;
import io.qameta.allure.core.LaunchResults;
import io.qameta.allure.core.ResultsVisitor;
import io.qameta.allure.entity.ExecutorInfo;
import java8.util.Comparators;
import java8.util.Optional;
import java8.util.Spliterator;
import java8.util.Spliterators;
import java8.util.function.Function;
import java8.util.function.Predicate;
import java8.util.stream.StreamSupport;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


import java8.util.stream.Collectors;
import java8.util.stream.Stream;


import static com.github.allure.executor.ExecutorPlugin.EXECUTORS_BLOCK_NAME;
import static io.qameta.allure.Constants.HISTORY_DIR;
import static java.util.Comparator.*;

public abstract class AbstractTrendPlugin<T> extends CompositeAggregator implements Reader {


    private final String jsonFileName;
    private final String trendBlockName;

    protected AbstractTrendPlugin(final List<Aggregator> aggregators, final String jsonFileName,
                                  final String trendBlockName) {
        super(aggregators);
        this.jsonFileName = jsonFileName;
        this.trendBlockName = trendBlockName;
    }

    @Override
    public void readResults(final Configuration configuration,
                            final ResultsVisitor visitor,
                            final List<File> fileList) {
        final JacksonContext context = configuration.getContext(JacksonContext.class);
        Optional<File> historyFile = StreamSupport.stream(fileList).filter(new Predicate<File>() {
            @Override
            public boolean test(File file) {
                return file.getName().equals(HISTORY_DIR);
            }
        }).findFirst();
        if(historyFile.isPresent()) {
            Optional<File> file = StreamSupport.stream(Arrays.asList(historyFile.get().listFiles())).filter(new Predicate<File>() {
                @Override
                public boolean test(File file) {
                    return file.getName().equals(jsonFileName);
                }
            }).findFirst();

            if (file.get().exists()) {
                try  {
                    InputStream is = new FileInputStream(file.get());
                    final ObjectMapper mapper = context.getValue();
                    final JsonNode jsonNode = mapper.readTree(is);
                    final List<T> history;
                    if (jsonNode != null) {
                        history = getStream(jsonNode)
                                .map(new Function<JsonNode, Optional<T>>() {
                                    @Override
                                    public Optional<T> apply(JsonNode jsonNode) {
                                        return parseItem(file.get(), mapper, jsonNode);
                                    }
                                })
                                .filter(new Predicate<Optional<T>>() {
                                    @Override
                                    public boolean test(Optional<T> tOptional) {
                                        return tOptional.isPresent();
                                    }
                                })
                                .map(new Function<Optional<T>, T>() {
                                    @Override
                                    public T apply(Optional<T> tOptional) {
                                        return tOptional.get();
                                    }
                                })
                                .collect(Collectors.toList());
                    } else {
                        history = Collections.emptyList();
                    }
                    visitor.visitExtra(trendBlockName, history);
                } catch (IOException e) {
                    visitor.error("Could not read " + trendBlockName + " file " + historyFile, e);
                }
            }
        }
    }

    private Stream<JsonNode> getStream(final JsonNode jsonNode) {
        return StreamSupport.stream(Spliterators.
                        spliteratorUnknownSize(jsonNode.elements(), Spliterator.ORDERED),
                false);
    }

    private Optional<T> parseItem(final File historyFile, final ObjectMapper mapper, final JsonNode child) {
        try {
            return parseItem(mapper, child);
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }

    protected abstract Optional<T> parseItem(ObjectMapper mapper, JsonNode child) throws JsonProcessingException;

    protected static Optional<ExecutorInfo> extractLatestExecutor(final List<LaunchResults> launches) {
        final Comparator<ExecutorInfo> comparator = Comparators.comparing(ExecutorInfo::getBuildOrder, Comparators.nullsFirst(Comparators.naturalOrder()));
        return StreamSupport.stream(launches)
                .map(new Function<LaunchResults, Optional<Object>>() {
                    @Override
                    public Optional<Object> apply(LaunchResults launchResults) {
                        return launchResults.getExtra(EXECUTORS_BLOCK_NAME);
                    }
                })
                .filter(new Predicate<Optional<Object>>() {
                    @Override
                    public boolean test(Optional<Object> objectOptional) {
                        return objectOptional.isPresent();
                    }
                })
                .map(new Function<Optional<Object>, Object>() {
                    @Override
                    public Object apply(Optional<Object> objectOptional) {
                        return objectOptional.get();
                    }
                })
                .filter(new Predicate<Object>() {
                    @Override
                    public boolean test(Object o) {
                        return ExecutorInfo.class.isInstance(o);
                    }
                })
                .map(new Function<Object, ExecutorInfo>() {
                    @Override
                    public ExecutorInfo apply(Object o) {
                        return ExecutorInfo.class.cast(o);
                    }
                })
                .max(comparator);
    }
}
