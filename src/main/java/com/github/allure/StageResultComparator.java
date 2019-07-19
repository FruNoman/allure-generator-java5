package com.github.allure;

import java.util.Comparator;

import io.qameta.allure.entity.StageResult;

public class StageResultComparator implements Comparator<StageResult> {

    @Override
    public int compare(StageResult o1, StageResult o2) {
        return (int) (o1.getTime().getStart() - o2.getTime().getStart());
    }
}
