package org.openremote.manager.rules.flow;

public interface NodeImplementation {
    Object execute(NodeExecutionRequestInfo info);
}

