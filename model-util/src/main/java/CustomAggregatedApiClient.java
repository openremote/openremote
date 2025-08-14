/*
 * Copyright 2024, OpenRemote Inc.
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
import cz.habarta.typescript.generator.Settings;
import cz.habarta.typescript.generator.emitter.EmitterExtension;
import cz.habarta.typescript.generator.emitter.EmitterExtensionFeatures;
import cz.habarta.typescript.generator.emitter.TsBeanModel;
import cz.habarta.typescript.generator.emitter.TsModel;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Creates a wrapper class around all of the clients the generator creates so that they can easily be exported as a single
 * entity; this is specifically for custom projects and creates a reference to the main rest client.
 */
public class CustomAggregatedApiClient extends EmitterExtension {

    private static final String fieldFormatString = "protected %1$s! : Axios%2$sClient;";
    private static final String getter1FormatString = "get %1$s() : Axios%1$sClient {";
    private static final String getter2FormatString = "if (this.%1$s != null) return this.%1$s;";
    private static final String getter3FormatString = "this.%1$s = new Axios%2$sClient(rest.baseUrl, rest.axiosInstance);";
    private static final String getter4FormatString = "return this.%1$s;";

    private static class FieldAndGetter {
        String field;
        String getter;

        public FieldAndGetter(String field, String getter) {
            this.field = field;
            this.getter = getter;
        }
    }

    @Override
    public EmitterExtensionFeatures getFeatures() {
        final EmitterExtensionFeatures features = new EmitterExtensionFeatures();
        features.generatesRuntimeCode = true;
        return features;
    }


    @Override
    public void emitElements(Writer writer, Settings settings, boolean exportKeyword, TsModel model) {

        writer.writeIndentedLine("import rest from \"@openremote/rest\";");
        writer.writeIndentedLine("");
        writer.writeIndentedLine("export class ApiClient {");
        writer.writeIndentedLine("");
        List<TsBeanModel> clients = model.getBeans().stream()
                .filter(TsBeanModel::isJaxrsApplicationClientBean)
                .collect(Collectors.toList());

        List<FieldAndGetter> fieldsAndGetters = clients.stream().map(this::getOutputs).collect(Collectors.toList());

        fieldsAndGetters.forEach(fieldAndGetter -> this.emitField(writer, fieldAndGetter));

        writer.writeIndentedLine("");

        fieldsAndGetters.forEach(fieldAndGetter -> this.emitGetter(writer, fieldAndGetter));

        writer.writeIndentedLine("}");
    }

    private void emitField(Writer writer, FieldAndGetter fieldAndGetter) {
        writer.writeIndentedLine(String.format(fieldFormatString, fieldAndGetter.field, fieldAndGetter.getter));
    }

    private void emitGetter(Writer writer, FieldAndGetter fieldAndGetter) {
        writer.writeIndentedLine(String.format(getter1FormatString, fieldAndGetter.getter));
        writer.writeIndentedLine(String.format(getter2FormatString, fieldAndGetter.field));
        writer.writeIndentedLine(String.format(getter3FormatString, fieldAndGetter.field, fieldAndGetter.getter));
        writer.writeIndentedLine(String.format(getter4FormatString, fieldAndGetter.field));
        writer.writeIndentedLine("}");
    }

    private FieldAndGetter getOutputs(TsBeanModel client) {
        String getterName = client.getName().getSimpleName().substring(0, client.getName().getSimpleName().length()-6);
        String fieldName = "_" + Character.toLowerCase(getterName.charAt(0)) + getterName.substring(1);
        return new FieldAndGetter(fieldName, getterName);
    }
}
