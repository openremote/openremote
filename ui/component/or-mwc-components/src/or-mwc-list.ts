import {
    css,
    html,
    LitElement,
    PropertyValues,
    TemplateResult,
    unsafeCSS
} from "lit";
import {customElement, property, query} from "lit/decorators.js";
import {styleMap} from "lit/directives/style-map.js";
import {ifDefined} from "lit/directives/if-defined.js";
import {MDCList, MDCListActionEvent} from "@material/list";
import { DefaultColor8, DefaultColor4, Util } from "@openremote/core";
import "@openremote/or-translate";
import { i18next } from "@openremote/or-translate";
const listStyle = require("@material/list/dist/mdc.list.css");
const checkboxStyle = require("@material/checkbox/dist/mdc.checkbox.css");

export {MDCListActionEvent};

export interface ListItem {
    icon?: string;
    trailingIcon?: string;
    text?: string;
    translate?: boolean;
    secondaryText?: string;
    value: any;
    data?: any;
    styleMap?: {[style: string]: string};
}

export class OrMwcListChangedEvent extends CustomEvent<ListItem[]> {

    public static readonly NAME = "or-mwc-list-changed";

    constructor(items: ListItem[]) {
        super(OrMwcListChangedEvent.NAME, {
            detail: items,
            bubbles: true,
            composed: true
        });
    }
}
declare global {
    export interface HTMLElementEventMap {
        [OrMwcListChangedEvent.NAME]: OrMwcListChangedEvent;
    }
}

export enum ListType {
    PLAIN = "PLAIN",
    SELECT = "SELECT",
    RADIO = "RADIO",
    MULTI_CHECKBOX = "MULTI_CHECKBOX",
    MULTI_TICK = "MULTI_TICK"
}

export type ListGroupItem = {
    heading: string;
    list: TemplateResult;
};

export function createListGroup(lists: ListGroupItem[]) {
    return html`
        <div class="mdc-list-group">
            ${lists.map((list) => {
                return html`
                    <h3 class="mdc-list-group__subheader">${list.heading}</h3>
                    ${list.list}
                `                            
            })}
        </div>    
    `;
}

export function getListTemplate(type: ListType, content: TemplateResult, isTwoLine?: boolean, role?: string, actionHandler?: (ev: MDCListActionEvent) => void): TemplateResult {

    role = role || "listbox";

    switch (type) {
        case ListType.RADIO:
            role = "radiogroup";
            break;
        case ListType.MULTI_CHECKBOX:
            role = "group";
            break;
    }

    return html`
        <ul id="list" class="mdc-list${isTwoLine ? " mdc-list--two-line" : ""}" role="${ifDefined(role)}" @MDCList:action="${(ev: MDCListActionEvent) => actionHandler && actionHandler(ev)}" aria-hidden="true" aria-orientation="vertical" tabindex="-1">
            ${content}
        </ul>
    `;
}

export function getItemTemplate(item: ListItem | null, index: number, selectedValues: any[], type: ListType, translate?: boolean, itemClickCallback?: (e: MouseEvent, item: ListItem) => void): TemplateResult {

    if (item === null) {
        // Divider
        return html`<li role="separator" class="mdc-list-divider"></li>`;
    }

    const listItem = item as ListItem;
    const multiSelect = type === ListType.MULTI_CHECKBOX || type === ListType.MULTI_TICK;
    const value = listItem.value;
    const isSelected = type !== ListType.PLAIN && selectedValues.length > 0 && selectedValues.some((v) => v === value);
    const text = listItem.text !== undefined ? listItem.text : listItem.value;
    const secondaryText = listItem.secondaryText;
    let role: string | undefined = "menuitem";
    let ariaSelected: string | undefined;
    let ariaChecked: string | undefined;
    let tabIndex: string | undefined;
    let textTemplate: TemplateResult | string = ``;
    let leftTemplate: TemplateResult | string = ``;
    let rightTemplate: TemplateResult | string = ``;
    let icon = listItem.icon;
    let selectedClassName = "mdc-list-item--selected";
    translate = translate || item.translate;
    
    if (multiSelect && type === ListType.MULTI_TICK) {
        icon = isSelected ? "checkbox-marked" : "checkbox-blank-outline";
    }

    if (type === ListType.MULTI_TICK || icon) {
        leftTemplate = html`
                <span class="mdc-list-item__graphic">
                    <or-icon icon="${icon}"></or-icon>
                </span>
            `;
    }

    if (listItem.trailingIcon) {
        rightTemplate = html`
                <span class="mdc-list-item__meta" aria-hidden="true">
                    <or-icon icon="${listItem.trailingIcon}"></or-icon>
                </span>
            `;
    }

    switch (type) {
        case ListType.SELECT:
            ariaSelected = isSelected ? "true" : "false";
            tabIndex = isSelected || ((!selectedValues || selectedValues.length === 0) && index === 0) ? "0" : undefined;
            role = "option";
            break;
        case ListType.RADIO:
            ariaChecked = isSelected ? "true" : "false";
            role = "radio";
            leftTemplate = html`
                    <span class="mdc-list-item__graphic">
                        <div class="mdc-radio">
                            <input class="mdc-radio__native-control" id="radio-item-${index+1}" type="radio" value="${value}" />
                            <div class="mdc-radio__background">
                                <div class="mdc-radio__outer-circle"></div>
                                <div class="mdc-radio__inner-circle"></div>
                            </div>
                        </div>
                    </span>
                `;
            break;
        case ListType.MULTI_CHECKBOX:
            ariaChecked = isSelected ? "true" : "false";
            role = "checkbox";
            leftTemplate = html`
                    <div class="mdc-checkbox">
                        <input type="checkbox" class="mdc-checkbox__native-control" />
                        <div class="mdc-checkbox__background">
                            <svg class="mdc-checkbox__checkmark" viewBox="0 0 24 24">
                                <path class="mdc-checkbox__checkmark-path" fill="none" d="M1.73,12.91 8.1,19.28 22.79,4.59"/>
                            </svg>
                            <div class="mdc-checkbox__mixedmark"></div>
                        </div>
                    </div>
                `;
            break;
        case ListType.MULTI_TICK:
            ariaChecked = isSelected ? "true" : "false";
            selectedClassName = "mdc-list-item--selected";
            break;
    }

    if (text) {
        if (secondaryText !== undefined) {
            textTemplate = html`
                    <span class="mdc-list-item__text">
                        <span class="mdc-list-item__primary-text">${translate && !!text ? html`<or-translate value="${text}"></or-translate>` : text}</span>
                        <span class="mdc-list-item__secondary-text">${translate && !!secondaryText ? html`<or-translate value="${secondaryText}"></or-translate>` : secondaryText}</span>
                    </span>
                `;
        } else {
            if (type === ListType.RADIO) {
                textTemplate = html`<label class="mdc-list-item__text" for="radio-item-${index+1}">${translate && !!text ? html`<or-translate value="${text}"></or-translate>` : text}</label>`;
            } else {
                textTemplate = html`<span class="mdc-list-item__text" title="${translate && !!text ? i18next.t(text) : text}">${translate && !!text ? html`<or-translate value="${text}"></or-translate>` : text}</span>`;
            }
        }
    }

    return html`
        <li @click="${(e: MouseEvent) => { itemClickCallback && itemClickCallback(e, item)}}" style="${listItem.styleMap ? styleMap(listItem.styleMap) : ""}" class="mdc-list-item ${isSelected ? selectedClassName : ""}" role="${ifDefined(role)}" tabindex="${ifDefined(tabIndex)}" aria-checked="${ifDefined(ariaChecked)}" aria-selected="${ifDefined(ariaSelected)}" data-value="${value}">
            <span class="mdc-list-item__ripple"></span>
            ${leftTemplate}
            ${textTemplate}
            ${rightTemplate}
        </li>
    `;
}

// language=CSS
const style = css`
    :host {
        white-space: nowrap;
        --internal-or-mwc-input-color: var(--or-mwc-input-color, var(--or-app-color4, ${unsafeCSS(DefaultColor4)}));    
        --internal-or-mwc-input-text-color: var(--or-mwc-input-text-color, var(--or-app-color8, ${unsafeCSS(DefaultColor8)}));
        
        --mdc-theme-primary: var(--internal-or-mwc-input-color);
        --mdc-theme-on-primary: var(--internal-or-mwc-input-text-color);
        --mdc-theme-secondary: var(--internal-or-mwc-input-color);
    }
    
    .mdc-list-item__graphic {
        margin-right: 16px;
    }

    a {
        text-decoration: none;
        color: rgba(0, 0, 0, 0.87);
    }     
`;

@customElement("or-mwc-list")
export class OrMwcList extends LitElement {

    static get styles() {
        return [
            css`${unsafeCSS(listStyle)}`,
            css`${unsafeCSS(checkboxStyle)}`,
            style
        ];
    }

    @property({type: Array})
    public listItems?: (ListItem | null)[];

    @property({type: Array})
    public values?: string[] | string;

    @property({type: String, attribute: true})
    public type: ListType = ListType.SELECT;

    @query("#wrapper")
    protected _wrapperElem!: HTMLElement;

    @query("#list")
    protected _mdcElem!: HTMLElement;

    protected _mdcComponent?: MDCList;

    constructor() {
        super();
    }

    disconnectedCallback(): void {
        super.disconnectedCallback();
        if (this._mdcComponent) {
            this._mdcComponent.destroy();
            this._mdcComponent = undefined;
        }
    }

    protected shouldUpdate(_changedProperties: PropertyValues): boolean {
        if (this._mdcComponent && _changedProperties.has("values")) {
            if (!Util.objectsEqual(this.values, _changedProperties.get("values"))) {
                const vals = this.values ? Array.isArray(this.values) ? this.values : [this.values] : [];
                this.setSelectedItems(this.values && this.listItems ? this.listItems.filter((li) => li && vals?.includes(li.value)) as ListItem[] : undefined);
            }
        }

        return true;
    }

    protected render() {
        const content = !this.listItems ? html`` : html`${this.listItems.map((listItem, index) => getItemTemplate(listItem, index, (Array.isArray(this.values) ? this.values : this.values ? [this.values] : []), this.type))}`;
        const isTwoLine = this.listItems && this.listItems.some((item) => item && !!item.secondaryText);
        return getListTemplate(this.type, content, isTwoLine, undefined, (ev) => this._onSelected(ev));
    }

    protected firstUpdated(_changedProperties: PropertyValues): void {
        super.firstUpdated(_changedProperties);
        if (this._mdcElem) {
            this._mdcComponent = new MDCList(this._mdcElem);
            if (this.type === ListType.SELECT || this.type === ListType.RADIO) {
                this._mdcComponent.singleSelection = true;
            }
        }
    }

    public get selectedItems(): ListItem[] {
        if (!this._mdcComponent) {
            return [];
        }

        const selectedIndexes = Array.isArray(this._mdcComponent.selectedIndex) ? this._mdcComponent.selectedIndex : [this._mdcComponent.selectedIndex];
        const items = this.listItems ? this.listItems.filter((item) => item !== null) as ListItem[] : [];
        return selectedIndexes.map((index) => items![index]);
    }

    public setSelectedItems(items: ListItem | ListItem[] | string | string[] | undefined) {
        if (!this._mdcComponent || !this.listItems) {
            return;
        }
        if (!items) {
            this._mdcComponent.selectedIndex = -1;
            return;
        }

        const itemArr = (!Array.isArray(items) ? [items] : items).map((item) => typeof(item) === "string" ? item : item.value);
        const listItems = this.listItems.filter((item) => item !== null) as ListItem[];

        const indexes = listItems.reduce((indexes, listItem, index) => {
            if (listItem && itemArr.includes(listItem.value)) {
                indexes.push(index);
            }
            return indexes;
        }, [] as number[]);

        this._mdcComponent.selectedIndex = this.type === ListType.MULTI_CHECKBOX ? indexes : indexes.length >= 1 ? indexes[0] : -1;
    }

    protected _onSelected(ev: MDCListActionEvent) {
        this.values = this.selectedItems.map((item) => item.value!);
        ev.stopPropagation();
        this.dispatchEvent(new OrMwcListChangedEvent(this.selectedItems));
    }
}
