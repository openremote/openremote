package org.openremote.manager.rules.flow;

import org.openremote.manager.rules.RulesBuilder;
import org.openremote.manager.rules.RulesEngine;
import org.openremote.model.attribute.AttributeInfo;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.datapoint.ValueDatapoint;
import org.openremote.model.datapoint.query.AssetDatapointLTTBQuery;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.rules.flow.*;
import org.openremote.model.util.ValueUtil;
import org.openremote.model.value.ValueHolder;

import java.text.NumberFormat;
import java.text.ParseException;
import java.time.Instant;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

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
				System.out.println(new AttributeRef(assetId, attributeName));
                Optional<AttributeInfo> readValue = info.getFacts().matchFirstAssetState(new AssetQuery().ids(assetId).attributeName(attributeName));
                return readValue.flatMap(ValueHolder::getValue).orElse(null);
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
	HISTORIC_VALUE(
			new Node(NodeType.INPUT, new NodeInternal[]
					{
						new NodeInternal("attribute", new Picker(PickerType.ASSET_ATTRIBUTE), NodeInternal.BreakType.NEW_LINE),
						new NodeInternal("time_period", new Picker(PickerType.NUMBER),  NodeInternal.BreakType.SPACER),
						new NodeInternal("time_unit", new Picker(PickerType.DROPDOWN, TimeUnit.getHistoricValueOptions()), NodeInternal.BreakType.SPACER)
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
				try {
					timePeriod = NumberFormat.getInstance().parse(info.getInternals()[1].getValue().toString());
					timeUnit = Long.parseLong(info.getInternals()[2].getValue().toString());
				} catch (ParseException e) {
					throw new RuntimeException(e);
				}
				long currentMillis = info.getFacts().getClock().getCurrentTimeMillis();

				Instant pastInstant = Instant.ofEpochMilli(currentMillis-(timePeriod.longValue()*timeUnit.longValue()));

				final ValueDatapoint<?>[] valueDatapoints = info.getHistoricDatapoints().getValueDatapoints(ref, new AssetDatapointLTTBQuery(pastInstant.toEpochMilli(), pastInstant.toEpochMilli(), 1));
				return valueDatapoints.length > 0 ? valueDatapoints[0] : null;
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

	DEBUG_TO_CONSOLE(new Node(NodeType.OUTPUT, new NodeInternal[0], new NodeSocket[]{
			new NodeSocket("value", NodeDataType.ANY)
	}, new NodeSocket[0]),
			info -> ((RulesBuilder.Action) facts -> {
				info.setFacts(facts);
				Object obj = info.getValueFromInput(0);
				info.getFacts().logVars(Logger.getLogger("NodeModel.DEBUG_TO_CONSOLE"), Level.INFO, ValueUtil.asJSON(obj).orElseGet(() -> "Couldn't parse JSON"));
			})),

    WRITE_ATTRIBUTE(new Node(NodeType.OUTPUT, new NodeInternal[]{
            new NodeInternal("Attribute", new Picker(PickerType.ASSET_ATTRIBUTE))
    }, new NodeSocket[]{
            new NodeSocket("value", NodeDataType.ANY)
    }, new NodeSocket[0]),
            info -> ((RulesBuilder.Action) facts -> {
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
                if (value == null) return false;
                if (!(value instanceof Boolean)) return false;
                return value;
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
                return a && b;
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
                return a || b;
            }),

    NOT_GATE(new Node(NodeType.PROCESSOR, "!", new NodeInternal[0], new NodeSocket[]{
            new NodeSocket("i", NodeDataType.BOOLEAN),
    }, new NodeSocket[]{
            new NodeSocket("o", NodeDataType.BOOLEAN),
    }),
            info -> {
                boolean a = (boolean) info.getValueFromInput(0);
                return !a;
            }),

    NUMBER_INPUT(new Node(NodeType.INPUT, new NodeInternal[]{
            new NodeInternal("value", new Picker(PickerType.NUMBER))
    }, new NodeSocket[0], new NodeSocket[]{
            new NodeSocket("value", NodeDataType.NUMBER)
    }),
            info -> ValueUtil.convert(info.getInternals()[0].getValue(), Double.class)),

    ADD_OPERATOR(new Node(NodeType.PROCESSOR, "+", new NodeInternal[0], new NodeSocket[]{
            new NodeSocket("a", NodeDataType.NUMBER),
            new NodeSocket("b", NodeDataType.NUMBER),
    }, new NodeSocket[]{
            new NodeSocket("c", NodeDataType.NUMBER),
    }),
            info -> {
                Number a = (Number) info.getValueFromInput(0);
                Number b = (Number) info.getValueFromInput(1);
                return a != null && b != null ? a.doubleValue() + b.doubleValue() : null;
            }),
    SUM_PROCESSOR(new Node(NodeType.PROCESSOR, "Σ", new NodeInternal[0], new NodeSocket[]{
            new NodeSocket("a", NodeDataType.NUMBER_ARRAY)
    }, new NodeSocket[]{
            new NodeSocket("b", NodeDataType.NUMBER),
    }),
    info -> {
	        Object[] a = info.getValuesFromInput(info.getInputs());
            return Arrays.stream(a).map(Object::toString).mapToDouble(Double::parseDouble).sum();
        }
	),
	MAX_PROCESSOR(new Node(NodeType.PROCESSOR, "max", new NodeInternal[0], new NodeSocket[]{
			new NodeSocket("a", NodeDataType.NUMBER_ARRAY)
	}, new NodeSocket[]{
			new NodeSocket("b", NodeDataType.NUMBER),
	}),
			info -> {
				Object[] a = info.getValuesFromInput(info.getInputs());
				return Arrays.stream(a).map(Object::toString).mapToDouble(Double::parseDouble).max();
			}
	),
	MIN_PROCESSOR(new Node(NodeType.PROCESSOR, "min", new NodeInternal[0], new NodeSocket[]{
			new NodeSocket("a", NodeDataType.NUMBER_ARRAY)
	}, new NodeSocket[]{
			new NodeSocket("b", NodeDataType.NUMBER),
	}),
			info -> {
				Object[] a = info.getValuesFromInput(info.getInputs());
				return Arrays.stream(a).map(Object::toString).mapToDouble(Double::parseDouble).min();
			}
	),
	AVERAGE_PROCESSOR(new Node(NodeType.PROCESSOR, "avg", new NodeInternal[0], new NodeSocket[]{
			new NodeSocket("a", NodeDataType.NUMBER_ARRAY)
	}, new NodeSocket[]{
			new NodeSocket("b", NodeDataType.NUMBER),
	}),
			info -> {
				Object[] a = info.getValuesFromInput(info.getInputs());
				return Arrays.stream(a).map(Object::toString).mapToDouble(Double::parseDouble).average();
			}
	),
	MEDIAN_PROCESSOR(new Node(NodeType.PROCESSOR, "med", new NodeInternal[0], new NodeSocket[]{
			new NodeSocket("a", NodeDataType.NUMBER_ARRAY)
	}, new NodeSocket[]{
			new NodeSocket("b", NodeDataType.NUMBER),
	}),
			info -> {
				Object[] a = info.getValuesFromInput(info.getInputs());
				final double[] sortedDoubles = Arrays.stream(a).map(Object::toString).mapToDouble(Double::parseDouble).sorted().toArray();
				if (sortedDoubles.length == 0) {
					return 0.0;
				} else if (sortedDoubles.length % 2 == 0) {
					return (sortedDoubles[sortedDoubles.length / 2 - 1] + sortedDoubles[sortedDoubles.length / 2]) / 2.0;
				} else {
					return sortedDoubles[sortedDoubles.length / 2];
				}
			}
	),

    SUBTRACT_OPERATOR(new Node(NodeType.PROCESSOR, "-", new NodeInternal[0], new NodeSocket[]{
            new NodeSocket("a", NodeDataType.NUMBER),
            new NodeSocket("b", NodeDataType.NUMBER),
    }, new NodeSocket[]{
            new NodeSocket("c", NodeDataType.NUMBER),
    }),
            info -> {
                Number a = (Number) info.getValueFromInput(0);
                Number b = (Number) info.getValueFromInput(1);
                return a != null && b != null ? a.doubleValue() - b.doubleValue() : null;
            }),

    MULTIPLY_OPERATOR(new Node(NodeType.PROCESSOR, "×", new NodeInternal[0], new NodeSocket[]{
            new NodeSocket("a", NodeDataType.NUMBER),
            new NodeSocket("b", NodeDataType.NUMBER),
    }, new NodeSocket[]{
            new NodeSocket("c", NodeDataType.NUMBER),
    }),
            info -> {
                Number a = (Number) info.getValueFromInput(0);
                Number b = (Number) info.getValueFromInput(1);
                return a != null && b != null ? a.doubleValue() * b.doubleValue() : null;
            }),

    DIVIDE_OPERATOR(new Node(NodeType.PROCESSOR, "÷", new NodeInternal[0], new NodeSocket[]{
            new NodeSocket("a", NodeDataType.NUMBER),
            new NodeSocket("b", NodeDataType.NUMBER),
    }, new NodeSocket[]{
            new NodeSocket("c", NodeDataType.NUMBER),
    }),
            info -> {
                Number a = (Number) info.getValueFromInput(0);
                Number b = (Number) info.getValueFromInput(1);

                if (a == null || b == null) {
                    return null;
                }

                if (b.doubleValue() == 0d)
                    return 0d;

                return a.doubleValue() / b.doubleValue();
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
                return a != null && b != null && Objects.equals(a, b);
            }),

    GREATER_THAN(new Node(NodeType.PROCESSOR, ">", new NodeInternal[0], new NodeSocket[]{
            new NodeSocket("a", NodeDataType.NUMBER),
            new NodeSocket("b", NodeDataType.NUMBER),
    }, new NodeSocket[]{
            new NodeSocket("c", NodeDataType.BOOLEAN),
    }),
            info -> {
                Number a = (Number) info.getValueFromInput(0);
                Number b = (Number) info.getValueFromInput(1);
                return a != null && b != null && a.doubleValue() > b.doubleValue();
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
                return a != null && b != null && a.doubleValue() < b.doubleValue();
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
                    return null;
                }

                return switch ((String) info.getInternals()[0].getValue()) {
                    case "round" -> Math.round(a.doubleValue());
                    case "ceil" -> Math.ceil(a.doubleValue());
                    case "floor" -> Math.floor(a.doubleValue());
                    default -> a;
                };
            }),

    ABS_OPERATOR(new Node(NodeType.PROCESSOR, "|x|", new NodeInternal[0], new NodeSocket[]{
            new NodeSocket("value", NodeDataType.NUMBER),
    }, new NodeSocket[]{
            new NodeSocket("absolute", NodeDataType.NUMBER),
    }),
            info -> {
                Number a = (Number) info.getValueFromInput(0);
                return a != null ? Math.abs(a.doubleValue()) : null;
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
                return a != null && b != null ? Math.pow(a.doubleValue(), b.doubleValue()) : null;
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

                return condition ? (a != null ? a.doubleValue() : null) : (b != null ? b.doubleValue() : null);
            }),

    TEXT_INPUT(new Node(NodeType.INPUT, new NodeInternal[]{
            new NodeInternal("value", new Picker(PickerType.MULTILINE))
    }, new NodeSocket[0], new NodeSocket[]{
            new NodeSocket("value", NodeDataType.STRING)
    }),
            info -> {
                Object value = info.getInternals()[0].getValue();
                if (!(value instanceof CharSequence charSequence)) return null;
                return charSequence.toString();
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
                return "" + (a != null ? a : "") + joiner + (b != null ? b : "");
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
                return condition ? a : b;
            }),

    SIN(new Node(NodeType.PROCESSOR, "sin", new NodeInternal[0], new NodeSocket[]{
            new NodeSocket("in", NodeDataType.NUMBER),
    }, new NodeSocket[]{
            new NodeSocket("out", NodeDataType.NUMBER)
    }),
            info -> {
                try {
                    Number a = (Number) info.getValueFromInput(0);
                    return a != null ? Math.sin(a.doubleValue()) : null;
                } catch (Exception e) {
                    return 0;
                }
            }),

    COS(new Node(NodeType.PROCESSOR, "cos", new NodeInternal[0], new NodeSocket[]{
            new NodeSocket("in", NodeDataType.NUMBER),
    }, new NodeSocket[]{
            new NodeSocket("out", NodeDataType.NUMBER)
    }),
            info -> {
                Number a = (Number) info.getValueFromInput(0);
                return a != null ? Math.cos(a.doubleValue()) : null;
            }),

    TAN(new Node(NodeType.PROCESSOR, "tan", new NodeInternal[0], new NodeSocket[]{
            new NodeSocket("in", NodeDataType.NUMBER),
    }, new NodeSocket[]{
            new NodeSocket("out", NodeDataType.NUMBER)
    }),
            info -> {
                try {
                    Number a = (Number) info.getValueFromInput(0);
                    return a != null ? Math.tan(a.doubleValue()) : null;
                } catch (Exception e) {
                    return 0;
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
                    return a != null ? Math.sqrt(a.doubleValue()) : null;
                } catch (Exception e) {
                    return 0;
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
                    return a != null  && b != null ? a.doubleValue() % b.doubleValue() : null;
                } catch (Exception e) {
                    return 0;
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


	private enum TimeUnit {
		SECONDS("seconds ago"),
		MINUTES("minutes ago"),
		HOURS("hours ago"),
		DAYS("days ago"),
		MONTHS("months ago");

		private final String label;

		TimeUnit(String label) {
			this.label = label;
		}

		public String getLabel() {
			return label;
		}
		public static Option[] getHistoricValueOptions(){
			Map<TimeUnit, Long> dict = new HashMap<>();
			dict.put(TimeUnit.SECONDS, 1000L);
			dict.put(TimeUnit.MINUTES, dict.get(TimeUnit.SECONDS)*60);
			dict.put(TimeUnit.HOURS, dict.get(TimeUnit.MINUTES)*24);
			dict.put(TimeUnit.DAYS, dict.get(TimeUnit.HOURS)*30);
			dict.put(TimeUnit.MONTHS, dict.get(TimeUnit.DAYS)*12);
			return dict.entrySet().stream().map(e -> new Option(e.getKey().getLabel(), e.getValue())).toArray(Option[]::new);
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
