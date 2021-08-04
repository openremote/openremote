import {computeLabel, createDefaultValue, isControl, JsonFormsState, LayoutProps, mapStateToControlProps,
    OwnPropsOfControl, OwnPropsOfRenderer, RankedTester, rankWith, StatePropsOfControl, uiTypeIs, update, VerticalLayout} from "@jsonforms/core";
import {css, html, PropertyValues, TemplateResult, unsafeCSS} from "lit";
import {customElement} from "lit/decorators.js";
import {LayoutBaseElement} from "./layout-base-element";
import {getTemplateFromProps} from "../util";
import { InputType, OrMwcInput } from "@openremote/or-mwc-components/or-mwc-input";
import { i18next } from "@openremote/or-translate";
import { showDialog } from "@openremote/or-mwc-components/or-mwc-dialog";
import "@openremote/or-mwc-components/or-mwc-list";
import {baseStyle} from "../styles";
import {ListItem, OrMwcListChangedEvent } from "@openremote/or-mwc-components/or-mwc-list";
import { DefaultColor5 } from "@openremote/core";
import "../json-editor";
import {JsonEditor} from "../json-editor";

// language=CSS
const style = css`
    :host {
        border-color: rgba(0,0,0,0.12);
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
`;

@customElement("or-json-forms-vertical-layout")
export class LayoutVerticalElement extends LayoutBaseElement<VerticalLayout> {

    public static get styles() {
        return [
            baseStyle,
            style
        ];
    }

    render() {

        const optionalProps: StatePropsOfControl[] = [];

        return html`
            <div id="panel">
                <div id="header">
                    <div id="expander"><or-icon icon="chevron-right"></or-icon><span>${this.title || this.schema.title || ""}</span></div>
                    <div id="header-buttons"><or-mwc-input .type="${InputType.BUTTON}" outlined .label="${i18next.t("json")}" icon="pencil" @click="${() => this._showJson()}"></or-mwc-input></div>
                </div>
                <div id="content">
                    
                    
                    ${this.getChildProps().map((props) => {
                        
                        if (isControl(props.uischema)) {
                            const controlProps = props as OwnPropsOfControl;
                            const stateControlProps = mapStateToControlProps({jsonforms: {...this.state}}, controlProps);
                            if (!stateControlProps.required && stateControlProps.data === undefined) {
                                // Optional property with no data so show this in the dialog
                                optionalProps.push(stateControlProps);
                                return html``;
                            }
                        }
                        
                        return getTemplateFromProps(this.state, props);
                    })}
                </div>
                
                ${optionalProps.length === 0 ? html`` : html`
                    <div id="add-parameter">
                        <or-mwc-input .type="${InputType.BUTTON}" .label="${i18next.t("addParameter")}" icon="plus" @click="${() => this._addParameter(optionalProps)}"></or-mwc-input>
                    </div>
                `}
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
                                this.state.dispatch(
                                    update(this.path || "", (d) => {
                                        return data;
                                    })
                                )
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

    protected _addParameter(optionalProps: StatePropsOfControl[]) {

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

        const dialog = showDialog({
            content: html`
                <div class="col">
                    <form id="mdc-dialog-form-add" class="row">
                        <div id="type-list" class="col">
                            <or-mwc-list @or-mwc-list-changed="${(evt: OrMwcListChangedEvent) => {if (evt.detail.length === 1) onParamChanged((evt.detail[0] as ListItem).data as StatePropsOfControl); }}" .listItems="${listItems}" id="parameter-list"></or-mwc-list>
                        </div>
                        <div id="parameter-desc" class="col"></div>
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
                        if (selectedParameter) {
                            this.state.dispatch(
                                update(this.path || "", (data) => {
                                    data = data || {};
                                    data[selectedParameter!.path.split(".").pop()!] = createDefaultValue(selectedParameter!.schema);
                                    return data;
                                })
                            )
                        }
                    },
                    content: html`<or-mwc-input id="add-btn" .type="${InputType.BUTTON}" disabled .label="${i18next.t("add")}"></or-mwc-input>`
                }
            ],
            dismissAction: null
        });
    }
}
