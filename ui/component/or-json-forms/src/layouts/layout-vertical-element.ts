import {
    computeLabel,
    ControlElement,
    createDefaultValue,
    getSchema,
    GroupLayout,
    isControl,
    mapStateToControlProps,
    OwnPropsOfControl,
    OwnPropsOfRenderer,
    StatePropsOfControl,
    VerticalLayout
} from "@jsonforms/core";
import {css, html, TemplateResult, unsafeCSS} from "lit";
import {customElement} from "lit/decorators.js";
import {LayoutBaseElement} from "./layout-base-element";
import {getLabel, getTemplateFromProps} from "../util";
import {InputType, OrInputChangedEvent, OrMwcInput} from "@openremote/or-mwc-components/or-mwc-input";
import {i18next} from "@openremote/or-translate";
import {showDialog} from "@openremote/or-mwc-components/or-mwc-dialog";
import "@openremote/or-mwc-components/or-mwc-list";
import {baseStyle} from "../styles";
import {ListItem, OrMwcListChangedEvent} from "@openremote/or-mwc-components/or-mwc-list";
import {DefaultColor5} from "@openremote/core";
import "../json-editor";
import {JsonEditor} from "../json-editor";
import {WithLabelAndRequired} from "../base-element";

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
    }

    .key-container or-mwc-input, .value-container or-mwc-input {
        display: block;
    }
    
    .delete-container {
        vertical-align: middle;
    }
`;

@customElement("or-json-forms-vertical-layout")
export class LayoutVerticalElement extends LayoutBaseElement<VerticalLayout | GroupLayout> {

    public handleChange!: (path: string, data: any) => void;

    public static get styles() {
        return [
            baseStyle,
            style
        ];
    }

    get dynamic(): boolean {
        return this.schema.allOf === undefined && this.schema.anyOf === undefined && this.schema.properties === undefined;
    }

    render() {

        const optionalProps: StatePropsOfControl[] = [];
        const jsonFormsState = {jsonforms: {...this.state}};
        const rootSchema = getSchema(jsonFormsState);
        const dynamic = this.dynamic;
        let dynamicValueType: InputType = InputType.JSON;

        if (this.schema.patternProperties && this.schema.patternProperties.hasOwnProperty("*") && this.schema.patternProperties["*"].type && !Array.isArray(this.schema.patternProperties["*"].type)) {
            switch(this.schema.patternProperties["*"].type) {
                case "boolean":
                    dynamicValueType = InputType.CHECKBOX;
                    break;
                case "string":
                    dynamicValueType = InputType.TEXT;
                    break;
                case "array":
                    // TODO: Implement array if required
                    console.log("Array support not implemented.");
                    break;
                case "number":
                    dynamicValueType = InputType.NUMBER;
                    break;
            }
        }

        return html`
            <div id="panel">
                <div id="header">
                    <div id="expander"><or-icon icon="chevron-right"></or-icon><span>${this.label ? computeLabel(this.label, this.required, false) : ""}</span></div>
                    <div id="header-buttons"><or-mwc-input .type="${InputType.BUTTON}" outlined .label="${i18next.t("json")}" icon="pencil" @click="${() => this._showJson()}"></or-mwc-input></div>
                </div>
                <div id="content">
                    
                    
                    ${dynamic ? 
                        this._getDynamicContentTemplate(dynamicValueType)   
                        : this.getChildProps().map((props: OwnPropsOfRenderer) => {
                        
                        const contentProps: OwnPropsOfRenderer & WithLabelAndRequired = {
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
                
                <div id="add-parameter">
                    <or-mwc-input .type="${InputType.BUTTON}" .label="${i18next.t("addParameter")}" icon="plus" .disabled="${!dynamic && optionalProps.length === 0}" @click="${() => this._addParameter(optionalProps, dynamicValueType)}"></or-mwc-input>
                </div>
            </div>
        `;
    }

    protected _getDynamicContentTemplate(dynamicValueType: InputType): TemplateResult {
        if (!this.data) {
            return html``;
        }

        const deleteHandler = (key: string) => {
            const data = this.data || {};
            delete data[key];
            this.handleChange(this.path, data);
        };

        return html`
            <div id="dynamic-wrapper">
                ${Object.entries(this.data).map(([key, value]) => {
                    return html`
                        <div class="row">
                            <div class="key-container">
                                <or-mwc-input .type="${InputType.TEXT}" .value="${key}"></or-mwc-input>
                            </div>
                            <div class="value-container">
                                <or-mwc-input .type="${dynamicValueType}" .value="${value}"></or-mwc-input>
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

    protected _addParameter(optionalProps: StatePropsOfControl[], dynamicValueType: InputType) {

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

        const keyValue: [any, any] = [undefined, undefined];
        const onKeyValueChanged = (value: any, index: 0 | 1) => {
            keyValue[index] = value;
            const valid = keyValue[0] && keyValue[1] !== undefined;
            (dialog.shadowRoot!.getElementById("add-btn") as OrMwcInput).disabled = !valid;
        };

        const dialog = showDialog({
            content: html`
                <div class="col">
                    <form id="mdc-dialog-form-add" class="row">
                        <div id="type-list" class="col">
                            ${dynamic ? `` : html`<or-mwc-list @or-mwc-list-changed="${(evt: OrMwcListChangedEvent) => {if (evt.detail.length === 1) onParamChanged((evt.detail[0] as ListItem).data as StatePropsOfControl); }}" .listItems="${listItems}" id="parameter-list"></or-mwc-list>`}
                        </div>
                        <div id="parameter-desc" class="col">
                            ${!dynamic ? `` : html`
                                <style>
                                    #dynamic-wrapper > or-mwc-input {
                                        display: block;
                                        margin: 10px;
                                    }
                                </style>
                                <div id="dynamic-wrapper">
                                    <or-mwc-input .type="${InputType.TEXT}" .label="${i18next.t("key")}" @or-mwc-input-changed="${(evt: OrInputChangedEvent) => onKeyValueChanged(evt.detail.value, 0)}"></or-mwc-input>
                                    <or-mwc-input .type="${dynamicValueType}" .label="${i18next.t("value")}" @or-mwc-input-changed="${(evt: OrInputChangedEvent) => onKeyValueChanged(evt.detail.value, 1)}"></or-mwc-input>
                                </div>
                            `}
                        </div>
                    </form>
                </div>
            `,
            styles: html`
                <style>
                    .mdc-dialog__surface {
                        width: 800px;
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
                    }
                    form {
                        display: flex;
                    }
                    #type-list {
                        overflow: auto;
                        min-width: 150px;
                        max-width: 300px;
                        flex: 0 0 40%;
                        border-right: 1px solid var(--or-app-color5, #CCC);
                    }
                    #parameter-list {
                        display: flex;
                    }
                    #parameter-desc {
                        flex: 1;
                        padding: 5px;
                    }
                </style>
            `,
            title: i18next.t("addParameter"),
            actions: [
                {
                    actionName: "cancel",
                    content: i18next.t("cancel")
                },
                {
                    default: true,
                    actionName: "add",
                    action: () => {
                        const data = {...this.data};
                        const key = dynamic ? keyValue[0] as string : selectedParameter!.path.split(".").pop()!;
                        data[key] = dynamic ? keyValue[1] : createDefaultValue(selectedParameter!.schema);
                        this.handleChange(this.path || "", data);
                    },
                    content: html`<or-mwc-input id="add-btn" .type="${InputType.BUTTON}" disabled .label="${i18next.t("add")}"></or-mwc-input>`
                }
            ],
            dismissAction: null
        });
    }
}
