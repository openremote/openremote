import {
    computeLabel,
    ControlElement,
    createDefaultValue,
    getSchema,
    GroupLayout,
    isControl,
    JsonSchema,
    mapStateToControlProps,
    OwnPropsOfControl,
    OwnPropsOfRenderer,
    Paths,
    RendererProps,
    StatePropsOfControl,
    VerticalLayout
} from "@jsonforms/core";
import {css, html, TemplateResult, unsafeCSS} from "lit";
import {customElement, property} from "lit/decorators.js";
import {LayoutBaseElement} from "./layout-base-element";
import {controlWithoutLabel, getLabel, getTemplateFromProps} from "../util";
import {InputType, OrInputChangedEvent, OrMwcInput} from "@openremote/or-mwc-components/or-mwc-input";
import {i18next} from "@openremote/or-translate";
import {showDialog} from "@openremote/or-mwc-components/or-mwc-dialog";
import "@openremote/or-mwc-components/or-mwc-list";
import {addItemOrParameterDialogStyle, baseStyle} from "../styles";
import {ListItem, OrMwcListChangedEvent} from "@openremote/or-mwc-components/or-mwc-list";
import {DefaultColor5} from "@openremote/core";
import "../json-editor";
import {JsonEditor} from "../json-editor";
import {AdditionalProps} from "../base-element";

// language=CSS
const style = css`
    :host {
        border-color: var(--or-app-color5, ${unsafeCSS(DefaultColor5)});
        border-radius: 4px;
        border-width: 1px;
        border-style: solid;
    }
    
    #expander {
        --or-icon-width: 20px;
        --or-icon-height: 20px;
        cursor: pointer;
        flex: 1 1 auto;
    }
    #expander > * {
        pointer-events: none;
    }
    #expander > or-icon {
        vertical-align: middle;
        margin-right: 6px;
        margin-left: -5px;
    }
    
    #errors {
        color: red;
        margin-right: 10px;
    }
    
    #errors > or-icon {
        margin-right: 10px;
    }
    
    #header {
        display: flex;
        align-items: center;
        height: 50px;
    }
    
    #content {
        display: flex;
        flex-direction: column;
    }
    
    #add-parameter {
        margin: 10px 0 0 4px;
    }
    
    :host, .padded {
        padding: 10px 16px;
    }
    
    #dynamic-wrapper {
        display: table;
    }
    
    #dynamic-wrapper .row {
        display: table-row;
    }
    
    #dynamic-wrapper .row:hover .button-clear {
        visibility: visible;
    }
    
    #dynamic-wrapper .row > div {
        display: table-cell;
    }
    
    .key-container, .value-container {
        padding: 10px;
        vertical-align: top;
    }

    .key-container or-mwc-input, .value-container or-mwc-input {
        display: block;
    }
    
    .delete-container {
        vertical-align: middle;
    }

    .value-container > .item-container {
        margin: 0;
    }

    .value-container > .item-container > .delete-container {
        display: none;
    }

    .value-container > .item-container :first-child {
        border: 0;
        padding: 0;
        margin: 0;
        flex: 1;
    }
`;

function isDynamic(schema: JsonSchema): boolean {
    return schema.allOf === undefined && schema.anyOf === undefined && (schema.properties === undefined || Object.keys(schema.properties).length === 0);
}

@customElement("or-json-forms-vertical-layout")
export class LayoutVerticalElement extends LayoutBaseElement<VerticalLayout | GroupLayout> {

    @property()
    protected minimal?: boolean;
    public handleChange!: (path: string, data: any) => void;

    public static get styles() {
        return [
            baseStyle,
            style
        ];
    }

    render() {

        const optionalProps: StatePropsOfControl[] = [];
        const jsonFormsState = {jsonforms: {...this.state}};
        const rootSchema = getSchema(jsonFormsState);
        const dynamic = isDynamic(this.schema);
        let dynamicPropertyRegex = ".+";
        let dynamicValueSchema: JsonSchema | undefined;

        if (dynamic) {
            if (typeof this.schema.patternProperties === "object") {
                const patternObjs = Object.entries(this.schema.patternProperties);
                if (patternObjs.length === 1) {
                    dynamicPropertyRegex = patternObjs[0][0];
                    dynamicValueSchema = (patternObjs[0][1] as JsonSchema);
                }
            } else if (typeof this.schema.additionalProperties === "object") {
                dynamicValueSchema = this.schema.additionalProperties;
            }
        }

        return html`
            <div id="panel">
                ${this.minimal ? html`` : html`
                    <div id="header">
                        <div id="expander">
                            <or-icon icon="chevron-right"></or-icon>
                            <span>${this.label ? computeLabel(this.label, this.required, false) : ""}</span>
                        </div>
                        ${!this.errors ? `` : html`<div id="errors"><or-icon icon="alert"></or-icon><span>${this.errors}</span></div>`}
                        <div id="header-buttons"><or-mwc-input .type="${InputType.BUTTON}" outlined .label="${i18next.t("json")}" icon="pencil" @click="${() => this._showJson()}"></or-mwc-input></div>
                    </div>
                `}
                <div id="content">
                    
                    ${this.errors ? ``
                    : dynamic && dynamicValueSchema ? 
                        this._getDynamicContentTemplate(dynamicPropertyRegex, dynamicValueSchema)   
                    : this.getChildProps().map((props: OwnPropsOfRenderer) => {
                        
                        const contentProps: OwnPropsOfRenderer & AdditionalProps = {
                            ...props,
                            label: "",
                            required: false
                        };
                        
                        if (isControl(props.uischema)) {
                            
                            const controlProps = props as OwnPropsOfControl;
                            const stateControlProps = mapStateToControlProps(jsonFormsState, controlProps);
                            stateControlProps.label = stateControlProps.label || getLabel(this.schema, rootSchema, undefined, (props.uischema as ControlElement).scope) || "";
                            contentProps.label = stateControlProps.label;
                            contentProps.required = !!stateControlProps.required;
                            if (!stateControlProps.required && stateControlProps.data === undefined) {
                                // Optional property with no data so show this in the add parameter dialog
                                optionalProps.push(stateControlProps);
                                return html``;
                            }
                        }
                        
                        return getTemplateFromProps(this.state, contentProps);
                    })}
                </div>
                
                ${this.errors ? `` : html`
                <div id="add-parameter">
                    <or-mwc-input .type="${InputType.BUTTON}" .label="${i18next.t("addParameter")}" icon="plus" .disabled="${!dynamic && optionalProps.length === 0}" @click="${() => this._addParameter(optionalProps, dynamicPropertyRegex, dynamicValueSchema)}"></or-mwc-input>
                </div>`}
            </div>
        `;
    }

    protected _getDynamicContentTemplate(dynamicPropertyRegex: string, dynamicValueSchema: JsonSchema): TemplateResult {
        if (!this.data) {
            return html``;
        }

        const deleteHandler = (key: string) => {
            const data = {...this.data};
            delete data[key];
            this.handleChange(this.path, data);
        };

        const keyChangeHandler = (orInput: OrMwcInput, oldKey: string, newKey: string) => {

            if (!orInput.valid) {
                return;
            }

            if (this.data[newKey] !== undefined) {
                orInput.setCustomValidity(i18next.t("validation.keyAlreadyExists"));
                return;
            } else {
                orInput.setCustomValidity(undefined);
            }
            const data = {...this.data};
            const value = data[oldKey];
            delete data[oldKey];
            data[newKey] = value;
            this.handleChange(this.path, data);
        };

        const props: RendererProps & AdditionalProps = {
            renderers: this.renderers,
            uischema: controlWithoutLabel("#"),
            enabled: this.enabled,
            visible: this.visible,
            path: "",
            schema: dynamicValueSchema,
            minimal: true,
            required: false,
            label: ""
        };

        const getDynamicValueTemplate: (key: string, value: any) => TemplateResult = (key, value) => {
            props.path = Paths.compose(this.path, key);
            return getTemplateFromProps(this.state, props);
        };

        return html`
            <div id="dynamic-wrapper">
                ${Object.entries(this.data).map(([key, value]) => {
                    return html`
                        <div class="row">
                            <div class="key-container">
                                <or-mwc-input .type="${InputType.TEXT}" @or-mwc-input-changed="${(ev:OrInputChangedEvent) => keyChangeHandler(ev.currentTarget as OrMwcInput, key, ev.detail.value)}" required .pattern="${dynamicPropertyRegex}" .value="${key}"></or-mwc-input>
                            </div>
                            <div class="value-container">
                                ${getDynamicValueTemplate(key, value)}
                            </div>
                            <div class="delete-container">
                                <button class="button-clear" @click="${() => deleteHandler(key)}"><or-icon icon="close-circle"></or-icon></input>
                            </div>
                        </div>
                    `;
                })}
            </div>
        `;
    }

    protected _showJson() {
        const dialog = showDialog(
            {
                content: html`
                    <or-json-forms-json-editor id="json-editor" .json="${this.data === undefined ? "" : JSON.stringify(this.data, null, 2)}"></or-json-forms-json-editor>
                `,
                actions: [
                    {
                        actionName: "cancel",
                        content: i18next.t("cancel")
                    },
                    {
                        default: true,
                        actionName: "update",
                        action: () => {
                            const editor = dialog.shadowRoot!.getElementById("json-editor") as JsonEditor;
                            if (editor.validate()) {
                                const data = !!editor.getValue() ? JSON.parse(editor.getValue()!) : undefined;
                                this.handleChange(this.path || "", data);
                            }
                        },
                        content: html`<or-mwc-input id="add-btn" .type="${InputType.BUTTON}" .label="${i18next.t("update")}"></or-mwc-input>`
                    }
                ],
                title: this.title || this.schema.title,
                dismissAction: null,
                styles: html`
                <style>
                    .mdc-dialog__surface {
                        width: 1024px;
                        overflow-x: visible !important;
                        overflow-y: visible !important;
                    }
                    #dialog-content {
                        border-color: var(--or-app-color5, ${unsafeCSS(DefaultColor5)});
                        border-top-width: 1px;
                        border-top-style: solid;
                        border-bottom-width: 1px;
                        border-bottom-style: solid;
                        padding: 0;
                        overflow: visible;
                        height: 60vh;
                    }
                </style>
            `
            }
        )
    }

    protected _addParameter(optionalProps: StatePropsOfControl[], dynamicPropertyRegex?: string, dynamicValueSchema?: JsonSchema) {

        const dynamic = optionalProps.length === 0;
        let selectedParameter: StatePropsOfControl | undefined;

        const listItems: ListItem[] = optionalProps.map(props => {
            const labelStr = computeLabel(props.label, !!props.required, false);
            return {
                text: labelStr,
                value: labelStr,
                data: props
            }
        });

        const onParamChanged = (selected: StatePropsOfControl) => {
            selectedParameter = selected;
            const descElem = dialog.shadowRoot!.getElementById("parameter-desc") as HTMLDivElement;
            descElem.innerHTML = selected.description || "";
            (dialog.shadowRoot!.getElementById("add-btn") as OrMwcInput).disabled = false;
        };

        // const keyValue: [any, any] = [undefined, undefined];
        // const onKeyValueChanged = (value: any, index: 0 | 1) => {
        //     keyValue[index] = value;
        //     const valid = keyValue[0] && keyValue[1] !== undefined;
        //     (dialog.shadowRoot!.getElementById("add-btn") as OrMwcInput).disabled = !valid;
        // };
        let keyValue: string | undefined;
        const onKeyChanged = (event: OrInputChangedEvent) => {
            const keyInput = event.currentTarget as OrMwcInput;
            keyInput.setCustomValidity(undefined);
            keyValue = event.detail.value as string;
            let valid = keyInput.valid;

            if (this.data[keyValue] !== undefined) {
                valid = false;
                keyInput.setCustomValidity(i18next.t("validation.keyAlreadyExists"));
            }
            (dialog.shadowRoot!.getElementById("add-btn") as OrMwcInput).disabled = !valid;
        };

        const dialog = showDialog({
            content: html`
                <div class="col">
                    <form id="mdc-dialog-form-add" class="row">
                        ${dynamic ? `` : html`
                            <div id="type-list" class="col">
                                <or-mwc-list @or-mwc-list-changed="${(evt: OrMwcListChangedEvent) => {if (evt.detail.length === 1) onParamChanged((evt.detail[0] as ListItem).data as StatePropsOfControl); }}" .listItems="${listItems}" id="parameter-list"></or-mwc-list>
                            </div>
                        `}
                        <div id="parameter-desc" class="col">
                            ${!dynamic ? `` : html`
                                <style>
                                    #dynamic-wrapper > or-mwc-input {
                                        display: block;
                                        margin: 10px;
                                    }
                                </style>
                                <div id="dynamic-wrapper">
                                    <or-mwc-input required .type="${InputType.TEXT}" .pattern="${dynamicPropertyRegex}" .label="${i18next.t("key")}" @or-mwc-input-changed="${(evt: OrInputChangedEvent) => onKeyChanged(evt)}"></or-mwc-input>
                                </div>
                            `}
                        </div>
                    </form>
                </div>
            `,
            styles: addItemOrParameterDialogStyle,
            title: (this.label ? computeLabel(this.label, this.required, false) + " - " : "") + i18next.t("addParameter"),
            actions: [
                {
                    actionName: "cancel",
                    content: i18next.t("cancel")
                },
                {
                    default: true,
                    actionName: "add",
                    action: () => {
                        const key = dynamic ? keyValue as string : selectedParameter!.path.split(".").pop()!;
                        const data = {...this.data};
                        const schema = dynamic ? dynamicValueSchema! : selectedParameter!.schema;
                        data[key] = Array.isArray(schema.type) ? null : createDefaultValue(schema);
                        this.handleChange(this.path || "", data);
                    },
                    content: html`<or-mwc-input id="add-btn" .type="${InputType.BUTTON}" disabled .label="${i18next.t("add")}"></or-mwc-input>`
                }
            ],
            dismissAction: null
        });
    }
}
