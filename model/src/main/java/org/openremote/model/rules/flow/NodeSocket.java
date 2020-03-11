package org.openremote.model.rules.flow;

public class NodeSocket {
    // Generation of ID is the responsibility of the npm package
    private String id;
    private String name;
    private NodeDataType type;
    // Assignment of Node ID is the responsibility of the npm package
    private String nodeId;
    // Assignment of index is the responsibility of the npm package
    private int index;

    public NodeSocket(String name, NodeDataType type) {
        this.id = "INVALID ID";
        this.name = name;
        this.type = type;
        this.nodeId = "INVALID NODE ID";
        this.index = 0;
    }

    public NodeSocket() {
        id = "INVALID ID";
        name = "Unnamed socket";
        type = NodeDataType.ANY;
        nodeId = "INVALID NODE ID";
        index = -1;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public NodeDataType getType() {
        return type;
    }

    public void setType(NodeDataType type) {
        this.type = type;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof NodeSocket)
            return ((NodeSocket) obj).id.equals(id);
        else return false;
    }
}
