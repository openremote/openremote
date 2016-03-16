package org.openremote.manager.shared.ngsi.simplequery;

public class BinaryStatement extends Statement {

    public enum Operator {
        EQUAL("=="),
        UNEQUAL("!="),
        GREATER_THAN(">"),
        LESS_THAN("<"),
        GREATER_OR_EQUAL_THAN(">="),
        LESS_OR_EQUAL_THAN("<=");

        String op;

        Operator(String op) {
            this.op = op;
        }

        @Override
        public String toString() {
            return op;
        }
    }

    public BinaryStatement(String statement) {
        super(statement);
    }

    public BinaryStatement(String attributeName, Operator operator, QueryValue queryValue) {
        this(attributeName + operator + queryValue);
    }
}
