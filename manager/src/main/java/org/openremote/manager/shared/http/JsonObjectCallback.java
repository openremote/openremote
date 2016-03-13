package org.openremote.manager.shared.http;

import elemental.json.JsonObject;
import org.openremote.manager.shared.Consumer;

public class JsonObjectCallback extends AbstractCallback<JsonObject> {

    public JsonObjectCallback(Consumer<JsonObject> onSuccess, Consumer<Exception> onFailure) {
        super(onSuccess, onFailure);
    }

    public JsonObjectCallback(int expectedStatusCode, Consumer<JsonObject> onSuccess, Consumer<Exception> onFailure) {
        super(expectedStatusCode, onSuccess, onFailure);
    }

    @Override
    protected JsonObject readMessageBody(Object entity) {
        return (JsonObject)entity;
        /* TODO The simple cast works, or we'd need something like this:
        if (entity instanceof JreJsonObject) {
            return (JreJsonObject) entity;
        }
        JavaScriptObject jso = (JavaScriptObject) entity;
        return jso.<JsJsonObject>cast();
        */
    }
}