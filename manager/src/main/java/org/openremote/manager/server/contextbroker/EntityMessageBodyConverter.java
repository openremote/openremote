package org.openremote.manager.server.contextbroker;

import elemental.json.Json;
import elemental.json.JsonObject;
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
import java.util.logging.Logger;

@Provider
public class EntityMessageBodyConverter
    implements MessageBodyReader<AbstractEntity>, MessageBodyWriter<AbstractEntity> {

    private static final Logger LOG = Logger.getLogger(EntityMessageBodyConverter.class.getName());

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return AbstractEntity.class.isAssignableFrom(type) && mediaType.equals(MediaType.APPLICATION_JSON_TYPE);
    }

    @Override
    public AbstractEntity readFrom(Class<AbstractEntity> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        String str = ProviderHelper.readString(entityStream, mediaType);
        JsonObject jsonObject = Json.parse(str);
        if (type.equals(Entity.class)) {
            return new Entity(jsonObject);
        } else if (type.equals(KeyValueEntity.class)) {
            return new KeyValueEntity(jsonObject);
        } else {
            throw new IOException("Unsupported type: " + type);
        }
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return AbstractEntity.class.isAssignableFrom(type) && mediaType.equals(MediaType.APPLICATION_JSON_TYPE);
    }

    @Override
    public long getSize(AbstractEntity value, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(AbstractEntity value, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        String str = value.getJsonObject().toJson();
        String charset = mediaType.getParameters().get("charset");
        if (charset == null) entityStream.write(str.getBytes("UTF-8"));
        else entityStream.write(str.getBytes(charset));
    }
}