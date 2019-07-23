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
package com.github.allure.history;

import com.fasterxml.jackson.core.type.TypeReference;


import io.qameta.allure.Aggregator;
import io.qameta.allure.Reader;
import io.qameta.allure.context.JacksonContext;
import io.qameta.allure.core.Configuration;
import io.qameta.allure.core.LaunchResults;
import io.qameta.allure.core.ResultsVisitor;
import io.qameta.allure.entity.ExecutorInfo;
import io.qameta.allure.entity.Statistic;
import io.qameta.allure.entity.Status;
import io.qameta.allure.entity.TestResult;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import java8.util.Objects;
import java8.util.Optional;
import java8.util.function.BinaryOperator;
import java8.util.function.Consumer;
import java8.util.function.Function;
import java8.util.function.Predicate;
import java8.util.function.Supplier;
import java8.util.stream.Collectors;
import java8.util.stream.RefStreams;
import java8.util.stream.Stream;
import java8.util.stream.StreamSupport;

import static io.qameta.allure.Constants.HISTORY_DIR;


/**
 * Plugin that adds history to the report.
 *
 * @since 2.0
 */
public class HistoryPlugin implements Reader, Aggregator {

    public static final String EXECUTORS_BLOCK_NAME = "executor";
    private static final String HISTORY_BLOCK_NAME = "history";

    private static final String HISTORY_FILE_NAME = "history.json";

    //@formatter:off
    private static final TypeReference<Map<String, HistoryData>> HISTORY_TYPE =
            new TypeReference<Map<String, HistoryData>>() {
            };
    //@formatter:on

    private boolean isNewFailed(final List<HistoryItem> histories) {
        final List<Status> statuses = StreamSupport.stream(histories)
//                .sorted(comparingByTime())
                .map(new Function<HistoryItem, Status>() {
                    @Override
                    public Status apply(HistoryItem historyItem) {
                        return historyItem.getStatus();
                    }
                })
                .collect(Collectors.toList());
        return statuses.size() > 1
                && statuses.get(0) == Status.FAILED
                && statuses.get(1) == Status.PASSED;
    }

    private boolean isFlaky(final List<HistoryItem> histories) {
        if (histories.size() > 1 && histories.get(0).status == Status.FAILED) {
            final List<Status> statuses = StreamSupport.stream(histories.subList(1, histories.size()))
//                    .sorted(comparingByTime())
                    .map(new Function<HistoryItem, Status>() {
                        @Override
                        public Status apply(HistoryItem historyItem) {
                            return historyItem.getStatus();
                        }
                    })
                    .collect(Collectors.toList());
            return statuses.indexOf(Status.PASSED) < statuses.lastIndexOf(Status.FAILED)
                    && statuses.indexOf(Status.PASSED) != -1;
        }
        return false;
    }

    public Map<String, HistoryData> getData(final List<LaunchResults> launches) {
        return StreamSupport.stream(launches)
                .map(new Function<LaunchResults, Map<String, HistoryData>>() {
                    @Override
                    public Map<String, HistoryData> apply(LaunchResults launchResults) {
                        final ExecutorInfo executorInfo = launchResults.getExtra(
                                EXECUTORS_BLOCK_NAME,
                                new Supplier<ExecutorInfo>() {
                                    @Override
                                    public ExecutorInfo get() {
                                        return new ExecutorInfo();
                                    }
                                }
                        );


                        final Map<String, HistoryData> history = launchResults.getExtra(HISTORY_BLOCK_NAME, new Supplier<Map<String, HistoryData>>() {
                            @Override
                            public Map<String, HistoryData> get() {
                                return new HashMap<String, HistoryData>();
                            }
                        });
                        StreamSupport.stream(launchResults.getAllResults())
                                .filter(new Predicate<TestResult>() {
                                    @Override
                                    public boolean test(TestResult testResult) {
                                        return Objects.nonNull(testResult.getHistoryId());
                                    }
                                })
                                .forEach(new Consumer<TestResult>() {
                                    @Override
                                    public void accept(TestResult testResult) {
                                        updateHistory(history, testResult, executorInfo);
                                    }
                                });
                        return history;
                    }
                }).reduce(new HashMap<String, HistoryData>(), new BinaryOperator<Map<String, HistoryData>>() {
                    @Override
                    public Map<String, HistoryData> apply(Map<String, HistoryData> o, Map<String, HistoryData> o2) {
                        o.putAll(o2);
                        return o;
                    }
                });
    }

    private void updateHistory(final Map<String, HistoryData> history,
                               final TestResult result,
                               final ExecutorInfo info) {

        HistoryData data = history.get(result.getHistoryId());
        if (data == null) {
            data = new HistoryData().setStatistic(new Statistic());
            history.put(result.getHistoryId(), data);
        }
        data.getStatistic().update(result);
        if (!data.getItems().isEmpty()) {
            result.addExtraBlock(HISTORY_BLOCK_NAME, copy(data));
        }
        final HistoryItem newItem = new HistoryItem()
                .setUid(result.getUid())
                .setStatus(result.getStatus())
                .setStatusDetails(result.getStatusMessage())
                .setTime(result.getTime());

        if (Objects.nonNull(info.getReportUrl())) {
            newItem.setReportUrl(createReportUrl(info.getReportUrl(), result.getUid()));
        }

        final List<HistoryItem> newItems = RefStreams.concat(RefStreams.of(newItem), StreamSupport.stream(data.getItems()))
                .limit(5)
                .collect(Collectors.toList());
        result.setNewFailed(isNewFailed(newItems));
        result.setFlaky(isFlaky(newItems));
        data.setItems(newItems);
    }

    private static HistoryData copy(final HistoryData other) {
        final Statistic statistic = new Statistic();
        statistic.merge(other.getStatistic());
        final List<HistoryItem> items = new ArrayList<>(other.getItems());
        return new HistoryData()
                .setStatistic(statistic)
                .setItems(items);
    }

    private static String createReportUrl(final String reportUrl, final String uuid) {
        final String pattern = reportUrl.endsWith("index.html") ? "%s#testresult/%s" : "%s/#testresult/%s";
        return String.format(pattern, reportUrl, uuid);
    }


    @Override
    public void readResults(Configuration configuration, ResultsVisitor visitor, List<File> fileList) {
        final JacksonContext context = configuration.getContext(JacksonContext.class);
        Optional<File> historyFile = StreamSupport.stream(fileList).filter(new Predicate<File>() {
            @Override
            public boolean test(File file) {
                return file.getName().equals(HISTORY_BLOCK_NAME);
            }
        }).findFirst();
        if (historyFile.isPresent()) {
            Optional<File> file = StreamSupport.stream(Arrays.asList(historyFile.get().listFiles())).filter(new Predicate<File>() {
                @Override
                public boolean test(File file) {
                    return file.getName().equals(HISTORY_FILE_NAME);
                }
            }).findFirst();
            if (file.isPresent()) {
                if (file.get().exists()) {
                    try (InputStream is = new FileInputStream(file.get())) {
                        final Map<String, HistoryData> history = context.getValue().readValue(is, HISTORY_TYPE);
                        visitor.visitExtra(HISTORY_BLOCK_NAME, history);
                    } catch (IOException e) {
                        visitor.error("Could not read history file " + file.get(), e);
                    }
                }
            }
        }
    }

    @Override
    public void aggregate(Configuration configuration, List<LaunchResults> launchesResults, String outputDirectory) throws IOException {
        final JacksonContext context = configuration.getContext(JacksonContext.class);
        final File historyFile = new File(outputDirectory + File.separator + HISTORY_BLOCK_NAME + File.separator + HISTORY_FILE_NAME);
        historyFile.getParentFile().mkdirs();
        if (!historyFile.exists()) {
            historyFile.createNewFile();
        }
        try (OutputStream os = new FileOutputStream(historyFile)) {
            context.getValue().writeValue(os, getData(launchesResults));
        }
    }
}
