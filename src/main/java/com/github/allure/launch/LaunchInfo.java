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
package com.github.allure.launch;

import io.qameta.allure.entity.Statistic;

import java.io.Serializable;

/**
 * @author charlie (Dmitry Baev).
 */
public class LaunchInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    protected String uid;
    protected String name;
    protected Statistic statistic = new Statistic();

    public LaunchInfo() {
    }

    public String getUid() {
        return this.uid;
    }

    public String getName() {
        return this.name;
    }

    public Statistic getStatistic() {
        return this.statistic;
    }

    public LaunchInfo setUid(String uid) {
        this.uid = uid;
        return this;
    }

    public LaunchInfo setName(String name) {
        this.name = name;
        return this;
    }

    public LaunchInfo setStatistic(Statistic statistic) {
        this.statistic = statistic;
        return this;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof LaunchInfo)) return false;
        final LaunchInfo other = (LaunchInfo) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$uid = this.getUid();
        final Object other$uid = other.getUid();
        if (this$uid == null ? other$uid != null : !this$uid.equals(other$uid)) return false;
        final Object this$name = this.getName();
        final Object other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
        final Object this$statistic = this.getStatistic();
        final Object other$statistic = other.getStatistic();
        if (this$statistic == null ? other$statistic != null : !this$statistic.equals(other$statistic)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof LaunchInfo;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $uid = this.getUid();
        result = result * PRIME + ($uid == null ? 43 : $uid.hashCode());
        final Object $name = this.getName();
        result = result * PRIME + ($name == null ? 43 : $name.hashCode());
        final Object $statistic = this.getStatistic();
        result = result * PRIME + ($statistic == null ? 43 : $statistic.hashCode());
        return result;
    }

    public String toString() {
        return "LaunchInfo(uid=" + this.getUid() + ", name=" + this.getName() + ", statistic=" + this.getStatistic() + ")";
    }
}
