package org.openremote.model.rules.flow;

import java.util.Arrays;
import java.util.List;

public class SocketTypeMatcher {

    private static class MatchRule {
        NodeDataType type;
        List<NodeDataType> matches;

        MatchRule(NodeDataType type, List<NodeDataType> matches) {
            this.type = type;
            this.matches = matches;
        }
    }

    private static final List<MatchRule> matches = Arrays.asList(
            new MatchRule(NodeDataType.NUMBER_ARRAY, Arrays.asList(NodeDataType.NUMBER)),
            new MatchRule(NodeDataType.NUMBER, Arrays.asList(NodeDataType.NUMBER, NodeDataType.NUMBER_ARRAY, NodeDataType.STRING)),
            new MatchRule(NodeDataType.STRING, Arrays.asList(NodeDataType.STRING)),
            new MatchRule(NodeDataType.TRIGGER, Arrays.asList(NodeDataType.TRIGGER)),
            new MatchRule(NodeDataType.BOOLEAN, Arrays.asList(NodeDataType.BOOLEAN, NodeDataType.STRING)),
            new MatchRule(NodeDataType.COLOR, Arrays.asList(NodeDataType.COLOR, NodeDataType.STRING)),
            new MatchRule(NodeDataType.NUMBER_ARRAY, Arrays.asList(NodeDataType.NUMBER))
    );

    public static boolean match(NodeDataType a, NodeDataType b) {
        if (a == NodeDataType.ANY || b == NodeDataType.ANY) {
            return true;
        }
        return matches.stream()
                .filter(rule -> rule.type == a)
                .findFirst()
                .map(rule -> rule.matches.contains(b))
                .orElse(false);
    }
}
