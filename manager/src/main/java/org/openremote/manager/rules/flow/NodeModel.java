package org.openremote.manager.rules.flow;

import org.openremote.manager.rules.RulesBuilder;
import org.openremote.model.attribute.AttributeInfo;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.datapoint.ValueDatapoint;
import org.openremote.model.datapoint.query.AssetDatapointNearestQuery;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.rules.flow.*;
import org.openremote.model.util.ValueUtil;
import org.openremote.model.value.ValueHolder;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.IntStream;

public enum NodeModel {
    READ_ATTRIBUTE(
            new Node(NodeType.INPUT, new NodeInternal[]{
                    new NodeInternal("Attribute", new Picker(PickerType.ASSET_ATTRIBUTE))
            },
                    new NodeSocket[0], new NodeSocket[]{
                    new NodeSocket("value", NodeDataType.ANY)
            }),
            info -> {
                AttributeInternalValue assetAttributePair = ValueUtil.JSON.convertValue(info.getInternals()[0].getValue(), AttributeInternalValue.class);
                String assetId = assetAttributePair.getAssetId();
                String attributeName = assetAttributePair.getAttributeName();
                AttributeRef attributeRef = new AttributeRef(assetId, attributeName);
                Optional<AttributeInfo> readValue = info.getFacts().findCachedAttribute(attributeRef)
                        .or(() -> info.getFacts().matchFirstAssetState(new AssetQuery().ids(assetId).attributeName(attributeName)));
                Object value = readValue.flatMap(ValueHolder::getValue).orElse(null);
                return new NodeExecutionResult(value, attributeRef, readValue.orElse(null));
            },
            params -> {
                AttributeInternalValue internal = ValueUtil.JSON.convertValue(params.getNode().getInternals()[0].getValue(), AttributeInternalValue.class);
                String assetId = internal.getAssetId();
                String attributeName = internal.getAttributeName();
                List<AttributeInfo> allAssets = params.getFacts().matchAssetState(new AssetQuery().ids(assetId).attributeName(attributeName)
                ).toList();

                return allAssets.stream().anyMatch(state -> {
                    long timestamp = state.getTimestamp();
                    long triggerStamp = params.getBuilder().getTriggerMap().getOrDefault(params.getRuleName(), -1L);
                    if (triggerStamp == -1L) return true; //The flow has never been executed
                    return timestamp > triggerStamp && !Objects.equals(state.getValue().orElse(null), state.getOldValue().orElse(null));
                });
            }
    ),

    DERIVATIVE(
            new Node(NodeType.INPUT, new NodeInternal[]{
                    new NodeInternal("Attribute", new Picker(PickerType.ASSET_ATTRIBUTE))
            },
                    new NodeSocket[0], new NodeSocket[]{
                    new NodeSocket("value", NodeDataType.NUMBER)
            }),
            info -> {
                AttributeInternalValue assetAttributePair = ValueUtil.JSON.convertValue(info.getInternals()[0].getValue(), AttributeInternalValue.class);
                String assetId = assetAttributePair.getAssetId();
                String attributeName = assetAttributePair.getAttributeName();
                AttributeRef attributeRef = new AttributeRef(assetId, attributeName);
                Optional<AttributeInfo> attr = info.getFacts().findCachedAttribute(attributeRef)
                        .or(() -> info.getFacts().matchFirstAssetState(new AssetQuery().ids(assetId).attributeName(attributeName)));
                if (attr.isPresent()) {
                    AttributeInfo attributeInfo = attr.get();
                    Optional<Object> currentValueOpt = attributeInfo.getValue();
                    Optional<Object> oldValueOpt = attributeInfo.getOldValue();
                    long currentTimestamp = attributeInfo.getTimestamp();
                    long oldTimestamp = attributeInfo.getOldValueTimestamp();

                    if (currentValueOpt.isPresent() && oldValueOpt.isPresent()) {
                        double currentValue = ((Number) currentValueOpt.get()).doubleValue();
                        double oldValue = ((Number) oldValueOpt.get()).doubleValue();
                        double timeDifference = (currentTimestamp - oldTimestamp) / 1000.0; // Convert milliseconds to seconds

                        if (timeDifference != 0) {
                            double result = (currentValue - oldValue) / timeDifference; // Δx formula
                            return new NodeExecutionResult(result, attributeRef, attributeInfo);
                        }
                    }
                }

                return new NodeExecutionResult(null);
            },
            params -> {
                AttributeInternalValue internal = ValueUtil.JSON.convertValue(params.getNode().getInternals()[0].getValue(), AttributeInternalValue.class);
                String assetId = internal.getAssetId();
                String attributeName = internal.getAttributeName();
                Optional<AttributeInfo> x = params.getFacts().getAssetStates().stream()
                        .filter(assetState ->
                                Objects.equals(assetState.getId(), assetId)
                                && Objects.equals(assetState.getName(), attributeName))
                        .findFirst();

                List<AttributeInfo> allAssets = params.getFacts().matchAssetState(new AssetQuery().ids(assetId).attributeName(attributeName)
                ).toList();

                return allAssets.stream().anyMatch(state -> {
                    long timestamp = state.getTimestamp();
                    long triggerStamp = params.getBuilder().getTriggerMap().getOrDefault(params.getRuleName(), -1L);
                    if (triggerStamp == -1L) return true; //The flow has never been executed
                    return timestamp > triggerStamp && !Objects.equals(state.getValue().orElse(null), state.getOldValue().orElse(null));
                });
            }
    ),
    INTEGRAL(
            new Node(NodeType.INPUT, new NodeInternal[]{
                    new NodeInternal("Attribute", new Picker(PickerType.ASSET_ATTRIBUTE))
            },
                    new NodeSocket[0], new NodeSocket[]{
                    new NodeSocket("value", NodeDataType.NUMBER)
            }),
            info -> {
                AttributeInternalValue assetAttributePair = ValueUtil.JSON.convertValue(info.getInternals()[0].getValue(), AttributeInternalValue.class);
                String assetId = assetAttributePair.getAssetId();
                String attributeName = assetAttributePair.getAttributeName();
                AttributeRef attributeRef = new AttributeRef(assetId, attributeName);
                Optional<AttributeInfo> attr = info.getFacts().findCachedAttribute(attributeRef)
                        .or(() -> info.getFacts().matchFirstAssetState(new AssetQuery().ids(assetId).attributeName(attributeName)));

                double integral = 0.0;
                if (attr.isPresent()) {
                    AttributeInfo attributeInfo = attr.get();
                    Optional<Object> currentValueOpt = attributeInfo.getValue();
                    Optional<Object> oldValueOpt = attributeInfo.getOldValue();
                    long currentTimestamp = attributeInfo.getTimestamp();
                    long oldTimestamp = attributeInfo.getOldValueTimestamp();

                    if (currentValueOpt.isPresent() && oldValueOpt.isPresent()) {
                        double currentValue = ((Number) currentValueOpt.get()).doubleValue();
                        double previousValue = ((Number) oldValueOpt.get()).doubleValue();
                        double timeDifference = (currentTimestamp - oldTimestamp) / 1000.0; // Convert milliseconds to seconds

                        integral += ((currentValue + previousValue) * timeDifference)  / 2;
                    }
                }

                return new NodeExecutionResult(integral, attributeRef, attr.orElse(null));
            },
            params -> {
                AttributeInternalValue internal = ValueUtil.JSON.convertValue(params.getNode().getInternals()[0].getValue(), AttributeInternalValue.class);
                String assetId = internal.getAssetId();
                String attributeName = internal.getAttributeName();
                List<AttributeInfo> allAssets = params.getFacts().matchAssetState(new AssetQuery().ids(assetId).attributeName(attributeName)).toList();

                return allAssets.stream().anyMatch(state -> {
                    long timestamp = state.getTimestamp();
                    long triggerStamp = params.getBuilder().getTriggerMap().getOrDefault(params.getRuleName(), -1L);
                    if (triggerStamp == -1L) return true; // The flow has never been executed
                    return timestamp > triggerStamp && !Objects.equals(state.getValue().orElse(null), state.getOldValue().orElse(null));
                });
            }
    ),
    HISTORIC_VALUE(
            new Node(NodeType.INPUT, new NodeInternal[]
                    {
                        new NodeInternal("attribute", new Picker(PickerType.ASSET_ATTRIBUTE), NodeInternal.BreakType.NEW_LINE),
                        new NodeInternal("time_period", new Picker(PickerType.DATE),  NodeInternal.BreakType.SPACER),
                        new NodeInternal("time_unit", new Picker(PickerType.DROPDOWN, Arrays.stream(TimePeriod.values()).map(it -> new Option(it.label, it.name())).toArray(Option[]::new)), NodeInternal.BreakType.SPACER)
                    },
                    new NodeSocket[0],
                    new NodeSocket[]{
                        new NodeSocket("value", NodeDataType.ANY)
                    }
            ),
            info -> {
                AttributeInternalValue assetAttributePair = ValueUtil.JSON.convertValue(info.getInternals()[0].getValue(), AttributeInternalValue.class);
                AttributeRef ref = new AttributeRef(assetAttributePair.getAssetId(), assetAttributePair.getAttributeName());
                Number timePeriod;
                Number timeUnit;
                timePeriod = TimePeriod.valueOf(info.getInternals()[2].getValue().toString()).getMillis();
                timeUnit = Long.parseLong(info.getInternals()[1].getValue().toString());
                if(timePeriod == null) return new NodeExecutionResult(null);

                long currentMillis = info.getFacts().getClock().getCurrentTimeMillis();

                Instant pastInstant = Instant.ofEpochMilli(currentMillis-(timePeriod.longValue()*timeUnit.longValue()));

                final ValueDatapoint<?>[] valueDatapoints = info.getHistoricDatapoints().getValueDatapoints(ref, new AssetDatapointNearestQuery(pastInstant.toEpochMilli()));
                if (valueDatapoints.length == 0) return new NodeExecutionResult(null);
                return new NodeExecutionResult(valueDatapoints[0].getValue());
            },
            params -> {
                AttributeInternalValue internal = ValueUtil.JSON.convertValue(params.getNode().getInternals()[0].getValue(), AttributeInternalValue.class);
                String assetId = internal.getAssetId();
                String attributeName = internal.getAttributeName();
                List<AttributeInfo> allAssets = params.getFacts().matchAssetState(new AssetQuery().ids(assetId).attributeName(attributeName)
                ).toList();

                return allAssets.stream().anyMatch(state -> {
                    long timestamp = state.getTimestamp();
                    long triggerStamp = params.getBuilder().getTriggerMap().getOrDefault(params.getRuleName(), -1L);
                    if (triggerStamp == -1L) return true; //The flow has never been executed
                    return timestamp > triggerStamp && !Objects.equals(state.getValue().orElse(null), state.getOldValue().orElse(null));
                });
            }
    ),

    LOG_OUTPUT(new Node(NodeType.OUTPUT, new NodeInternal[0], new NodeSocket[]{
            new NodeSocket("value", NodeDataType.ANY)
    }, new NodeSocket[0]),
            info -> new NodeExecutionResult((RulesBuilder.Action) facts -> {
                info.setFacts(facts);
                Object obj = info.getValueFromInput(0);
                info.LOG.log(Level.INFO, ValueUtil.asJSON(obj).orElseGet(() -> "Couldn't parse JSON"));
            })),

    WRITE_ATTRIBUTE(new Node(NodeType.OUTPUT, new NodeInternal[]{
            new NodeInternal("Attribute", new Picker(PickerType.ASSET_ATTRIBUTE))
    }, new NodeSocket[]{
            new NodeSocket("value", NodeDataType.ANY)
    }, new NodeSocket[0]),
            info -> new NodeExecutionResult((RulesBuilder.Action) facts -> {
                info.setFacts(facts);
                Object value = info.getValueFromInput(0);
                if (value == null) {
                    return;
                }
                AttributeInternalValue assetAttributePair = ValueUtil.JSON.convertValue(info.getInternals()[0].getValue(), AttributeInternalValue.class);
                Optional<AttributeInfo> existingValue = info.getFacts().matchFirstAssetState(new AssetQuery().ids(assetAttributePair.getAssetId()).attributeName(assetAttributePair.getAttributeName()));

                if (existingValue.isPresent())
                    if (existingValue.get().getValue().isPresent())
                        if (existingValue.get().getValue().get().equals(value)) return;

                info.getAssets().dispatch(
                    assetAttributePair.getAssetId(),
                    assetAttributePair.getAttributeName(),
                    value);
            })),

    BOOLEAN_INPUT(new Node(NodeType.INPUT, new NodeInternal[]{
            new NodeInternal("value", new Picker(PickerType.CHECKBOX))
    }, new NodeSocket[0], new NodeSocket[]{
            new NodeSocket("value", NodeDataType.BOOLEAN)
    }),
            info -> {
                Object value = info.getInternals()[0].getValue();
                if (!(value instanceof Boolean)) return new NodeExecutionResult(false);
                return new NodeExecutionResult(value);
            }),

    AND_GATE(new Node(NodeType.PROCESSOR, "&", new NodeInternal[0], new NodeSocket[]{
            new NodeSocket("a", NodeDataType.BOOLEAN),
            new NodeSocket("b", NodeDataType.BOOLEAN),
    }, new NodeSocket[]{
            new NodeSocket("c", NodeDataType.BOOLEAN),
    }),
            info -> {
                boolean a = (boolean) info.getValueFromInput(0);
                boolean b = (boolean) info.getValueFromInput(1);
                return new NodeExecutionResult(a && b);
            }),

    OR_GATE(new Node(NodeType.PROCESSOR, "∥", new NodeInternal[0], new NodeSocket[]{
            new NodeSocket("a", NodeDataType.BOOLEAN),
            new NodeSocket("b", NodeDataType.BOOLEAN),
    }, new NodeSocket[]{
            new NodeSocket("c", NodeDataType.BOOLEAN),
    }),
            info -> {
                boolean a = (boolean) info.getValueFromInput(0);
                boolean b = (boolean) info.getValueFromInput(1);
                return new NodeExecutionResult(a || b);
            }),

    NOT_GATE(new Node(NodeType.PROCESSOR, "!", new NodeInternal[0], new NodeSocket[]{
            new NodeSocket("i", NodeDataType.BOOLEAN),
    }, new NodeSocket[]{
            new NodeSocket("o", NodeDataType.BOOLEAN),
    }),
            info -> {
                boolean a = (boolean) info.getValueFromInput(0);
                return new NodeExecutionResult(!a);
            }),

    NUMBER_INPUT(new Node(NodeType.INPUT, new NodeInternal[]{
            new NodeInternal("value", new Picker(PickerType.NUMBER))
    }, new NodeSocket[0], new NodeSocket[]{
            new NodeSocket("value", NodeDataType.NUMBER)
    }),
            info -> new NodeExecutionResult(ValueUtil.convert(info.getInternals()[0].getValue(), Double.class))),

    ADD_OPERATOR(new Node(NodeType.PROCESSOR, 1, "+", new NodeInternal[0], new NodeSocket[]{
            new NodeSocket("a", NodeDataType.NUMBER),
            new NodeSocket("b", NodeDataType.NUMBER),
    }, new NodeSocket[]{
            new NodeSocket("c", NodeDataType.NUMBER),
    }),
            info -> {
                Number a = (Number) info.getValueFromInput(0);
                Number b = (Number) info.getValueFromInput(1);
                return new NodeExecutionResult(a != null && b != null ? a.doubleValue() + b.doubleValue() : null);
            }),

    SUBTRACT_OPERATOR(new Node(NodeType.PROCESSOR, 2, "-", new NodeInternal[0], new NodeSocket[]{
            new NodeSocket("a", NodeDataType.NUMBER),
            new NodeSocket("b", NodeDataType.NUMBER),
    }, new NodeSocket[]{
            new NodeSocket("c", NodeDataType.NUMBER),
    }),
            info -> {
                Number a = (Number) info.getValueFromInput(0);
                Number b = (Number) info.getValueFromInput(1);
                return new NodeExecutionResult(a != null && b != null ? a.doubleValue() - b.doubleValue() : null);
            }),

    MULTIPLY_OPERATOR(new Node(NodeType.PROCESSOR, 3, "×", new NodeInternal[0], new NodeSocket[]{
            new NodeSocket("a", NodeDataType.NUMBER),
            new NodeSocket("b", NodeDataType.NUMBER),
    }, new NodeSocket[]{
            new NodeSocket("c", NodeDataType.NUMBER),
    }),
            info -> {
                Number a = (Number) info.getValueFromInput(0);
                Number b = (Number) info.getValueFromInput(1);
                return new NodeExecutionResult(a != null && b != null ? a.doubleValue() * b.doubleValue() : null);
            }),

    DIVIDE_OPERATOR(new Node(NodeType.PROCESSOR,4, "÷", new NodeInternal[0], new NodeSocket[]{
            new NodeSocket("a", NodeDataType.NUMBER),
            new NodeSocket("b", NodeDataType.NUMBER),
    }, new NodeSocket[]{
            new NodeSocket("c", NodeDataType.NUMBER),
    }),
            info -> {
                Number a = (Number) info.getValueFromInput(0);
                Number b = (Number) info.getValueFromInput(1);

                if (a == null || b == null) {
                    return new NodeExecutionResult(null);
                }

                if (b.doubleValue() == 0d)
                    return new NodeExecutionResult(0d);

                return new NodeExecutionResult(a.doubleValue() / b.doubleValue());
            }),

    EQUALS_COMPARATOR(new Node(NodeType.PROCESSOR, "=", new NodeInternal[0], new NodeSocket[]{
            new NodeSocket("a", NodeDataType.ANY),
            new NodeSocket("b", NodeDataType.ANY),
    }, new NodeSocket[]{
            new NodeSocket("c", NodeDataType.BOOLEAN),
    }),
            info -> {
                Object a = info.getValueFromInput(0);
                Object b = info.getValueFromInput(1);
                return new NodeExecutionResult(a != null && b != null && Objects.equals(a, b));
            }),

    SUM_PROCESSOR(new Node(NodeType.PROCESSOR,5, "Σ", new NodeInternal[0], new NodeSocket[]{
            new NodeSocket("a", NodeDataType.NUMBER_ARRAY)
    }, new NodeSocket[]{
            new NodeSocket("b", NodeDataType.NUMBER),
    }),
            info -> new NodeExecutionResult(IntStream.range(0, info.getInputs().length)
                    .mapToObj(info::getValueFromInput).mapToDouble(value -> ((Number) value).doubleValue()).sum())
    ),
    MAX_PROCESSOR(new Node(NodeType.PROCESSOR, "max", new NodeInternal[0], new NodeSocket[]{
            new NodeSocket("a", NodeDataType.NUMBER_ARRAY)
    }, new NodeSocket[]{
            new NodeSocket("b", NodeDataType.NUMBER),
    }),
            info -> new NodeExecutionResult(IntStream.range(0, info.getInputs().length)
                        .mapToObj(info::getValueFromInput).mapToDouble(value -> ((Number) value).doubleValue()).max().orElseThrow())
    ),
    MIN_PROCESSOR(new Node(NodeType.PROCESSOR, "min", new NodeInternal[0], new NodeSocket[]{
            new NodeSocket("a", NodeDataType.NUMBER_ARRAY)
    }, new NodeSocket[]{
            new NodeSocket("b", NodeDataType.NUMBER),
    }),
            info -> new NodeExecutionResult(IntStream.range(0, info.getInputs().length)
                    .mapToObj(info::getValueFromInput).mapToDouble(value -> ((Number) value).doubleValue()).min().orElseThrow())
    ),
    AVERAGE_PROCESSOR(new Node(NodeType.PROCESSOR, "avg", new NodeInternal[0], new NodeSocket[]{
            new NodeSocket("a", NodeDataType.NUMBER_ARRAY)
    }, new NodeSocket[]{
            new NodeSocket("b", NodeDataType.NUMBER),
    }),
            info -> new NodeExecutionResult(IntStream.range(0, info.getInputs().length)
                    .mapToObj(info::getValueFromInput).mapToDouble(value -> ((Number) value).doubleValue()).average().orElseThrow())
    ),
    MEDIAN_PROCESSOR(new Node(NodeType.PROCESSOR, "med", new NodeInternal[0], new NodeSocket[]{
            new NodeSocket("a", NodeDataType.NUMBER_ARRAY)
    }, new NodeSocket[]{
            new NodeSocket("b", NodeDataType.NUMBER),
    }),
            info -> {
                final double[] sortedDoubles = IntStream.range(0, info.getInputs().length)
                        .mapToObj(info::getValueFromInput).mapToDouble(value -> ((Number) value).doubleValue()).sorted().toArray();
                if (sortedDoubles.length == 0) {
                    return new NodeExecutionResult(0.0);
                } else if (sortedDoubles.length % 2 == 0) {
                    return new NodeExecutionResult((sortedDoubles[sortedDoubles.length / 2 - 1] + sortedDoubles[sortedDoubles.length / 2]) / 2.0);
                } else {
                    return new NodeExecutionResult(sortedDoubles[sortedDoubles.length / 2]);
                }
            }
    ),
    GREATER_THAN(new Node(NodeType.PROCESSOR, ">", new NodeInternal[0], new NodeSocket[]{
            new NodeSocket("a", NodeDataType.NUMBER),
            new NodeSocket("b", NodeDataType.NUMBER),
    }, new NodeSocket[]{
            new NodeSocket("c", NodeDataType.BOOLEAN),
    }),
            info -> {
                Number a = (Number) info.getValueFromInput(0);
                Number b = (Number) info.getValueFromInput(1);
                return new NodeExecutionResult(a != null && b != null && a.doubleValue() > b.doubleValue());
            }),

    LESS_THAN(new Node(NodeType.PROCESSOR, "<", new NodeInternal[0], new NodeSocket[]{
            new NodeSocket("a", NodeDataType.NUMBER),
            new NodeSocket("b", NodeDataType.NUMBER),
    }, new NodeSocket[]{
            new NodeSocket("c", NodeDataType.BOOLEAN),
    }),
            info -> {
                Number a = (Number) info.getValueFromInput(0);
                Number b = (Number) info.getValueFromInput(1);
                return new NodeExecutionResult(a != null && b != null && a.doubleValue() < b.doubleValue());
            }),

    ROUND_NODE(new Node(NodeType.PROCESSOR, new NodeInternal[]{
            new NodeInternal("Rounding method", new Picker(PickerType.DROPDOWN, new Option[]{
                    new Option("Round", "round"),
                    new Option("Ceiling", "ceil"),
                    new Option("Floor", "floor")
            }))
    }, new NodeSocket[]{
            new NodeSocket("value", NodeDataType.NUMBER),
    }, new NodeSocket[]{
            new NodeSocket("value", NodeDataType.NUMBER),
    }),
            info -> {
                Number a = (Number) info.getValueFromInput(0);

                if (a == null) {
                    return new NodeExecutionResult(null);
                }

                return new NodeExecutionResult(
                        switch ((String) info.getInternals()[0].getValue()) {
                            case "round" -> Math.round(a.doubleValue());
                            case "ceil" -> Math.ceil(a.doubleValue());
                            case "floor" -> Math.floor(a.doubleValue());
                            default -> a;
                        });
            }),

    ABS_OPERATOR(new Node(NodeType.PROCESSOR, "|x|", new NodeInternal[0], new NodeSocket[]{
            new NodeSocket("value", NodeDataType.NUMBER),
    }, new NodeSocket[]{
            new NodeSocket("absolute", NodeDataType.NUMBER),
    }),
            info -> {
                Number a = (Number) info.getValueFromInput(0);
                return new NodeExecutionResult(a != null ? Math.abs(a.doubleValue()) : null);
            }),

    POW_OPERATOR(new Node(NodeType.PROCESSOR, "^", new NodeInternal[0], new NodeSocket[]{
            new NodeSocket("base", NodeDataType.NUMBER),
            new NodeSocket("exponent", NodeDataType.NUMBER),
    }, new NodeSocket[]{
            new NodeSocket("value", NodeDataType.NUMBER),
    }),
            info -> {
                Number a = (Number) info.getValueFromInput(0);
                Number b = (Number) info.getValueFromInput(1);
                return new NodeExecutionResult(a != null && b != null ? Math.pow(a.doubleValue(), b.doubleValue()) : null);
            }),

    NUMBER_SWITCH(new Node(NodeType.PROCESSOR, new NodeInternal[0], new NodeSocket[]{
            new NodeSocket("when", NodeDataType.BOOLEAN),
            new NodeSocket("then", NodeDataType.NUMBER),
            new NodeSocket("else", NodeDataType.NUMBER),
    }, new NodeSocket[]{
            new NodeSocket("output", NodeDataType.NUMBER),
    }),
            info -> {
                boolean condition = ValueUtil.convert(info.getValueFromInput(0), Boolean.class);

                Number a = (Number) info.getValueFromInput(1);
                Number b = (Number) info.getValueFromInput(2);

                return new NodeExecutionResult(condition ? (a != null ? a.doubleValue() : null) : (b != null ? b.doubleValue() : null));
            }),

    TEXT_INPUT(new Node(NodeType.INPUT, new NodeInternal[]{
            new NodeInternal("value", new Picker(PickerType.MULTILINE))
    }, new NodeSocket[0], new NodeSocket[]{
            new NodeSocket("value", NodeDataType.STRING)
    }),
            info -> {
                Object value = info.getInternals()[0].getValue();
                if (!(value instanceof CharSequence charSequence)) return new NodeExecutionResult(null);
                return new NodeExecutionResult(charSequence.toString());
            }),

    COMBINE_TEXT(new Node(NodeType.PROCESSOR, new NodeInternal[]{
            new NodeInternal("joiner", new Picker(PickerType.TEXT))
    }, new NodeSocket[]{
            new NodeSocket("a", NodeDataType.STRING),
            new NodeSocket("b", NodeDataType.STRING),
    }, new NodeSocket[]{
            new NodeSocket("c", NodeDataType.STRING),
    }),
            info -> {
                Object joiner = ValueUtil.convert(info.getInternals()[0].getValue(), String.class);
                Object a = ValueUtil.convert(info.getValueFromInput(0), String.class);
                Object b = ValueUtil.convert(info.getValueFromInput(1), String.class);

                joiner = joiner == null ? "" : joiner;
                return new NodeExecutionResult("" + (a != null ? a : "") + joiner + (b != null ? b : ""));
            }),

    TEXT_SWITCH(new Node(NodeType.PROCESSOR, new NodeInternal[0], new NodeSocket[]{
            new NodeSocket("when", NodeDataType.BOOLEAN),
            new NodeSocket("then", NodeDataType.STRING),
            new NodeSocket("else", NodeDataType.STRING),
    }, new NodeSocket[]{
            new NodeSocket("output", NodeDataType.STRING),
    }),
            info -> {
                boolean condition = ValueUtil.convert(info.getValueFromInput(0), Boolean.class);
                String a = ValueUtil.convert(info.getValueFromInput(1), String.class);
                String b = ValueUtil.convert(info.getValueFromInput(2), String.class);
                return new NodeExecutionResult(condition ? a : b);
            }),

    SIN(new Node(NodeType.PROCESSOR, "sin", new NodeInternal[0], new NodeSocket[]{
            new NodeSocket("in", NodeDataType.NUMBER),
    }, new NodeSocket[]{
            new NodeSocket("out", NodeDataType.NUMBER)
    }),
            info -> {
                try {
                    Number a = (Number) info.getValueFromInput(0);
                    return new NodeExecutionResult(a != null ? Math.sin(a.doubleValue()) : null);
                } catch (Exception e) {
                    return new NodeExecutionResult(0);
                }
            }),

    COS(new Node(NodeType.PROCESSOR, "cos", new NodeInternal[0], new NodeSocket[]{
            new NodeSocket("in", NodeDataType.NUMBER),
    }, new NodeSocket[]{
            new NodeSocket("out", NodeDataType.NUMBER)
    }),
            info -> {
                Number a = (Number) info.getValueFromInput(0);
                return new NodeExecutionResult(a != null ? Math.cos(a.doubleValue()) : null);
            }),

    TAN(new Node(NodeType.PROCESSOR, "tan", new NodeInternal[0], new NodeSocket[]{
            new NodeSocket("in", NodeDataType.NUMBER),
    }, new NodeSocket[]{
            new NodeSocket("out", NodeDataType.NUMBER)
    }),
            info -> {
                try {
                    Number a = (Number) info.getValueFromInput(0);
                    return new NodeExecutionResult(a != null ? Math.tan(a.doubleValue()) : null);
                } catch (Exception e) {
                    return new NodeExecutionResult(0);
                }
            }),

    SQRT(new Node(NodeType.PROCESSOR, "√", new NodeInternal[0], new NodeSocket[]{
            new NodeSocket("in", NodeDataType.NUMBER),
    }, new NodeSocket[]{
            new NodeSocket("out", NodeDataType.NUMBER)
    }),
            info -> {
                try {
                    Number a = (Number) info.getValueFromInput(0);
                    return new NodeExecutionResult(a != null ? Math.sqrt(a.doubleValue()) : null);
                } catch (Exception e) {
                    return new NodeExecutionResult(0);
                }
            }),

    MOD(new Node(NodeType.PROCESSOR, "%", new NodeInternal[0], new NodeSocket[]{
            new NodeSocket("a", NodeDataType.NUMBER),
            new NodeSocket("b", NodeDataType.NUMBER),
    }, new NodeSocket[]{
            new NodeSocket("c", NodeDataType.NUMBER)
    }),
            info -> {
                try {
                    Number a = (Number) info.getValueFromInput(0);
                    Number b = (Number) info.getValueFromInput(1);
                    return new NodeExecutionResult(a != null  && b != null ? a.doubleValue() % b.doubleValue() : null);
                } catch (Exception e) {
                    return new NodeExecutionResult(0);
                }
            }),
    ;


    NodeModel(Node definition, NodeImplementation implementation) {
        this.definition = definition;
        definition.setName(this.name());
        this.implementation = implementation;
        this.triggerFunction = (params) -> false;
    }

    NodeModel(Node definition, NodeImplementation implementation, NodeTriggerFunction triggerFunction) {
        this.definition = definition;
        definition.setName(this.name());
        this.implementation = implementation;
        this.triggerFunction = triggerFunction;
    }


    private enum TimePeriod {
        SECONDS("seconds ago", 1000L),
        MINUTES("minutes ago", SECONDS.millis * 60),
        HOURS("hours ago", MINUTES.millis * 24),
        DAYS("days ago", HOURS.millis * 30),
        MONTHS("months ago", DAYS.millis * 12);

        private final String label;
        private final Long millis;

        TimePeriod(String label, Long millis) {
            this.label = label;
            this.millis = millis;
        }

        public String getLabel() {
            return label;
        }

        public Long getMillis() {
            return millis;
        }
    }

    private Node definition;
    private NodeImplementation implementation;
    private NodeTriggerFunction triggerFunction;

    public Node getDefinition() {
        return definition;
    }

    public NodeImplementation getImplementation() {
        return implementation;
    }

    public NodeTriggerFunction getTriggerFunction() {
        return triggerFunction;
    }

    public static NodeImplementation getImplementationFor(String name) {
        return NodeModel.valueOf(name).implementation;
    }

    public static NodeTriggerFunction getTriggerFunctionFor(String name) {
        return NodeModel.valueOf(name).triggerFunction;
    }

    public static Node getDefinitionFor(String name) {
        return NodeModel.valueOf(name).definition;
    }
}
