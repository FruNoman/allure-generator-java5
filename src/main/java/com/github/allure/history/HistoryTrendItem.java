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
package com.github.allure.history;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.qameta.allure.entity.Statistic;


import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class HistoryTrendItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("data")
    @XmlElement(required = true)
    protected Statistic data;
    @XmlElement(required = true, type = Long.class, nillable = true)
    protected Long buildOrder;
    @XmlElement(required = true)
    protected String reportUrl;
    @XmlElement(required = true)
    protected String reportName;

    @JsonIgnore
    public Statistic getStatistic() {
        return data;
    }

    @JsonSetter("statistic")
    public HistoryTrendItem setStatistic(final Statistic statistic) {
        this.data = statistic;
        return this;
    }

    public Statistic getData() {
        return data;
    }

    public void setData(Statistic data) {
        this.data = data;
    }

    public Long getBuildOrder() {
        return buildOrder;
    }

    public void setBuildOrder(Long buildOrder) {
        this.buildOrder = buildOrder;
    }

    public String getReportUrl() {
        return reportUrl;
    }

    public void setReportUrl(String reportUrl) {
        this.reportUrl = reportUrl;
    }

    public String getReportName() {
        return reportName;
    }

    public void setReportName(String reportName) {
        this.reportName = reportName;
    }
}
