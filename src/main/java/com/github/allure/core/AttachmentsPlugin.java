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
import io.qameta.allure.core.Configuration;
import io.qameta.allure.core.LaunchResults;
import io.qameta.allure.entity.Attachment;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

/**
 * Plugin that stores attachments to report data folder.
 *
 * @since 2.0
 */
public class AttachmentsPlugin implements Aggregator {

    @Override
    public void aggregate(final Configuration configuration,
                          final List<LaunchResults> launchesResults,
                          final String outputDirectory) throws IOException {
        final File historyFolder = new File(outputDirectory + File.separator + Constants.DATA_DIR + File.separator + "attachments");
        historyFolder.mkdirs();
        for (LaunchResults launch : launchesResults) {
            for (Map.Entry<String, Attachment> entry : launch.getAttachments().entrySet()) {
                File file = new File(historyFolder.getAbsolutePath()+File.separator+entry.getValue().getSource());
            }
        }
    }
}
