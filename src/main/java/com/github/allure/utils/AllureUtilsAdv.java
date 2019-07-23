package com.github.allure.utils;


import java.util.Map;
import java8.util.Objects;
import java8.util.function.Function;

import static java.nio.file.Files.newDirectoryStream;

public class AllureUtilsAdv<K, V> {

    public V computeIfAbsent(Map<K, V> map, K key,
                             Function<? super K, ? extends V> mappingFunction) {
        Objects.requireNonNull(mappingFunction);
        V v;
        if ((v = map.get(key)) == null) {
            V newValue;
            if ((newValue = mappingFunction.apply(key)) != null) {
                map.put(key, newValue);
                return newValue;
            }
        }
        return v;
    }


}
