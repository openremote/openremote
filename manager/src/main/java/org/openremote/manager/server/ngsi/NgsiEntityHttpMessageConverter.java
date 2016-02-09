package org.openremote.manager.server.ngsi;

import com.google.common.base.Charsets;
import com.hubrick.vertx.rest.HttpInputMessage;
import com.hubrick.vertx.rest.HttpOutputMessage;
import com.hubrick.vertx.rest.MediaType;
import com.hubrick.vertx.rest.converter.AbstractHttpMessageConverter;
import com.hubrick.vertx.rest.exception.HttpMessageConverterException;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.impl.JsonUtil;
import org.openremote.manager.shared.model.ngsi.AbstractEntity;
import org.openremote.manager.shared.model.ngsi.Entity;
import org.openremote.manager.shared.model.ngsi.KeyValueEntity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NgsiEntityHttpMessageConverter extends AbstractHttpMessageConverter<Object> {

    public NgsiEntityHttpMessageConverter() {
        super(
            new MediaType("application", "json", Charsets.UTF_8),
            new MediaType("application", "*+json", Charsets.UTF_8)
        );
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return clazz.equals(Entity.class)
            || clazz.equals(Entity[].class)
            || clazz.equals(KeyValueEntity[].class)
            || clazz.equals(KeyValueEntity.class);
    }

    @Override
    protected Object readInternal(Class<?> clazz, HttpInputMessage input) throws HttpMessageConverterException {
        try {
            if (clazz.equals(Entity.class)) {
                JsonObject jsonObject = JsonUtil.parse(new String(input.getBody(), "utf-8"));
                return new Entity(jsonObject);
            } else if (clazz.equals(Entity[].class)) {
                List<Entity> list = new ArrayList<>();
                JsonArray jsonArray = JsonUtil.parse(new String(input.getBody(), "utf-8"));
                for (int i = 0; i < jsonArray.length(); i++) {
                    JsonObject jsonObject = jsonArray.get(i);
                    list.add(new Entity(jsonObject));
                }
                return list.toArray(new Entity[list.size()]);
            } else if (clazz.equals(KeyValueEntity.class)) {
                JsonObject jsonObject = JsonUtil.parse(new String(input.getBody(), "utf-8"));
                return new KeyValueEntity(jsonObject);
            } else if (clazz.equals(KeyValueEntity[].class)) {
                List<KeyValueEntity> list = new ArrayList<>();
                JsonArray jsonArray = JsonUtil.parse(new String(input.getBody(), "utf-8"));
                for (int i = 0; i < jsonArray.length(); i++) {
                    JsonObject jsonObject = jsonArray.get(i);
                    list.add(new KeyValueEntity(jsonObject));
                }
                return list.toArray(new KeyValueEntity[list.size()]);
            } else {
                throw new HttpMessageConverterException("Unsupported type: " + clazz);
            }
        } catch (IOException ex) {
            throw new HttpMessageConverterException("Error converting from JSON", ex);
        }
    }

    @Override
    protected void writeInternal(Object object, HttpOutputMessage output) throws HttpMessageConverterException {
        try {
            if (object instanceof AbstractEntity) {
                AbstractEntity entity = (AbstractEntity) object;
                output.write(entity.getJsonObject().toJson().getBytes("utf-8"));
            } else if (object instanceof AbstractEntity[]) {
                AbstractEntity[] abstractEntities = (AbstractEntity[]) object;
                JsonArray jsonArray = Json.createArray();
                for (int i = 0; i < abstractEntities.length; i++) {
                    jsonArray.set(i, abstractEntities[i].getJsonObject());
                }
                output.write(jsonArray.toJson().getBytes("utf-8"));
            }
        } catch (Exception ex) {
            throw new HttpMessageConverterException("Error converting to JSON", ex);
        }
    }
}
