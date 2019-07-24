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
package com.github.allure.tags;

import io.qameta.allure.Aggregator;
import io.qameta.allure.core.Configuration;
import io.qameta.allure.core.LaunchResults;
import io.qameta.allure.entity.LabelName;
import io.qameta.allure.entity.TestResult;
import java8.util.function.Consumer;
import java8.util.function.Function;
import java8.util.stream.Stream;
import java8.util.stream.StreamSupport;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author charlie (Dmitry Baev).
 */
public class TagsPlugin implements Aggregator {

    public static final String TAGS_BLOCK_NAME = "tags";

    @Override
    public void aggregate(final Configuration configuration,
                          final List<LaunchResults> launchesResults,
                          final String outputDirectory) {
        StreamSupport.stream(launchesResults)
                .map(new Function<LaunchResults, Set<TestResult>>() {
                    @Override
                    public Set<TestResult> apply(LaunchResults launchResults) {
                        return launchResults.getAllResults();
                    }
                })
                .flatMap(new Function<Set<TestResult>, Stream<TestResult>>() {
                    @Override
                    public Stream<TestResult> apply(Set<TestResult> testResults) {
                        return StreamSupport.stream(testResults);
                    }
                })
                .forEach(new Consumer<TestResult>() {
                    @Override
                    public void accept(TestResult testResult) {
                        final Set<String> tags = new HashSet<>(testResult.findAllLabels(LabelName.TAG));
                        testResult.addExtraBlock(TAGS_BLOCK_NAME, tags);
                    }
                });
    }
}
