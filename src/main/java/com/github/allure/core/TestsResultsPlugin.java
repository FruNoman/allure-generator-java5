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
package com.github.allure.core;

import io.qameta.allure.Aggregator;
import io.qameta.allure.Constants;
import io.qameta.allure.context.JacksonContext;
import io.qameta.allure.core.Configuration;
import io.qameta.allure.core.LaunchResults;
import io.qameta.allure.entity.TestResult;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class TestsResultsPlugin implements Aggregator {

    @Override
    public void aggregate(final Configuration configuration,
                          final List<LaunchResults> launchesResults,
                          final String outputDirectory) throws IOException {
        final JacksonContext context = configuration.getContext(JacksonContext.class);
//        final Path testCasesFolder = Files.createDirectories(
//                outputDirectory.resolve(Constants.DATA_DIR).resolve("test-cases")
//        );
//        final List<TestResult> results = launchesResults.stream()
//                .flatMap(launch -> launch.getAllResults().stream())
//                .collect(Collectors.toList());
//        for (TestResult result : results) {
//            final Path file = testCasesFolder.resolve(result.getSource());
//            try (OutputStream os = Files.newOutputStream(file)) {
//                context.getValue().writeValue(os, result);
//            }
//        }
    }
}
