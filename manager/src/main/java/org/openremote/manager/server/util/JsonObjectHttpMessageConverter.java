package org.openremote.manager.server.util;

import com.google.common.base.Charsets;
import com.hubrick.vertx.rest.HttpInputMessage;
import com.hubrick.vertx.rest.HttpOutputMessage;
import com.hubrick.vertx.rest.MediaType;
import com.hubrick.vertx.rest.converter.AbstractHttpMessageConverter;
import com.hubrick.vertx.rest.exception.HttpMessageConverterException;
import elemental.json.Json;
import elemental.json.JsonObject;

import java.io.IOException;

/**
 * TODO We might need this, still not sure how much we'd use the elemental.json API on server side _and_ in shared code
 */
public class JsonObjectHttpMessageConverter extends AbstractHttpMessageConverter<JsonObject> {

    public JsonObjectHttpMessageConverter() {
        super(
            new MediaType("application", "json", Charsets.UTF_8),
            new MediaType("application", "*+json", Charsets.UTF_8)
        );
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return JsonObject.class.isAssignableFrom(clazz);
    }

    @Override
    protected JsonObject readInternal(Class<? extends JsonObject> clazz, HttpInputMessage input) throws HttpMessageConverterException {
        try {
            return Json.parse(new String(input.getBody(), "utf-8"));
        } catch (IOException ex) {
            throw new HttpMessageConverterException("Error converting from raw JSON", ex);
        }
    }

    @Override
    protected void writeInternal(JsonObject object, HttpOutputMessage output) throws HttpMessageConverterException {
        try {
            output.write(object.toJson().getBytes("utf-8"));
        } catch (Exception ex) {
            throw new HttpMessageConverterException("Error converting to raw JSON", ex);
        }
    }

}
