import {css, html, LitElement, PropertyValues} from "lit";
import {customElement, property, state} from "lit/decorators.js";
import {ErrorObject} from "ajv";
import {
    Actions,
    configReducer,
    CoreActions,
    coreReducer,
    createAjv,
    Dispatch,
    generateDefaultUISchema,
    generateJsonSchema,
    JsonFormsCellRendererRegistryEntry,
    JsonFormsCore,
    JsonFormsProps,
    JsonFormsRendererRegistryEntry,
    JsonFormsSubStates,
    JsonFormsUISchemaRegistryEntry,
    JsonSchema,
    mapStateToJsonFormsRendererProps,
    OwnPropsOfJsonFormsRenderer,
    setConfig,
    UISchemaElement
} from "@jsonforms/core";
import {getTemplateWrapper, StandardRenderers} from "./standard-renderers";
import {getLabel, getTemplateFromProps} from "./util";
import {baseStyle} from "./styles";
import {Util} from "@openremote/core";
import {AdditionalProps} from "./base-element";

declare global {
    interface SymbolConstructor {
        readonly observable: symbol;
    }
}

export {
    ErrorObject,
    StandardRenderers,
    getTemplateWrapper,
    JsonFormsRendererRegistryEntry,
    UISchemaElement
};

export interface JsonFormsStateContext extends JsonFormsSubStates {
    dispatch: Dispatch<CoreActions>;
}

// language=CSS
const styles = css`
    .delete-container {
        width: 0;
    }

    .item-container {
        margin: 0; /* Remove inherited margin */
    }
`;

@customElement("or-json-forms")
export class OrJSONForms extends LitElement implements OwnPropsOfJsonFormsRenderer, AdditionalProps {

    @property({type: Object})
    public uischema?: UISchemaElement;

    @property({type: Object})
    public schema?: JsonSchema;

    @property({type: Object, attribute: false})
    public data: any;

    @property({type: Array})
    public renderers?: JsonFormsRendererRegistryEntry[] = StandardRenderers;

    @property({type: Array})
    public cells?: JsonFormsCellRendererRegistryEntry[];

    @property({type: String, attribute: false})
    public onChange?: (dataAndErrors: {errors: ErrorObject[] | undefined, data: any}) => void;

    @property({type: String, attribute: false})
    public config: any;

    @property({type: Array})
    public uischemas?: JsonFormsUISchemaRegistryEntry[];

    @property({type: Boolean})
    public readonly: boolean = false;

    @property({type: String})
    public label!: string;

    @property({type: Boolean})
    public required: boolean = false;

    public static get styles() {
        return [
            baseStyle,
            styles
        ];
    }

    @state()
    protected core?: JsonFormsCore;
    @state()
    protected contextValue?: JsonFormsSubStates;
    protected previousData: any;
    protected previousErrors: ErrorObject[] = [];

    public checkValidity() {
        return this.previousErrors.length === 0;
    }

    shouldUpdate(_changedProperties: PropertyValues) {
        super.shouldUpdate(_changedProperties);

        if (!this.schema) {
            this.schema = this.data !== undefined ? generateJsonSchema(this.data) : {};
        }

        if (!this.uischema) {
            this.uischema = generateDefaultUISchema(this.schema!);
        }

        if (!this.core) {
            this.core = {
                ajv: createAjv({useDefaults: true, format: false}),
                data: {},
                schema: this.schema!,
                uischema: this.uischema!
            };
            this.updateCore(Actions.init(this.data, this.schema, this.uischema));
            this.config = configReducer(undefined, setConfig(this.config));
        }

        if (_changedProperties.has("data") || _changedProperties.has("schema") || _changedProperties.has("uischema")) {
            this.updateCore(
                Actions.updateCore(this.data, this.schema, this.uischema)
            );
        }

        if (!this.contextValue || _changedProperties.has("core") || _changedProperties.has("renderers") || _changedProperties.has("cells") || _changedProperties.has("config") || _changedProperties.has("readonly")) {
            this.contextValue = {
                core: this.core,
                renderers: this.renderers,
                cells: this.cells,
                config: this.config,
                uischemas: this.uischemas,
                readonly: this.readonly,
                dispatch: (action: CoreActions) => this.updateCore(action)
            }
        }

        if (_changedProperties.has("core")) {
            const data = this.core!.data;
            const errors = this.core!.errors;

            if (this.onChange && (!Util.objectsEqual(data, this.previousData, true) || (errors && !Util.objectsEqual(errors, this.previousErrors, true)))) {
                this.previousErrors = errors || [];
                this.previousData = data;
                this.onChange({data: data, errors: errors});
            }
        }

        return true;
    }

    updateCore<T extends CoreActions>(coreAction: T): T {
        const coreState = coreReducer(this.core, coreAction);
        if(coreState !== this.core) {
            this.core = coreState;
        }
        return coreAction;
    }

    render() {

        if (!this.contextValue) {
            return html``;
        }

        const props: JsonFormsProps & AdditionalProps = {
            ...mapStateToJsonFormsRendererProps({jsonforms: {...this.contextValue}}, this),
            label: getLabel(this.schema!, this.uischema!, this.label, undefined) || "",
            required: this.required
        };
        return getTemplateFromProps(this.contextValue, props) || html``;
    }
}
