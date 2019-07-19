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
package com.github.allure.environment;




import io.qameta.allure.CommonJsonAggregator;
import io.qameta.allure.core.LaunchResults;
import io.qameta.allure.entity.EnvironmentItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java8.util.function.Function;
import java8.util.function.Supplier;
import java8.util.stream.Collectors;
import java8.util.stream.Stream;
import java8.util.stream.StreamSupport;


/**
 * @author Egor Borisov ehborisov@gmail.com
 */
public class Allure1EnvironmentPlugin extends CommonJsonAggregator {
    public static final String ENVIRONMENT_BLOCK_NAME = "environment";

    public Allure1EnvironmentPlugin() {
        super("widgets", "environment.json");
    }

    @Override
    public List<EnvironmentItem> getData(final List<LaunchResults> launches) {
        final List<Map.Entry<String, String>> launchEnvironments = StreamSupport.stream(launches)
                .flatMap(new Function<LaunchResults, Stream<Map.Entry<String, String>>>() {
                    @Override
                    public Stream<Map.Entry<String, String>> apply(LaunchResults launchResults) {
                        return StreamSupport.stream(launchResults.getExtra(ENVIRONMENT_BLOCK_NAME, new Supplier<Map<String, String>>() {
                            @Override
                            public Map<String, String> get() {
                                return new HashMap<>();
                            }
                        }).entrySet());
                    }
                })
                .collect(Collectors.toList());

        return StreamSupport.stream(StreamSupport.stream(launchEnvironments)
                .collect(Collectors.groupingBy(new Function<Map.Entry<String, String>, String>() {
                                                   @Override
                                                   public String apply(Map.Entry<String, String> o) {
                                                       return o.getKey();
                                                   }
                                               },
                        Collectors.mapping(new Function<Map.Entry<String, String>, String>() {
                            @Override
                            public String apply(Map.Entry<String, String> o) {
                                return o.getValue();
                            }
                        }, Collectors.toSet())))
                .entrySet())
                .map(new Function<Map.Entry<String, Set<String>>, EnvironmentItem>() {
                    @Override
                    public EnvironmentItem apply(Map.Entry<String, Set<String>> stringSetEntry) {
                        return aggregateItem(stringSetEntry);
                    }
                })
                .collect(Collectors.toList());
    }

    private static EnvironmentItem aggregateItem(final Map.Entry<String, Set<String>> entry) {
        return new EnvironmentItem()
                .setName(entry.getKey())
                .setValues(new ArrayList<>(entry.getValue()));
    }
}
