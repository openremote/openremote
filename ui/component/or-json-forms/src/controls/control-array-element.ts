import {
    computeLabel,
    createDefaultValue,
    JsonSchema,
    mapDispatchToArrayControlProps,
    OwnPropsOfRenderer,
    Paths,
    Resolve,
    update
} from "@jsonforms/core";
import {css, html, PropertyValues, TemplateResult, unsafeCSS} from "lit";
import {customElement, property} from "lit/decorators.js";
import {CombinatorInfo, controlWithoutLabel, getCombinatorInfos, getTemplateFromProps, showJsonEditor} from "../util";
import {InputType, OrMwcInput} from "@openremote/or-mwc-components/or-mwc-input";
import {i18next} from "@openremote/or-translate";
import {OrMwcDialog, showDialog} from "@openremote/or-mwc-components/or-mwc-dialog";
import "@openremote/or-mwc-components/or-mwc-list";
import {addItemOrParameterDialogStyle, baseStyle, panelStyle} from "../styles";
import {ListItem, OrMwcListChangedEvent} from "@openremote/or-mwc-components/or-mwc-list";
import {DefaultColor4, DefaultColor5} from "@openremote/core";
import {ControlBaseElement} from "./control-base-element";
import {getTemplateWrapper} from "../index";
import {AdditionalProps} from "../base-element";

// language=CSS
const style = css`
    .item-border, .drag-handle {
        border-color: var(--or-app-color5, ${unsafeCSS(DefaultColor5)});
        border-radius: 4px;
        border-width: 1px;
        border-style: solid;
    }

    .item-wrapper {
        display: flex;
    }
    
    .item-wrapper > .item-container {
        flex: 1;
    }

    .item-wrapper + .item-wrapper {
        padding-top: 10px;
    }
    
    .item-wrapper > .item-container > .item-container {
        margin: 0;
        flex: 1;
    }

    .item-wrapper > .item-container > .item-container > .delete-container {
        display: none;
    }

    .item-wrapper > .item-container > .item-container :first-child {
        padding: 0;
        margin: 0;
        flex: 1;
    }

    .item-wrapper.dragging > .item-container {
        opacity: 0.5;
    }

    .item-wrapper.indicator-after {
        border-bottom-width: 3px;
        border-bottom-style: solid;
        border-bottom-color: var(--or-app-color4, ${unsafeCSS(DefaultColor4)});
    }

    .item-wrapper.indicator-before {
        border-top-width: 3px;
        border-top-style: solid;
        border-top-color: var(--or-app-color4, ${unsafeCSS(DefaultColor4)});
    }
    
    .drag-container > button {
        cursor: grab;
    }
`;

@customElement("or-json-forms-array-control")
export class ControlArrayElement extends ControlBaseElement {

    @property()
    protected minimal?: boolean;
    protected resolvedSchema!: JsonSchema;
    protected itemInfos: CombinatorInfo[] | undefined;
    protected addItem!: (value: any) => void;
    protected removeItem!: (index: number) => void;
    protected moveItem!: (fromIndex: number, toIndex: number) => void;

    public static get styles() {
        return [
            baseStyle,
            panelStyle,
            style
        ];
    }

    shouldUpdate(_changedProperties: PropertyValues): boolean {
        if (_changedProperties.has("schema")) {
            this.itemInfos = undefined;
            this.resolvedSchema = Resolve.schema(this.schema, 'items', this.rootSchema);

            if (Array.isArray(this.resolvedSchema.anyOf)) {
                this.itemInfos = getCombinatorInfos(this.resolvedSchema.anyOf, this.rootSchema);
            } else if (Array.isArray(this.resolvedSchema.oneOf)) {
                this.itemInfos = getCombinatorInfos(this.resolvedSchema.oneOf, this.rootSchema);
            }
        }

        if (_changedProperties.has("state")) {
            const dispatchMethods = mapDispatchToArrayControlProps(this.state.dispatch);
            this.addItem = (value) => dispatchMethods.addItem(this.path, value)();
            this.removeItem = (index) => {
                dispatchMethods.removeItems!(this.path, [index])();
            };
            this.moveItem = (fromIndex, toIndex) => {
                const move = (input: [], from: number, to: number) => {
                    let numberOfDeletedElm = 1;
                    const elm = input.splice(from, numberOfDeletedElm)[0];
                    numberOfDeletedElm = 0;
                    input.splice(to, numberOfDeletedElm, elm);
                };

                this.state.dispatch(
                    update(this.path, array => {
                        move(array, fromIndex, toIndex);
                        return array;
                    })
                );
            };
        }

        return super.shouldUpdate(_changedProperties);
    }

    render() {

        const maxItems = this.schema.maxItems ?? Number.MAX_SAFE_INTEGER;
        const itemCount = Array.isArray(this.data) ? (this.data as []).length : 0;

        const header = this.minimal ? `` : html`
            <span slot="header">${this.label ? computeLabel(this.label, this.required, false) : ""}</span>
            <div id="header-description" slot="header-description">
                <div id="errors">
                    ${!this.errors ? `` : html`<or-icon icon="alert"></or-icon><span>${this.errors}</span>`}
                </div>
                <div id="header-buttons"><or-mwc-input .type="${InputType.BUTTON}" outlined label="json" icon="pencil" @or-mwc-input-changed="${(ev: Event) => this._showJson(ev)}"></or-mwc-input></div>
            </div>
        `;

        const content = html`
            ${header}
            <div id="content-wrapper" slot="content">
                <div id="content" @dragover="${(ev: DragEvent) => this._onDragOver(ev)}">
                    
                    ${!Array.isArray(this.data) ? `` : (this.data as any[]).map((item, index) => {

                        const childPath = Paths.compose(this.path, "" + index);
            
                        const props: OwnPropsOfRenderer | AdditionalProps = {
                            renderers: this.renderers,
                            uischema: controlWithoutLabel("#"),
                            schema: this.resolvedSchema,
                            path: childPath
                        }
            
                        return this.getArrayItemWrapper(getTemplateFromProps(this.state, props) || html``, index);
                    })}

                </div>
                ${this.errors ? `` : html`
                    <div id="footer">
                        <or-mwc-input .disabled="${itemCount && itemCount >= maxItems}" .type="${InputType.BUTTON}" label="addItem" icon="plus" @or-mwc-input-changed="${() => this.doAddItem()}"></or-mwc-input>
                    </div>
                `}
            </div>
        `;

        return this.minimal ? html`<div>${content}</div>` : html`<or-collapsible-panel>${content}</or-collapsible-panel>`;
    }

    protected getArrayItemWrapper(elementTemplate: TemplateResult, index: number) {

        return html`
            <div class="item-wrapper" data-index="${index}">
                <div class="drag-container">
                    <button draggable="true" @dragstart="${(ev: DragEvent) => this._onDragStart(ev)}" @dragend="${(ev: DragEvent) => this._onDragEnd(ev)}" class="draggable button-clear"><or-icon icon="menu"></or-icon></input>
                </div>
                ${getTemplateWrapper(elementTemplate, () => this.removeItem(index))}
            </div>
        `;
    }

    protected _onDragStart(ev: DragEvent) {
        const buttonElem = ev.currentTarget as HTMLButtonElement;
        const itemWrapperElem = buttonElem!.parentElement!.parentElement as HTMLDivElement;
        itemWrapperElem.classList.add("dragging");
        const itemContainerElem = itemWrapperElem!.lastElementChild!;
        ev.dataTransfer!.setDragImage(itemContainerElem, (itemContainerElem.getBoundingClientRect().width/2) - 50, 0);
    }

    protected _onDragEnd(ev: DragEvent) {
        const draggables = [...((this.shadowRoot!.querySelectorAll(".item-wrapper") as any) as HTMLDivElement[])];
        const buttonElem = ev.currentTarget as HTMLButtonElement;
        const itemWrapperElem = buttonElem!.parentElement!.parentElement as HTMLDivElement;
        itemWrapperElem.classList.remove("dragging");
        const index = Number(itemWrapperElem.getAttribute("data-index"));
        const afterIndex = Math.max(0, (itemWrapperElem.getAttribute("data-after-index") !== null ? Number(itemWrapperElem.getAttribute("data-after-index")) : draggables.length) - 1);

        draggables.forEach(draggable => draggable.classList.remove("indicator-before", "indicator-after"));

        if (index === afterIndex) {
            return;
        }

        this.moveItem(index, afterIndex);
    }

    protected _onDragOver(ev: DragEvent) {
        const dragging = this.shadowRoot!.querySelector(".dragging") as HTMLDivElement;

        if (!dragging) {
            return;
        }

        ev.preventDefault();

        const draggables = [...((this.shadowRoot!.querySelectorAll(".item-wrapper:not(.dragging)") as any) as HTMLDivElement[])];
        const initial = {offset: Number.NEGATIVE_INFINITY, element: null} as {
            offset: number,
            element: HTMLDivElement | null
        };

        const afterItem = draggables.reduce((closest, child) => {
            const box = child.getBoundingClientRect();
            const offset = ev.clientY - box.top - box.height / 2;
            if (offset < 0 && offset > closest.offset) {
                return {offset: offset, element: child};
            } else {
                return closest;
            }
        }, initial).element;

        draggables.forEach(draggable => draggable.classList.remove("indicator-before", "indicator-after"));

        if (afterItem === null) {
            draggables[draggables.length-1].classList.add("indicator-after");
            dragging.removeAttribute("data-after-index");
        } else {
            afterItem.classList.add("indicator-before");
            const afterIndex = afterItem.getAttribute("data-index");
            dragging.setAttribute("data-after-index", afterIndex!);
        }
    }

    protected _showJson(ev: Event) {
        ev.stopPropagation();

        showJsonEditor(this.title || this.schema.title || "", this.data, ((newValue) => {
            this.handleChange(this.path || "", newValue);
        }));
    }

    protected doAddItem() {
        if (!this.resolvedSchema) {
            return;
        }

        if (this.itemInfos) {
            this.showAddDialog();
        } else {
            this.addItem(createDefaultValue(this.resolvedSchema));
        }
    }

    protected showAddDialog() {

        let selectedItemInfo: CombinatorInfo | undefined;

        const listItems: ListItem[] = this.itemInfos!.map((itemInfo, index) => {
            const labelStr = itemInfo.title ? computeLabel(itemInfo.title, false, true) : "";
            return {
                text: labelStr,
                value: labelStr,
                data: itemInfo
            }
        });

        const onParamChanged = (itemInfo: CombinatorInfo) => {
            selectedItemInfo = itemInfo;
            const descElem = dialog.shadowRoot!.getElementById("parameter-desc") as HTMLDivElement;
            descElem.innerHTML = itemInfo.description || "";
            (dialog.shadowRoot!.getElementById("add-btn") as OrMwcInput).disabled = false;
        };

        const dialog = showDialog(new OrMwcDialog()
            .setContent(html`
                <div class="col">
                    <form id="mdc-dialog-form-add" class="row">
                        <div id="type-list" class="col">
                            <or-mwc-list @or-mwc-list-changed="${(evt: OrMwcListChangedEvent) => {if (evt.detail.length === 1) onParamChanged((evt.detail[0] as ListItem).data as CombinatorInfo); }}" .listItems="${listItems.sort((a, b) => a.text!.localeCompare(b.text!))}" id="parameter-list"></or-mwc-list>
                        </div>
                        <div id="parameter-desc" class="col"></div>
                    </form>
                </div>
            `)
            .setStyles(addItemOrParameterDialogStyle)
            .setHeading((this.label ? computeLabel(this.label, this.required, false) + " - " : "") + i18next.t("addItem"))
            .setActions([
                {
                    actionName: "cancel",
                    content: "cancel"
                },
                {
                    default: true,
                    actionName: "add",
                    action: () => {
                        if (selectedItemInfo) {
                            const value = selectedItemInfo.defaultValueCreator();
                            this.addItem(value);
                        }
                    },
                    content: html`<or-mwc-input id="add-btn" .type="${InputType.BUTTON}" disabled label="add"></or-mwc-input>`
                }
            ])
            .setDismissAction(null));
    }
}
