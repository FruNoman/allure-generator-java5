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
package com.github.allure.suites;


import io.qameta.allure.Aggregator;
import io.qameta.allure.CommonJsonAggregator;
import io.qameta.allure.CompositeAggregator;
import io.qameta.allure.Constants;
import io.qameta.allure.core.LaunchResults;
import io.qameta.allure.entity.TestResult;
import io.qameta.allure.tree.*;


import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import java8.util.function.Consumer;
import java8.util.function.Function;
import java8.util.function.Predicate;
import java8.util.stream.Collectors;
import java8.util.stream.Stream;
import java8.util.stream.StreamSupport;

import static io.qameta.allure.entity.LabelName.PARENT_SUITE;
import static io.qameta.allure.entity.LabelName.SUB_SUITE;
import static io.qameta.allure.entity.LabelName.SUITE;
import static io.qameta.allure.tree.TreeUtils.calculateStatisticByLeafs;
import static io.qameta.allure.tree.TreeUtils.groupByLabels;

@SuppressWarnings("PMD.UseUtilityClass")
public class SuitesPlugin extends CompositeAggregator {
    private static final String SUITES = "suites";

    protected static final String JSON_FILE_NAME = "suites.json";

    protected static final String CSV_FILE_NAME = "suites.csv";

    public SuitesPlugin() {
        super(Arrays.asList(new JsonAggregator(),new WidgetAggregator()));
    }

    public static  Tree<TestResult> getData(final List<LaunchResults> launchResults) {
        final Tree<TestResult> xunit = new TestResultTree(SUITES, new TreeClassifier<TestResult>() {
            @Override
            public List<TreeLayer> classify(TestResult item) {
                return groupByLabels(item, PARENT_SUITE, SUITE, SUB_SUITE);
            }
        });

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
                .sorted(new Comparator<TestResult>() {
                    @Override
                    public int compare(TestResult o1, TestResult o2) {
                        return (int) (o1.getTime().getStart() - o2.getTime().getStart());
                    }
                })
                .forEach(new Consumer<TestResult>() {
                    @Override
                    public void accept(TestResult testResult) {
                        xunit.add(testResult);
                    }
                });
        return xunit;
    }


    private static class JsonAggregator extends CommonJsonAggregator {

        JsonAggregator() {
            super(JSON_FILE_NAME);
        }

        @Override
        protected Tree<TestResult> getData(final List<LaunchResults> launches) {
            return SuitesPlugin.getData(launches);
        }
    }

    private static class WidgetAggregator extends CommonJsonAggregator {

        WidgetAggregator() {
            super(Constants.WIDGETS_DIR, JSON_FILE_NAME);
        }

    protected Object getData(final List<LaunchResults> launches) {
        final Tree<TestResult> data = SuitesPlugin.getData(launches);
        final List<TreeWidgetItem> items = StreamSupport.stream(data.getChildren())
                .filter(new Predicate<TreeNode>() {
                    @Override
                    public boolean test(TreeNode treeNode) {
                        return TestResultTreeGroup.class.isInstance(treeNode);
                    }
                })
                .map(new Function<TreeNode, TestResultTreeGroup>() {
                    @Override
                    public TestResultTreeGroup apply(TreeNode treeNode) {
                        return TestResultTreeGroup.class.cast(treeNode);
                    }
                })
                .map(new Function<TestResultTreeGroup, TreeWidgetItem>() {
                    @Override
                    public TreeWidgetItem apply(TestResultTreeGroup testResultTreeGroup) {
                        return toWidgetItem(testResultTreeGroup);
                    }
                })
                .sorted(new Comparator<TreeWidgetItem>() {
                    @Override
                    public int compare(TreeWidgetItem o1, TreeWidgetItem o2) {
                        return (int) (o1.getStatistic().getTotal()-o2.getStatistic().getTotal());
                    }
                })
                .limit(10)
                .collect(Collectors.toList());
        return new TreeWidgetData().setItems(items).setTotal(data.getChildren().size());
    }

    private static TreeWidgetItem toWidgetItem(final TestResultTreeGroup group) {
        return new TreeWidgetItem()
                .setUid(group.getUid())
                .setName(group.getName())
                .setStatistic(calculateStatisticByLeafs(group));
    }
}
}
