package org.openremote.manager.rules.flow;

public interface NodeTriggerFunction {
    boolean satisfies(NodeTriggerParameters parameters);
}
