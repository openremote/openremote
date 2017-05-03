/*
 * Copyright 2017, OpenRemote Inc.
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
package org.openremote.container.json;

import elemental.json.impl.JsonUtil;
import org.jboss.resteasy.plugins.providers.ProviderHelper;
import org.openremote.model.value.Value;
import org.openremote.model.value.Values;

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

@Provider
public class ElementalMessageBodyConverter implements MessageBodyReader<Value>, MessageBodyWriter<Value> {

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return Value.class.isAssignableFrom(type) && mediaType.equals(MediaType.APPLICATION_JSON_TYPE);
    }

    @Override
    public Value readFrom(Class<Value> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        String str = ProviderHelper.readString(entityStream, mediaType);
        return Values.parse(str).orElseThrow(() -> new IOException("Empty JSON data"));
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return Value.class.isAssignableFrom(type) && mediaType.equals(MediaType.APPLICATION_JSON_TYPE);
    }

    @Override
    public long getSize(Value value, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(Value value, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        String str = value.toJson();
        String charset = mediaType.getParameters().get("charset");
        if (charset == null) entityStream.write(str.getBytes("UTF-8"));
        else entityStream.write(str.getBytes(charset));
    }
}
