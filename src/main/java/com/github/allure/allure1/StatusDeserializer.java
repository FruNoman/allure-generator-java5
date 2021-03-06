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
package com.github.allure.allure1;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java8.util.function.Predicate;
import java8.util.stream.RefStreams;
import ru.yandex.qatools.allure.model.Status;

import java.io.IOException;

/**
 * @author charlie (Dmitry Baev).
 */
public class StatusDeserializer extends StdDeserializer<Status> {

    protected StatusDeserializer() {
        super(io.qameta.allure.model.Status.class);
    }

    @Override
    public Status deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException {
        final String value = p.readValueAs(String.class);
        return RefStreams.of(Status.values())
                .filter(new Predicate<Status>() {
                    @Override
                    public boolean test(Status status) {
                        return status.value().equalsIgnoreCase(value);
                    }
                })
                .findAny()
                .orElse(null);
    }
}
