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
package com.github.allure.launch;

import io.qameta.allure.CommonJsonAggregator;
import io.qameta.allure.Constants;
import io.qameta.allure.Reader;
import io.qameta.allure.context.JacksonContext;
import io.qameta.allure.core.Configuration;
import io.qameta.allure.core.LaunchResults;
import io.qameta.allure.core.ResultsVisitor;
import io.qameta.allure.entity.Statistic;
import java8.util.function.Function;
import java8.util.function.Predicate;
import java8.util.stream.StreamSupport;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.Arrays;
import java.util.List;

import java8.util.Optional;
import java8.util.stream.Collectors;

/**
 * @author charlie (Dmitry Baev).
 */
public class LaunchPlugin extends CommonJsonAggregator implements Reader {

    private static final String LAUNCH_BLOCK_NAME = "launch";
    private static final String JSON_FILE_NAME = "launch.json";

    public LaunchPlugin() {
        super(Constants.WIDGETS_DIR, JSON_FILE_NAME);
    }

    @Override
    public void readResults(final Configuration configuration,
                            final ResultsVisitor visitor,
                            final List<File> fileList) {
        final JacksonContext context = configuration.getContext(JacksonContext.class);

        Optional<File> launchFolder = StreamSupport.stream(fileList).filter(new Predicate<File>() {
            @Override
            public boolean test(File file) {
                return file.getName().equals(LAUNCH_BLOCK_NAME);
            }
        }).findFirst();
        if (launchFolder.isPresent()) {
            Optional<File> launchFile = StreamSupport.stream(Arrays.asList(launchFolder.get().listFiles())).filter(new Predicate<File>() {
                @Override
                public boolean test(File file) {
                    return file.getName().equals(JSON_FILE_NAME);
                }
            }).findFirst();
            if (launchFile.isPresent()) {
                if (launchFile.get().exists()) {
                    InputStream is = null;
                    try  {
                        is = new FileInputStream(launchFile.get());
                        final LaunchInfo info = context.getValue().readValue(is, LaunchInfo.class);
                        visitor.visitExtra(LAUNCH_BLOCK_NAME, info);
                    } catch (IOException e) {
                        visitor.error("Could not read launch file " + launchFile.get(), e);
                    }finally {
                        try {
                            is.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    @Override
    public List<LaunchInfo> getData(final List<LaunchResults> launches) {
        return StreamSupport.stream(launches)
                .map(new Function<LaunchResults, Optional<LaunchInfo>>() {
                    @Override
                    public Optional<LaunchInfo> apply(LaunchResults launchResults) {
                        return updateLaunchInfo(launchResults);
                    }
                })
                .filter(new Predicate<Optional<LaunchInfo>>() {
                    @Override
                    public boolean test(Optional<LaunchInfo> launchInfoOptional) {
                        return launchInfoOptional.isPresent();
                    }
                })
                .map(new Function<Optional<LaunchInfo>, LaunchInfo>() {
                    @Override
                    public LaunchInfo apply(Optional<LaunchInfo> launchInfoOptional) {
                        return launchInfoOptional.get();
                    }
                })
                .collect(Collectors.toList());
    }

    private Optional<LaunchInfo> updateLaunchInfo(final LaunchResults results) {
        final Optional<LaunchInfo> extra = results.getExtra(LAUNCH_BLOCK_NAME);
        extra.map(new Function<LaunchInfo, LaunchInfo>() {
            @Override
            public LaunchInfo apply(LaunchInfo launchInfo) {
                final Statistic statistic = new Statistic();
                launchInfo.setStatistic(statistic);
                results.getResults().forEach(statistic::update);
                return launchInfo;
            }
        });
        return extra;
    }
}
