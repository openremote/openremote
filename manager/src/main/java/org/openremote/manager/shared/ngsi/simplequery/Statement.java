package org.openremote.manager.shared.ngsi.simplequery;

public class Statement {

    final protected String statement;

    public Statement(String statement) {
        this.statement = statement;
    }

    @Override
    public String toString() {
        return statement;
    }
}
