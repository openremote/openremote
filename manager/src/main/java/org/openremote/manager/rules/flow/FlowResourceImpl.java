package org.openremote.manager.rules.flow;

import org.openremote.container.timer.TimerService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebResource;
import org.openremote.model.http.RequestParams;
import org.openremote.model.rules.flow.*;

import java.util.Arrays;
import java.util.Objects;
import java.util.logging.Logger;

public class FlowResourceImpl extends ManagerWebResource implements FlowResource {

    private static final Logger LOG = Logger.getLogger(FlowResourceImpl.class.getName());

    private final Node[] nodes;

    public FlowResourceImpl(TimerService timerService, ManagerIdentityService identityService) {
        super(timerService, identityService);
        Arrays.stream(NodeModel.values()).map(NodeModel::getDefinition).forEach(n -> {
            LOG.finest("Node found: " + n.getName());
        });

        nodes = Arrays.stream(NodeModel.values()).
                map(NodeModel::getDefinition)
                // Filter out LOG_OUTPUT node
                .filter(definition -> !Objects.equals(definition.getName(), "LOG_OUTPUT"))
                .toArray(Node[]::new);

    }

    @Override
    public Node[] getAllNodeDefinitions(RequestParams requestParams) {
        return nodes;
    }

    @Override
    public Node[] getAllNodeDefinitionsByType(RequestParams requestParams, NodeType type) {
        return Arrays.stream(nodes).filter(n -> n.getType() == type).toArray(Node[]::new);
    }

    @Override
    public Node getNodeDefinition(RequestParams requestParams, String name) {
        return NodeModel.getDefinitionFor(name);
    }
}
