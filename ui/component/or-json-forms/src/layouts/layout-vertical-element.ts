import {
    computeLabel,
    ControlElement,
    createDefaultValue,
    getSchema,
    GroupLayout,
    isControl,
    JsonSchema,
    mapStateToControlProps,
    mapStateToControlWithDetailProps,
    mapStateToJsonFormsRendererProps,
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
import {
    CombinatorInfo,
    controlWithoutLabel,
    getLabel,
    getSchemaPicker,
    getTemplateFromProps,
    showJsonEditor
} from "../util";
import {InputType, OrInputChangedEvent, OrMwcInput} from "@openremote/or-mwc-components/or-mwc-input";
import {i18next} from "@openremote/or-translate";
import {OrMwcDialog, showDialog} from "@openremote/or-mwc-components/or-mwc-dialog";
import "@openremote/or-mwc-components/or-mwc-list";
import "@openremote/or-components/or-collapsible-panel";
import {addItemOrParameterDialogStyle, baseStyle, panelStyle} from "../styles";
import {ListItem, OrMwcListChangedEvent} from "@openremote/or-mwc-components/or-mwc-list";
import {DefaultColor5} from "@openremote/core";
import {AdditionalProps} from "../base-element";

// language=CSS
const style = css`
    
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
    
    .value-container {
        padding: 0 0 20px 10px;
    }

    .key-container {
        padding: 0 10px 20px 0;
    }

    .value-container, .key-container {
        vertical-align: top;
    }

    .key-container or-mwc-input, .value-container or-mwc-input {
        display: block;
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
    @property()
    protected type?: string;
    public handleChange!: (path: string, data: any) => void;

    public static get styles() {
        return [
            baseStyle,
            panelStyle,
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

        const header = this.minimal ? `` : html`
            <div slot="header">
                <span>${this.label ? computeLabel(this.label, this.required, false) : ""}</span>
                ${this.type ? html`<span id="type-label">${this.type}</span>` : ``}
            </div>
            <div id="header-description" slot="header-description">
                <div id="errors">
                    ${!this.errors ? `` : html`<or-icon icon="alert"></or-icon><span>${this.errors}</span>`}
                </div>
                <div id="header-buttons"><or-mwc-input .type="${InputType.BUTTON}" outlined label="json" icon="pencil" @or-mwc-input-changed="${(ev: Event) => this._showJson(ev)}"></or-mwc-input></div>
            </div>
        `;

        let contentTemplate: TemplateResult | TemplateResult[] | undefined;

        if (dynamic && dynamicValueSchema) {
            contentTemplate = this._getDynamicContentTemplate(dynamicPropertyRegex, dynamicValueSchema);
        } else if (this.getChildProps().length > 0) {
            contentTemplate = this.getChildProps().map((childProps: OwnPropsOfRenderer & AdditionalProps) => {

                if (isControl(childProps.uischema)) {

                    const controlProps = childProps as OwnPropsOfControl;
                    const stateControlProps = mapStateToControlProps(jsonFormsState, controlProps);
                    stateControlProps.label = stateControlProps.label || getLabel(this.schema, rootSchema, undefined, (childProps.uischema as ControlElement).scope) || "";
                    childProps.label = stateControlProps.label;
                    childProps.required = !!stateControlProps.required;
                    if (!stateControlProps.required && stateControlProps.data === undefined) {
                        // Optional property with no data so show this in the add parameter dialog
                        optionalProps.push(stateControlProps);
                        return html``;
                    }
                }

                return getTemplateFromProps(this.state, childProps);
            }).filter(t => t !== undefined) as TemplateResult[];
        }

        const expandable = (!!contentTemplate && (!Array.isArray(contentTemplate) || contentTemplate.length > 0)) || (!this.errors && optionalProps.length > 0);

        const content = html`
            ${header}
            <div id="content-wrapper" slot="content">
                <div id="content">
                    ${contentTemplate || ``}
                </div>

                ${this.errors || (optionalProps.length === 0 && !dynamic) ? `` : html`
                        <div id="footer">
                            <or-mwc-input .type="${InputType.BUTTON}" label="addParameter" icon="plus" @or-mwc-input-changed="${() => this._addParameter(rootSchema, optionalProps, dynamicPropertyRegex, dynamicValueSchema)}"></or-mwc-input>
                        </div>`}
            </div>
        `;

        return this.minimal ? html`<div>${content}</div>` : html`<or-collapsible-panel .expandable="${expandable}">${content}</or-collapsible-panel>`;
    }

    protected _getDynamicContentTemplate(dynamicPropertyRegex: string, dynamicValueSchema: JsonSchema): TemplateResult | undefined {
        if (!this.data) {
            return undefined;
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
            return getTemplateFromProps(this.state, props) || html``;
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

    protected _showJson(ev: Event) {
        ev.stopPropagation();

        showJsonEditor(this.title || this.schema.title || "", this.data, (newValue) => {
            this.handleChange(this.path || "", newValue);
        });
    }

    protected _addParameter(rootSchema: JsonSchema, optionalProps: StatePropsOfControl[], dynamicPropertyRegex?: string, dynamicValueSchema?: JsonSchema) {

        const dynamic = optionalProps.length === 0;
        let selectedParameter: StatePropsOfControl | undefined;
        let selectedOneOf: CombinatorInfo;

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
            const isOneOf = !!(selectedParameter && selectedParameter.schema && selectedParameter.schema.oneOf);
            (dialog.shadowRoot!.getElementById("add-btn") as OrMwcInput).disabled = isOneOf;
            dialog.requestUpdate();
        };

        // const keyValue: [any, any] = [undefined, undefined];
        // const onKeyValueChanged = (value: any, index: 0 | 1) => {
        //     keyValue[index] = value;
        //     const valid = keyValue[0] && keyValue[1] !== undefined;
        //     (dialog.shadowRoot!.getElementById("add-btn") as OrMwcInput).disabled = !valid;
        // };
        let keyValue: string | undefined;
        const onKeyChanged = (event: KeyboardEvent) => {
            const keyInput = event.currentTarget as OrMwcInput;
            keyInput.setCustomValidity(undefined);
            keyValue = keyInput.currentValue as string;
            let valid = keyInput.valid;

            if (this.data[keyValue] !== undefined) {
                valid = false;
                keyInput.setCustomValidity(i18next.t("validation.keyAlreadyExists"));
            }
            (dialog.shadowRoot!.getElementById("add-btn") as OrMwcInput).disabled = !valid;
        };

        const dialogContentProvider: () => TemplateResult = () => {

            // Only set when !dynamic
            let schemaPicker: TemplateResult | undefined = undefined;

            if (selectedParameter && selectedParameter.schema && selectedParameter.schema.oneOf) {
                const handleChange = (selectedSchema: CombinatorInfo) => {
                    selectedOneOf = selectedSchema;
                    (dialog.shadowRoot!.getElementById("add-btn") as OrMwcInput).disabled = !selectedOneOf;
                    (dialog.shadowRoot!.getElementById("schema-description") as HTMLParagraphElement).innerHTML = (selectedOneOf ? selectedOneOf.description : i18next.t("schema.selectTypeMessage")) || i18next.t("schema.noDescriptionAvailable");
                };
                schemaPicker = getSchemaPicker(rootSchema, selectedParameter.schema, selectedParameter.path, "oneOf", selectedParameter.label, handleChange);
            }

            return html`
                <div class="col">
                    <form id="mdc-dialog-form-add" class="row">
                        ${dynamic ? `` : html`
                            <div id="type-list" class="col">
                                <or-mwc-list @or-mwc-list-changed="${(evt: OrMwcListChangedEvent) => {if (evt.detail.length === 1) onParamChanged((evt.detail[0] as ListItem).data as StatePropsOfControl); }}" .listItems="${listItems}" id="parameter-list"></or-mwc-list>
                            </div>
                        `}
                        <div id="parameter-desc" class="col">
                            ${!selectedParameter ? `` : html`
                                <or-translate id="parameter-title" value="${selectedParameter.label}"></or-translate>
                                <p>${selectedParameter.description}</p>`}
                            ${!dynamic ? !schemaPicker ? `` : html`
                                <style>
                                    #schema-picker {
                                        align-self: stretch;
                                        margin: 10px;
                                        display: flex;
                                        align-items: center;
                                    }
                                    #schema-picker > or-translate {
                                        padding-right: 20px;
                                    }
                                    #schema-picker > or-mwc-input {
                                        flex: 1;
                                    }
                                </style>
                                <div id="schema-picker">
                                    <or-translate style="justify-self: left;" value="type"></or-translate>
                                    ${schemaPicker}
                                </div>
                                <p id="schema-description">${i18next.t("schema.selectTypeMessage")}</p>`
                            : html`
                                <style>
                                    #dynamic-wrapper > or-mwc-input {
                                        display: block;
                                        margin: 10px;
                                    }
                                </style>
                                <div id="dynamic-wrapper">
                                    <or-mwc-input required .type="${InputType.TEXT}" .pattern="${dynamicPropertyRegex}" .label="${i18next.t("schema.keyInputLabel")}" @keyup="${(evt: KeyboardEvent) => onKeyChanged(evt)}"></or-mwc-input>
                                </div>
                            `}
                        </div>
                    </form>
                </div>
            `;
        };

        const dialog = showDialog(new OrMwcDialog()
            .setContent(dialogContentProvider)
            .setStyles(addItemOrParameterDialogStyle)
            .setHeading((this.label ? computeLabel(this.label, this.required, false) + " - " : "") + i18next.t("addParameter"))
            .setActions([
                {
                    actionName: "cancel",
                    content: "cancel"
                },
                {
                    default: true,
                    actionName: "add",
                    action: () => {
                        const key = dynamic ? keyValue as string : selectedParameter!.path.split(".").pop()!;
                        const data = {...this.data};
                        const schema = dynamic ? dynamicValueSchema! : selectedParameter!.schema;
                        data[key] = Array.isArray(schema.type) ? null : ((selectedOneOf ? selectedOneOf.defaultValueCreator() : undefined) || createDefaultValue(schema));
                        this.handleChange(this.path || "", data);
                    },
                    content: html`<or-mwc-input id="add-btn" .type="${InputType.BUTTON}" disabled label="add"></or-mwc-input>`
                }
            ])
            .setDismissAction(null));
    }
}
