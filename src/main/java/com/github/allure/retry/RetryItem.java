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
package com.github.allure.retry;

import io.qameta.allure.entity.Status;
import io.qameta.allure.entity.Time;

import java.io.Serializable;

public class RetryItem implements Serializable {

    private static final long serialVersionUID = 1L;

    protected String uid;
    protected Status status;
    protected String statusDetails;
    protected Time time;

    public RetryItem() {
    }

    public String getUid() {
        return this.uid;
    }

    public Status getStatus() {
        return this.status;
    }

    public String getStatusDetails() {
        return this.statusDetails;
    }

    public Time getTime() {
        return this.time;
    }

    public RetryItem setUid(String uid) {
        this.uid = uid;
        return this;
    }

    public RetryItem setStatus(Status status) {
        this.status = status;
        return this;
    }

    public RetryItem setStatusDetails(String statusDetails) {
        this.statusDetails = statusDetails;
        return this;
    }

    public RetryItem setTime(Time time) {
        this.time = time;
        return this;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof RetryItem)) return false;
        final RetryItem other = (RetryItem) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$uid = this.getUid();
        final Object other$uid = other.getUid();
        if (this$uid == null ? other$uid != null : !this$uid.equals(other$uid)) return false;
        final Object this$status = this.getStatus();
        final Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final Object this$statusDetails = this.getStatusDetails();
        final Object other$statusDetails = other.getStatusDetails();
        if (this$statusDetails == null ? other$statusDetails != null : !this$statusDetails.equals(other$statusDetails))
            return false;
        final Object this$time = this.getTime();
        final Object other$time = other.getTime();
        if (this$time == null ? other$time != null : !this$time.equals(other$time)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof RetryItem;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $uid = this.getUid();
        result = result * PRIME + ($uid == null ? 43 : $uid.hashCode());
        final Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final Object $statusDetails = this.getStatusDetails();
        result = result * PRIME + ($statusDetails == null ? 43 : $statusDetails.hashCode());
        final Object $time = this.getTime();
        result = result * PRIME + ($time == null ? 43 : $time.hashCode());
        return result;
    }

    public String toString() {
        return "RetryItem(uid=" + this.getUid() + ", status=" + this.getStatus() + ", statusDetails=" + this.getStatusDetails() + ", time=" + this.getTime() + ")";
    }
}
