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
package com.github.allure;

import com.github.allure.allure2.Allure2Plugin;
import com.github.allure.category.CategoriesPlugin;
import com.github.allure.category.CategoriesTrendPlugin;
import com.github.allure.duration.DurationPlugin;
import com.github.allure.duration.DurationTrendPlugin;
import com.github.allure.environment.Allure1EnvironmentPlugin;
import com.github.allure.executor.ExecutorPlugin;
import com.github.allure.history.HistoryPlugin;
import com.github.allure.launch.LaunchPlugin;
import com.github.allure.retry.RetryPlugin;
import com.github.allure.retry.RetryTrendPlugin;
import com.github.allure.severity.SeverityPlugin;
import com.github.allure.status.StatusChartPlugin;
import com.github.allure.suites.SuitesPlugin;
import com.github.allure.summary.SummaryPlugin;

import com.github.allure.timeline.TimelinePlugin;
import io.qameta.allure.Extension;

import io.qameta.allure.context.FreemarkerContext;
import io.qameta.allure.context.JacksonContext;
import io.qameta.allure.context.MarkdownContext;
import io.qameta.allure.context.RandomUidContext;
import io.qameta.allure.core.*;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Builder for {@link Configuration}.
 *
 * @see Configuration
 * @since 2.0
 */
@SuppressWarnings({
        "PMD.ExcessiveImports",
        "ClassDataAbstractionCoupling",
        "ClassFanOutComplexity"
})
public class ConfigurationBuilder {

    private final List<Extension> extensions = new ArrayList<>();

    private final List<Plugin> plugins = new ArrayList<>();

    public ConfigurationBuilder useDefault() {
        fromExtensions(Arrays.asList(
                new JacksonContext(),
                new MarkdownContext(),
                new FreemarkerContext(),
                new RandomUidContext(),
//                new MarkdownDescriptionsPlugin(),
                new RetryPlugin(),
                new RetryTrendPlugin(),
//                new TagsPlugin(),
                new SeverityPlugin(),
//                new OwnerPlugin(),
//                new IdeaLinksPlugin(),
                new HistoryPlugin(),
//                new HistoryTrendPlugin(),
                new CategoriesPlugin(),
                new CategoriesTrendPlugin(),
                new DurationPlugin(),
                new DurationTrendPlugin(),
                new StatusChartPlugin(),
                new TimelinePlugin(),
                new SuitesPlugin(),
//                new ReportWebPlugin(),
//                new TestsResultsPlugin(),
//                new AttachmentsPlugin(),
//                new MailPlugin(),
//                new InfluxDbExportPlugin(),
//                new PrometheusExportPlugin(),
                new SummaryPlugin(),
                new ExecutorPlugin(),
                new LaunchPlugin(),
//                new Allure1Plugin(),
                new Allure1EnvironmentPlugin(),
                new Allure2Plugin()
//                new GaPlugin()
        ));
        return this;
    }

    public ConfigurationBuilder fromExtensions(final List<Extension> extensions) {
        this.extensions.addAll(extensions);
        return this;
    }

    public ConfigurationBuilder fromPlugins(final List<Plugin> plugins) {
        this.plugins.addAll(plugins);
        plugins.stream()
                .map(Plugin::getExtensions)
                .forEach(this::fromExtensions);
        return this;
    }

    public Configuration build() {
        return new DefaultConfiguration(
                Collections.unmodifiableList(extensions),
                Collections.unmodifiableList(plugins)
        );
    }
}
