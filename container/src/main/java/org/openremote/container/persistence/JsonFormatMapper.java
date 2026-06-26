package org.openremote.container.persistence;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.format.FormatMapper;
import org.openremote.model.util.ValueUtil;

import java.lang.reflect.Modifier;
import java.lang.reflect.Type;

/**
 * A {@link FormatMapper} that uses our own {@link ObjectMapper}.
 */
@SuppressWarnings("unchecked")
public class JsonFormatMapper implements FormatMapper {

    @Override
    public <T> T fromString(CharSequence charSequence, JavaType<T> javaType, WrapperOptions wrapperOptions) {
        if ( javaType.getJavaType() == String.class ) {
            return (T) charSequence.toString();
        }
        ObjectMapper mapper = ValueUtil.JSON;
        try {
            return mapper.readValue( charSequence.toString(), mapper.constructType( javaType.getJavaType() ) );
        }
        catch (JacksonException e) {
            throw new IllegalArgumentException( "Could not deserialize string to java type: " + javaType, e );
        }
    }

    @Override
    public <T> String toString(T value, JavaType<T> javaType, WrapperOptions wrapperOptions) {
        if ( javaType.getJavaType() == String.class ) {
            return (String) value;
        }
        ObjectMapper mapper = ValueUtil.JSON;
        try {
            Type targetType = javaType.getJavaType();
            Class<?> rawClass = mapper.constructType( targetType ).getRawClass();
            if ( value != null && ( rawClass.isInterface() || Modifier.isAbstract( rawClass.getModifiers() ) ) ) {
                return mapper.writeValueAsString( value );
            }
            return mapper.writerFor( mapper.constructType( targetType ) )
                .writeValueAsString( value );
        }
        catch (JacksonException e) {
            throw new IllegalArgumentException( "Could not serialize object of java type: " + javaType, e );
        }
    }
}
