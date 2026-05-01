package org.openremote.manager.rules.flow;

import org.openremote.model.attribute.AttributeInfo;
import org.openremote.model.attribute.AttributeRef;

/**
 * A wrapper class for the result of a node execution,
 * optionally including AttributeRef and AttributeInfo for optimization purposes.
 * All fields are nullable
 */
public class NodeExecutionResult {
    private final Object value;
    private final AttributeRef attributeRef;
    private final AttributeInfo attributeInfo;

    public NodeExecutionResult(Object value, AttributeRef attributeRef, AttributeInfo attributeInfo) {
        this.value = value;
        this.attributeRef = attributeRef;
        this.attributeInfo = attributeInfo;
    }

    public NodeExecutionResult(Object value) {
        this.value = value;
        this.attributeRef = null;
        this.attributeInfo = null;
    }

    public Object getValue() {
        return value;
    }

    public AttributeRef getAttributeRef() {
        return attributeRef;
    }

    public AttributeInfo getAttributeInfo() {
        return attributeInfo;
    }
}
