package org.openremote.manager.shared.ngsi.simplequery;

public class Query {

    public static final String STATEMENT_SEPARATOR = ";";

    public Statement[] statements;

    public Query() {
    }

    public Query(Statement... statements) {
        this.statements = statements;
    }

    public Query statements(Statement... statements) {
        this.statements = statements;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (statements != null) {
            for (Statement statement : statements) {
                sb.append(statement).append(STATEMENT_SEPARATOR);
            }
        }
        return sb.toString();
    }

    public static Query valueOf(String query) {
        if (query == null)
            return null;
        String[] split = query.split(STATEMENT_SEPARATOR);
        Statement[] statements = new Statement[split.length];
        for (int i = 0; i < statements.length; i++) {
            statements[i] = new Statement(split[i]);
        }
        return new Query(statements);
    }
}
