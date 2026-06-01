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

import com.fasterxml.jackson.annotation.JsonProperty;
import cz.habarta.typescript.generator.Extension;
import cz.habarta.typescript.generator.TsType;
import cz.habarta.typescript.generator.compiler.ModelCompiler;
import cz.habarta.typescript.generator.compiler.ModelTransformer;
import cz.habarta.typescript.generator.compiler.SymbolTable;
import cz.habarta.typescript.generator.compiler.TsModelTransformer;
import cz.habarta.typescript.generator.emitter.EmitterExtensionFeatures;
import cz.habarta.typescript.generator.emitter.TsBeanModel;
import cz.habarta.typescript.generator.emitter.TsPropertyModel;
import cz.habarta.typescript.generator.parser.BeanModel;
import cz.habarta.typescript.generator.parser.Model;
import cz.habarta.typescript.generator.parser.PropertyAccess;
import cz.habarta.typescript.generator.parser.PropertyModel;
import org.openremote.model.asset.AssetTypeInfo;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.provisioning.X509ProvisioningConfig;
import org.openremote.model.util.TsIgnoreTypeParams;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Does some custom processing for our specific model and fixes any anomalies in the plugin itself:
 * <ul>
 * <li>Removes some or all type params from classes annotated with {@link TsIgnoreTypeParams}
 * <li>Special processing for AssetModelInfo meta item value descriptors as JsonSerialize extension doesn't support @JsonSerialize(contentConverter=...)
 * </ul>
 */
public class CustomExtension extends Extension {

    @Override
    public EmitterExtensionFeatures getFeatures() {
        return new EmitterExtensionFeatures();
    }

    @Override
    public List<TransformerDefinition> getTransformers() {
        return Arrays.asList(
            // This is a hack to fix breaking change with latest version of this plugin
            new TransformerDefinition(ModelCompiler.TransformationPhase.AfterDeclarationSorting, (TsModelTransformer) (context, model) -> {
                TsBeanModel provBean = model.getBean(X509ProvisioningConfig.class);
                if (provBean != null) {
                    provBean.getExtendsList().removeFirst();
                    provBean.getExtendsList().add(provBean.getParent());
                }
                return model;
            }),
            new TransformerDefinition(ModelCompiler.TransformationPhase.BeforeEnums, (TsModelTransformer) (context, model) -> {

                TsBeanModel assetTypeInfoBean = model.getBean(AssetTypeInfo.class);
                if (assetTypeInfoBean != null) {
                    assetTypeInfoBean.getProperties().replaceAll(p -> p.getName().equals("metaItemDescriptors") || p.getName().equals("valueDescriptors") ? new TsPropertyModel(p.getName(), new TsType.BasicArrayType(TsType.String), p.modifiers, p.ownProperty, p.comments) : p);
                }

                // Remove the type parameter - this works in conjunction with the CustomTypeProcessor which replaces
                // field references
                model.getBeans().replaceAll(bean -> {

                    if (bean.getOrigin() != null && bean.getOrigin().getAnnotation(TsIgnoreTypeParams.class) != null) {
                        if (bean.getTypeParameters() != null) {
                            TsIgnoreTypeParams ignoreTypeParams = bean.getOrigin().getAnnotation(TsIgnoreTypeParams.class);
                            if (ignoreTypeParams.paramIndexes().length == 0) {
                                bean.getTypeParameters().clear();
                            } else {
                                Arrays.stream(ignoreTypeParams.paramIndexes())
                                    .boxed()
                                    .sorted(Collections.reverseOrder())
                                    .forEach(index -> bean.getTypeParameters().remove(index.intValue()));
                            }
                        }
                    }

                    return bean;
                });

                return model;
            }),
            new cz.habarta.typescript.generator.Extension.TransformerDefinition(ModelCompiler.TransformationPhase.BeforeTsModel, new ModelTransformer() {
                @Override
                public Model transformModel(SymbolTable symbolTable, Model model) {

                    model.getBeans().replaceAll(CustomExtension::addExplicitJsonProperties);

                    // Remove attribute state from attribute event (can't do this with annotations)
                    BeanModel attrEventBean = model.getBean(AttributeEvent.class);
                    if (attrEventBean != null) {
                        attrEventBean.getProperties().removeIf(pm -> pm.getName().equals("attributeState"));
                    }
                    return model;
                }
            })
        );
    }

    private static BeanModel addExplicitJsonProperties(BeanModel bean) {
        Class<?> origin = bean.getOrigin();
        if (origin == null) {
            return bean;
        }

        List<PropertyModel> properties = new ArrayList<>(bean.getProperties());
        Set<String> propertyNames = new HashSet<>();
        properties.forEach(property -> propertyNames.add(property.getName()));

        for (Field field : origin.getDeclaredFields()) {
            JsonProperty jsonProperty = field.getAnnotation(JsonProperty.class);
            if (jsonProperty == null || Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            String propertyName = getJsonPropertyName(jsonProperty, field.getName());
            if (propertyNames.add(propertyName)) {
                properties.add(new PropertyModel(propertyName, field.getGenericType(), true, PropertyAccess.ReadWrite, field, null, null, null));
            }
        }

        for (Method method : origin.getMethods()) {
            JsonProperty jsonProperty = method.getAnnotation(JsonProperty.class);
            if (jsonProperty == null || Modifier.isStatic(method.getModifiers()) || method.getParameterCount() != 0 || method.getReturnType() == Void.TYPE) {
                continue;
            }

            String defaultName = getBeanPropertyName(method);
            if (defaultName == null) {
                continue;
            }

            String propertyName = getJsonPropertyName(jsonProperty, defaultName);
            if (propertyNames.add(propertyName)) {
                properties.add(new PropertyModel(propertyName, method.getGenericReturnType(), true, PropertyAccess.ReadWrite, method, null, null, null));
            }
        }

        if (properties.size() == bean.getProperties().size()) {
            return bean;
        }

        return new BeanModel(bean.getOrigin(), bean.getParent(), bean.getTaggedUnionClasses(), bean.getDiscriminantProperty(), bean.getDiscriminantLiteral(), bean.getInterfaces(), properties, bean.getComments());
    }

    private static String getJsonPropertyName(JsonProperty jsonProperty, String defaultName) {
        return jsonProperty.value() == null || jsonProperty.value().isEmpty() ? defaultName : jsonProperty.value();
    }

    private static String getBeanPropertyName(Method method) {
        String methodName = method.getName();
        if (methodName.startsWith("get") && methodName.length() > 3) {
            return Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
        }
        if (methodName.startsWith("is") && methodName.length() > 2 && method.getReturnType() == Boolean.TYPE) {
            return Character.toLowerCase(methodName.charAt(2)) + methodName.substring(3);
        }
        return null;
    }
}
