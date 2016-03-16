package org.openremote.manager.shared.ngsi.simplequery;

public class UnaryStatement extends Statement {

    enum Operator {
        EXISTS(""),
        NOT_EXISTS("!");

        final protected String op;

        Operator(String op) {
            this.op = op;
        }

        @Override
        public String toString() {
            return op;
        }
    }

    public UnaryStatement(String statement) {
        super(statement);
    }

    public UnaryStatement(String attributeName, Operator operator) {
        this(operator + attributeName);
    }

}
