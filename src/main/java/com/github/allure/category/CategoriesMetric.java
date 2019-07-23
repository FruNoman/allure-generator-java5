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
package com.github.allure.category;

import com.github.allure.utils.AllureUtilsAdv;
import io.qameta.allure.entity.TestResult;
import io.qameta.allure.metric.Metric;
import io.qameta.allure.metric.MetricLine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import java8.util.function.BiFunction;
import java8.util.function.Consumer;
import java8.util.function.Function;
import java8.util.stream.Collectors;
import java8.util.stream.StreamSupport;

/**
 * @author charlie (Dmitry Baev).
 */
public class CategoriesMetric implements Metric {

    private final Map<String, AtomicLong> values = new HashMap<>();

    private final BiFunction<String, Long, MetricLine> lineFactory;

    public CategoriesMetric(final BiFunction<String, Long, MetricLine> lineFactory) {
        this.lineFactory = lineFactory;
    }

    @Override
    public void update(final TestResult testResult) {
        if (testResult.isRetry()) {
            return;
        }
        StreamSupport.stream(testResult.<List<Category>>getExtraBlock("categories", new ArrayList<>()))
                .map(new Function<Category, String>() {
                    @Override
                    public String apply(Category category) {
                        return category.getName();
                    }
                })
                .forEach(new Consumer<String>() {
                    @Override
                    public void accept(String str) {
                        new AllureUtilsAdv<String, AtomicLong>().computeIfAbsent(values, str, new Function<String, AtomicLong>() {
                            @Override
                            public AtomicLong apply(String s) {
                                return new AtomicLong();
                            }
                        }).incrementAndGet();
                    }
                });
    }

    @Override
    public List<MetricLine> getLines() {
        return StreamSupport.stream(values.entrySet())
                .map(new Function<Map.Entry<String, AtomicLong>, MetricLine>() {
                    @Override
                    public MetricLine apply(Map.Entry<String, AtomicLong> stringAtomicLongEntry) {
                        return lineFactory.apply(stringAtomicLongEntry.getKey(), stringAtomicLongEntry.getValue().longValue());
                    }
                })
                .collect(Collectors.toList());
    }
}
