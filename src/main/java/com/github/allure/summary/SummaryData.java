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
package com.github.allure.summary;

import io.qameta.allure.entity.GroupTime;
import io.qameta.allure.entity.Statistic;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author charlie (Dmitry Baev).
 */

public class SummaryData implements Serializable {

    private static final long serialVersionUID = 1L;

    protected String reportName;
    protected List<String> testRuns = new ArrayList<>();
    protected Statistic statistic = new Statistic();
    protected GroupTime time = new GroupTime();

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public String getReportName() {
        return reportName;
    }

    public SummaryData setReportName(String reportName) {
        this.reportName = reportName;
        return this;
    }

    public List<String> getTestRuns() {
        return testRuns;
    }

    public SummaryData setTestRuns(List<String> testRuns) {
        this.testRuns = testRuns;
        return this;
    }

    public Statistic getStatistic() {
        return statistic;
    }

    public SummaryData setStatistic(Statistic statistic) {
        this.statistic = statistic;
        return this;
    }

    public GroupTime getTime() {
        return time;
    }

    public SummaryData setTime(GroupTime time) {
        this.time = time;
        return this;
    }
}
