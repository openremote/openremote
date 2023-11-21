
package org.openremote.model.SensorThings;

public class UnitOfMeasurement {

    public String name;
    public String symbol;
    public String definition;


    public UnitOfMeasurement(String name, String symbol, String definition) {
        this.name = name;
        this.symbol = symbol;
        this.definition = definition;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getDefinition() {
        return definition;
    }

    public void setDefinition(String definition) {
        this.definition = definition;
    }

    @Override
    public String toString() {
        return "UnitOfMeasurement{" +
                "name='" + name + '\'' +
                ", symbol='" + symbol + '\'' +
                ", definition='" + definition + '\'' +
                '}';
    }
}
