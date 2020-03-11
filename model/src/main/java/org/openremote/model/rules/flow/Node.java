package org.openremote.model.rules.flow;

public class Node {
    // Generation of ID is the responsibility of the front end and is unique for every node in a structure, not for every node in the system
    private String id;
    private NodeType type;
    private String name;
    private NodePosition position;
    private NodePosition size;
    private NodeInternal[] internals;
    private NodeSocket[] inputs;
    private NodeSocket[] outputs;
    private String displayCharacter;

    public Node(NodeType type, NodeInternal[] internals, NodeSocket[] inputs, NodeSocket[] outputs) {
        this.id = "INVALID ID";
        this.type = type;
        this.position = new NodePosition(0, 0);
        this.size = new NodePosition(0, 0);
        this.internals = internals;
        this.inputs = inputs;
        this.outputs = outputs;
        this.displayCharacter = null;
    }

    public Node(NodeType type, String displayCharacter, NodeInternal[] internals, NodeSocket[] inputs, NodeSocket[] outputs) {
        if (internals.length != 0)
            throw new IllegalArgumentException("A node cannot have internals when a display character is specified");

        this.id = "INVALID ID";
        this.type = type;
        this.position = new NodePosition(0, 0);
        this.size = new NodePosition(0, 0);
        this.internals = internals;
        this.inputs = inputs;
        this.outputs = outputs;
        this.displayCharacter = displayCharacter;
    }

    public Node() {
        id = "INVALID ID";
        type = NodeType.INPUT;
        name = "Unnamed node";
        position = new NodePosition();
        size = new NodePosition();
        internals = new NodeInternal[]{};
        inputs = new NodeSocket[]{};
        outputs = new NodeSocket[]{};
        displayCharacter = null;
    }

    public String getId() {
        return id;
    }

    public NodeType getType() {
        return type;
    }

    public void setType(NodeType type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public NodePosition getPosition() {
        return position;
    }

    public void setPosition(NodePosition position) {
        this.position = position;
    }

    public NodeInternal[] getInternals() {
        return internals;
    }

    public NodeSocket[] getInputs() {
        return inputs;
    }

    public NodeSocket[] getOutputs() {
        return outputs;
    }

    public String getDisplayCharacter() {
        return displayCharacter;
    }

    public NodePosition getSize() {
        return size;
    }

    public void setSize(NodePosition size) {
        this.size = size;
    }
}

