package org.openremote.manager.rules.flow;

import org.openremote.container.timer.TimerService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebResource;
import org.openremote.model.http.RequestParams;
import org.openremote.model.rules.flow.FlowResource;
import org.openremote.model.rules.flow.Node;
import org.openremote.model.rules.flow.NodeType;

import java.util.Arrays;
import java.util.logging.Logger;

public class FlowResourceImpl extends ManagerWebResource implements FlowResource {

    private static final Logger LOG = Logger.getLogger(FlowResourceImpl.class.getName());

    public FlowResourceImpl(TimerService timerService, ManagerIdentityService identityService) {
        super(timerService, identityService);
        for (Node node : Arrays.stream(NodeModel.values()).map(NodeModel::getDefinition).toArray(Node[]::new)) {
            LOG.finest("Node found: " + node.getName());
        }
    }

    @Override
    public Node[] getAllNodeDefinitions(RequestParams requestParams) {
        return Arrays.stream(NodeModel.values()).map(NodeModel::getDefinition).toArray(Node[]::new);
    }

    @Override
    public Node[] getAllNodeDefinitionsByType(RequestParams requestParams, NodeType type) {
        return Arrays.stream(NodeModel.values()).filter((n) -> n.getDefinition().getType().equals(type)).map(NodeModel::getDefinition).toArray(Node[]::new);
    }

    @Override
    public Node getNodeDefinition(RequestParams requestParams, String name) {
        return NodeModel.getDefinitionFor(name);
    }
}
