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
package com.github.allure.trend;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Trend item data.
 *
 * @author eroshenkoam
 */

public class TrendItem implements Serializable {

    private static final long serialVersionUID = 1L;

    protected Long buildOrder;

    protected String reportUrl;

    protected String reportName;

    protected Map<String, Long> data = new HashMap<>();

    protected void increaseMetric(final String metric) {
        final long current = Optional.ofNullable(data.get(metric)).orElse(0L);
        data.put(metric, current + 1);
    }

    protected void setMetric(final String metric, final long value) {
        this.data.put(metric, value);
    }

    public Long getBuildOrder() {
        return this.buildOrder;
    }

    public String getReportUrl() {
        return this.reportUrl;
    }

    public String getReportName() {
        return this.reportName;
    }

    public Map<String, Long> getData() {
        return this.data;
    }

    public TrendItem setBuildOrder(Long buildOrder) {
        this.buildOrder = buildOrder;
        return this;
    }

    public TrendItem setReportUrl(String reportUrl) {
        this.reportUrl = reportUrl;
        return this;
    }

    public TrendItem setReportName(String reportName) {
        this.reportName = reportName;
        return this;
    }

    public TrendItem setData(Map<String, Long> data) {
        this.data = data;
        return this;
    }
}
