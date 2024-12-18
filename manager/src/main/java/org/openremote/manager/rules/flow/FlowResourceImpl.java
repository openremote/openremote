package org.openremote.manager.rules.flow;

import org.openremote.model.Container;
import org.openremote.container.timer.TimerService;
import org.openremote.container.util.MapAccess;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebResource;
import org.openremote.model.http.RequestParams;
import org.openremote.model.rules.flow.*;

import java.util.Arrays;
import java.util.logging.Logger;

public class FlowResourceImpl extends ManagerWebResource implements FlowResource {

	private static final Logger LOG = Logger.getLogger(FlowResourceImpl.class.getName());

	private Node[] nodes;

	public FlowResourceImpl(TimerService timerService, ManagerIdentityService identityService, Container container) {
		super(timerService, identityService);
		for (Node node : Arrays.stream(NodeModel.values()).map(NodeModel::getDefinition).toArray(Node[]::new)) {
			LOG.finest("Node found: " + node.getName());
		}

		// If OR_DEV_MODE, then include the DEBUG_TO_CONSOLE block.
		if (MapAccess.getBoolean(container.getConfig(), "OR_DEV_MODE", false)) {
			nodes = Arrays.stream(NodeModel.values()).map(NodeModel::getDefinition).toArray(Node[]::new);
		} else {
			nodes = Arrays.stream(NodeModel.values()).filter(e -> e != NodeModel.DEBUG_TO_CONSOLE).map(NodeModel::getDefinition).toArray(Node[]::new);
		}
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

	@Override
	public boolean getDoesMatch(RequestParams requestParams, NodeDataType fromType, NodeDataType toType) {
		return SocketTypeMatcher.match(fromType, toType);
	}
}
