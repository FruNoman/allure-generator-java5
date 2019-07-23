package com.github.allure.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class AllureUtils {
    public static final String TEST_SUITE_FILE_SUFFIX = "-testsuite";
    public static final String TEST_SUITE_XML_FILE_GLOB = String.format("*%s.xml", TEST_SUITE_FILE_SUFFIX);
    public static final String TEST_SUITE_JSON_FILE_GLOB = String.format("*%s.json", TEST_SUITE_FILE_SUFFIX);

    public static List<File> listTestSuiteJsonFiles(List<File>... directories) throws IOException {
        return listFiles(TEST_SUITE_JSON_FILE_GLOB, directories);
    }

    public static List<File> listFiles(String glob, List<File>... directories) throws IOException {
        List<File> result = new ArrayList<>();
        for (List<File> directory : directories) {
            for(File file:directory) {
                result.addAll(listFiles(glob, file));
            }
        }
        return result;
    }

    public static List<File> listFiles(String glob, File directory) throws IOException {
        List<File> result = new ArrayList<>();
        if (!directory.isDirectory()) {
            return result;
        }
        {
            for (File file : directory.listFiles()) {
                if (!file.isDirectory()) {
                    result.add(file);
                }
            }
            return result;
        }
    }

    public static List<File> listTestSuiteXmlFiles(List<File>... directories) throws IOException {
        return listFiles(TEST_SUITE_XML_FILE_GLOB, directories);
    }


}
