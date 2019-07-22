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
 * Plugin that generates data for Categories-trend graph.
 */
public class CategoriesTrendPlugin extends AbstractTrendPlugin<CategoriesTrendItem> {

    private static final String JSON_FILE_NAME = "categories-trend.json";

    public static final String CATEGORIES_TREND_BLOCK_NAME = "categories-trend";

    public CategoriesTrendPlugin() {
        super(Arrays.asList(new JsonAggregator(), new WidgetAggregator()), JSON_FILE_NAME, CATEGORIES_TREND_BLOCK_NAME);
    }

    @Override
    protected Optional<CategoriesTrendItem> parseItem(final ObjectMapper mapper, final JsonNode child)
            throws JsonProcessingException {
        return Optional.ofNullable(mapper.treeToValue(child, CategoriesTrendItem.class));
    }

    public  List<CategoriesTrendItem> getData(final List<LaunchResults> launchesResults) {
        final CategoriesTrendItem item = createCurrent(launchesResults);
        final List<CategoriesTrendItem> data = getHistoryItems(launchesResults);

        return RefStreams.concat(RefStreams.of(item), StreamSupport.stream(data))
                .limit(20)
                .collect(Collectors.toList());
    }

    public static List<CategoriesTrendItem> getHistoryItems(final List<LaunchResults> launchesResults) {
        return StreamSupport.stream(launchesResults)
                .map(new Function<LaunchResults, List<CategoriesTrendItem>>() {
                    @Override
                    public List<CategoriesTrendItem> apply(LaunchResults launchResults) {
                        return getPreviousTrendData(launchResults);
                    }
                })
                .reduce(new ArrayList<>(), new BinaryOperator<List<CategoriesTrendItem>>() {
                    @Override
                    public List<CategoriesTrendItem> apply(List<CategoriesTrendItem> categoriesTrendItems, List<CategoriesTrendItem> categoriesTrendItems2) {
                        categoriesTrendItems.addAll(categoriesTrendItems2);
                        return categoriesTrendItems;
                    }
                });
    }

    public static List<CategoriesTrendItem> getPreviousTrendData(final LaunchResults results) {
        return results.getExtra(CATEGORIES_TREND_BLOCK_NAME, new Supplier<List<CategoriesTrendItem>>() {
            @Override
            public List<CategoriesTrendItem> get() {
                return new ArrayList<>();
            }
        });
    }

    private  CategoriesTrendItem createCurrent(final List<LaunchResults> launchesResults) {
        final CategoriesTrendItem item = new CategoriesTrendItem();
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
                        item.increaseCategories(testResult);
                    }
                });
        return item;
    }

    /**
     * Generates history trend data.
     */
    private static class JsonAggregator extends CommonJsonAggregator {

        JsonAggregator() {
            super(Constants.HISTORY_DIR, JSON_FILE_NAME);
        }

        @Override
        protected List<CategoriesTrendItem> getData(final List<LaunchResults> launches) {
            return new CategoriesTrendPlugin().getData(launches);
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
        public List<CategoriesTrendItem> getData(final List<LaunchResults> launches) {
            return new CategoriesTrendPlugin().getData(launches);
        }
    }
}
