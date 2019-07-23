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
package com.github.allure.severity;

import io.qameta.allure.Aggregator;
import io.qameta.allure.CommonJsonAggregator;
import io.qameta.allure.CompositeAggregator;
import io.qameta.allure.core.Configuration;
import io.qameta.allure.core.LaunchResults;
import io.qameta.allure.entity.TestResult;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import java8.util.Optional;
import java8.util.function.Consumer;
import java8.util.function.Function;
import java8.util.stream.Collectors;
import java8.util.stream.Stream;
import java8.util.stream.StreamSupport;

import static io.qameta.allure.entity.LabelName.SEVERITY;

/**
 * Plugin that adds severity information to tests results.
 *
 * @since 2.0
 */
public class SeverityPlugin extends CompositeAggregator {

    public static final String SEVERITY_BLOCK_NAME = "severity";

    protected static final String JSON_FILE_NAME = "severity.json";

    public SeverityPlugin() {
        super(Arrays.asList(
                new SeverityAggregator(), new WidgetAggregator()
        ));
    }

    private static class SeverityAggregator implements Aggregator {

        private void setSeverityLevel(final TestResult result) {
            final SeverityLevel severityLevel = result.findOneLabel(SEVERITY)
                    .flatMap(new Function<String, Optional<SeverityLevel>>() {
                        @Override
                        public Optional<SeverityLevel> apply(String s) {
                            return SeverityLevel.fromValue(s);
                        }
                    })
                    .orElse(SeverityLevel.NORMAL);
            result.addExtraBlock(SEVERITY_BLOCK_NAME, severityLevel);
        }

        @Override
        public void aggregate(Configuration configuration, List<LaunchResults> list, String s) throws IOException {
            StreamSupport.stream(list)
                    .flatMap(new Function<LaunchResults, Stream<TestResult>>() {
                        @Override
                        public Stream<TestResult> apply(LaunchResults launchResults) {
                            return StreamSupport.stream(launchResults.getResults());
                        }
                    })
                    .forEach(new Consumer<TestResult>() {
                        @Override
                        public void accept(TestResult testResult) {
                            setSeverityLevel(testResult);
                        }
                    });
        }
    }

    public static class WidgetAggregator extends CommonJsonAggregator {
        public WidgetAggregator() {
            super("widgets", JSON_FILE_NAME);
        }

        public List<SeverityData> getData(final List<LaunchResults> launchesResults) {
            return StreamSupport.stream(launchesResults)
                    .flatMap(new Function<LaunchResults, Stream<TestResult>>() {
                        @Override
                        public Stream<TestResult> apply(LaunchResults launchResults) {
                            return StreamSupport.stream(launchResults.getResults());
                        }
                    })
                    .map(new Function<TestResult, SeverityData>() {
                        @Override
                        public SeverityData apply(TestResult testResult) {
                            return createData(testResult);
                        }
                    })
                    .collect(Collectors.toList());
        }

        private SeverityData createData(final TestResult result) {
            return new SeverityData()
                    .setUid(result.getUid())
                    .setName(result.getName())
                    .setStatus(result.getStatus())
                    .setTime(result.getTime())
                    .setSeverity(result.getExtraBlock(SEVERITY_BLOCK_NAME));
        }
    }
}
