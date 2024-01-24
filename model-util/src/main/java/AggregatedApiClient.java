import cz.habarta.typescript.generator.Settings;
import cz.habarta.typescript.generator.emitter.EmitterExtension;
import cz.habarta.typescript.generator.emitter.EmitterExtensionFeatures;
import cz.habarta.typescript.generator.emitter.TsBeanModel;
import cz.habarta.typescript.generator.emitter.TsModel;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Creates a wrapper class around all of the clients the generator creates so that they can easily be exported as a single
 * entity.
 */
public class AggregatedApiClient extends EmitterExtension {

    private static final String fieldFormatString = "protected %1$s : Axios%2$sClient;";
    private static final String ctorFormatString = "this.%1$s = new Axios%2$sClient(baseURL, axiosInstance);";
    private static final String getter1FormatString = "get %1$s() : Axios%1$sClient {";
    private static final String getter2FormatString = "return this.%1$s;";

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

        writer.writeIndentedLine("export class ApiClient {");
        writer.writeIndentedLine("");
        List<TsBeanModel> clients = model.getBeans().stream()
                .filter(TsBeanModel::isJaxrsApplicationClientBean)
                .collect(Collectors.toList());

        List<FieldAndGetter> fieldsAndGetters = clients.stream().map(this::getOutputs).collect(Collectors.toList());

        fieldsAndGetters.forEach(fieldAndGetter -> this.emitField(writer, fieldAndGetter));

        writer.writeIndentedLine("");
        writer.writeIndentedLine("constructor(baseURL: string, axiosInstance: Axios.AxiosInstance = axios.create()) {");

        fieldsAndGetters.forEach(fieldAndGetter -> this.emitCtor(writer, fieldAndGetter));

        writer.writeIndentedLine("this._assetResource = new AxiosAssetResourceClient(baseURL, axiosInstance);");
        writer.writeIndentedLine("}");
        writer.writeIndentedLine("");

        fieldsAndGetters.forEach(fieldAndGetter -> this.emitGetter(writer, fieldAndGetter));

        writer.writeIndentedLine("}");
    }

    private void emitField(Writer writer, FieldAndGetter fieldAndGetter) {
        writer.writeIndentedLine(String.format(fieldFormatString, fieldAndGetter.field, fieldAndGetter.getter));
    }

    private void emitCtor(Writer writer, FieldAndGetter fieldAndGetter) {
        writer.writeIndentedLine(String.format(ctorFormatString, fieldAndGetter.field, fieldAndGetter.getter));
    }

    private void emitGetter(Writer writer, FieldAndGetter fieldAndGetter) {
        writer.writeIndentedLine(String.format(getter1FormatString, fieldAndGetter.getter));
        writer.writeIndentedLine(String.format(getter2FormatString, fieldAndGetter.field));
        writer.writeIndentedLine("}");
    }

    private FieldAndGetter getOutputs(TsBeanModel client) {
        String getterName = client.getName().getSimpleName().substring(0, client.getName().getSimpleName().length()-6);
        String fieldName = "_" + Character.toLowerCase(getterName.charAt(0)) + getterName.substring(1);
        return new FieldAndGetter(fieldName, getterName);
    }
}
