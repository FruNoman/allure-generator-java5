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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.allure.trend.AbstractTrendPlugin;
import io.qameta.allure.CommonJsonAggregator;
import io.qameta.allure.Constants;
import io.qameta.allure.core.LaunchResults;
import io.qameta.allure.entity.ExecutorInfo;
import io.qameta.allure.entity.Statistic;
import io.qameta.allure.entity.Status;
import io.qameta.allure.entity.TestResult;

import java8.util.*;
import java8.util.function.*;
import java8.util.stream.Collectors;
import java8.util.stream.RefStreams;
import java8.util.stream.Stream;
import java8.util.stream.StreamSupport;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Plugin that adds history trend widget.
 *
 * @since 2.0
 */
public class HistoryTrendPlugin extends AbstractTrendPlugin<HistoryTrendItem> {

    public static final String JSON_FILE_NAME = "history-trend.json";

    public static final String HISTORY_TREND_BLOCK_NAME = "history-trend";

    public HistoryTrendPlugin() {
        super(Arrays.asList(new JsonAggregator(), new WidgetAggregator()), JSON_FILE_NAME, HISTORY_TREND_BLOCK_NAME);
    }

    @Override
    protected Optional<HistoryTrendItem> parseItem(final ObjectMapper mapper, final JsonNode child)
            throws JsonProcessingException {

        if (Objects.nonNull(child.get("total"))) {
            final Statistic statistic = mapper.treeToValue(child, Statistic.class);
            return Optional.of(new HistoryTrendItem().setStatistic(statistic));
        }
        return Optional.ofNullable(mapper.treeToValue(child, HistoryTrendItem.class));
    }

    @SuppressWarnings("PMD.DefaultPackage")
    /* default */ static List<HistoryTrendItem> getData(final List<LaunchResults> launchesResults) {
        final HistoryTrendItem item = new HistoryTrendPlugin().createCurrent(launchesResults);
        final List<HistoryTrendItem> data = getHistoryItems(launchesResults);

        return RefStreams.concat(RefStreams.of(item), StreamSupport.stream(data))
                .limit(20)
                .collect(Collectors.toList());
    }



    private  HistoryTrendItem createCurrent(final List<LaunchResults> launchesResults) {
        final Statistic statistic = StreamSupport.stream(launchesResults)
                .flatMap(new Function<LaunchResults, Stream<TestResult>>() {
                    @Override
                    public Stream<TestResult> apply(LaunchResults launchResults) {
                        return StreamSupport.stream(launchResults.getResults());
                    }
                })
                .map(new Function<TestResult, Status>() {
                    @Override
                    public Status apply(TestResult testResult) {
                        return testResult.getStatus();
                    }
                })
                .collect(new Supplier<Statistic>() {
                    @Override
                    public Statistic get() {
                        return new Statistic();
                    }
                }, new BiConsumer<Statistic, Status>() {
                    @Override
                    public void accept(Statistic statistic, Status status) {
                        statistic.update(status);
                    }
                }, new BiConsumer<Statistic, Statistic>() {
                    @Override
                    public void accept(Statistic statistic, Statistic statistic2) {
                        statistic.merge(statistic2);
                    }
                });

        final HistoryTrendItem item = new HistoryTrendItem()
                .setStatistic(statistic);

        extractLatestExecutor(launchesResults).ifPresent(new Consumer<ExecutorInfo>() {
            @Override
            public void accept(ExecutorInfo executorInfo) {
                item.setBuildOrder(executorInfo.getBuildOrder());
                item.setReportName(executorInfo.getReportName());
                item.setReportUrl(executorInfo.getReportUrl());
            }
        });
        return item;
    }

    private static List<HistoryTrendItem> getHistoryItems(final List<LaunchResults> launchesResults) {
        return StreamSupport.stream(launchesResults)
                .map(new Function<LaunchResults, List<HistoryTrendItem>>() {
                    @Override
                    public List<HistoryTrendItem> apply(LaunchResults launchResults) {
                        return getPreviousTrendData(launchResults);
                    }
                })
                .reduce(new ArrayList<>(), new BinaryOperator<List<HistoryTrendItem>>() {
                    @Override
                    public List<HistoryTrendItem> apply(List<HistoryTrendItem> historyTrendItems, List<HistoryTrendItem> historyTrendItems2) {
                        historyTrendItems.addAll(historyTrendItems2);
                        return historyTrendItems;
                    }
                });
    }

    private static List<HistoryTrendItem> getPreviousTrendData(final LaunchResults results) {
        return results.getExtra(HISTORY_TREND_BLOCK_NAME, new Supplier<List<HistoryTrendItem>>() {
            @Override
            public List<HistoryTrendItem> get() {
                return new ArrayList<>();
            }
        });
    }

    /**
     * Generates history trend data.
     */
    protected static class JsonAggregator extends CommonJsonAggregator {

        JsonAggregator() {
            super(Constants.HISTORY_DIR, JSON_FILE_NAME);
        }

        @Override
        protected List<HistoryTrendItem> getData(final List<LaunchResults> launches) {
            return HistoryTrendPlugin.getData(launches);
        }
    }

    /**
     * Generates widget data.
     */
    private static class WidgetAggregator extends CommonJsonAggregator {

        WidgetAggregator() {
            super(Constants.WIDGETS_DIR, JSON_FILE_NAME);
        }

        @Override
        public List<HistoryTrendItem> getData(final List<LaunchResults> launches) {
            return HistoryTrendPlugin.getData(launches);
        }
    }


}
