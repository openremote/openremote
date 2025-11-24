package org.openremote.manager.rules.flow;

import org.openremote.manager.rules.RulesFacts;
import org.openremote.model.attribute.AttributeInfo;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.rules.*;
import org.openremote.model.rules.flow.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
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

    private Map<AttributeRef, AttributeInfo> attributeInfoCache;
    protected Logger LOG;

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
        attributeInfoCache = null;
    }

    public NodeExecutionRequestInfo(NodeCollection collection, int outputSocketIndex, NodeSocket outputSocket,
                                    Node node, NodeSocket[] inputs, NodeSocket[] outputs, NodeInternal[] internals,
                                    RulesFacts facts, Assets assets, Users users, Notifications notifications,
                                    HistoricDatapoints historicDatapoints, PredictedDatapoints predictedDatapoints,
                                    Map<AttributeRef, AttributeInfo> attributeInfoCache) {
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
        this.attributeInfoCache = attributeInfoCache;
    }

    public NodeExecutionRequestInfo(NodeCollection collection, Node node, NodeSocket socket, RulesFacts facts,
                                    Assets assets, Users users, Notifications notifications,
                                    HistoricDatapoints historicDatapoints, PredictedDatapoints predictedDatapoints,
                                    Map<AttributeRef, AttributeInfo> attributeInfoCache, Logger log) {
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
        this.attributeInfoCache = attributeInfoCache;
        this.LOG = log;
    }

    public Object getValueFromInput(int index) {
        NodeSocket aSocket = getInputs()[index];
        Node aNode = getCollection().getNodeById(aSocket.getNodeId());
        NodeExecutionResult result = NodeModel.getImplementationFor(aNode.getName()).execute(
            new NodeExecutionRequestInfo(getCollection(), aNode, aSocket, getFacts(), getAssets(), getUsers(), getNotifications(), getHistoricDatapoints(), getPredictedDatapoints(), getAttributeInfoCache(), LOG)
        );
        if (result == null) {
            return null;
        }
        cacheNodeExecutionResult(result);
        return result.getValue();
    }

    public NodeDataType getTypeFromInput(int index) {
        NodeSocket aSocket = getInputs()[index];
        return aSocket.getType();
    }

    public Optional<AttributeInfo> findCachedAttribute(AttributeRef attributeRef) {
        if (attributeInfoCache == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(attributeInfoCache.get(attributeRef));
    }

    public void cacheNodeExecutionResult(NodeExecutionResult result) {
        if (result.getAttributeInfo() == null) {
            return;
        }
        if (attributeInfoCache == null) {
            attributeInfoCache = new ConcurrentHashMap<>();
        }
        attributeInfoCache.put(result.getAttributeRef(), result.getAttributeInfo());
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

    public Map<AttributeRef, AttributeInfo> getAttributeInfoCache() {
        return attributeInfoCache;
    }

    public void setAttributeInfoCache(Map<AttributeRef, AttributeInfo> attributeInfoCache) {
        this.attributeInfoCache = attributeInfoCache;
    }

    public RulesFacts getFacts() {
        return facts;
    }

    public void setFacts(RulesFacts facts) {
        this.facts = facts;
    }
}
