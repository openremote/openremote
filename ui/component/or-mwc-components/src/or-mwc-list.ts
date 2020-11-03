import {
    css,
    customElement,
    html,
    LitElement,
    property,
    PropertyValues,
    query,
    TemplateResult,
    unsafeCSS
} from "lit-element";
import {styleMap} from "lit-html/directives/style-map";
import {ifDefined} from "lit-html/directives/if-defined";
import {MDCList, MDCListActionEvent} from "@material/list";
import { DefaultColor8, DefaultColor4, Util } from "@openremote/core";
import i18next from "i18next";
const listStyle = require("!!raw-loader!@material/list/dist/mdc.list.css");
const checkboxStyle = require("!!raw-loader!@material/checkbox/dist/mdc.checkbox.css");

export interface ListItem {
    icon?: string;
    trailingIcon?: string;
    text?: string;
    translate?: boolean;
    secondaryText?: string;
    value: string;
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

// language=CSS
const style = css`
    :host {
        white-space: nowrap;
        --internal-or-input-color: var(--or-input-color, var(--or-app-color4, ${unsafeCSS(DefaultColor4)}));    
        --internal-or-input-text-color: var(--or-input-text-color, var(--or-app-color1, ${unsafeCSS(DefaultColor8)}));
        
        --mdc-theme-primary: var(--internal-or-input-color);
        --mdc-theme-on-primary: var(--internal-or-input-text-color);
        --mdc-theme-secondary: var(--internal-or-input-color);
    }
    
    .mdc-list-item__graphic {
        margin-right: 16px;
    }

    a {
        text-decoration: none;
        color: rgba(0, 0, 0, 0.87);
    }     
`;

export enum ListType {
    PLAIN = "PLAIN",
    SELECTABLE = "SELECTABLE",
    RADIO = "RADIO",
    CHECKBOX = "CHECKBOX"
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
    public type: ListType = ListType.SELECTABLE;

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

        if (!this.listItems || this.listItems.length === 0) {
            return html``;
        }

        return this.getItemsTemplate(this.listItems, this.values, this.type);
    }

    protected getItemsTemplate(items: (ListItem | null)[], values: string | string[] | undefined, type: ListType): TemplateResult {

        const isTwoLine = items.some((item) => item && !!item.secondaryText);
        let role: string | undefined;

        switch (type) {
            case ListType.SELECTABLE:
                role = "listbox";
                break;
            case ListType.RADIO:
                role = "radiogroup";
                break;
            case ListType.CHECKBOX:
                role = "group";
                break;
        }

        return html`
            <ul id="list" class="mdc-list${isTwoLine ? " mdc-list--two-line" : ""}" role="${ifDefined(role)}" @MDCList:action="${(ev: MDCListActionEvent) => this._onSelected(ev)}">
                ${items.map((listItem, index) => this.getItemTemplate(listItem, index, values, type))}
            </ul>
        `;
    }

    protected getItemTemplate(item: ListItem | null, index: number, values: string | string[] | undefined, type: ListType): TemplateResult {

        if (item === null) {
            // Divider
            return html`<li role="separator" class="mdc-list-divider"></li>`;
        }

        const listItem = item as ListItem;
        const multiSelect = type === ListType.CHECKBOX;
        const value = listItem.value;
        const isSelected = type !== ListType.PLAIN && (values === value || (Array.isArray(values) && values.length > 0 && ((multiSelect && values.some((v) => v === value)) || (!multiSelect && values[0] === value))));
        let text = listItem.text !== undefined ? listItem.text : listItem.value;
        let secondaryText = listItem.secondaryText;

        if (listItem.translate !== false) {
            text = i18next.t(text);
            if (secondaryText) {
                secondaryText = i18next.t(secondaryText);
            }
        }
        let role: string | undefined;
        let ariaSelected: string | undefined;
        let ariaChecked: string | undefined;
        let tabIndex: string | undefined;
        let textTemplate: TemplateResult | string = ``;
        let leftTemplate: TemplateResult | string = ``;
        let rightTemplate: TemplateResult | string = ``;

        if (listItem.icon) {
            leftTemplate = html`
                <span class="mdc-list-item__graphic">
                    <or-icon icon="${listItem.icon}"></or-icon>
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
            case ListType.SELECTABLE:
                ariaSelected = isSelected ? "true" : "false";
                tabIndex = isSelected || ((!values || values.length === 0) && index === 0) ? "0" : undefined;
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
            case ListType.CHECKBOX:
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
        }

        if (text) {
            if (secondaryText !== undefined) {
                textTemplate = html`
                    <span class="mdc-list-item__text">
                        <span class="mdc-list-item__primary-text">${text}</span>
                        <span class="mdc-list-item__secondary-text">${secondaryText || ""}</or-translate></span>
                    </span>
                `;
            } else {
                if (type === ListType.RADIO) {
                    textTemplate = html`<label class="mdc-list-item__text" for="radio-item-${index+1}">${text}</label>`;
                } else {
                    textTemplate = html`<span class="mdc-list-item__text">${text}</span>`;
                }
            }
        }

        return html`
            <li style="${listItem.styleMap ? styleMap(listItem.styleMap) : ""}" class="mdc-list-item${isSelected ? " mdc-list-item--selected" : ""}" role="${ifDefined(role)}" tabindex="${ifDefined(tabIndex)}" aria-checked="${ifDefined(ariaChecked)}" aria-selected="${ifDefined(ariaSelected)}">
                <span class="mdc-list-item__ripple"></span>
                ${leftTemplate}
                ${textTemplate}
                ${rightTemplate}
            </li>
        `;
    }

    protected firstUpdated(_changedProperties: PropertyValues): void {
        super.firstUpdated(_changedProperties);
        if (this._mdcElem) {
            this._mdcComponent = new MDCList(this._mdcElem);
            if (this.type === ListType.SELECTABLE || this.type === ListType.RADIO) {
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

        this._mdcComponent.selectedIndex = indexes.length === 1 ? indexes[0] : indexes.length > 1 ? indexes : -1;
    }

    protected _onSelected(ev: MDCListActionEvent) {
        this.values = this.selectedItems.map((item) => item.value!);
        ev.stopPropagation();
        this.dispatchEvent(new OrMwcListChangedEvent(this.selectedItems));
    }

    // private _itemClicked(e: MouseEvent, item: MenuItem) {
    //     e.stopPropagation();
    //     const value = item.value;
    //
    //     if (!this.multiSelect) {
    //         this.values = value;
    //         if(!this.noSurface){
    //             this._mdcComponent!.open = false;
    //         }
    //     } else {
    //         if (!Array.isArray(this.values)) {
    //             this.values = this.values ? [this.values] : [];
    //         }
    //         const index = this.values.findIndex((v) => v === value);
    //         if (index >= 0) {
    //             this.values.splice(index, 1);
    //         } else {
    //             this.values.push(value);
    //         }
    //         this.requestUpdate();
    //     }
    //     this.dispatchEvent(new OrMwcMenuChangedEvent(this.values));
    // }
}
