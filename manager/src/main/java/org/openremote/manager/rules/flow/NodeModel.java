package org.openremote.manager.rules.flow;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.openremote.container.Container;
import org.openremote.manager.rules.RulesBuilder;
import org.openremote.manager.rules.RulesEngine;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.rules.AssetState;
import org.openremote.model.rules.flow.*;
import org.openremote.model.value.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public enum NodeModel {
    READ_ATTRIBUTE(
            new Node(NodeType.INPUT, new NodeInternal[]{
                    new NodeInternal("Attribute", new Picker(PickerType.ASSET_ATTRIBUTE))
            },
                    new NodeSocket[0], new NodeSocket[]{
                    new NodeSocket("value", NodeDataType.ANY)
            }),
            info -> {
                AssetAttributeInternalValue assetAttributePair = Container.JSON.convertValue(info.getInternals()[0].getValue(), AssetAttributeInternalValue.class);
                String assetId = assetAttributePair.getAssetId();
                String attributeName = assetAttributePair.getAttributeName();
                Optional<AssetState> readValue = info.getFacts().matchFirstAssetState(new AssetQuery().ids(assetId).attributeName(attributeName));
                if (!readValue.isPresent()) return null;
                return readValue.get().getValue().orElse(null);
            },
            params -> {
                AssetAttributeInternalValue internal = Container.JSON.convertValue(params.getNode().getInternals()[0].getValue(), AssetAttributeInternalValue.class);
                String assetId = internal.getAssetId();
                String attributeName = internal.getAttributeName();
                List<AssetState> allAssets = params.getFacts().matchAssetState(new AssetQuery().ids(assetId).attributeName(attributeName)
                ).collect(Collectors.toList());

                return allAssets.stream().anyMatch(state -> {
                    long timestamp = state.getTimestamp();
                    long triggerStamp = params.getBuilder().getTriggerMap().getOrDefault(params.getRuleName(), -1L);
                    if (triggerStamp == -1L) return true; //The flow has never been executed
                    return timestamp > triggerStamp && state.isValueChanged();
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
                    RulesEngine.LOG.warning("Flow rule error: node " + info.getNode().getName() + " receives invalid value");
                    return;
                }
                AssetAttributeInternalValue assetAttributePair = Container.JSON.convertValue(info.getInternals()[0].getValue(), AssetAttributeInternalValue.class);
                Optional<AssetState> existingValue = info.getFacts().matchFirstAssetState(new AssetQuery().ids(assetAttributePair.getAssetId()).attributeName(assetAttributePair.getAttributeName()));

                if (existingValue.isPresent())
                    if (existingValue.get().getValue().isPresent())
                        if (existingValue.get().getValue().get().equals(value)) return;

                try {
                    if (value instanceof Value) {
                        info.getAssets().dispatch(
                                assetAttributePair.getAssetId(),
                                assetAttributePair.getAttributeName(),
                                (Value) value
                        );
                    } else {
                        info.getAssets().dispatch(
                                assetAttributePair.getAssetId(),
                                assetAttributePair.getAttributeName(),
                                Values.parseOrNull(Container.JSON.writeValueAsString(value))
                        );
                    }
                } catch (JsonProcessingException e) {
                    RulesEngine.LOG.severe("Flow rule error: node " + info.getNode().getName() + " receives invalid value");
                }
            })),

    BOOLEAN_INPUT(new Node(NodeType.INPUT, new NodeInternal[]{
            new NodeInternal("value", new Picker(PickerType.CHECKBOX))
    }, new NodeSocket[0], new NodeSocket[]{
            new NodeSocket("value", NodeDataType.BOOLEAN)
    }),
            info -> {
                Object value = info.getInternals()[0].getValue();
                if (value == null) return Values.create(false);
                if (!(value instanceof Boolean)) return Values.create(false);
                return Values.create((boolean) value);
            }),

    AND_GATE(new Node(NodeType.PROCESSOR, "&", new NodeInternal[0], new NodeSocket[]{
            new NodeSocket("a", NodeDataType.BOOLEAN),
            new NodeSocket("b", NodeDataType.BOOLEAN),
    }, new NodeSocket[]{
            new NodeSocket("c", NodeDataType.BOOLEAN),
    }),
            info -> {
                try {
                    BooleanValue a = (BooleanValue) info.getValueFromInput(0);
                    BooleanValue b = (BooleanValue) info.getValueFromInput(1);
                    return Values.create(a.getBoolean() && b.getBoolean());
                } catch (Exception e) {
                    RulesEngine.LOG.warning("Flow rule processing error: " + e.getMessage());
                    return Values.create(false);
                }
            }),

    OR_GATE(new Node(NodeType.PROCESSOR, "∥", new NodeInternal[0], new NodeSocket[]{
            new NodeSocket("a", NodeDataType.BOOLEAN),
            new NodeSocket("b", NodeDataType.BOOLEAN),
    }, new NodeSocket[]{
            new NodeSocket("c", NodeDataType.BOOLEAN),
    }),
            info -> {
                try {
                    BooleanValue a = (BooleanValue) info.getValueFromInput(0);
                    BooleanValue b = (BooleanValue) info.getValueFromInput(1);
                    return Values.create(a.getBoolean() || b.getBoolean());
                } catch (Exception e) {
                    RulesEngine.LOG.warning("Flow rule processing error: " + e.getMessage());
                    return Values.create(false);
                }
            }),

    NOT_GATE(new Node(NodeType.PROCESSOR, "!", new NodeInternal[0], new NodeSocket[]{
            new NodeSocket("i", NodeDataType.BOOLEAN),
    }, new NodeSocket[]{
            new NodeSocket("o", NodeDataType.BOOLEAN),
    }),
            info -> {
                try {
                    BooleanValue a = (BooleanValue) info.getValueFromInput(0);
                    return Values.create(!a.getBoolean());
                } catch (Exception e) {
                    RulesEngine.LOG.warning("Flow rule processing error: " + e.getMessage());
                    return Values.create(true);
                }
            }),

    NUMBER_INPUT(new Node(NodeType.INPUT, new NodeInternal[]{
            new NodeInternal("value", new Picker(PickerType.NUMBER))
    }, new NodeSocket[0], new NodeSocket[]{
            new NodeSocket("value", NodeDataType.NUMBER)
    }),
            info -> {
                try {
                    return Values.create(Float.parseFloat(Container.JSON.writeValueAsString(info.getInternals()[0].getValue())));
                } catch (JsonProcessingException e) {
                    RulesEngine.RULES_LOG.warning("Number node returned invalid value");
                    return Values.create(0f);
                }
            }),

    ADD_OPERATOR(new Node(NodeType.PROCESSOR, "+", new NodeInternal[0], new NodeSocket[]{
            new NodeSocket("a", NodeDataType.NUMBER),
            new NodeSocket("b", NodeDataType.NUMBER),
    }, new NodeSocket[]{
            new NodeSocket("c", NodeDataType.NUMBER),
    }),
            info -> {
                try {
                    NumberValue a = (NumberValue) info.getValueFromInput(0);
                    NumberValue b = (NumberValue) info.getValueFromInput(1);
                    return Values.create(a.getNumber() + b.getNumber());
                } catch (Exception e) {
                    RulesEngine.LOG.warning("Flow rule processing error: " + e.getMessage());
                    return Values.create(0);
                }
            }),

    SUBTRACT_OPERATOR(new Node(NodeType.PROCESSOR, "-", new NodeInternal[0], new NodeSocket[]{
            new NodeSocket("a", NodeDataType.NUMBER),
            new NodeSocket("b", NodeDataType.NUMBER),
    }, new NodeSocket[]{
            new NodeSocket("c", NodeDataType.NUMBER),
    }),
            info -> {
                try {
                    NumberValue a = (NumberValue) info.getValueFromInput(0);
                    NumberValue b = (NumberValue) info.getValueFromInput(1);
                    return Values.create(a.getNumber() - b.getNumber());
                } catch (Exception e) {
                    RulesEngine.LOG.warning("Flow rule processing error: " + e.getMessage());
                    return Values.create(0);
                }
            }),

    MULTIPLY_OPERATOR(new Node(NodeType.PROCESSOR, "×", new NodeInternal[0], new NodeSocket[]{
            new NodeSocket("a", NodeDataType.NUMBER),
            new NodeSocket("b", NodeDataType.NUMBER),
    }, new NodeSocket[]{
            new NodeSocket("c", NodeDataType.NUMBER),
    }),
            info -> {
                try {
                    NumberValue a = (NumberValue) info.getValueFromInput(0);
                    NumberValue b = (NumberValue) info.getValueFromInput(1);
                    return Values.create(a.getNumber() * b.getNumber());
                } catch (Exception e) {
                    RulesEngine.LOG.warning("Flow rule processing error: " + e.getMessage());
                    return Values.create(0);
                }
            }),

    DIVIDE_OPERATOR(new Node(NodeType.PROCESSOR, "÷", new NodeInternal[0], new NodeSocket[]{
            new NodeSocket("a", NodeDataType.NUMBER),
            new NodeSocket("b", NodeDataType.NUMBER),
    }, new NodeSocket[]{
            new NodeSocket("c", NodeDataType.NUMBER),
    }),
            info -> {
                try {
                    NumberValue a = (NumberValue) info.getValueFromInput(0);
                    NumberValue b = (NumberValue) info.getValueFromInput(1);

                    if (b.getNumber() == 0)
                        return Values.create(0f);

                    return Values.create(a.getNumber() / b.getNumber());
                } catch (Exception e) {
                    RulesEngine.LOG.warning("Flow rule processing error: " + e.getMessage());
                    return Values.create(0);
                }
            }),

    EQUALS_COMPARATOR(new Node(NodeType.PROCESSOR, "=", new NodeInternal[0], new NodeSocket[]{
            new NodeSocket("a", NodeDataType.ANY),
            new NodeSocket("b", NodeDataType.ANY),
    }, new NodeSocket[]{
            new NodeSocket("c", NodeDataType.BOOLEAN),
    }),
            info -> {
                try {
                    Object a = info.getValueFromInput(0);
                    Object b = info.getValueFromInput(1);
                    return Values.create(a.equals(b));
                } catch (Exception e) {
                    RulesEngine.LOG.warning("Flow rule processing error: " + e.getMessage());
                    return Values.create(false);
                }
            }),

    GREATER_THAN(new Node(NodeType.PROCESSOR, ">", new NodeInternal[0], new NodeSocket[]{
            new NodeSocket("a", NodeDataType.NUMBER),
            new NodeSocket("b", NodeDataType.NUMBER),
    }, new NodeSocket[]{
            new NodeSocket("c", NodeDataType.BOOLEAN),
    }),
            info -> {
                try {
                    NumberValue a = (NumberValue) info.getValueFromInput(0);
                    NumberValue b = (NumberValue) info.getValueFromInput(1);
                    return Values.create(a.getNumber() > b.getNumber());
                } catch (Exception e) {
                    RulesEngine.LOG.warning("Flow rule processing error: " + e.getMessage());
                    return Values.create(false);
                }
            }),

    LESS_THAN(new Node(NodeType.PROCESSOR, "<", new NodeInternal[0], new NodeSocket[]{
            new NodeSocket("a", NodeDataType.NUMBER),
            new NodeSocket("b", NodeDataType.NUMBER),
    }, new NodeSocket[]{
            new NodeSocket("c", NodeDataType.BOOLEAN),
    }),
            info -> {
                try {
                    NumberValue a = (NumberValue) info.getValueFromInput(0);
                    NumberValue b = (NumberValue) info.getValueFromInput(1);
                    return Values.create(a.getNumber() < b.getNumber());
                } catch (Exception e) {
                    RulesEngine.LOG.warning("Flow rule processing error: " + e.getMessage());
                    return Values.create(false);
                }
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
                try {
                    NumberValue a = (NumberValue) info.getValueFromInput(0);
                    switch ((String) info.getInternals()[0].getValue()) {
                        case "round":
                            return Values.create((float) Math.round(a.getNumber()));
                        case "ceil":
                            return Values.create((float) Math.ceil(a.getNumber()));
                        case "floor":
                            return Values.create((float) Math.floor(a.getNumber()));
                    }
                    return Values.create((float) Math.round(a.getNumber()));
                } catch (Exception e) {
                    RulesEngine.LOG.warning("Flow rule processing error: " + e.getMessage());
                    return Values.create(0);
                }
            }),

    ABS_OPERATOR(new Node(NodeType.PROCESSOR, "|x|", new NodeInternal[0], new NodeSocket[]{
            new NodeSocket("value", NodeDataType.NUMBER),
    }, new NodeSocket[]{
            new NodeSocket("absolute", NodeDataType.NUMBER),
    }),
            info -> {
                try {
                    NumberValue a = (NumberValue) info.getValueFromInput(0);
                    return Values.create((float) Math.abs(a.getNumber()));
                } catch (Exception e) {
                    RulesEngine.LOG.warning("Flow rule processing error: " + e.getMessage());
                    return Values.create(0);
                }
            }),

    POW_OPERATOR(new Node(NodeType.PROCESSOR, "^", new NodeInternal[0], new NodeSocket[]{
            new NodeSocket("base", NodeDataType.NUMBER),
            new NodeSocket("exponent", NodeDataType.NUMBER),
    }, new NodeSocket[]{
            new NodeSocket("value", NodeDataType.NUMBER),
    }),
            info -> {
                try {
                    NumberValue a = (NumberValue) info.getValueFromInput(0);
                    NumberValue b = (NumberValue) info.getValueFromInput(1);
                    return Values.create((float) Math.pow(a.getNumber(), b.getNumber()));
                } catch (Exception e) {
                    RulesEngine.LOG.warning("Flow rule processing error: " + e.getMessage());
                    return Values.create(0);
                }
            }),

    NUMBER_SWITCH(new Node(NodeType.PROCESSOR, new NodeInternal[0], new NodeSocket[]{
            new NodeSocket("when", NodeDataType.BOOLEAN),
            new NodeSocket("then", NodeDataType.NUMBER),
            new NodeSocket("else", NodeDataType.NUMBER),
    }, new NodeSocket[]{
            new NodeSocket("output", NodeDataType.NUMBER),
    }),
            info -> {
                try {
                    BooleanValue condition;
                    try {
                        condition = (BooleanValue) info.getValueFromInput(0);
                    } catch (Exception e) {
                        condition = Values.create(false);
                    }
                    NumberValue then = (NumberValue) info.getValueFromInput(1);
                    NumberValue _else = (NumberValue) info.getValueFromInput(2);
                    return Values.create(condition.getBoolean() ? then.getNumber() : _else.getNumber());
                } catch (Exception e) {
                    RulesEngine.LOG.warning("Flow rule processing error: " + e.getMessage());
                    return Values.create(0);
                }
            }),

    TEXT_INPUT(new Node(NodeType.INPUT, new NodeInternal[]{
            new NodeInternal("value", new Picker(PickerType.MULTILINE))
    }, new NodeSocket[0], new NodeSocket[]{
            new NodeSocket("value", NodeDataType.STRING)
    }),
            info -> {
                Object value = info.getInternals()[0].getValue();
                if (value == null) return Values.create("");
                if (!(value instanceof String)) return Values.create("");
                return Values.create((String) value);
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
                try {
                    Object rJoiner = info.getInternals()[0].getValue();
                    Object rA = info.getValueFromInput(0);
                    Object rB = info.getValueFromInput(1);
                    StringValue a, b;

                    if (rA instanceof StringValue)
                        a = (StringValue) rA;
                    else a = Values.create(rA.toString());

                    if (rB instanceof StringValue)
                        b = (StringValue) rB;
                    else b = Values.create(rB.toString());

                    String joiner = rJoiner == null ? "" : (String) rJoiner;
                    return Values.create(a.getString() + joiner + b.getString());
                } catch (Exception e) {
                    RulesEngine.LOG.warning("Flow rule processing error: " + e.getMessage());
                    return Values.create(0);
                }
            }),

    TEXT_SWITCH(new Node(NodeType.PROCESSOR, new NodeInternal[0], new NodeSocket[]{
            new NodeSocket("when", NodeDataType.BOOLEAN),
            new NodeSocket("then", NodeDataType.STRING),
            new NodeSocket("else", NodeDataType.STRING),
    }, new NodeSocket[]{
            new NodeSocket("output", NodeDataType.STRING),
    }),
            info -> {
                try {
                    BooleanValue condition;
                    try {
                        condition = (BooleanValue) info.getValueFromInput(0);
                    } catch (Exception e) {
                        condition = Values.create(false);
                    }
                    StringValue then = (StringValue) info.getValueFromInput(1);
                    StringValue _else = (StringValue) info.getValueFromInput(2);
                    return Values.create(condition.getBoolean() ? then.getString() : _else.getString());
                } catch (Exception e) {
                    RulesEngine.LOG.warning("Flow rule processing error: " + e.getMessage());
                    return Values.create(0);
                }
            }),

    SIN(new Node(NodeType.PROCESSOR, "sin", new NodeInternal[0], new NodeSocket[]{
            new NodeSocket("in", NodeDataType.NUMBER),
    }, new NodeSocket[]{
            new NodeSocket("out", NodeDataType.NUMBER)
    }),
            info -> {
                try {
                    NumberValue a = (NumberValue) info.getValueFromInput(0);
                    return Values.create((float) Math.sin(a.getNumber()));
                } catch (Exception e) {
                    return Values.create(0);
                }
            }),

    COS(new Node(NodeType.PROCESSOR, "cos", new NodeInternal[0], new NodeSocket[]{
            new NodeSocket("in", NodeDataType.NUMBER),
    }, new NodeSocket[]{
            new NodeSocket("out", NodeDataType.NUMBER)
    }),
            info -> {
                try {
                    NumberValue a = (NumberValue) info.getValueFromInput(0);
                    return Values.create((float) Math.cos(a.getNumber()));
                } catch (Exception e) {
                    return Values.create(0);
                }
            }),

    TAN(new Node(NodeType.PROCESSOR, "tan", new NodeInternal[0], new NodeSocket[]{
            new NodeSocket("in", NodeDataType.NUMBER),
    }, new NodeSocket[]{
            new NodeSocket("out", NodeDataType.NUMBER)
    }),
            info -> {
                try {
                    NumberValue a = (NumberValue) info.getValueFromInput(0);
                    return Values.create((float) Math.tan(a.getNumber()));
                } catch (Exception e) {
                    return Values.create(0);
                }
            }),

    SQRT(new Node(NodeType.PROCESSOR, "√", new NodeInternal[0], new NodeSocket[]{
            new NodeSocket("in", NodeDataType.NUMBER),
    }, new NodeSocket[]{
            new NodeSocket("out", NodeDataType.NUMBER)
    }),
            info -> {
                try {
                    NumberValue a = (NumberValue) info.getValueFromInput(0);
                    return Values.create((float) Math.sqrt(a.getNumber()));
                } catch (Exception e) {
                    return Values.create(0);
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
                    NumberValue a = (NumberValue) info.getValueFromInput(0);
                    NumberValue b = (NumberValue) info.getValueFromInput(1);
                    return Values.create((float) (a.getNumber() % b.getNumber()));
                } catch (Exception e) {
                    return Values.create(0);
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
