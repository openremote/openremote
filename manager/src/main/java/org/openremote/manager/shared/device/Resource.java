package org.openremote.manager.shared.device;

public class Resource {
    public enum Access {
        R,
        W,
        RW
    }

    public enum Type {
        NONE,
        BOOLEAN,
        STRING,
        INTEGER,
        DOUBLE,
        DATETIME
    }

    protected String name;
    protected String description;
    protected Access access;
    protected Type type;
    protected String units;
    protected boolean isCollection;

    public Resource() {
    }

    public Resource(String name, String description, Type type, Access access, boolean isCollection, String units) {
        this.type = type;
        this.name = name;
        this.access = access;
        this.units = units;
        this.description = description;
        this.isCollection = isCollection;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Type getType() {
        return type;
    }

    public Access getAccess() {
        return access;
    }

    public String getUnits() {
        return units;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setAccess(Access access) {
        this.access = access;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public void setUnits(String units) {
        this.units = units;
    }

    public boolean isCollection() {
        return isCollection;
    }

    public void setCollection(boolean collection) {
        this.isCollection = collection;
    }
}
