package org.openremote.manager.rules;

import org.jeasy.rules.api.Rule;
import org.jeasy.rules.core.RuleBuilder;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.rules.flow.NodeExecutionRequestInfo;
import org.openremote.manager.rules.flow.NodeModel;
import org.openremote.manager.rules.flow.NodeTriggerFunction;
import org.openremote.manager.rules.flow.NodeTriggerParameters;
import org.openremote.model.rules.*;
import org.openremote.model.rules.flow.Node;
import org.openremote.model.rules.flow.NodeCollection;
import org.openremote.model.rules.flow.NodeSocket;
import org.openremote.model.rules.flow.NodeType;

import java.util.*;
import java.util.logging.Logger;

public class FlowRulesBuilder {

    protected final Logger LOG;
    protected final AssetStorageService assetStorageService;
    protected final Map<String, Long> triggerMap = new HashMap<>();
    protected final List<NodeCollection> nodeCollections = new ArrayList<>();
    protected final Assets assetsFacade;
    protected final Users usersFacade;
    protected final Notifications notificationFacade;
    protected final HistoricDatapoints historicDatapointsFacade;
    protected final PredictedDatapoints predictedDatapointsFacade;
    protected final TimerService timerService;
  
    public FlowRulesBuilder(
        Logger logger,
        TimerService timerService,
        AssetStorageService assetStorageService,
        Assets assetsFacade,
        Users usersFacade,
        Notifications notificationFacade,
        HistoricDatapoints historicDatapointsFacade,
        PredictedDatapoints predictedDatapointsFacade) {
        this.timerService = timerService;
        this.assetStorageService = assetStorageService;
        this.assetsFacade = assetsFacade;
        this.usersFacade = usersFacade;
        this.notificationFacade = notificationFacade;
        this.historicDatapointsFacade = historicDatapointsFacade;
        this.predictedDatapointsFacade = predictedDatapointsFacade;
        LOG = logger;
    }

    public void add(NodeCollection nodeCollection) {
        nodeCollections.add(nodeCollection);
    }

    public Rule[] build() {
        int count = 0;
        List<Rule> rules = new ArrayList<>();
        for (NodeCollection collection : nodeCollections) {
            for (Node node : collection.getNodes()) {
                if (node.getType() != NodeType.OUTPUT) continue;
                try {
                    LOG.fine("Flow rule created");
                    rules.add(createRule(collection.getName() + " - " + count, collection, node));
                    count++;
                } catch (Exception e) {
                    LOG.severe("Flow rule error: " + e.getMessage());
                }
            }
        }
        return rules.toArray(new Rule[0]);
    }

    private Rule createRule(String name, NodeCollection collection, Node outputNode) throws Exception {
        Object implementationResult = NodeModel.getImplementationFor(outputNode.getName()).execute(new NodeExecutionRequestInfo(collection, outputNode, null, null, assetsFacade, usersFacade, notificationFacade, historicDatapointsFacade, predictedDatapointsFacade));

        if (!(implementationResult instanceof RulesBuilder.Action action))
            throw new Exception(outputNode.getName() + " node does not return an action");

        RulesBuilder.Condition condition = facts -> {
            List<Node> connectedTree = backtrackFrom(collection, outputNode);

            return connectedTree.stream().anyMatch(node -> {
                NodeTriggerFunction function = NodeModel.getTriggerFunctionFor(node.getName());
                return function.satisfies(new NodeTriggerParameters(name, facts, this, collection, node));
            });
        };

        triggerMap.put(name, -1L);

        return new RuleBuilder().
                name(name).
                description(collection.getDescription()).
                when(facts -> {
                    Object result = condition.evaluate((RulesFacts) facts);

                    if (result instanceof Boolean) {
                        return (boolean) result;
                    } else {
                        String msg = "Error evaluating condition of rule, expected boolean but got " + (result != null ? result.getClass() : "null");
                        LOG.warning(msg);
                        throw new IllegalArgumentException(msg);
                    }
                }).
                then(facts -> {
                    action.execute((RulesFacts) facts);
                    triggerMap.put(name, timerService.getCurrentTimeMillis());
                }).
                build();
    }

    private List<Node> backtrackFrom(NodeCollection collection, Node node) {
        List<Node> total = new ArrayList<>();
        List<Node> children = new ArrayList<>();

        for (NodeSocket s : node.getInputs()) {
            children.addAll(Arrays.stream(collection.getConnections()).filter(c -> c.getTo().equals(s.getId())).map(c -> collection.getNodeById(collection.getSocketById(c.getFrom()).getNodeId())).toList());
        }

        for (Node child : children) {
            total.add(child);
            total.addAll(backtrackFrom(collection, child));
        }

        return total;
    }

    public Map<String, Long> getTriggerMap() {
        return triggerMap;
    }
}
