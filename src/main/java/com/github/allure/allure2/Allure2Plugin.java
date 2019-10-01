package com.github.allure.allure2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.allure.ParameterComparator;

import com.github.allure.StageResultComparator;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import io.qameta.allure.Reader;
import io.qameta.allure.context.RandomUidContext;
import io.qameta.allure.core.Configuration;
import io.qameta.allure.core.ResultsVisitor;
import io.qameta.allure.entity.Attachment;
import io.qameta.allure.entity.Label;
import io.qameta.allure.entity.Link;
import io.qameta.allure.entity.Parameter;
import io.qameta.allure.entity.StageResult;
import io.qameta.allure.entity.Status;
import io.qameta.allure.entity.Step;
import io.qameta.allure.entity.Time;
import io.qameta.allure.model.Allure2ModelJackson;
import io.qameta.allure.model.FixtureResult;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.model.TestResultContainer;
import java8.util.Comparators;
import java8.util.Objects;
import java8.util.Optional;
import java8.util.function.Consumer;
import java8.util.function.Function;
import java8.util.function.Predicate;
import java8.util.function.Supplier;
import java8.util.stream.Collectors;
import java8.util.stream.RefStreams;
import java8.util.stream.Stream;
import java8.util.stream.StreamSupport;

import static io.qameta.allure.entity.LabelName.RESULT_FORMAT;

import static java8.util.Objects.nonNull;

public class Allure2Plugin implements Reader {
    @SuppressWarnings("WeakerAccess")
    public static final String ALLURE2_RESULTS_FORMAT = "allure2";
    private static final Comparator<StageResult> BY_START = Comparators.comparing(
            new Function<StageResult, Time>() {
                @Override
                public Time apply(StageResult stageResult) {
                    return stageResult.getTime();
                }
            },
            Comparators.nullsFirst(Comparators.comparing(new Function<Time, Long>() {
                @Override
                public Long apply(Time time) {
                    return time.getStart();
                }
            }, Comparators.nullsFirst(Comparators.naturalOrder())))
    );

    private final ObjectMapper mapper;

    public Allure2Plugin() {
        mapper = Allure2ModelJackson.createMapper()
                .addMixIn(TestResultContainer.class, TestContainerIgnoreConflictsMixin.class);
    }

    @Override
    public void readResults(Configuration configuration, ResultsVisitor visitor, List<File> fileList) {
        final RandomUidContext context = configuration.getContext(RandomUidContext.class);
        final List<TestResultContainer> groups = readTestResultsContainers(fileList)
                .collect(Collectors.toList());
        readTestResults(fileList).forEach(new Consumer<TestResult>() {
            @Override
            public void accept(TestResult testResult) {
                convert(context.getValue(), fileList, visitor, groups, testResult);
            }
        });
    }

    private void convert(final Supplier<String> uidGenerator,
                         final List<File> fileList,
                         final ResultsVisitor visitor,
                         final List<TestResultContainer> groups, final TestResult result) {
        final io.qameta.allure.entity.TestResult dest = new io.qameta.allure.entity.TestResult();
        dest.setUid(uidGenerator.get());
        dest.setHistoryId(result.getHistoryId());
        dest.setFullName(result.getFullName());
        dest.setName(firstNonNull(result.getName(), result.getFullName(), "Unknown test"));
        dest.setTime(Time.create(result.getStart(), result.getStop()));
        dest.setDescription(result.getDescription());
        dest.setDescriptionHtml(result.getDescriptionHtml());
        dest.setStatus(convert(result.getStatus()));
        Optional.ofNullable(result.getStatusDetails()).ifPresent(new Consumer<StatusDetails>() {
            @Override
            public void accept(StatusDetails statusDetails) {
                dest.setStatusMessage(statusDetails.getMessage());
                dest.setStatusTrace(statusDetails.getTrace());
            }
        });
        dest.setLinks(convert(result.getLinks(), new Function<io.qameta.allure.model.Link, Link>() {
            @Override
            public Link apply(io.qameta.allure.model.Link t) {
                return convert(t);
            }
        }));
        dest.setLabels(convert(result.getLabels(), new Function<io.qameta.allure.model.Label, Label>() {
            @Override
            public Label apply(io.qameta.allure.model.Label t) {
                return convert(t);
            }
        }));
        dest.setParameters(getParameters(result));
        dest.addLabelIfNotExists(RESULT_FORMAT, ALLURE2_RESULTS_FORMAT);


        if (hasTestStage(result)) {
            dest.setTestStage(getTestStage(fileList, visitor, result));
        }

        final List<TestResultContainer> parents = findAllParents(groups, result.getUuid(), new HashSet<>());
        dest.getBeforeStages().addAll(getStages(parents, new Function<TestResultContainer, Stream<StageResult>>() {
            @Override
            public Stream<StageResult> apply(TestResultContainer testResultContainer) {
                return getBefore(fileList, visitor, testResultContainer);
            }
        }));
        dest.getAfterStages().addAll(getStages(parents, new Function<TestResultContainer, Stream<StageResult>>() {
            @Override
            public Stream<StageResult> apply(TestResultContainer testResultContainer) {
                return getAfter(fileList, visitor, testResultContainer);
            }
        }));
        visitor.visitTestResult(dest);
    }

    private Status convert(final io.qameta.allure.model.Status status) {
        if (Objects.isNull(status)) {
            return Status.UNKNOWN;
        }
        return RefStreams.of(Status.values())
                .filter(new Predicate<Status>() {
                    @Override
                    public boolean test(Status item) {
                        return item.value().equalsIgnoreCase(status.value());
                    }
                })
                .findAny()
                .orElse(Status.UNKNOWN);
    }

    private <T, R> List<R> convert(final List<T> source, final Function<T, R> converter) {
        return Objects.isNull(source) ? Collections.emptyList() : StreamSupport.stream(source)
                .map(converter)
                .collect(Collectors.toList());
    }

    private Link convert(final io.qameta.allure.model.Link link) {
        return new Link()
                .setName(link.getName())
                .setType(link.getType())
                .setUrl(link.getUrl());
    }

    private Label convert(final io.qameta.allure.model.Label label) {
        return new Label()
                .setName(label.getName())
                .setValue(label.getValue());
    }

    private Parameter convert(final io.qameta.allure.model.Parameter parameter) {
        return new Parameter()
                .setName(parameter.getName())
                .setValue(parameter.getValue());
    }

    private Time convert(final Long start, final Long stop) {
        return new Time()
                .setStart(start)
                .setStop(stop)
                .setDuration(nonNull(start) && nonNull(stop) ? stop - start : null);
    }

    private boolean hasTestStage(final TestResult result) {
        return !result.getSteps().isEmpty() || !result.getAttachments().isEmpty();
    }

    private List<Parameter> getParameters(final TestResult result) {
        final TreeSet<Parameter> parametersSet = new TreeSet<Parameter>(new ParameterComparator());
//                Comparators.comparing(new Function<Parameter, String>() {
//                    @Override
//                    public String apply(Parameter parameter) {
//                        return parameter.getName();
//                    }
//                },Comparators.nullsFirst(Comparators.naturalOrder()))
//                        .thenComparing(new java.util.function.Function<Parameter, String>() {
//                            @Override
//                            public String apply(Parameter parameter) {
//                                return parameter.getValue();
//                            }
//                        }, Comparators.nullsFirst(Comparators.naturalOrder()))
//        );
        parametersSet.addAll(convert(result.getParameters(), new Function<io.qameta.allure.model.Parameter, Parameter>() {
            @Override
            public Parameter apply(io.qameta.allure.model.Parameter parameter) {
                return convert(parameter);
            }
        }));
        return new ArrayList<>(parametersSet);
    }

    private StageResult getTestStage(final List<File> fileList,
                                     final ResultsVisitor visitor,
                                     final TestResult result) {
        final StageResult testStage = new StageResult();
        testStage.setSteps(convert(result.getSteps(), new Function<StepResult, Step>() {
            @Override
            public Step apply(StepResult stepResult) {
                return convert(fileList, visitor, stepResult);
            }
        }));
        testStage.setAttachments(convert(result.getAttachments(), new Function<io.qameta.allure.model.Attachment, Attachment>() {
            @Override
            public Attachment apply(io.qameta.allure.model.Attachment attachment) {
                return convert(fileList, visitor, attachment);
            }
        }));
        testStage.setStatus(convert(result.getStatus()));
        testStage.setDescription(result.getDescription());
        testStage.setDescriptionHtml(result.getDescriptionHtml());
        Optional.of(result)
                .map(new Function<TestResult, StatusDetails>() {
                    @Override
                    public StatusDetails apply(TestResult testResult) {
                        return testResult.getStatusDetails();
                    }
                })
                .ifPresent(new Consumer<StatusDetails>() {
                    @Override
                    public void accept(StatusDetails statusDetails) {
                        testStage.setStatusMessage(statusDetails.getMessage());
                        testStage.setStatusTrace(statusDetails.getTrace());
                    }
                });
        return testStage;
    }

    private Step convert(final List<File> fileList,
                         final ResultsVisitor visitor,
                         final StepResult step) {
        final Step result = new Step()
                .setName(step.getName())
                .setStatus(convert(step.getStatus()))
                .setTime(convert(step.getStart(), step.getStop()))
                .setParameters(convert(step.getParameters(), new Function<io.qameta.allure.model.Parameter, Parameter>() {
                    @Override
                    public Parameter apply(io.qameta.allure.model.Parameter parameter) {
                        return convert(parameter);
                    }
                }))
                .setAttachments(convert(step.getAttachments(), new Function<io.qameta.allure.model.Attachment, Attachment>() {
                    @Override
                    public Attachment apply(io.qameta.allure.model.Attachment attachment) {
                        return convert(fileList, visitor, attachment);
                    }
                }))
                .setSteps(convert(step.getSteps(), new Function<StepResult, Step>() {
                    @Override
                    public Step apply(StepResult stepResult) {
                        return convert(fileList, visitor, stepResult);
                    }
                }));
        Optional.of(step)
                .map(new Function<StepResult, StatusDetails>() {
                    @Override
                    public StatusDetails apply(StepResult stepResult) {
                        return stepResult.getStatusDetails();
                    }
                })
                .ifPresent(new Consumer<StatusDetails>() {
                    @Override
                    public void accept(StatusDetails statusDetails) {
                        result.setStatusMessage(statusDetails.getMessage());
                        result.setStatusTrace(statusDetails.getTrace());
                    }
                });
        return result;
    }

    private Attachment convert(final List<File> fileList,
                               final ResultsVisitor visitor,
                               final io.qameta.allure.model.Attachment attachment) {
        final File attachmentFile = getFileFromList(fileList, attachment.getSource());
        if (attachmentFile.isFile() && attachmentFile.exists()) {
            final Attachment found = visitor.visitAttachmentFile(attachmentFile);
            if (nonNull(attachment.getType())) {
                found.setType(attachment.getType());
            }
            if (nonNull(attachment.getName())) {
                found.setName(attachment.getName());
            }
            return found;
        } else {
            visitor.error("Could not find attachment " + attachment.getSource() + " in directory " + fileList);
            return new Attachment()
                    .setType(attachment.getType())
                    .setName(attachment.getName())
                    .setSize(0L);
        }
    }

    private StageResult convert(final List<File> fileList,
                                final ResultsVisitor visitor,
                                final FixtureResult result) {
        final StageResult stageResult = new StageResult()
                .setName(result.getName())
                .setTime(convert(result.getStart(), result.getStop()))
                .setStatus(convert(result.getStatus()))
                .setSteps(convert(result.getSteps(), new Function<StepResult, Step>() {
                    @Override
                    public Step apply(StepResult stepResult) {
                        return convert(fileList, visitor, stepResult);
                    }
                }))
                .setDescription(result.getDescription())
                .setDescriptionHtml(result.getDescriptionHtml())
                .setAttachments(convert(result.getAttachments(), new Function<io.qameta.allure.model.Attachment, Attachment>() {
                    @Override
                    public Attachment apply(io.qameta.allure.model.Attachment attachment) {
                        return convert(fileList, visitor, attachment);
                    }
                }))
                .setParameters(convert(result.getParameters(), new Function<io.qameta.allure.model.Parameter, Parameter>() {
                    @Override
                    public Parameter apply(io.qameta.allure.model.Parameter parameter) {
                        return convert(parameter);
                    }
                }));
        Optional.of(result)
                .map(new Function<FixtureResult, StatusDetails>() {
                    @Override
                    public StatusDetails apply(FixtureResult fixtureResult) {
                        return fixtureResult.getStatusDetails();
                    }
                })
                .ifPresent(new Consumer<StatusDetails>() {
                    @Override
                    public void accept(StatusDetails statusDetails) {
                        stageResult.setStatusMessage(statusDetails.getMessage());
                        stageResult.setStatusTrace(statusDetails.getTrace());
                    }
                });

        return stageResult;
    }

    public static Stream<File> listFiles(List<File> fileList, String glob) {
        List<File> list = new ArrayList<>();
        for (File file : fileList) {
            if (file.getName().endsWith(glob)) {
                    list.add(file);
            }
        }
        return StreamSupport.stream(list);
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

    private Stream<TestResultContainer> readTestResultsContainers(final List<File> fileList) {
        return listFiles(fileList, "-container.json")
                .map(new Function<File, Optional<TestResultContainer>>() {
                    @Override
                    public Optional<TestResultContainer> apply(File file) {
                        return readTestResultContainer(file);
                    }
                })
                .filter(new Predicate<Optional<TestResultContainer>>() {
                    @Override
                    public boolean test(Optional<TestResultContainer> testResultContainerOptional) {
                        return testResultContainerOptional.isPresent();
                    }
                })
                .map(new Function<Optional<TestResultContainer>, TestResultContainer>() {
                    @Override
                    public TestResultContainer apply(Optional<TestResultContainer> testResultContainerOptional) {
                        return testResultContainerOptional.get();
                    }
                });
    }

    private Optional<TestResultContainer> readTestResultContainer(final File file) {
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            return Optional.ofNullable(mapper.readValue(inputStream, TestResultContainer.class));
        } catch (IOException e) {
            return Optional.empty();
        }finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Stream<TestResult> readTestResults(final List<File> fileList) {
        return listFiles(fileList, "-result.json")
                .map(new Function<File, Optional<TestResult>>() {
                    @Override
                    public Optional<TestResult> apply(File file) {
                        return readTestResult(file);
                    }
                })
                .filter(new Predicate<Optional<TestResult>>() {
                    @Override
                    public boolean test(Optional<TestResult> testResultOptional) {
                        return testResultOptional.isPresent();
                    }
                })
                .map(new Function<Optional<TestResult>, TestResult>() {
                    @Override
                    public TestResult apply(Optional<TestResult> testResultOptional) {
                        return testResultOptional.get();
                    }
                });
    }

    private Optional<TestResult> readTestResult(final File file) {
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            return Optional.ofNullable(mapper.readValue(inputStream, TestResult.class));
        } catch (IOException e) {
            return Optional.empty();
        }finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private List<TestResultContainer> findAllParents(final List<TestResultContainer> groups,
                                                     final String id,
                                                     final Set<String> seen) {
        final List<TestResultContainer> parents = findParents(groups, id, seen);
        final List<TestResultContainer> result = new ArrayList<>(parents);
        for (TestResultContainer container : parents) {
            result.addAll(findAllParents(groups, container.getUuid(), seen));
        }
        return result;
    }

    private List<TestResultContainer> findParents(final List<TestResultContainer> groups,
                                                  final String id,
                                                  final Set<String> seen) {
        return StreamSupport.stream(groups)
                .filter(new Predicate<TestResultContainer>() {
                    @Override
                    public boolean test(TestResultContainer testResultContainer) {
                        return testResultContainer.getChildren().contains(id);
                    }
                })
                .filter(new Predicate<TestResultContainer>() {
                    @Override
                    public boolean test(TestResultContainer testResultContainer) {
                        return !seen.contains(testResultContainer.getUuid());
                    }
                })
                .collect(Collectors.toList());
    }

    private List<StageResult> getStages(final List<TestResultContainer> parents,
                                        final Function<TestResultContainer, Stream<StageResult>> getter) {
        return StreamSupport.stream(parents)
                .flatMap(getter)
                .collect(Collectors.toList());
    }

    private Stream<StageResult> getBefore(final List<File> fileList,
                                          final ResultsVisitor visitor,
                                          final TestResultContainer container) {
        return StreamSupport.stream(convert(container.getBefores(), new Function<FixtureResult, StageResult>() {
            @Override
            public StageResult apply(FixtureResult fixtureResult) {
                return convert(fileList, visitor, fixtureResult);
            }
        })).sorted(BY_START);
    }

    private Stream<StageResult> getAfter(final List<File> fileList,
                                         final ResultsVisitor visitor,
                                         final TestResultContainer container) {
        return StreamSupport.stream(convert(container.getAfters(), new Function<FixtureResult, StageResult>() {
            @Override
            public StageResult apply(FixtureResult fixtureResult) {
                return convert(fileList, visitor, fixtureResult);
            }
        })).sorted(BY_START);
    }


    @SafeVarargs
    private static <T> T firstNonNull(final T... items) {
        return RefStreams.of(items)
                .filter(new Predicate<T>() {
                    @Override
                    public boolean test(T t) {
                        return Objects.nonNull(t);
                    }
                })
                .findFirst()
                .orElseThrow();
    }


}
