package org.openremote.manager.shared.ngsi;

public class ModelValidationError {

    protected final String affectedItem;
    protected final ModelProblem problem;

    public ModelValidationError(String affectedItem, ModelProblem problem) {
        this.affectedItem = affectedItem;
        this.problem = problem;
    }

    public String getAffectedItem() {
        return affectedItem;
    }

    public ModelProblem getProblem() {
        return problem;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "affectedItem='" + affectedItem + '\'' +
            ", problem=" + problem +
            '}';
    }
}