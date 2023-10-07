package org.openremote.container.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.format.FormatMapper;
import org.openremote.model.util.ValueUtil;

/**
 * A {@link FormatMapper} that uses our own {@link com.fasterxml.jackson.databind.ObjectMapper}
 */
@SuppressWarnings("unchecked")
public class JsonFormatMapper implements FormatMapper {

    @Override
    public <T> T fromString(CharSequence charSequence, JavaType<T> javaType, WrapperOptions wrapperOptions) {
        if ( javaType.getJavaType() == String.class ) {
            return (T) charSequence.toString();
        }
        try {
            return ValueUtil.JSON.readValue( charSequence.toString(), ValueUtil.JSON.constructType( javaType.getJavaType() ) );
        }
        catch (JsonProcessingException e) {
            throw new IllegalArgumentException( "Could not deserialize string to java type: " + javaType, e );
        }
    }

    @Override
    public <T> String toString(T value, JavaType<T> javaType, WrapperOptions wrapperOptions) {
        if ( javaType.getJavaType() == String.class ) {
            return (String) value;
        }
        try {
            return ValueUtil.JSON.writerFor( ValueUtil.JSON.constructType( javaType.getJavaType() ) )
                .writeValueAsString( value );
        }
        catch (JsonProcessingException e) {
            throw new IllegalArgumentException( "Could not serialize object of java type: " + javaType, e );
        }
    }
}
