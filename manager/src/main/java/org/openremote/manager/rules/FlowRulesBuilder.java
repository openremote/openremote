package org.openremote.manager.rules;

import org.jeasy.rules.api.Rule;
import org.jeasy.rules.core.RuleBuilder;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.rules.facade.NotificationsFacade;
import org.openremote.manager.rules.flow.*;
import org.openremote.model.rules.*;
import org.openremote.model.rules.flow.*;

import java.util.*;
import java.util.stream.Collectors;

public class FlowRulesBuilder {
    private AssetStorageService assetStorageService;
    private Map<String, Long> triggerMap = new LinkedHashMap<>();
    private List<NodeCollection> nodeCollections = new ArrayList<>();
    private Assets assetsFacade;
    private Users usersFacade;
    private Notifications notificationFacade;
    private HistoricDatapoints historicDatapointsFacade;
    private PredictedDatapoints predictedDatapointsFacade;

    private TimerService timerService;

    public FlowRulesBuilder(
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
                    RulesEngine.RULES_LOG.info("Flow rule created");
                    rules.add(createRule(collection.getName() + " - " + count, collection, node));
                    count++;
                } catch (Exception e) {
                    RulesEngine.RULES_LOG.severe("Flow rule error: " + e.getMessage());
                }
            }
        }
        return rules.toArray(new Rule[0]);
    }

    private Rule createRule(String name, NodeCollection collection, Node outputNode) throws Exception {
        Object implementationResult = NodeModel.getImplementationFor(outputNode.getName()).execute(new NodeExecutionRequestInfo(collection, outputNode, null, null, assetsFacade, usersFacade, notificationFacade, historicDatapointsFacade, predictedDatapointsFacade));

        if (implementationResult == null)
            throw new NullPointerException(outputNode.getName() + " node returns null");

        if (!(implementationResult instanceof RulesBuilder.Action))
            throw new Exception(outputNode.getName() + " node does not return an action");

        RulesBuilder.Action action = (RulesBuilder.Action) implementationResult;

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
                    Object result;
                    try {
                        result = condition.evaluate((RulesFacts) facts);
                    } catch (Exception ex) {
                        throw new RuntimeException("Error evaluating condition of rule '" + name + "': " + ex.getMessage(), ex);
                    }
                    if (result instanceof Boolean) {
                        return (boolean) result;
                    } else {
                        throw new IllegalArgumentException("Error evaluating condition of rule '" + name + "': result is not boolean but " + result);
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
            children.addAll(Arrays.stream(collection.getConnections()).filter(c -> c.getTo().equals(s.getId())).map(c -> collection.getNodeById(collection.getSocketById(c.getFrom()).getNodeId())).collect(Collectors.toList()));
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
