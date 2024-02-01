package org.openremote.manager.rules.flow;

import org.openremote.manager.rules.RulesBuilder;
import org.openremote.model.attribute.AttributeInfo;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.rules.flow.*;
import org.openremote.model.util.ValueUtil;
import org.openremote.model.value.ValueHolder;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
