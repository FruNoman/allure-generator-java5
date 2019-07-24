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
package com.github.allure.owner;

import io.qameta.allure.Aggregator;
import io.qameta.allure.core.Configuration;
import io.qameta.allure.core.LaunchResults;
import io.qameta.allure.entity.LabelName;
import io.qameta.allure.entity.TestResult;

import java.util.List;

import java8.util.function.Consumer;
import java8.util.function.Function;
import java8.util.stream.Stream;
import java8.util.stream.StreamSupport;

public class OwnerPlugin implements Aggregator {

    public static final String OWNER_BLOCK_NAME = "owner";

    @Override
    public void aggregate(final Configuration configuration,
                          final List<LaunchResults> launchesResults,
                          final String outputDirectory) {
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
                        setOwner(testResult);
                    }
                });
    }

    private void setOwner(final TestResult result) {
        result.findOneLabel(LabelName.OWNER)
                .ifPresent(new Consumer<String>() {
                    @Override
                    public void accept(String s) {
                        result.addExtraBlock(OWNER_BLOCK_NAME, s);
                    }
                });
    }
}
