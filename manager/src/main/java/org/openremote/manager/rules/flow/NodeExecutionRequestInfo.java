package org.openremote.manager.rules.flow;

import org.openremote.manager.rules.RulesFacts;
import org.openremote.model.rules.*;
import org.openremote.model.rules.flow.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class NodeExecutionRequestInfo {
    private NodeCollection collection;

    private int outputSocketIndex;
    private NodeSocket outputSocket;

    private Node node;
    private NodeSocket[] inputs;
    private NodeSocket[] outputs;
    private NodeInternal[] internals;

    private RulesFacts facts;

    private Assets assets;
    private Users users;
    private Notifications notifications;
    private HistoricDatapoints historicDatapoints;
    private PredictedDatapoints predictedDatapoints;

    public NodeExecutionRequestInfo() {
        collection = new NodeCollection();
        outputSocketIndex = -1;
        outputSocket = null;
        node = null;
        inputs = new NodeSocket[]{};
        outputs = new NodeSocket[]{};
        internals = new NodeInternal[]{};
        facts = null;
        assets = null;
        users = null;
        notifications = null;
        historicDatapoints = null;
        predictedDatapoints = null;
    }

    public NodeExecutionRequestInfo(NodeCollection collection, int outputSocketIndex, NodeSocket outputSocket,
                                    Node node, NodeSocket[] inputs, NodeSocket[] outputs, NodeInternal[] internals,
                                    RulesFacts facts, Assets assets, Users users, Notifications notifications,
                                    HistoricDatapoints historicDatapoints, PredictedDatapoints predictedDatapoints) {
        this.collection = collection;
        this.outputSocketIndex = outputSocketIndex;
        this.outputSocket = outputSocket;
        this.node = node;
        this.inputs = inputs;
        this.outputs = outputs;
        this.internals = internals;
        this.facts = facts;
        this.assets = assets;
        this.users = users;
        this.notifications = notifications;
        this.historicDatapoints = historicDatapoints;
        this.predictedDatapoints = predictedDatapoints;
    }

    public NodeExecutionRequestInfo(NodeCollection collection, Node node, NodeSocket socket, RulesFacts facts,
                                    Assets assets, Users users, Notifications notifications,
                                    HistoricDatapoints historicDatapoints, PredictedDatapoints predictedDatapoints) {
        if (socket != null && Arrays.stream(node.getOutputs()).noneMatch(c -> c.getNodeId().equals(node.getId())))
            throw new IllegalArgumentException("Given socket does not belong to given node");

        this.collection = collection;
        this.outputSocketIndex = Arrays.asList(node.getOutputs()).indexOf(socket);
        this.outputSocket = socket;
        this.node = node;

        List<NodeSocket> inputs = new ArrayList<>();
        for (NodeSocket s : node.getInputs()) {
            inputs.addAll(Arrays.stream(collection.getConnections()).filter(c -> c.getTo().equals(s.getId())).map(c -> collection.getSocketById(c.getFrom())).collect(Collectors.toList()));
        }
        this.inputs = inputs.toArray(new NodeSocket[0]);

        List<NodeSocket> outputs = new ArrayList<>();
        for (NodeSocket s : node.getOutputs()) {
            outputs.addAll(Arrays.stream(collection.getConnections()).filter(c -> c.getFrom().equals(s.getId())).map(c -> collection.getSocketById(c.getTo())).collect(Collectors.toList()));
        }

        this.outputs = outputs.toArray(new NodeSocket[0]);
        this.internals = node.getInternals();

        this.facts = facts;
        this.assets = assets;
        this.users = users;
        this.notifications = notifications;
        this.historicDatapoints = historicDatapoints;
        this.predictedDatapoints = predictedDatapoints;
    }

    public Object getValueFromInput(int index) {
        NodeSocket aSocket = getInputs()[index];
        Node aNode = getCollection().getNodeById(aSocket.getNodeId());
        return NodeModel.getImplementationFor(aNode.getName()).execute(
            new NodeExecutionRequestInfo(getCollection(), aNode, aSocket, getFacts(), getAssets(), getUsers(), getNotifications(), getHistoricDatapoints(), getPredictedDatapoints())
        );
    }

    public NodeDataType getTypeFromInput(int index) {
        NodeSocket aSocket = getInputs()[index];
        return aSocket.getType();
    }

    public NodeCollection getCollection() {
        return collection;
    }

    public void setCollection(NodeCollection collection) {
        this.collection = collection;
    }

    public int getOutputSocketIndex() {
        return outputSocketIndex;
    }

    public void setOutputSocketIndex(int outputSocketIndex) {
        this.outputSocketIndex = outputSocketIndex;
    }

    public NodeSocket getOutputSocket() {
        return outputSocket;
    }

    public void setOutputSocket(NodeSocket outputSocket) {
        this.outputSocket = outputSocket;
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    public NodeSocket[] getInputs() {
        return inputs;
    }

    public void setInputs(NodeSocket[] inputs) {
        this.inputs = inputs;
    }

    public NodeSocket[] getOutputs() {
        return outputs;
    }

    public void setOutputs(NodeSocket[] outputs) {
        this.outputs = outputs;
    }

    public NodeInternal[] getInternals() {
        return internals;
    }

    public void setInternals(NodeInternal[] internals) {
        this.internals = internals;
    }

    public Assets getAssets() {
        return assets;
    }

    public void setAssets(Assets assets) {
        this.assets = assets;
    }

    public Users getUsers() {
        return users;
    }

    public void setUsers(Users users) {
        this.users = users;
    }

    public Notifications getNotifications() {
        return notifications;
    }

    public void setNotifications(Notifications notifications) {
        this.notifications = notifications;
    }

    public HistoricDatapoints getHistoricDatapoints() {
        return historicDatapoints;
    }

    public PredictedDatapoints getPredictedDatapoints() {
        return predictedDatapoints;
    }

    public void setPredictedDatapoints(PredictedDatapoints predictedDatapoints) {
        this.predictedDatapoints = predictedDatapoints;
    }

    public RulesFacts getFacts() {
        return facts;
    }

    public void setFacts(RulesFacts facts) {
        this.facts = facts;
    }
}
