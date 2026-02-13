package org.openremote.manager.rules.flow;

public interface NodeImplementation {
    NodeExecutionResult execute(NodeExecutionRequestInfo info) throws RuntimeException;
}

