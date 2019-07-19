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
package com.github.allure.status;

import com.github.allure.severity.SeverityLevel;

import io.qameta.allure.entity.Status;
import io.qameta.allure.entity.Time;


import java.io.Serializable;

public class StatusChartData implements Serializable {

    private static final long serialVersionUID = 1L;

    protected String uid;
    protected String name;
    protected Time time;
    protected Status status;
    protected SeverityLevel severity;

    public StatusChartData() {
    }

    public String getUid() {
        return this.uid;
    }

    public String getName() {
        return this.name;
    }

    public Time getTime() {
        return this.time;
    }

    public Status getStatus() {
        return this.status;
    }

    public SeverityLevel getSeverity() {
        return this.severity;
    }

    public StatusChartData setUid(String uid) {
        this.uid = uid;
        return this;
    }

    public StatusChartData setName(String name) {
        this.name = name;
        return this;
    }

    public StatusChartData setTime(Time time) {
        this.time = time;
        return this;
    }

    public StatusChartData setStatus(Status status) {
        this.status = status;
        return this;
    }

    public StatusChartData setSeverity(SeverityLevel severity) {
        this.severity = severity;
        return this;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof StatusChartData)) return false;
        final StatusChartData other = (StatusChartData) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$uid = this.getUid();
        final Object other$uid = other.getUid();
        if (this$uid == null ? other$uid != null : !this$uid.equals(other$uid)) return false;
        final Object this$name = this.getName();
        final Object other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
        final Object this$time = this.getTime();
        final Object other$time = other.getTime();
        if (this$time == null ? other$time != null : !this$time.equals(other$time)) return false;
        final Object this$status = this.getStatus();
        final Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final Object this$severity = this.getSeverity();
        final Object other$severity = other.getSeverity();
        if (this$severity == null ? other$severity != null : !this$severity.equals(other$severity)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof StatusChartData;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $uid = this.getUid();
        result = result * PRIME + ($uid == null ? 43 : $uid.hashCode());
        final Object $name = this.getName();
        result = result * PRIME + ($name == null ? 43 : $name.hashCode());
        final Object $time = this.getTime();
        result = result * PRIME + ($time == null ? 43 : $time.hashCode());
        final Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final Object $severity = this.getSeverity();
        result = result * PRIME + ($severity == null ? 43 : $severity.hashCode());
        return result;
    }

    public String toString() {
        return "StatusChartData(uid=" + this.getUid() + ", name=" + this.getName() + ", time=" + this.getTime() + ", status=" + this.getStatus() + ", severity=" + this.getSeverity() + ")";
    }
}
