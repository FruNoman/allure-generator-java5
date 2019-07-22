import com.github.allure.ConfigurationBuilder;
import com.github.allure.DefaultResultsVisitor;
import com.github.allure.allure2.Allure2Plugin;
import com.github.allure.category.CategoriesPlugin;
import com.github.allure.category.CategoriesTrendItem;
import com.github.allure.category.CategoriesTrendPlugin;
import com.github.allure.environment.Allure1EnvironmentPlugin;
import com.github.allure.history.HistoryData;
import com.github.allure.history.HistoryPlugin;
import com.github.allure.severity.SeverityData;
import com.github.allure.severity.SeverityPlugin;
import com.github.allure.status.StatusChartData;
import com.github.allure.status.StatusChartPlugin;
import com.github.allure.suites.SuitesPlugin;
import com.github.allure.summary.SummaryData;
import com.github.allure.summary.SummaryPlugin;
import io.qameta.allure.core.Configuration;
import io.qameta.allure.core.LaunchResults;
import io.qameta.allure.entity.EnvironmentItem;
import io.qameta.allure.entity.TestResult;
import io.qameta.allure.tree.Tree;
import org.junit.Test;

import java.io.File;
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
        Map<String, HistoryData> dataHistory = historyPlugin.getData(list);
        System.out.println(dataHistory);

        Allure1EnvironmentPlugin environmentPlugin = new Allure1EnvironmentPlugin();
        List<EnvironmentItem> envList = environmentPlugin.getData(list);
        System.out.println(envList);

        SeverityPlugin.WidgetAggregator severityPlugin = new SeverityPlugin.WidgetAggregator();
        List<SeverityData> severityData =  severityPlugin.getData(list);
        System.out.println(severityData);

        CategoriesPlugin categoriesPlugin = new CategoriesPlugin();
        Tree<TestResult> cattree = categoriesPlugin.getData(list);
        System.out.println(cattree);

        CategoriesTrendPlugin categoriesTrendPlugin = new CategoriesTrendPlugin();
        List<CategoriesTrendItem> catItem = categoriesTrendPlugin.getData(list);
        System.out.println(catItem);
    }
}
