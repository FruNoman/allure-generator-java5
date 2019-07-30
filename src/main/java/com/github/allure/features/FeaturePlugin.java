package com.github.allure.features;

import com.github.allure.category.CategoriesPlugin;
import io.qameta.allure.Aggregator;
import io.qameta.allure.CompositeAggregator;
import io.qameta.allure.Reader;
import io.qameta.allure.core.Configuration;
import io.qameta.allure.core.LaunchResults;
import io.qameta.allure.core.ResultsVisitor;
import io.qameta.allure.entity.LabelName;
import io.qameta.allure.entity.Statistic;
import io.qameta.allure.entity.TestResult;
import io.qameta.allure.tree.*;
import java8.util.Comparators;
import java8.util.function.Consumer;
import java8.util.function.Function;
import java8.util.function.Predicate;
import java8.util.stream.Collectors;
import java8.util.stream.Stream;
import java8.util.stream.StreamSupport;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static io.qameta.allure.entity.Statistic.comparator;
import static io.qameta.allure.tree.TreeUtils.calculateStatisticByLeafs;
import static io.qameta.allure.tree.TreeUtils.groupByLabels;

public class FeaturePlugin  {

    public Tree<TestResult> getData(final List<LaunchResults> launchResults) {

        final Tree<TestResult> features = new TestResultTree(
                "features", new TreeClassifier<TestResult>() {
            @Override
            public List<TreeLayer> classify(TestResult testResult) {
                return groupByLabels(testResult, LabelName.FEATURE);
            }
        });
        StreamSupport.stream(launchResults)
                .map(new Function<LaunchResults, Set<TestResult>>() {
                    @Override
                    public Set<TestResult> apply(LaunchResults launchResults) {
                        return launchResults.getResults();
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
                        features.add(testResult);
                    }
                });
        return features;
    }


    public List<TreeWidgetItem> getTreeWidgetItemData(final List<LaunchResults> launchResults){
        final Tree<TestResult> data = getData(launchResults);
        final List<TreeWidgetItem> items = StreamSupport.stream(data.getChildren())
                .filter(new Predicate<TreeNode>() {
                    @Override
                    public boolean test(TreeNode treeNode) {
                        return (TestResultTreeGroup.class.isInstance(treeNode));
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
                .sorted(Comparators.reversed(Comparators.comparing(new Function<TreeWidgetItem, Statistic>() {
                    @Override
                    public Statistic apply(TreeWidgetItem treeWidgetItem) {
                        return treeWidgetItem.getStatistic();
                    }
                }, comparator())))
                .collect(Collectors.toList());
        return items;
    }

    public static TreeWidgetItem toWidgetItem(final TestResultTreeGroup group) {
        return new TreeWidgetItem()
                .setUid(group.getUid())
                .setName(group.getName())
                .setStatistic(calculateStatisticByLeafs(group));
    }
}
