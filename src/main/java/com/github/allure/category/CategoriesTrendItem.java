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

import com.github.allure.trend.TrendItem;
import io.qameta.allure.entity.TestResult;
import java8.util.function.Consumer;
import java8.util.function.Function;
import java8.util.stream.StreamSupport;

import java.util.ArrayList;
import java.util.List;

/**
 * @author charlie (Dmitry Baev).
 */
public class CategoriesTrendItem extends TrendItem {

    public void increaseCategories(final TestResult result) {
        StreamSupport.stream(result.<List<Category>>getExtraBlock("categories", new ArrayList<>()))
                .map(new Function<Category, String>() {
                    @Override
                    public String apply(Category category) {
                        return category.getName();
                    }
                })
                .forEach(new Consumer<String>() {
                    @Override
                    public void accept(String s) {
                        increaseCategories(s);
                    }
                });
    }

    private void increaseCategories(final String name) {
        increaseMetric(name);
    }

}
