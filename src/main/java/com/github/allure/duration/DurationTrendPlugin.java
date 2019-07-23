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
package com.github.allure.duration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.allure.trend.AbstractTrendPlugin;
import io.qameta.allure.CommonJsonAggregator;
import io.qameta.allure.Constants;
import io.qameta.allure.core.LaunchResults;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.qameta.allure.entity.ExecutorInfo;
import io.qameta.allure.entity.TestResult;
import java8.util.Optional;
import java8.util.function.BinaryOperator;
import java8.util.function.Consumer;
import java8.util.function.Function;
import java8.util.function.Supplier;
import java8.util.stream.Collectors;
import java8.util.stream.RefStreams;
import java8.util.stream.Stream;
import java8.util.stream.StreamSupport;

/**
 * Plugin that generates data for Duration-Trend graph.
 */
public class DurationTrendPlugin extends AbstractTrendPlugin<DurationTrendItem> {

    protected static final String JSON_FILE_NAME = "duration-trend.json";

    private static final String DURATION_TREND_BLOCK_NAME = "duration-trend";

    public DurationTrendPlugin() {
        super(Arrays.asList(new JsonAggregator(), new WidgetAggregator()), JSON_FILE_NAME, DURATION_TREND_BLOCK_NAME);
    }

    @Override
    protected Optional<DurationTrendItem> parseItem(final ObjectMapper mapper, final JsonNode child)
            throws JsonProcessingException {
        return Optional.ofNullable(mapper.treeToValue(child, DurationTrendItem.class));
    }

    @SuppressWarnings("PMD.DefaultPackage")
    /* default */ static List<DurationTrendItem> getData(final List<LaunchResults> launchesResults) {
        final DurationTrendItem item = new DurationTrendPlugin().createCurrent(launchesResults);
        final List<DurationTrendItem> data = getHistoryItems(launchesResults);

        return RefStreams.concat(RefStreams.of(item), StreamSupport.stream(data))
                .limit(20)
                .collect(Collectors.toList());
    }

    private static List<DurationTrendItem> getHistoryItems(final List<LaunchResults> launchesResults) {
        return StreamSupport.stream(launchesResults)
                .map(new Function<LaunchResults, List<DurationTrendItem>>() {
                    @Override
                    public List<DurationTrendItem> apply(LaunchResults launchResults) {
                        return getPreviousTrendData(launchResults);
                    }
                })
                .reduce(new ArrayList<>(), new BinaryOperator<List<DurationTrendItem>>() {
                    @Override
                    public List<DurationTrendItem> apply(List<DurationTrendItem> durationTrendItems, List<DurationTrendItem> durationTrendItems2) {
                        durationTrendItems.addAll(durationTrendItems2);
                        return durationTrendItems;
                    }
                });
    }

    private static List<DurationTrendItem> getPreviousTrendData(final LaunchResults results) {
        return results.getExtra(DURATION_TREND_BLOCK_NAME, new Supplier<List<DurationTrendItem>>() {
            @Override
            public List<DurationTrendItem> get() {
                return new ArrayList<>();
            }
        });
    }

    private DurationTrendItem createCurrent(final List<LaunchResults> launchesResults) {
        final DurationTrendItem item = new DurationTrendItem();
        extractLatestExecutor(launchesResults).ifPresent(new Consumer<ExecutorInfo>() {
            @Override
            public void accept(ExecutorInfo executorInfo) {
                item.setBuildOrder(executorInfo.getBuildOrder());
                item.setReportName(executorInfo.getReportName());
                item.setReportUrl(executorInfo.getReportUrl());
            }
        });
        StreamSupport.stream(launchesResults)
                .flatMap(new Function<LaunchResults, Stream<TestResult>>() {
                    @Override
                    public Stream<TestResult> apply(LaunchResults launchResults) {
                        return StreamSupport.stream(launchResults.getResults());
                    }
                })
                .forEach(new Consumer<TestResult>() {
                    @Override
                    public void accept(TestResult testResult) {
                        item.updateTime(testResult);
                    }
                });
        return item;
    }

    /**
     * Generates tree data.
     */
    private static class JsonAggregator extends CommonJsonAggregator {

        JsonAggregator() {
            super(Constants.HISTORY_DIR, JSON_FILE_NAME);
        }

        @Override
        protected List<DurationTrendItem> getData(final List<LaunchResults> launches) {
            return DurationTrendPlugin.getData(launches);
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
        public List<DurationTrendItem> getData(final List<LaunchResults> launches) {
            return DurationTrendPlugin.getData(launches);
        }
    }
}
