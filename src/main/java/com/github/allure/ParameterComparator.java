package com.github.allure;

import java.util.Comparator;

import io.qameta.allure.entity.Parameter;

public class ParameterComparator implements Comparator<Parameter> {
    @Override
    public int compare(Parameter o1, Parameter o2) {
        return o1.getName().compareTo(o2.getName());
    }
}
