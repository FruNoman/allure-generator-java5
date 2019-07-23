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
package com.github.allure.timeline;

import io.qameta.allure.CommonJsonAggregator;
import io.qameta.allure.core.LaunchResults;
import io.qameta.allure.entity.LabelName;
import io.qameta.allure.entity.TestResult;
import io.qameta.allure.tree.TestResultTree;
import io.qameta.allure.tree.Tree;
import io.qameta.allure.tree.TreeClassifier;
import io.qameta.allure.tree.TreeLayer;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import java8.util.function.Consumer;
import java8.util.function.Function;
import java8.util.stream.Stream;
import java8.util.stream.StreamSupport;

import static io.qameta.allure.tree.TreeUtils.groupByLabels;

/**
 * Plugin that generates data for Timeline tab.
 *
 * @since 2.0
 */
public class TimelinePlugin extends CommonJsonAggregator {

    public TimelinePlugin() {
        super("timeline.json");
    }

    @Override
    protected Tree<TestResult> getData(final List<LaunchResults> launchResults) {

        // @formatter:off
        final Tree<TestResult> timeline = new TestResultTree(
                "timeline", new TreeClassifier<TestResult>() {
            @Override
            public List<TreeLayer> classify(TestResult testResult) {
                return groupByLabels(testResult, LabelName.HOST, LabelName.THREAD);
            }
        });
        // @formatter:on

        StreamSupport.stream(launchResults)
                .map(new Function<LaunchResults, Set<TestResult>>() {
                    @Override
                    public Set<TestResult> apply(LaunchResults launchResults) {
                        return launchResults.getAllResults();
                    }
                })
                .flatMap(new Function<Set<TestResult>, Stream<TestResult>>() {
                    @Override
                    public Stream<TestResult> apply(Set<TestResult> testResults) {
                        return StreamSupport.stream(testResults);
                    }
                })
                .forEach(new Consumer<TestResult>() {
                    @Override
                    public void accept(TestResult testResult) {
                        timeline.add(testResult);
                    }
                });
        return timeline;
    }
}
