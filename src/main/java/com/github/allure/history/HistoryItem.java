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

import io.qameta.allure.entity.Status;
import io.qameta.allure.entity.Time;


import java.io.Serializable;
import java.util.Comparator;

import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsFirst;

public class HistoryItem implements Serializable {

    private static final long serialVersionUID = 1L;

    protected String uid;
    protected String reportUrl;
    protected Status status;
    protected String statusDetails;
    protected Time time;

    public static Comparator<HistoryItem> comparingByTime() {
        return comparingByTimeAsc().reversed();
    }

    public static Comparator<HistoryItem> comparingByTimeAsc() {
        return comparing(HistoryItem::getTime,
                nullsFirst(comparing(Time::getStart, nullsFirst(naturalOrder())))
        );
    }

    public String getUid() {
        return uid;
    }

    public HistoryItem setUid(String uid) {
        this.uid = uid;
        return this;
    }

    public String getReportUrl() {
        return reportUrl;
    }

    public HistoryItem setReportUrl(String reportUrl) {
        this.reportUrl = reportUrl;
        return this;
    }

    public Status getStatus() {
        return status;
    }

    public HistoryItem setStatus(Status status) {
        this.status = status;
        return this;
    }

    public String getStatusDetails() {
        return statusDetails;
    }

    public HistoryItem setStatusDetails(String statusDetails) {
        this.statusDetails = statusDetails;
        return this;
    }

    public Time getTime() {
        return time;
    }

    public HistoryItem setTime(Time time) {
        this.time = time;
        return this;
    }
}
