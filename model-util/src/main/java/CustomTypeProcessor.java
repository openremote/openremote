/*
 * Copyright 2021, OpenRemote Inc.
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

import com.fasterxml.jackson.databind.JsonNode;
import cz.habarta.typescript.generator.TsType;
import cz.habarta.typescript.generator.TypeProcessor;
import cz.habarta.typescript.generator.util.GenericsResolver;
import cz.habarta.typescript.generator.util.Utils;
import org.openremote.model.util.TsIgnore;
import org.openremote.model.util.TsIgnoreTypeParams;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Does some custom processing for our specific model and fixes any anomalies in the plugin itself:
 * <ul>
 * <li>Ignore types/super types annotated with {@link TsIgnore}
 * <li>Will ignore types with a super type in the "com.fasterxml.jackson" package excluding those implementing {@link com.fasterxml.jackson.databind.JsonNode}</li>
 * <li>Removes some or all type params from classes annotated with {@link TsIgnoreTypeParams}
 * <li>Special processing for AssetModelInfo meta item value descriptors as JsonSerialize extension doesn't support @JsonSerialize(contentConverter=...)
 * </ul>
 */
public class CustomTypeProcessor implements TypeProcessor {

    public static final String JACKSON_PACKAGE = "com.fasterxml.jackson";

    @Override
    public Result processType(Type javaType, Context context) {
        Class<?> rawClass = Utils.getRawClassOrNull(javaType);

        if (rawClass == null) {
            return null;
        }

        // Look through type hierarchy
        while (rawClass != null && rawClass != Object.class) {
            // Look for TsIgnore annotation
            if (rawClass.getAnnotation(TsIgnore.class) != null) {
                return new Result(TsType.Any);
            }

            if (JsonNode.class.isAssignableFrom(rawClass)) {
                return null;
            }

            if (rawClass.getName().startsWith(JACKSON_PACKAGE)) {
                return new Result(TsType.Any);
            }
            rawClass = rawClass.getSuperclass();
        }

        rawClass = Utils.getRawClassOrNull(javaType);

        if (javaType instanceof ParameterizedType) {

            // Exclude type params
            TsIgnoreTypeParams ignoreTypeParams = rawClass.getAnnotation(TsIgnoreTypeParams.class);

            if (ignoreTypeParams != null) {

                if (ignoreTypeParams.paramIndexes().length == 0) {
                    // Ignore all
                    return new Result(new TsType.BasicType(rawClass.getSimpleName()));
                }

                // Ignore specified
                final ParameterizedType parameterizedType = (ParameterizedType) javaType;
                List<Integer> removeIndexes = Arrays.stream(ignoreTypeParams.paramIndexes()).boxed().toList();

                if (parameterizedType.getRawType() instanceof final Class<?> javaClass) {
                    final List<Class<?>> discoveredClasses = new ArrayList<>();
                    discoveredClasses.add(javaClass);
                    final Type[] typeArguments = parameterizedType.getActualTypeArguments();
                    final List<TsType> tsTypeArguments = new ArrayList<>();
                    for (int i=0; i<typeArguments.length; i++) {
                        if (!removeIndexes.contains(i)) {
                            final TypeProcessor.Result typeArgumentResult = context.processType(typeArguments[i]);
                            tsTypeArguments.add(typeArgumentResult.getTsType());
                            discoveredClasses.addAll(typeArgumentResult.getDiscoveredClasses());
                        }
                    }
                    return new Result(new TsType.GenericReferenceType(context.getSymbol(javaClass), tsTypeArguments), discoveredClasses);
                }
            }
        }

        return null;
    }
}
