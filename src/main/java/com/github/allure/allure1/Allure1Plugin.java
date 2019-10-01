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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import com.github.allure.utils.AllureUtils;
import io.qameta.allure.Reader;
import io.qameta.allure.context.RandomUidContext;
import io.qameta.allure.core.Configuration;
import io.qameta.allure.core.ResultsVisitor;
import io.qameta.allure.entity.*;
import io.qameta.allure.entity.Attachment;
import io.qameta.allure.entity.Label;
import io.qameta.allure.entity.LabelName;
import io.qameta.allure.entity.Parameter;
import io.qameta.allure.entity.Status;
import io.qameta.allure.entity.Step;
import java8.util.Comparators;
import java8.util.Optional;
import java8.util.function.*;
import java8.util.stream.Collectors;
import java8.util.stream.RefStreams;
import java8.util.stream.Stream;
import java8.util.stream.StreamSupport;
import ru.yandex.qatools.allure.model.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;


import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;


import static com.fasterxml.jackson.databind.MapperFeature.USE_WRAPPER_NAME_AS_PROPERTY_NAME;
import static io.qameta.allure.entity.LabelName.*;
import static io.qameta.allure.entity.Status.*;
import static java8.util.stream.Collectors.toList;

/**
 * Plugin that reads results from Allure1 data format.
 *
 * @since 2.0
 */
@SuppressWarnings({
        "PMD.ExcessiveImports",
        "PMD.GodClass",
        "PMD.TooManyMethods",
        "ClassDataAbstractionCoupling",
        "ClassFanOutComplexity",
        "MultipleStringLiterals"
})
public class Allure1Plugin implements Reader {

    private static final String UNKNOWN = "unknown";
    private static final String MD_5 = "md5";
    private static final String ISSUE_URL_PROPERTY = "allure.issues.tracker.pattern";
    private static final String TMS_LINK_PROPERTY = "allure.tests.management.pattern";
    private static final Comparator<Parameter> PARAMETER_COMPARATOR =
            Comparators.thenComparing(Comparators.comparing(new Function<Parameter, String>() {
                @Override
                public String apply(Parameter parameter) {
                    return parameter.getName();
                }
            }, Comparators.nullsFirst(Comparators.naturalOrder())), Comparators.comparing(new Function<Parameter, String>() {
                @Override
                public String apply(Parameter parameter) {
                    return parameter.getValue();
                }
            }, Comparators.nullsFirst(Comparators.naturalOrder())));

    public static final String ENVIRONMENT_BLOCK_NAME = "environment";
    public static final String ALLURE1_RESULTS_FORMAT = "allure1";

    private final ObjectMapper jsonMapper = new ObjectMapper();
    private final ObjectMapper xmlMapper;

    public Allure1Plugin() {
        final SimpleModule module = new XmlParserModule()
                .addDeserializer(ru.yandex.qatools.allure.model.Status.class, new StatusDeserializer());
        xmlMapper = new ObjectMapper()
                .configure(USE_WRAPPER_NAME_AS_PROPERTY_NAME, true)
                .setAnnotationIntrospector(new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()))
                .registerModule(module);
    }

    @Override
    public void readResults(final Configuration configuration,
                            final ResultsVisitor visitor,
                            final List<File> fileList) {
        final Properties allureProperties = loadAllureProperties(fileList);
        final RandomUidContext context = configuration.getContext(RandomUidContext.class);

        final Map<String, String> environment = processEnvironment(fileList);
        getStreamOfAllure1Results(fileList).forEach(new Consumer<TestSuiteResult>() {
                                                        @Override
                                                        public void accept(TestSuiteResult testSuiteResult) {
                                                            StreamSupport.stream(testSuiteResult.getTestCases()).forEach(new Consumer<TestCaseResult>() {
                                                                @Override
                                                                public void accept(TestCaseResult testCaseResult) {
                                                                    convert(context.getValue(), fileList, visitor, testSuiteResult, testCaseResult, allureProperties);
                                                                    StreamSupport.stream(getEnvironmentParameters(testCaseResult)).forEach(new Consumer<ru.yandex.qatools.allure.model.Parameter>() {
                                                                        @Override
                                                                        public void accept(ru.yandex.qatools.allure.model.Parameter parameter) {
                                                                            environment.put(parameter.getName(), parameter.getValue());
                                                                        }
                                                                    });
                                                                }
                                                            });
                                                        }
                                                    }
        );

        visitor.visitExtra(ENVIRONMENT_BLOCK_NAME, environment);
    }

    private List<ru.yandex.qatools.allure.model.Parameter> getEnvironmentParameters(final TestCaseResult testCase) {
        return StreamSupport.stream(testCase.getParameters()).filter(new Predicate<ru.yandex.qatools.allure.model.Parameter>() {
            @Override
            public boolean test(ru.yandex.qatools.allure.model.Parameter parameter) {
                return hasEnvType(parameter);
            }
        }).collect(Collectors.toList());
    }

    private Properties loadAllureProperties(final List<File> fileList) {
        Optional<File> allureProperties = StreamSupport.stream(fileList).filter(new Predicate<File>() {
            @Override
            public boolean test(File file) {
                return file.getName().equals("allure.properties");
            }
        }).findFirst();
        final Properties properties = new Properties();
        if (allureProperties.isPresent()) {
            if (allureProperties.get().exists()) {
                InputStream propFile = null;
                try {
                    propFile = new FileInputStream(allureProperties.get());
                    properties.load(propFile);
                } catch (IOException e) {
                }finally {
                    try {
                        propFile.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        properties.putAll(System.getProperties());
        return properties;
    }

    @SuppressWarnings({
            "PMD.ExcessiveMethodLength",
            "JavaNCSS",
            "ExecutableStatementCount",
            "PMD.NcssCount"
    })
    private void convert(final Supplier<String> randomUid,
                         final List<File> directory,
                         final ResultsVisitor visitor,
                         final TestSuiteResult testSuite,
                         final TestCaseResult source,
                         final Properties properties) {
        final TestResult dest = new TestResult();
        final String suiteName = firstNonNull(testSuite.getTitle(), testSuite.getName(), "unknown test suite");
        final String testClass = firstNonNull(
                findLabelValue(source.getLabels(), TEST_CLASS.value()),
                findLabelValue(testSuite.getLabels(), TEST_CLASS.value()),
                testSuite.getName(),
                UNKNOWN
        );
        final String testMethod = firstNonNull(
                findLabelValue(source.getLabels(), TEST_METHOD.value()),
                source.getName(),
                UNKNOWN
        );
        final String name = firstNonNull(source.getTitle(), source.getName(), "unknown test case");

        final List<Parameter> parameters = getParameters(source);
        final Optional<ru.yandex.qatools.allure.model.Label> historyId = findLabel(source.getLabels(), "historyId");
        if (historyId.isPresent()) {
            dest.setHistoryId(historyId.get().getValue());
        } else {
            dest.setHistoryId(getHistoryId(String.format("%s#%s", testClass, name), parameters));
        }
        dest.setUid(randomUid.get());
        dest.setName(name);
        dest.setFullName(String.format("%s.%s", testClass, testMethod));

        final Status status = convert(source.getStatus());
        dest.setStatus(status);
        dest.setTime(Time.create(source.getStart(), source.getStop()));
        dest.setParameters(parameters);
        dest.setDescription(getDescription(testSuite.getDescription(), source.getDescription()));
        dest.setDescriptionHtml(getDescriptionHtml(testSuite.getDescription(), source.getDescription()));
        Optional.ofNullable(source.getFailure()).ifPresent(new Consumer<Failure>() {
            @Override
            public void accept(Failure failure) {
                dest.setStatusMessage(failure.getMessage());
                dest.setStatusTrace(failure.getStackTrace());
            }
        });

        if (!source.getSteps().isEmpty() || !source.getAttachments().isEmpty()) {
            final StageResult testStage = new StageResult();
            if (!source.getSteps().isEmpty()) {
                //@formatter:off
                testStage.setSteps(convert(
                        source.getSteps(),
                        new Function<ru.yandex.qatools.allure.model.Step, Step>() {
                            @Override
                            public Step apply(ru.yandex.qatools.allure.model.Step step) {
                                return convert(directory, visitor, step, status, dest.getStatusMessage(), dest.getStatusTrace());
                            }
                        })
                );
            }
            if (!source.getAttachments().isEmpty()) {
                testStage.setAttachments(convert(source.getAttachments(), new Function<ru.yandex.qatools.allure.model.Attachment, Attachment>() {
                    @Override
                    public Attachment apply(ru.yandex.qatools.allure.model.Attachment attachment) {
                        return convert(directory, visitor, attachment);
                    }
                }));
            }
            testStage.setStatus(status);
            testStage.setStatusMessage(dest.getStatusMessage());
            testStage.setStatusTrace(dest.getStatusTrace());
            dest.setTestStage(testStage);
        }

        final Set<Label> set = new TreeSet<>(
                Comparators.thenComparing(Comparators.comparing(new Function<Label, String>() {
                            @Override
                            public String apply(Label label) {
                                return label.getValue();
                            }
                        }, Comparators.nullsFirst(Comparators.naturalOrder()))
                        , Comparators.comparing(new Function<Label, String>() {
                            @Override
                            public String apply(Label label) {
                                return label.getValue();
                            }
                        }, Comparators.nullsFirst(Comparators.naturalOrder()))
                ));
        set.addAll(convert(testSuite.getLabels(), new Function<ru.yandex.qatools.allure.model.Label, Label>() {
            @Override
            public Label apply(ru.yandex.qatools.allure.model.Label label) {
                return convert(label);
            }
        }));
        set.addAll(convert(source.getLabels(), new Function<ru.yandex.qatools.allure.model.Label, Label>() {
            @Override
            public Label apply(ru.yandex.qatools.allure.model.Label label) {
                return convert(label);
            }
        }));
        dest.setLabels(new ArrayList<>(set));
        StreamSupport.stream(dest.findAllLabels(ISSUE))
                .forEach(new Consumer<String>() {
                             @Override
                             public void accept(String s) {
                                 dest.getLinks().add(getLink(ISSUE, s, getIssueUrl(s, properties)));
                             }
                         }
                );
        dest.findOneLabel("testId")
                .ifPresent(new Consumer<String>() {
                               @Override
                               public void accept(String s) {
                                   dest.getLinks().add(new Link().setName(s).setType("tms")
                                           .setUrl(getTestCaseIdUrl(s, properties)));
                               }
                           }
                );

        //TestNG nested suite
        final Optional<String> testGroupLabel = dest.findOneLabel("testGroup");
        final Optional<String> testSuiteLabel = dest.findOneLabel("testSuite");

        if (testGroupLabel.isPresent() && testSuiteLabel.isPresent()) {
            dest.addLabelIfNotExists(PARENT_SUITE, testSuiteLabel.get());
            dest.addLabelIfNotExists(SUITE, testGroupLabel.get());
            dest.addLabelIfNotExists(SUB_SUITE, testClass);
        } else {
            dest.addLabelIfNotExists(SUITE, suiteName);
        }

        dest.addLabelIfNotExists(TEST_CLASS, testClass);
        dest.addLabelIfNotExists(TEST_METHOD, testMethod);
        dest.addLabelIfNotExists(PACKAGE, testClass);
        StreamSupport.stream(dest.findAllLabels("status_details"))
                .filter(new Predicate<String>() {
                    @Override
                    public boolean test(String s) {
                        return "flaky".equalsIgnoreCase(s);
                    }
                })
                .findAny()
                .ifPresent(new Consumer<String>() {
                    @Override
                    public void accept(String s) {
                        dest.setFlaky(true);
                    }
                });
        dest.addLabelIfNotExists(RESULT_FORMAT, ALLURE1_RESULTS_FORMAT);
        visitor.visitTestResult(dest);
    }

    private <T, R> List<R> convert(final Collection<T> source, final Function<T, R> converter) {
        return convert(source, t -> true, converter);
    }

    private <T, R> List<R> convert(final Collection<T> source,
                                   final Predicate<T> predicate,
                                   final Function<T, R> converter) {
        return Objects.isNull(source) ? null : StreamSupport.stream(source)
                .filter(predicate)
                .map(converter)
                .collect(toList());
    }

    private Step convert(final List<File> source,
                         final ResultsVisitor visitor,
                         final ru.yandex.qatools.allure.model.Step s,
                         final Status testStatus,
                         final String message,
                         final String trace) {
        final Status status = convert(s.getStatus());
        final Step current = new Step()
                .setName(s.getTitle() == null ? s.getName() : s.getTitle())
                .setTime(new Time()
                        .setStart(s.getStart())
                        .setStop(s.getStop())
                        .setDuration(s.getStop() - s.getStart()))
                .setStatus(status)
                .setSteps(convert(s.getSteps(), new Function<ru.yandex.qatools.allure.model.Step, Step>() {
                    @Override
                    public Step apply(ru.yandex.qatools.allure.model.Step step) {
                        return convert(source, visitor, step, testStatus, message, trace);
                    }
                }))
                .setAttachments(convert(s.getAttachments(), new Function<ru.yandex.qatools.allure.model.Attachment, Attachment>() {
                    @Override
                    public Attachment apply(ru.yandex.qatools.allure.model.Attachment attachment) {
                        return convert(source, visitor, attachment);
                    }
                }));
        //Copy test status details to each step set the same status
        if (Objects.equals(status, testStatus)) {
            current.setStatusMessage(message);
            current.setStatusMessage(trace);
        }
        return current;
    }

    private Label convert(final ru.yandex.qatools.allure.model.Label label) {
        return new Label()
                .setName(label.getName())
                .setValue(label.getValue());
    }

    private Parameter convert(final ru.yandex.qatools.allure.model.Parameter parameter) {
        return new Parameter()
                .setName(parameter.getName())
                .setValue(parameter.getValue());
    }

    private Attachment convert(final List<File> fileList,
                               final ResultsVisitor visitor,
                               final ru.yandex.qatools.allure.model.Attachment attachment) {
        final File attachmentFile = getFileFromList(fileList, attachment.getSource());
        if (attachmentFile.isFile() && attachmentFile.exists()) {
            final Attachment found = visitor.visitAttachmentFile(attachmentFile);
            if (Objects.nonNull(attachment.getType())) {
                found.setType(attachment.getType());
            }
            if (Objects.nonNull(attachment.getTitle())) {
                found.setName(attachment.getTitle());
            }
            return found;
        } else {
            visitor.error("Could not find attachment " + attachment.getSource());
            return new Attachment()
                    .setType(attachment.getType())
                    .setName(attachment.getTitle())
                    .setSize(0L);
        }
    }

    @SuppressWarnings("ReturnCount")
    public static Status convert(final ru.yandex.qatools.allure.model.Status status) {
        if (Objects.isNull(status)) {
            return Status.UNKNOWN;
        }
        switch (status) {
            case FAILED:
                return FAILED;
            case BROKEN:
                return BROKEN;
            case PASSED:
                return PASSED;
            case CANCELED:
            case SKIPPED:
            case PENDING:
                return SKIPPED;
            default:
                return Status.UNKNOWN;
        }
    }

    private List<Parameter> getParameters(final TestCaseResult source) {
        final TreeSet<Parameter> parametersSet = new TreeSet<>(
                Comparators.thenComparing(Comparators.comparing(new Function<Parameter, String>() {
                            @Override
                            public String apply(Parameter parameter) {
                                return parameter.getName();
                            }
                        }, Comparators.nullsFirst(Comparators.naturalOrder())),
                        Comparators.comparing(new Function<Parameter, String>() {
                            @Override
                            public String apply(Parameter parameter) {
                                return parameter.getName();
                            }
                        }, Comparators.nullsFirst(Comparators.naturalOrder())))
        );
        parametersSet.addAll(convert(source.getParameters(), new Predicate<ru.yandex.qatools.allure.model.Parameter>() {
            @Override
            public boolean test(ru.yandex.qatools.allure.model.Parameter parameter) {
                return hasArgumentType(parameter);
            }
        }, new Function<ru.yandex.qatools.allure.model.Parameter, Parameter>() {
            @Override
            public Parameter apply(ru.yandex.qatools.allure.model.Parameter parameter) {
                return convert(parameter);
            }
        }));
        return new ArrayList<>(parametersSet);
    }

    private String getDescription(final Description... descriptions) {
        return RefStreams.of(descriptions)
                .filter(new Predicate<Description>() {
                    @Override
                    public boolean test(Description description) {
                        return java8.util.Objects.nonNull(description);
                    }
                })
                .filter(Predicates.negate(isHtmlDescription()))
                .map(new Function<Description, String>() {
                    @Override
                    public String apply(Description description) {
                        return description.getValue();
                    }
                })
                .collect(Collectors.joining("\n\n"));
    }

    private String getDescriptionHtml(final Description... descriptions) {
        return RefStreams.of(descriptions)
                .filter(new Predicate<Description>() {
                    @Override
                    public boolean test(Description description) {
                        return java8.util.Objects.nonNull(description);
                    }
                })
                .filter(isHtmlDescription())
                .map(new Function<Description, String>() {
                    @Override
                    public String apply(Description description) {
                        return description.getValue();
                    }
                })
                .collect(Collectors.joining("</br>"));
    }

    private Predicate<Description> isHtmlDescription() {
        return new Predicate<Description>() {
            @Override
            public boolean test(Description description) {
                return DescriptionType.HTML.equals(description.getType());
            }
        };
    }

    private String findLabelValue(final List<ru.yandex.qatools.allure.model.Label> labels, final String labelName) {
        return StreamSupport.stream(labels)
                .filter(new Predicate<ru.yandex.qatools.allure.model.Label>() {
                    @Override
                    public boolean test(ru.yandex.qatools.allure.model.Label label) {
                        return labelName.equals(label.getName());
                    }
                })
                .map(new Function<ru.yandex.qatools.allure.model.Label, String>() {
                    @Override
                    public String apply(ru.yandex.qatools.allure.model.Label label) {
                        return label.getValue();
                    }
                })
                .findAny()
                .orElse(null);
    }

    private Optional<ru.yandex.qatools.allure.model.Label> findLabel(
            final List<ru.yandex.qatools.allure.model.Label> labels, final String labelName) {
        return StreamSupport.stream(labels)
                .filter(new Predicate<ru.yandex.qatools.allure.model.Label>() {
                    @Override
                    public boolean test(ru.yandex.qatools.allure.model.Label label) {
                        return labelName.equals(label.getName());
                    }
                })
                .findAny();
    }

    private boolean hasArgumentType(final ru.yandex.qatools.allure.model.Parameter parameter) {
        return Objects.isNull(parameter.getKind()) || ParameterKind.ARGUMENT.equals(parameter.getKind());
    }

    private boolean hasEnvType(final ru.yandex.qatools.allure.model.Parameter parameter) {
        return ParameterKind.ENVIRONMENT_VARIABLE.equals(parameter.getKind());
    }

    private Link getLink(final LabelName labelName, final String value, final String url) {
        return new Link().setName(value).setType(labelName.value()).setUrl(url);
    }

    private String getIssueUrl(final String issue, final Properties properties) {
        return String.format(properties.getProperty(ISSUE_URL_PROPERTY, "%s"), issue);
    }

    private String getTestCaseIdUrl(final String testCaseId, final Properties properties) {
        return String.format(properties.getProperty(TMS_LINK_PROPERTY, "%s"), testCaseId);
    }

    private Stream<TestSuiteResult> getStreamOfAllure1Results(final List<File> source) {
        return RefStreams.concat(xmlFiles(source), jsonFiles(source));
    }

    private Stream<TestSuiteResult> xmlFiles(final List<File> source) {
        try {
            return StreamSupport.stream(AllureUtils.listTestSuiteXmlFiles(source))
                    .map(new Function<File, Optional<TestSuiteResult>>() {
                        @Override
                        public Optional<TestSuiteResult> apply(File file) {
                            return readXmlTestSuiteFile(file);
                        }
                    })
                    .filter(new Predicate<Optional<TestSuiteResult>>() {
                        @Override
                        public boolean test(Optional<TestSuiteResult> testSuiteResultOptional) {
                            return testSuiteResultOptional.isPresent();
                        }
                    })
                    .map(new Function<Optional<TestSuiteResult>, TestSuiteResult>() {
                        @Override
                        public TestSuiteResult apply(Optional<TestSuiteResult> testSuiteResultOptional) {
                            return testSuiteResultOptional.get();
                        }
                    });
        } catch (IOException e) {
            return RefStreams.empty();
        }
    }

    private Stream<TestSuiteResult> jsonFiles(final List<File> source) {
        try {
            return StreamSupport.stream(AllureUtils.listTestSuiteJsonFiles(source))
                    .map(new Function<File, Optional<TestSuiteResult>>() {
                        @Override
                        public Optional<TestSuiteResult> apply(File file) {
                            return readJsonTestSuiteFile(file);
                        }
                    })
                    .filter(new Predicate<Optional<TestSuiteResult>>() {
                        @Override
                        public boolean test(Optional<TestSuiteResult> testSuiteResultOptional) {
                            return testSuiteResultOptional.isPresent();
                        }
                    })
                    .map(new Function<Optional<TestSuiteResult>, TestSuiteResult>() {
                        @Override
                        public TestSuiteResult apply(Optional<TestSuiteResult> testSuiteResultOptional) {
                            return testSuiteResultOptional.get();
                        }
                    });
        } catch (IOException e) {
            return RefStreams.empty();
        }
    }

    private Optional<TestSuiteResult> readXmlTestSuiteFile(final File source)  {
        InputStream is = null;
        try {
            is = new FileInputStream(source);
            return Optional.of(xmlMapper.readValue(is, TestSuiteResult.class));
        } catch (IOException e) {

        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return Optional.empty();
    }

    private Optional<TestSuiteResult> readJsonTestSuiteFile(final File source)  {
        InputStream is = null;
        try {
            is = new FileInputStream(source);
            return Optional.of(jsonMapper.readValue(is, TestSuiteResult.class));
        } catch (IOException e) {
            return Optional.empty();
        }finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @SafeVarargs
    private static <T> T firstNonNull(final T... items) {
        return RefStreams.of(items)
                .filter(new Predicate<T>() {
                    @Override
                    public boolean test(T t) {
                        return java8.util.Objects.nonNull(t);
                    }
                })
                .findFirst().get();
    }

    private static String getHistoryId(final String name, final List<Parameter> parameters) {
        final MessageDigest digest = getMessageDigest();
        digest.update(name.getBytes(Charset.forName("UTF-8")));
        StreamSupport.stream(parameters)
                .sorted(PARAMETER_COMPARATOR)
                .forEachOrdered(new Consumer<Parameter>() {
                    @Override
                    public void accept(Parameter parameter) {
                        digest.update(Objects.toString(parameter.getName()).getBytes(Charset.forName("UTF-8")));
                        digest.update(Objects.toString(parameter.getValue()).getBytes(Charset.forName("UTF-8")));
                    }
                });
        final byte[] bytes = digest.digest();
        return new BigInteger(1, bytes).toString(16);
    }

    private static MessageDigest getMessageDigest() {
        try {
            return MessageDigest.getInstance(MD_5);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Could not find md5 hashing algorithm", e);
        }
    }

    private Map<String, String> processEnvironment(final List<File> fileList) {
        final Map<String, String> environment = processEnvironmentProperties(fileList);
        environment.putAll(processEnvironmentXml(fileList));
        return environment;
    }

    private Map<String, String> processEnvironmentProperties(final List<File> fileList) {
        Optional<File> environmentProperties = StreamSupport.stream(fileList).filter(new Predicate<File>() {
            @Override
            public boolean test(File file) {
                return file.getName().equals("environment.properties");
            }
        }).findFirst();
        final Map<String, String> items = new HashMap<>();
        if (environmentProperties.isPresent()) {
            if (environmentProperties.get().exists()) {
                InputStream is = null;
                try {
                    is = new FileInputStream(environmentProperties.get());
                    final Properties properties = new Properties();
                    properties.load(is);
                    Enumeration<String> enums = (Enumeration<String>) properties.propertyNames();
                    while (enums.hasMoreElements()) {
                        String key = enums.nextElement();
                        String value = properties.getProperty(key);
                        items.put(String.valueOf(key), String.valueOf(value));
                    }
                } catch (IOException e) {
                }finally {
                    try {
                        is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return items;
    }

    private Map<String, String> processEnvironmentXml(final List<File> fileList) {
        Optional<File> environmentPropertiesXml = StreamSupport.stream(fileList).filter(new Predicate<File>() {
            @Override
            public boolean test(File file) {
                return file.getName().equals("environment.xml");
            }
        }).findFirst();
        final Map<String, String> items = new HashMap<>();
        if (environmentPropertiesXml.isPresent()) {
            if (environmentPropertiesXml.get().exists()) {
                InputStream fis = null;
                try {
                    fis = new FileInputStream(environmentPropertiesXml.get());
                    StreamSupport.stream(xmlMapper.readValue(fis, ru.yandex.qatools.commons.model.Environment.class)
                            .getParameter())
                            .forEach(new Consumer<ru.yandex.qatools.commons.model.Parameter>() {
                                @Override
                                public void accept(ru.yandex.qatools.commons.model.Parameter parameter) {
                                    items.put(parameter.getKey(), parameter.getValue());
                                }
                            });
                } catch (Exception e) {
                }finally {
                    try {
                        fis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        }
        return items;
    }

    private File getFileFromList(List<File> fileList, String name) {
        File targetFile = null;
        for (File file : fileList) {
            if (file.getName().contains(name)) {
                targetFile = file;
            }
        }
        return targetFile;
    }

}
