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
package com.github.allure.retry;

import io.qameta.allure.Aggregator;
import io.qameta.allure.core.Configuration;
import io.qameta.allure.core.LaunchResults;
import io.qameta.allure.entity.Status;
import io.qameta.allure.entity.TestResult;

import java.io.File;
import java.util.*;
import java.util.function.BiConsumer;

import java8.util.*;
import java8.util.Objects;
import java8.util.Optional;
import java8.util.function.BinaryOperator;
import java8.util.function.Consumer;
import java8.util.function.Function;
import java8.util.function.Predicate;
import java8.util.stream.Collectors;
import java8.util.stream.Stream;
import java8.util.stream.StreamSupport;

import static io.qameta.allure.entity.TestResult.comparingByTimeAsc;

/**
 * The plugin that process test retries.
 *
 * @since 2.0
 */
public class RetryPlugin implements Aggregator {

    public static final String RETRY_BLOCK_NAME = "retries";

    @SuppressWarnings({"PMD.AvoidLiteralsInIfCondition", "PMD.AvoidInstantiatingObjectsInLoops"})
    @Override
    public void aggregate(final Configuration configuration,
                          final List<LaunchResults> launchesResults,
                          final String outputDirectory) {

        final Map<String, List<TestResult>> byHistory = StreamSupport.stream(launchesResults)
                .flatMap(new Function<LaunchResults, Stream<TestResult>>() {
                    @Override
                    public Stream<TestResult> apply(LaunchResults launchResults) {
                        return StreamSupport.stream(launchResults.getAllResults());
                    }
                })
                .filter(new Predicate<TestResult>() {
                    @Override
                    public boolean test(TestResult testResult) {
                        return Objects.nonNull(testResult.getHistoryId());
                    }
                })
                .collect(Collectors.toMap(new Function<TestResult, String>() {
                    @Override
                    public String apply(TestResult testResult) {
                        return testResult.getHistoryId();
                    }
                }, new Function<TestResult, List<TestResult>>() {
                    @Override
                    public List<TestResult> apply(TestResult testResult) {
                        return Arrays.asList(testResult);
                    }
                }, new BinaryOperator<List<TestResult>>() {
                    @Override
                    public List<TestResult> apply(List<TestResult> testResults, List<TestResult> testResults2) {
                        return merge(testResults, testResults2);
                    }
                }));

        StreamSupport.stream(byHistory.entrySet()).forEach(new Consumer<Map.Entry<String, List<TestResult>>>() {
            @Override
            public void accept(Map.Entry<String, List<TestResult>> testResults) {
                findLatest(testResults.getValue()).ifPresent(addRetries(testResults.getValue()));
            }
        });
    }

    private Consumer<TestResult> addRetries(final List<TestResult> results) {
        return new Consumer<TestResult>() {
            @Override
            public void accept(TestResult latest) {
                final List<RetryItem> retries = StreamSupport.stream(results)
                        .sorted(Comparators.reversed(comparingByTimeAsc()))
                        .filter(new Predicate<TestResult>() {
                            @Override
                            public boolean test(TestResult result) {
                                return !latest.equals(result);
                            }
                        })
                        .map(new Function<TestResult, TestResult>() {
                            @Override
                            public TestResult apply(TestResult testResult) {
                                return prepareRetry(testResult);
                            }
                        })
                        .map(new Function<TestResult, RetryItem>() {
                            @Override
                            public RetryItem apply(TestResult testResult) {
                                return createRetryItem(testResult);
                            }
                        })
                        .collect(Collectors.toList());
                latest.addExtraBlock(RETRY_BLOCK_NAME, retries);
                final Set<Status> statuses = StreamSupport.stream(retries)
                        .map(new Function<RetryItem, Status>() {
                            @Override
                            public Status apply(RetryItem retryItem) {
                                return retryItem.getStatus();
                            }
                        })
                        .distinct()
                        .collect(Collectors.toSet());

                statuses.remove(Status.PASSED);
                statuses.remove(Status.SKIPPED);

                latest.setFlaky(!statuses.isEmpty());

            }
        };
    }

    private TestResult prepareRetry(final TestResult result) {
        result.setHidden(true);
        result.setRetry(true);
        return result;
    }

    private RetryItem createRetryItem(final TestResult result) {
        return new RetryItem()
                .setStatus(result.getStatus())
                .setStatusDetails(result.getStatusMessage())
                .setTime(result.getTime())
                .setUid(result.getUid());
    }

    private Optional<TestResult> findLatest(final List<TestResult> results) {
        return StreamSupport.stream(results)
                .filter(new Predicate<TestResult>() {
                    @Override
                    public boolean test(TestResult testResult) {
                        return !testResult.isHidden();
                    }
                })
                .min(Comparators.reversed(comparingByTimeAsc()));
    }

    private List<TestResult> merge(final List<TestResult> first,
                                   final List<TestResult> second) {
        final List<TestResult> merged = new ArrayList<>();
        merged.addAll(first);
        merged.addAll(second);
        return merged;
    }
}
