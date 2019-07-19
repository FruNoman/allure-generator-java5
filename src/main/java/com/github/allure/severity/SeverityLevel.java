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
package com.github.allure.severity;

import com.fasterxml.jackson.annotation.JsonValue;

import java.io.Serializable;

import java8.util.Optional;
import java8.util.function.Predicate;
import java8.util.stream.RefStreams;
import java8.util.stream.Stream;

/**
 * @author charlie (Dmitry Baev).
 */
public enum SeverityLevel implements Serializable {

    blocker("blocker"),
    critical("critical"),
    normal("normal"),
    minor("minor"),
    trivial("trivial");

    private static final long serialVersionUID = 1L;

    private final String value;

    SeverityLevel(final String v) {
        value = v;
    }

    @JsonValue
    public String value() {
        return value;
    }

    public static Optional<SeverityLevel> fromValue(final String value) {
        return RefStreams.of(values())
                .filter(new Predicate<SeverityLevel>() {
                    @Override
                    public boolean test(SeverityLevel severityLevel) {
                        return severityLevel.value().equalsIgnoreCase(value);
                    }
                })
                .findFirst();
    }
}
