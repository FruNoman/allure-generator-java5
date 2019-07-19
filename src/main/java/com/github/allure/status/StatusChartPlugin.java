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
package com.github.allure.status;

import com.github.allure.severity.SeverityPlugin;

import io.qameta.allure.CommonJsonAggregator;
import io.qameta.allure.Constants;
import io.qameta.allure.core.LaunchResults;
import io.qameta.allure.entity.TestResult;

import java.util.List;

import java8.util.function.Function;
import java8.util.stream.Collectors;
import java8.util.stream.Stream;
import java8.util.stream.StreamSupport;

public class StatusChartPlugin extends CommonJsonAggregator {

    public StatusChartPlugin() {
        super(Constants.WIDGETS_DIR, "status-chart.json");
    }

    public List<StatusChartData> getData(final List<LaunchResults> launchesResults) {
        return StreamSupport.stream(launchesResults)
                .flatMap(new Function<LaunchResults, Stream<TestResult>>() {
                    @Override
                    public Stream<TestResult> apply(LaunchResults launchResults) {
                        return StreamSupport.stream(launchResults.getAllResults());
                    }
                })
                .map(new Function<TestResult, StatusChartData>() {
                    @Override
                    public StatusChartData apply(TestResult testResult) {
                        return createData(testResult);
                    }
                })
                .collect(Collectors.toList());
    }

    public StatusChartData createData(final TestResult result) {
        return new StatusChartData()
                .setUid(result.getUid())
                .setName(result.getName())
                .setStatus(result.getStatus())
                .setTime(result.getTime())
                .setSeverity(result.getExtraBlock(SeverityPlugin.SEVERITY_BLOCK_NAME));
    }
}
