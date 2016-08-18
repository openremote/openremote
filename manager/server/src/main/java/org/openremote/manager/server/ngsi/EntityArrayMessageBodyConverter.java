/*
 * Copyright 2016, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.manager.server.ngsi;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.impl.JsonUtil;
import org.jboss.resteasy.plugins.providers.ProviderHelper;
import org.openremote.manager.shared.ngsi.AbstractEntity;
import org.openremote.manager.shared.ngsi.Entity;
import org.openremote.manager.shared.ngsi.KeyValueEntity;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Provider
public class EntityArrayMessageBodyConverter
    implements MessageBodyReader<AbstractEntity[]>, MessageBodyWriter<AbstractEntity[]> {

    private static final Logger LOG = Logger.getLogger(EntityArrayMessageBodyConverter.class.getName());

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return AbstractEntity[].class.isAssignableFrom(type) && mediaType.equals(MediaType.APPLICATION_JSON_TYPE);
    }

    @Override
    public AbstractEntity[] readFrom(Class<AbstractEntity[]> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        String str = ProviderHelper.readString(entityStream, mediaType);
        JsonArray jsonArray = JsonUtil.parse(str);
        if (type.equals(Entity[].class)) {
            List<Entity> list = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                JsonObject jsonObject = jsonArray.get(i);
                list.add(new Entity(jsonObject));
            }
            return list.toArray(new Entity[list.size()]);
        } else if (type.equals(KeyValueEntity[].class)) {
            List<KeyValueEntity> list = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                JsonObject jsonObject = jsonArray.get(i);
                list.add(new KeyValueEntity(jsonObject));
            }
            return list.toArray(new KeyValueEntity[list.size()]);
        } else {
            throw new IOException("Unsupported type: " + type);
        }
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return AbstractEntity[].class.isAssignableFrom(type) && mediaType.equals(MediaType.APPLICATION_JSON_TYPE);
    }

    @Override
    public long getSize(AbstractEntity[] value, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(AbstractEntity[] value, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        JsonArray jsonArray = Json.createArray();
        for (int i = 0; i < value.length; i++) {
            JsonObject jsonObject = value[i].getJsonObject();
            jsonArray.set(i, jsonObject);
        }
        String str = jsonArray.toJson();
        String charset = mediaType.getParameters().get("charset");
        if (charset == null) entityStream.write(str.getBytes("UTF-8"));
        else entityStream.write(str.getBytes(charset));
    }
}