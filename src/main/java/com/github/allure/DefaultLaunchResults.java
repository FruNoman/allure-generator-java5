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

import io.qameta.allure.core.LaunchResults;
import io.qameta.allure.entity.Attachment;
import io.qameta.allure.entity.TestResult;
import java8.util.function.Supplier;

import java.util.Map;
import java8.util.Optional;
import java.util.Set;


public class DefaultLaunchResults implements LaunchResults {

    private final Set<TestResult> results;

    private final Map<String, Attachment> attachments;

    private final Map<String, Object> extra;

    public DefaultLaunchResults(final Set<TestResult> results,
                                final Map<String, Attachment> attachments,
                                final Map<String, Object> extra) {
        this.results = results;
        this.attachments = attachments;
        this.extra = extra;
    }

    @Override
    public Set<TestResult> getResults() {
        return null;
    }

    @Override
    public Set<TestResult> getAllResults() {
        return results;
    }

    @Override
    public Map<String, Attachment> getAttachments() {
        return attachments;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getExtra(final String name) {
        return Optional.ofNullable((T) extra.get(name));
    }

    @Override
    public <T> T getExtra(String s, Supplier<T> supplier){
        final Optional<T> extra = getExtra(s);
        return extra.orElseGet(supplier);
    }
}
