import com.github.allure.ConfigurationBuilder;
import com.github.allure.DefaultResultsVisitor;
import com.github.allure.allure2.Allure2Plugin;
import com.github.allure.category.CategoriesPlugin;
import com.github.allure.category.CategoriesTrendItem;
import com.github.allure.category.CategoriesTrendPlugin;
import com.github.allure.environment.Allure1EnvironmentPlugin;
import com.github.allure.executor.ExecutorPlugin;
import com.github.allure.history.HistoryData;
import com.github.allure.history.HistoryPlugin;
import com.github.allure.severity.SeverityData;
import com.github.allure.severity.SeverityPlugin;
import com.github.allure.status.StatusChartData;
import com.github.allure.status.StatusChartPlugin;
import com.github.allure.suites.SuitesPlugin;
import com.github.allure.summary.SummaryData;
import com.github.allure.summary.SummaryPlugin;
import io.qameta.allure.Aggregator;
import io.qameta.allure.Reader;
import io.qameta.allure.core.Configuration;
import io.qameta.allure.core.LaunchResults;
import io.qameta.allure.entity.EnvironmentItem;
import io.qameta.allure.entity.ExecutorInfo;
import io.qameta.allure.entity.TestResult;
import io.qameta.allure.tree.Tree;
import java8.util.function.Consumer;
import java8.util.stream.StreamSupport;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Allure2PluginTests {
    @Test
    public void test(){
        File allureResultDirecory = new File("/home/dfrolov/allure-results");
        List<File> fileList = Arrays.asList(allureResultDirecory.listFiles());
        Allure2Plugin allure2Plugin = new Allure2Plugin();
        final Configuration configuration = new ConfigurationBuilder().useDefault().build();
        final DefaultResultsVisitor resultsVisitor = new DefaultResultsVisitor(configuration);
        allure2Plugin.readResults(configuration,resultsVisitor,fileList);
        LaunchResults results = resultsVisitor.getLaunchResults();
        List<LaunchResults> list = new ArrayList<>();
        list.add(results);

        SummaryPlugin summaryPlugin = new SummaryPlugin();
        SummaryData data = (SummaryData) summaryPlugin.getData(list);
        System.out.println(data);

        SuitesPlugin suitesPlugin = new SuitesPlugin();
        Tree<TestResult> testTree = suitesPlugin.getData(list);
        System.out.println(testTree);

        StatusChartPlugin statusChartPlugin = new StatusChartPlugin();
        List<StatusChartData> dataStatus = statusChartPlugin.getData(list);
        System.out.println(dataStatus);

        HistoryPlugin historyPlugin = new HistoryPlugin();
        historyPlugin.readResults(configuration,resultsVisitor,fileList);
        Map<String, HistoryData> dataHistory = historyPlugin.getData(list);
        System.out.println(dataHistory);

        Allure1EnvironmentPlugin environmentPlugin = new Allure1EnvironmentPlugin();
        List<EnvironmentItem> envList = environmentPlugin.getData(list);
        System.out.println(envList);

        SeverityPlugin.WidgetAggregator severityPlugin = new SeverityPlugin.WidgetAggregator();
        List<SeverityData> severityData =  severityPlugin.getData(list);
        System.out.println(severityData);

        CategoriesPlugin categoriesPlugin = new CategoriesPlugin();
        categoriesPlugin.readResults(configuration,resultsVisitor,fileList);
        Tree<TestResult> cattree = categoriesPlugin.getData(list);
        System.out.println(cattree);

        CategoriesTrendPlugin categoriesTrendPlugin = new CategoriesTrendPlugin();
        categoriesTrendPlugin.readResults(configuration,resultsVisitor,fileList);
        List<CategoriesTrendItem> catItem = categoriesTrendPlugin.getData(list);
        System.out.println(catItem);

        ExecutorPlugin executorPlugin = new ExecutorPlugin();
        executorPlugin.readResults(configuration,resultsVisitor,fileList);
        List<ExecutorInfo> listEnv = executorPlugin.getData(list);
        System.out.println(listEnv);
    }

    @Test
    public void someTest(){
        File allureResultDirecory = new File("/home/dfrolov/allure-results");
        List<File> fileList = Arrays.asList(allureResultDirecory.listFiles());
        final Configuration configuration = new ConfigurationBuilder().useDefault().build();
        final DefaultResultsVisitor visitor = new DefaultResultsVisitor(configuration);
        StreamSupport.stream(configuration.getReaders()).forEach(new Consumer<Reader>() {
            @Override
            public void accept(Reader reader) {
                reader.readResults(configuration, visitor, fileList);
            }
        });
        LaunchResults results = visitor.getLaunchResults();
        List<LaunchResults> launchList = new ArrayList<>();
        launchList.add(results);
        StreamSupport.stream(configuration.getAggregators()).forEach(new Consumer<Aggregator>() {
            @Override
            public void accept(Aggregator aggregator) {
                try {
                    aggregator.aggregate(configuration, launchList,"");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        SummaryPlugin summaryPlugin = new SummaryPlugin();
        SummaryData summaryData = summaryPlugin.getData(launchList);
        System.out.println(summaryData);

    }
}
