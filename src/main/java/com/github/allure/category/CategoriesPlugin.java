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

import com.fasterxml.jackson.core.type.TypeReference;
import io.qameta.allure.*;
import io.qameta.allure.context.JacksonContext;
import io.qameta.allure.core.Configuration;
import io.qameta.allure.core.LaunchResults;
import io.qameta.allure.core.ResultsVisitor;
import io.qameta.allure.csv.CsvExportCategory;
import io.qameta.allure.entity.Statistic;
import io.qameta.allure.entity.Status;
import io.qameta.allure.entity.TestResult;
import io.qameta.allure.tree.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.*;
import java.util.regex.Pattern;

import java8.util.Comparators;
import java8.util.Optional;
import java8.util.function.Consumer;
import java8.util.function.Function;
import java8.util.function.Predicate;
import java8.util.function.Supplier;
import java8.util.stream.Collectors;
import java8.util.stream.Stream;
import java8.util.stream.StreamSupport;

import static io.qameta.allure.entity.Statistic.comparator;
import static io.qameta.allure.entity.TestResult.comparingByTimeAsc;
import static io.qameta.allure.tree.TreeUtils.calculateStatisticByLeafs;
import static java8.util.Objects.isNull;
import static java8.util.Objects.nonNull;

/**
 * Plugin that generates data for Categories tab.
 *
 * @since 2.0
 */
@SuppressWarnings({"PMD.ExcessiveImports", "ClassDataAbstractionCoupling"})
public class CategoriesPlugin extends CompositeAggregator implements Reader {

    public static final String CATEGORIES = "categories";

    public static final Category FAILED_TESTS = new Category().setName("Product defects");

    public static final Category BROKEN_TESTS = new Category().setName("Test defects");

    public static final String JSON_FILE_NAME = "categories.json";

    public static final String CSV_FILE_NAME = "categories.csv";

    //@formatter:off
    private static final TypeReference<List<Category>> CATEGORIES_TYPE =
            new TypeReference<List<Category>>() {
            };
    //@formatter:on

    public CategoriesPlugin() {
        super(Arrays.asList(
                new JsonAggregator(), new CsvExportAggregator(), new WidgetAggregator()
        ));
    }

    @Override
    public void readResults(final Configuration configuration,
                            final ResultsVisitor visitor,
                            final List<File> fileList) {
        final JacksonContext context = configuration.getContext(JacksonContext.class);
        Optional<File> file = StreamSupport.stream(fileList).filter(new Predicate<File>() {
            @Override
            public boolean test(File file) {
                return file.getName().equals(JSON_FILE_NAME);
            }
        }).findFirst();

        if(file.isPresent()) {
            if (file.get().exists()) {
                try (InputStream is = new FileInputStream(file.get())) {
                    final List<Category> categories = context.getValue().readValue(is, CATEGORIES_TYPE);
                    visitor.visitExtra(CATEGORIES, categories);
                } catch (IOException e) {
                    visitor.error("Could not read categories file " + file.get(), e);
                }
            }
        }
    }

    @SuppressWarnings("PMD.DefaultPackage")
    /* default */public  Tree<TestResult> getData(final List<LaunchResults> launchResults) {

        final Tree<TestResult> categories = new TestResultTree(CATEGORIES, new TreeClassifier<TestResult>() {
            @Override
            public List<TreeLayer> classify(TestResult testResult) {
                return groupByCategories(testResult);
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
                .sorted(comparingByTimeAsc())
                .forEach(new Consumer<TestResult>() {
                    @Override
                    public void accept(TestResult testResult) {
                        categories.add(testResult);
                    }
                });
        return categories;
    }

    @SuppressWarnings("PMD.DefaultPackage")
    /* default */ static void addCategoriesForResults(final List<LaunchResults> launchesResults) {
        StreamSupport.stream(launchesResults).forEach(new Consumer<LaunchResults>() {
            @Override
            public void accept(LaunchResults launch) {
                final List<Category> categories = launch.getExtra(CATEGORIES, new Supplier<List<Category>>() {
                    @Override
                    public List<Category> get() {
                        return Collections.emptyList();
                    }
                });
                StreamSupport.stream(launch.getResults()).forEach(new Consumer<TestResult>() {
                    @Override
                    public void accept(TestResult result) {
                        final List<Category> resultCategories = result.getExtraBlock(CATEGORIES, new ArrayList<>());
                        StreamSupport.stream(categories).forEach(new Consumer<Category>() {
                            @Override
                            public void accept(Category category) {
                                if (matches(result, category)) {
                                    resultCategories.add(category);
                                }
                            }
                        });
                        if (resultCategories.isEmpty() && Status.FAILED.equals(result.getStatus())) {
                            result.getExtraBlock(CATEGORIES, new ArrayList<Category>()).add(FAILED_TESTS);
                        }
                        if (resultCategories.isEmpty() && Status.BROKEN.equals(result.getStatus())) {
                            result.getExtraBlock(CATEGORIES, new ArrayList<Category>()).add(BROKEN_TESTS);
                        }
                    }
                });
            }
        });
    }

    protected static List<TreeLayer> groupByCategories(final TestResult testResult) {
        final Set<String> categories = StreamSupport.stream(testResult.<List<Category>>getExtraBlock(CATEGORIES, new ArrayList<>()))
                .map(new Function<Category, String>() {
                    @Override
                    public String apply(Category category) {
                        return category.getName();
                    }
                })
                .collect(Collectors.toSet());
        final TreeLayer categoriesLayer = new DefaultTreeLayer(categories);
        final TreeLayer messageLayer = new DefaultTreeLayer(testResult.getStatusMessage());
        return Arrays.asList(categoriesLayer, messageLayer);
    }

    public static boolean matches(final TestResult result, final Category category) {
        final boolean matchesStatus = category.getMatchedStatuses().isEmpty()
                || nonNull(result.getStatus())
                && category.getMatchedStatuses().contains(result.getStatus());
        final boolean matchesMessage = isNull(category.getMessageRegex())
                || nonNull(result.getStatusMessage())
                && matches(result.getStatusMessage(), category.getMessageRegex());
        final boolean matchesTrace = isNull(category.getTraceRegex())
                || nonNull(result.getStatusTrace())
                && matches(result.getStatusTrace(), category.getTraceRegex());
        final boolean matchesFlaky = result.isFlaky() == category.isFlaky();
        return matchesStatus && matchesMessage && matchesTrace && matchesFlaky;
    }

    private static boolean matches(final String message, final String pattern) {
        return Pattern.compile(pattern, Pattern.DOTALL).matcher(message).matches();
    }

    protected static TreeWidgetItem toWidgetItem(final TestResultTreeGroup group) {
        return new TreeWidgetItem()
                .setUid(group.getUid())
                .setName(group.getName())
                .setStatistic(calculateStatisticByLeafs(group));
    }

    @Override
    public void aggregate(final Configuration configuration,
                          final List<LaunchResults> launchesResults,
                          final String outputDirectory) throws IOException {
        addCategoriesForResults(launchesResults);
        super.aggregate(configuration, launchesResults, outputDirectory);
    }

    /**
     * Generates tree data.
     */
    private static class JsonAggregator extends CommonJsonAggregator {

        JsonAggregator() {
            super(JSON_FILE_NAME);
        }

        @Override
        protected Tree<TestResult> getData(final List<LaunchResults> launches) {
            return new CategoriesPlugin().getData(launches);
        }
    }

    /**
     * Generates export data.
     */
    private static class CsvExportAggregator extends CommonCsvExportAggregator<CsvExportCategory> {

        CsvExportAggregator() {
            super(CSV_FILE_NAME, CsvExportCategory.class);
        }

        @Override
        protected List<CsvExportCategory> getData(final List<LaunchResults> launchesResults) {
            final List<CsvExportCategory> exportLabels = new ArrayList<>();
            final Tree<TestResult> data = new CategoriesPlugin().getData(launchesResults);
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
                    .sorted(Comparators.comparing(new Function<TreeWidgetItem, Statistic>() {
                        @Override
                        public Statistic apply(TreeWidgetItem treeWidgetItem) {
                            return treeWidgetItem.getStatistic();
                        }
                    }, comparator()).reversed())
                    .collect(Collectors.toList());
            StreamSupport.stream(items).forEach(new Consumer<TreeWidgetItem>() {
                @Override
                public void accept(TreeWidgetItem treeWidgetItem) {
                    exportLabels.add(new CsvExportCategory(treeWidgetItem));
                }
            });
            return exportLabels;
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
        protected Object getData(final List<LaunchResults> launches) {
            final Tree<TestResult> data = new CategoriesPlugin().getData(launches);
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
                    .sorted(Comparators.comparing(new Function<TreeWidgetItem, Statistic>() {
                        @Override
                        public Statistic apply(TreeWidgetItem treeWidgetItem) {
                            return treeWidgetItem.getStatistic();
                        }
                    }, comparator()).reversed())
                    .limit(10)
                    .collect(Collectors.toList());
            return new TreeWidgetData().setItems(items).setTotal(data.getChildren().size());
        }
    }
}
