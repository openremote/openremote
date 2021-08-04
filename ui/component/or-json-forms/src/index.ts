import {css, html} from "lit";
import {customElement, property} from "lit/decorators.js";
import {guard} from "lit/directives/guard";
import {ErrorObject} from "ajv";
import {HauntedLitElement} from "./haunted-element";
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
    JsonFormsRendererRegistryEntry,
    JsonFormsSubStates,
    JsonFormsUISchemaRegistryEntry,
    JsonSchema,
    mapStateToJsonFormsRendererProps,
    OwnPropsOfJsonFormsRenderer,
    StatePropsOfJsonFormsRenderer,
    UISchemaElement
} from "@jsonforms/core";
import {getTemplateWrapper, StandardRenderers} from "./standardRenderers";
import {getTemplateFromProps} from "./util";
import {useEffect, useMemo, useReducer, useRef} from "haunted";
import {baseStyle} from "./styles";
import { Util } from "@openremote/core";

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

export interface WithTitle {
    title?: string;
}

export interface WithRequired {
    required?: boolean;
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

/**
 * Hook similar to `useEffect` with the difference that the effect
 * is only executed from the second call onwards.
 */
const useEffectAfterFirstRender = (
    effect: () => void,
    dependencies: Array<any>
) => {
    const firstExecution = useRef(true);
    useEffect(() => {
        if (firstExecution.current) {
            firstExecution.current = false;
            return;
        }
        effect();
    }, dependencies);
};

@customElement("or-json-forms")
export class OrJSONForms extends HauntedLitElement implements OwnPropsOfJsonFormsRenderer {

    @property({type: Object})
    public uischema?: UISchemaElement;

    @property({type: Object})
    public schema?: JsonSchema;

    @property({type: String, attribute: false})
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

    public static get styles() {
        return [
            baseStyle,
            styles
        ];
    }

    render() {

        const schemaToUse = useMemo(
            () => (this.schema !== undefined ? this.schema : this.data !== undefined ? generateJsonSchema(this.data) : {}),
            [this.schema, this.data]
        );

        const uischemaToUse = useMemo(
            () => typeof this.uischema === "object" ? this.uischema : generateDefaultUISchema(schemaToUse),
            [this.uischema, schemaToUse]
        );

        const [core, coreDispatch] = useReducer(
            coreReducer,
            undefined,
            () => coreReducer(
                 {
                    ajv: createAjv({useDefaults: true, format: false}),
                    data: {},
                    schema: schemaToUse,
                    uischema: uischemaToUse
                },
                Actions.init(this.data, schemaToUse, uischemaToUse)
            )
        );

        useEffect(() => {
            coreDispatch(
                Actions.updateCore(this.data, schemaToUse, uischemaToUse)
            );
        }, [this.data, schemaToUse, uischemaToUse]);

        const [config, configDispatch] = useReducer(
            configReducer,
            undefined,
            () => configReducer(undefined, Actions.setConfig(this.config))
        );

        useEffectAfterFirstRender(() => {
            configDispatch(Actions.setConfig(this.config));
        }, [this.config]);

        // @ts-ignore
        const contextValue: JsonFormsStateContext = useMemo(() => ({
            core,
            renderers: this.renderers,
            cells: this.cells,
            config: config,
            uischemas: this.uischemas,
            readonly: this.readonly,
            // only core dispatch available
            dispatch: coreDispatch
        }), [core, this.renderers, this.cells, config, this.readonly]);

        useEffect(() => {
            if (this.onChange && !Util.objectsEqual(core.data, this.data, true)) {
                this.onChange({data: core.data, errors: core.errors});
            }
        }, [core.data, core.errors]);
    
        return html`${guard([contextValue], () => {
            let props: StatePropsOfJsonFormsRenderer & WithTitle = mapStateToJsonFormsRendererProps({jsonforms: {...contextValue}}, this);
            props = {
                ...props,
                title: this.title
            }
            return getTemplateFromProps(contextValue, props);
        })}`;
    }
}
