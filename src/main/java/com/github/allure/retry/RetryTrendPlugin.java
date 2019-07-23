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
 * Plugin that generates data for Retry-Trend graph.
 */
public class RetryTrendPlugin extends AbstractTrendPlugin<RetryTrendItem> {

    private static final String JSON_FILE_NAME = "retry-trend.json";

    public static final String RETRY_TREND_BLOCK_NAME = "retry-trend";

    public RetryTrendPlugin() {
        super(Arrays.asList(new JsonAggregator(), new WidgetAggregator()), JSON_FILE_NAME, RETRY_TREND_BLOCK_NAME);
    }

    @Override
    protected Optional<RetryTrendItem> parseItem(final ObjectMapper mapper, final JsonNode child)
            throws JsonProcessingException {
        return Optional.ofNullable(mapper.treeToValue(child, RetryTrendItem.class));
    }

    @SuppressWarnings("PMD.DefaultPackage")
    /* default */ static List<RetryTrendItem> getData(final List<LaunchResults> launchesResults) {
        final RetryTrendItem item = new RetryTrendPlugin().createCurrent(launchesResults);
        final List<RetryTrendItem> data = getHistoryItems(launchesResults);

        return RefStreams.concat(RefStreams.of(item), StreamSupport.stream(data))
                .limit(20)
                .collect(Collectors.toList());
    }

    private static List<RetryTrendItem> getHistoryItems(final List<LaunchResults> launchesResults) {
        return StreamSupport.stream(launchesResults)
                .map(new Function<LaunchResults, List<RetryTrendItem>>() {
                    @Override
                    public List<RetryTrendItem> apply(LaunchResults launchResults) {
                        return getPreviousTrendData(launchResults);
                    }
                })
                .reduce(new ArrayList<>(), new BinaryOperator<List<RetryTrendItem>>() {
                    @Override
                    public List<RetryTrendItem> apply(List<RetryTrendItem> o, List<RetryTrendItem> o2) {
                        o.addAll(o2);
                        return o;
                    }
                });
    }

    private static List<RetryTrendItem> getPreviousTrendData(final LaunchResults results) {
        return results.getExtra(RETRY_TREND_BLOCK_NAME, new Supplier<List<RetryTrendItem>>() {
            @Override
            public List<RetryTrendItem> get() {
                return new ArrayList<>();
            }
        });
    }

    private  RetryTrendItem createCurrent(final List<LaunchResults> launchesResults) {
        final RetryTrendItem item = new RetryTrendItem();
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
                        return StreamSupport.stream(launchResults.getAllResults());
                    }
                })
                .forEach(new Consumer<TestResult>() {
                    @Override
                    public void accept(TestResult testResult) {
                        item.update(testResult);
                    }
                });
        return item;
    }

    /**
     * Generates retries trend data.
     */
    protected static class JsonAggregator extends CommonJsonAggregator {

        JsonAggregator() {
            super(Constants.HISTORY_DIR, JSON_FILE_NAME);
        }

        @Override
        protected List<RetryTrendItem> getData(final List<LaunchResults> launches) {
            return RetryTrendPlugin.getData(launches);
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
        public List<RetryTrendItem> getData(final List<LaunchResults> launches) {
            return RetryTrendPlugin.getData(launches);
        }
    }
}
