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
package com.github.allure.executor;

import io.qameta.allure.CommonJsonAggregator;
import io.qameta.allure.Constants;
import io.qameta.allure.Reader;
import io.qameta.allure.context.JacksonContext;
import io.qameta.allure.core.Configuration;
import io.qameta.allure.core.LaunchResults;
import io.qameta.allure.core.ResultsVisitor;
import io.qameta.allure.entity.ExecutorInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.List;

import java8.util.Optional;
import java8.util.function.Function;
import java8.util.function.Predicate;
import java8.util.stream.Collectors;
import java8.util.stream.StreamSupport;
import org.apache.poi.ss.formula.functions.T;

/**
 * @author charlie (Dmitry Baev).
 */
public class ExecutorPlugin extends CommonJsonAggregator implements Reader {

    public static final String EXECUTORS_BLOCK_NAME = "executor";
    protected static final String JSON_FILE_NAME = "executor.json";

    public ExecutorPlugin() {
        super(Constants.WIDGETS_DIR, "executors.json");
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
                InputStream is = null;
                try  {
                    is = new FileInputStream(file.get());
                    final ExecutorInfo info = context.getValue().readValue(is, ExecutorInfo.class);
                    visitor.visitExtra(EXECUTORS_BLOCK_NAME, info);
                } catch (IOException e) {
                    visitor.error("Could not read executor file " + file.get(), e);
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

    @Override
    public List<ExecutorInfo> getData(final List<LaunchResults> launches) {
        return StreamSupport.stream(launches)
                .map(new Function<LaunchResults, Optional<ExecutorInfo>>() {
                    @Override
                    public Optional<ExecutorInfo> apply(LaunchResults launchResults) {
                        return launchResults.getExtra(EXECUTORS_BLOCK_NAME);
                    }
                })
                .filter(new Predicate<Optional<ExecutorInfo>>() {
                    @Override
                    public boolean test(Optional<ExecutorInfo> tOptional) {
                        return tOptional.isPresent();
                    }
                })
                .map(new Function<Optional<ExecutorInfo>, ExecutorInfo>() {
                    @Override
                    public ExecutorInfo apply(Optional<ExecutorInfo> tOptional) {
                        return tOptional.get();
                    }
                })
                .filter(new Predicate<ExecutorInfo>() {
                    @Override
                    public boolean test(ExecutorInfo t) {
                        return ExecutorInfo.class.isInstance(t);
                    }
                })
                .map(new Function<ExecutorInfo, ExecutorInfo>() {
                    @Override
                    public ExecutorInfo apply(ExecutorInfo t) {
                        return ExecutorInfo.class.cast(t);
                    }
                })
                .collect(Collectors.toList());
    }
}
