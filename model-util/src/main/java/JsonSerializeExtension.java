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

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.util.Converter;
import cz.habarta.typescript.generator.compiler.ModelCompiler;
import cz.habarta.typescript.generator.compiler.ModelTransformer;
import cz.habarta.typescript.generator.compiler.SymbolTable;
import cz.habarta.typescript.generator.emitter.EmitterExtensionFeatures;
import cz.habarta.typescript.generator.emitter.TsModel;
import cz.habarta.typescript.generator.parser.BeanModel;
import cz.habarta.typescript.generator.parser.Model;
import cz.habarta.typescript.generator.parser.PropertyModel;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Extension for applying {@link JsonSerialize} annotation. Supports:
 * <ul>
 * <li>{@link JsonSerialize#as}</li>
 * <li>{@link JsonSerialize#converter}</li>
 * </ul>
 */
@SuppressWarnings("deprecation")
public class JsonSerializeExtension extends cz.habarta.typescript.generator.Extension {

    @Override
    public EmitterExtensionFeatures getFeatures() {
        return new EmitterExtensionFeatures();
    }

    @Override
    public List<cz.habarta.typescript.generator.Extension.TransformerDefinition> getTransformers() {
        return Arrays.asList(
            new cz.habarta.typescript.generator.Extension.TransformerDefinition(ModelCompiler.TransformationPhase.BeforeTsModel, new ModelTransformer() {
                @Override
                public Model transformModel(SymbolTable symbolTable, Model model) {
                    // Look for @JsonSerialize annotation and modify the property type accordingly
                    List<BeanModel> beans = model.getBeans();
                    beans.replaceAll(bean -> {
                        AtomicBoolean modified = new AtomicBoolean(false);

                        List<PropertyModel> properties = bean.getProperties().stream().map(p -> {
                            Member member = p.getOriginalMember();
                            JsonSerialize jsonSerialize = null;

                            if (member instanceof Field) {
                                Field field = (Field)member;
                                jsonSerialize = field.getAnnotation(JsonSerialize.class);
                            } else if (member instanceof Method) {
                                Method method = (Method)member;
                                jsonSerialize = method.getAnnotation(JsonSerialize.class);
                            }

                            if (jsonSerialize != null) {
                                // TODO: Add support for other options
                                if (jsonSerialize.as() != Void.class) {
                                    modified.set(true);
                                    return new PropertyModel(p.getName(), jsonSerialize.as(), p.isOptional(), p.getAccess(), p.getOriginalMember(), p.getPullProperties(), p.getContext(), p.getComments());
                                }
                                if (jsonSerialize.converter() != Converter.None.class) {
                                    // Type info is not accessible with reflection so instantiate the converter
                                    Method convertMethod = Arrays.stream(jsonSerialize.converter().getMethods()).filter(m -> m.getName().equals("convert")).findFirst().orElse(null);
                                    if (convertMethod != null) {
                                        modified.set(true);
                                        return new PropertyModel(p.getName(), convertMethod.getGenericReturnType(), p.isOptional(), p.getAccess(), p.getOriginalMember(), p.getPullProperties(), p.getContext(), p.getComments());
                                    }
                                }
                            }

                            return p;
                        }).collect(Collectors.toList());

                        if (modified.get()) {
                            return new BeanModel(bean.getOrigin(), bean.getParent(), bean.getTaggedUnionClasses(), bean.getDiscriminantProperty(), bean.getDiscriminantLiteral(), bean.getInterfaces(), properties, bean.getComments());
                        }

                        return bean;
                    });

                    return model;
                }
            })
        );
    }
}
