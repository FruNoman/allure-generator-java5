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

import com.github.allure.utils.AllureUtilsAdv;
import io.qameta.allure.core.LaunchResults;
import io.qameta.allure.core.ResultsVisitor;
import org.apache.tika.metadata.Metadata;

import io.qameta.allure.context.RandomUidContext;
import io.qameta.allure.core.Configuration;
import io.qameta.allure.entity.Attachment;
import io.qameta.allure.entity.TestResult;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import java8.util.Optional;
import java8.util.function.Predicate;
import java8.util.function.Supplier;

import java.util.Set;
import java.util.function.Function;

import static org.apache.commons.io.FilenameUtils.getExtension;
import static org.apache.tika.mime.MimeTypes.getDefaultMimeTypes;


/**
 * @author charlie (Dmitry Baev).
 */
@SuppressWarnings("MultipleStringLiterals")
public class DefaultResultsVisitor implements ResultsVisitor {


    public static final String WILDCARD = "*/*";

    private final Configuration configuration;

    private final Map<String, Attachment> attachments;

    private final Set<TestResult> results;

    private final Map<String, Object> extra;

    public DefaultResultsVisitor(final Configuration configuration) {
        this.configuration = configuration;
        this.results = new HashSet<>();
        this.attachments = new HashMap<>();
        this.extra = new HashMap<>();
    }


    @Override
    public Attachment visitAttachmentFile(final File attachmentFile) {
        final RandomUidContext context = configuration.getContext(RandomUidContext.class);
        AllureUtilsAdv<String,Attachment> adv = new AllureUtilsAdv<>();
        return adv.computeIfAbsent(attachments, attachmentFile.getName(), new Function<String, Attachment>() {
            @Override
            public Attachment apply(String s) {
                final String uid = context.getValue().get();
                final String realType = probeContentType(attachmentFile);
                final String extension = Optional.of(getExtension(attachmentFile.getName()))
                        .filter(new Predicate<String>() {
                            @Override
                            public boolean test(String s) {
                                return !s.isEmpty();
                            }
                        })
                        .map(new java8.util.function.Function<String, String>() {
                            @Override
                            public String apply(String s) {
                                return "." + s;
                            }
                        })
                        .orElseGet(new Supplier<String>() {
                            @Override
                            public String get() {
                                return getExtensionByMimeType(realType);
                            }
                        });
                final String source = uid + (extension.isEmpty() ? "" : extension);
                final Long size = getFileSizeSafe(attachmentFile);
                return new Attachment()
                        .setUid(uid)
                        .setName(attachmentFile.getName())
                        .setSource(source)
                        .setType(realType)
                        .setSize(size);
            }
        });
    }

    @Override
    public void visitTestResult(final TestResult result) {
        results.add(result);
    }

    @Override
    public void visitExtra(final String name, final Object object) {
        extra.put(name, object);
    }

    @Override
    public void error(final String message, final Exception e) {
        //not implemented yet
    }

    @Override
    public void error(final String message) {
        //not implemented yet
    }

    public LaunchResults getLaunchResults() {
        return new DefaultLaunchResults(
                Collections.unmodifiableSet(results),
                Collections.unmodifiableMap(attachments),
                Collections.unmodifiableMap(extra)
        );
    }

    private static String getExtensionByMimeType(final String type) {
        try {
            return getDefaultMimeTypes().forName(type).getExtension();
        } catch (Exception e) {
            return "";
        }
    }

    public static String probeContentType(final File file) {
        try {
            InputStream inputStream = new FileInputStream(file);
            return probeContentType(inputStream, getNameFromInputStream(inputStream));
        } catch (Exception e) {
            return WILDCARD;
        }
    }

    public static String probeContentType(final InputStream inputStream, final String name) {
        try {
            final Metadata metadata = new Metadata();
            metadata.set(Metadata.RESOURCE_NAME_KEY, name);
            return getDefaultMimeTypes().detect(inputStream, metadata).toString();
        } catch (IOException e) {
            return WILDCARD;
        }
    }

    private static Long getFileSizeSafe(final File path) {
        try {
            return 0l;
        } catch (Exception e) {
            return 0l;
        }
    }

    private static String getNameFromInputStream(InputStream inputStream) {
        String name = "";
        File targetFile = null;
        try {
            byte[] buffer = new byte[inputStream.available()];
            inputStream.read(buffer);
            targetFile = new File("/sdcard//targetFile.tmp");
            OutputStream outStream = new FileOutputStream(targetFile);
            outStream.write(buffer);
            name = targetFile.getName();
        } catch (Exception e) {

        }
        targetFile.deleteOnExit();
        return name;
    }
}
