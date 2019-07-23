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
package com.github.allure.core;

import io.qameta.allure.Aggregator;
import io.qameta.allure.context.MarkdownContext;
import io.qameta.allure.core.Configuration;
import io.qameta.allure.core.LaunchResults;

import java.util.List;

import io.qameta.allure.entity.TestResult;
import java8.util.function.Function;
import java8.util.function.Predicate;
import java8.util.stream.Stream;
import java8.util.stream.StreamSupport;

import java8.util.Objects;
import java8.util.Optional;
import java8.util.function.Consumer;

/**
 * Plugin that converts descriptions from markdown to html.
 *
 * @since 2.0
 */
public class MarkdownDescriptionsPlugin implements Aggregator {

    @Override
    public void aggregate(final Configuration configuration,
                          final List<LaunchResults> launchesResults,
                          final String outputDirectory) {
        Optional.of(configuration.getContext(MarkdownContext.class))
                .ifPresent(new Consumer<MarkdownContext>() {
                    @Override
                    public void accept(MarkdownContext markdownContext) {
                        processDescriptions(launchesResults, markdownContext);
                    }
                });
    }

    private void processDescriptions(final List<LaunchResults> launches, final MarkdownContext context) {
        StreamSupport.stream(launches)
                .flatMap(new Function<LaunchResults, Stream<TestResult>>() {
                    @Override
                    public Stream<TestResult> apply(LaunchResults launchResults) {
                        return StreamSupport.stream(launchResults.getResults());
                    }
                })
                .filter(new Predicate<TestResult>() {
                    @Override
                    public boolean test(TestResult testResult) {
                        return  isEmpty(testResult.getDescriptionHtml()) && !isEmpty(testResult.getDescription());
                    }
                })
                .forEach(new Consumer<TestResult>() {
                    @Override
                    public void accept(TestResult testResult) {
                        final String html = context.getValue().apply(testResult.getDescription());
                        testResult.setDescriptionHtml(html);
                    }
                });
    }

    private static boolean isEmpty(final String string) {
        return Objects.isNull(string) || string.isEmpty();
    }

}
