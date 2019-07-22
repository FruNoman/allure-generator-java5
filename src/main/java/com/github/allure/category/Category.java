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

import io.qameta.allure.entity.Status;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author charlie (Dmitry Baev).
 */

public class Category implements Serializable {

    private static final long serialVersionUID = 1L;

    protected String name;
    protected String description;
    protected String descriptionHtml;
    protected String messageRegex;
    protected String traceRegex;
    protected List<Status> matchedStatuses = new ArrayList<>();
    protected boolean flaky;

    public String getName() {
        return name;
    }

    public Category setName(String name) {
        this.name = name;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public Category setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getDescriptionHtml() {
        return descriptionHtml;
    }

    public Category setDescriptionHtml(String descriptionHtml) {
        this.descriptionHtml = descriptionHtml;
        return this;
    }

    public String getMessageRegex() {
        return messageRegex;
    }

    public Category setMessageRegex(String messageRegex) {
        this.messageRegex = messageRegex;
        return this;
    }

    public String getTraceRegex() {
        return traceRegex;
    }

    public Category setTraceRegex(String traceRegex) {
        this.traceRegex = traceRegex;
        return this;
    }

    public List<Status> getMatchedStatuses() {
        return matchedStatuses;
    }

    public Category setMatchedStatuses(List<Status> matchedStatuses) {
        this.matchedStatuses = matchedStatuses;
        return this;
    }

    public boolean isFlaky() {
        return flaky;
    }

    public Category setFlaky(boolean flaky) {
        this.flaky = flaky;
        return this;
    }
}
