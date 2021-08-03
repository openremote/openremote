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

import cz.habarta.typescript.generator.Extension;
import cz.habarta.typescript.generator.TsType;
import cz.habarta.typescript.generator.compiler.ModelCompiler;
import cz.habarta.typescript.generator.compiler.TsModelTransformer;
import cz.habarta.typescript.generator.emitter.EmitterExtensionFeatures;
import cz.habarta.typescript.generator.emitter.TsBeanModel;
import cz.habarta.typescript.generator.emitter.TsPropertyModel;
import org.openremote.model.asset.AssetTypeInfo;

import java.util.Arrays;
import java.util.List;

/**
 * Does some custom processing for our specific model:
 * <ul>
 * <li>Removes type params from classes in {@link Constants#IGNORE_TYPE_PARAMS_ON_CLASSES}</li>
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
            new TransformerDefinition(ModelCompiler.TransformationPhase.BeforeEnums, (TsModelTransformer) (context, model) -> {

                // Special processing for AssetModelInfo meta item value descriptors as JsonSerialize extension doesn't support
                // @JsonSerialize(contentConverter=...)
                TsBeanModel assetTypeInfoBean = model.getBean(AssetTypeInfo.class);

                if (assetTypeInfoBean != null) {
                    assetTypeInfoBean.getProperties().replaceAll(p -> p.getName().equals("metaItemDescriptors") || p.getName().equals("valueDescriptors") ? new TsPropertyModel(p.getName(), new TsType.BasicArrayType(TsType.String), p.modifiers, p.ownProperty, p.comments) : p);
                }

                Constants.IGNORE_TYPE_PARAMS_ON_CLASSES.forEach(beanClass -> {
                    TsBeanModel bean = model.getBean(beanClass);

                    if (bean != null && bean.getTypeParameters() != null) {
                        // Remove the type parameter - this works in conjunction with the CustomTypeProcessor which replaces
                        // field references
                        bean.getTypeParameters().clear();
                    }
                });

                return model;
            })
        );
    }
}
