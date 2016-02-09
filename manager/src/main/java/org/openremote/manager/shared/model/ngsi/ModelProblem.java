package org.openremote.manager.shared.model.ngsi;

public enum ModelProblem {

    FIELD_EMPTY("Field can not be empty"),
    FIELD_INVALID_CHARS("Field contains invalid characters"),
    FIELD_TOO_LONG("Field too long");

    protected String message;

    ModelProblem(String message) {
        this.message = message;
    }
}
