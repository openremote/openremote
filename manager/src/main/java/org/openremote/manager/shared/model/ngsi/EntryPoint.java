package org.openremote.manager.shared.model.ngsi;

import elemental.json.JsonObject;

public class EntryPoint {

    final protected JsonObject jsonObject;

    public EntryPoint(JsonObject jsonObject) {
        this.jsonObject = jsonObject;
    }

    public JsonObject getJsonObject() {
        return jsonObject;
    }

    public String getEntitiesLocation() {
        return jsonObject.hasKey("entities_url") ? jsonObject.getString("entities_url") : null;
    }

    public void setEntitiesLocation(String entitiesLocation) {
        jsonObject.put("entities_url", entitiesLocation);
    }

    public String getTypesLocation() {
        return jsonObject.hasKey("types_url") ? jsonObject.getString("types_url") : null;
    }

    public void setTypesLocation(String typesLocation) {
        jsonObject.put("types_url", typesLocation);
    }

    public String getSubscriptionsLocation() {
        return jsonObject.hasKey("subscriptions_url") ? jsonObject.getString("subscriptions_url") : null;
    }

    public void setSubscriptionsLocation(String subscriptionsLocation) {
        jsonObject.put("subscriptions_url", subscriptionsLocation);
    }

    public String getRegistrationsLocation() {
        return jsonObject.hasKey("registrations_url") ? jsonObject.getString("registrations_url") : null;
    }

    public void setRegistrationsLocation(String registrationsLocation) {
        jsonObject.put("registrations_url", registrationsLocation);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": " + jsonObject.toJson();
    }
}
