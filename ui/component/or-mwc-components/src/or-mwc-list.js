var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { css, html, LitElement, unsafeCSS } from "lit";
import { customElement, property, query } from "lit/decorators.js";
import { styleMap } from "lit/directives/style-map.js";
import { ifDefined } from "lit/directives/if-defined.js";
import { MDCList } from "@material/list";
import { DefaultColor8, DefaultColor4, Util } from "@openremote/core";
import "@openremote/or-translate";
import { i18next } from "@openremote/or-translate";
const listStyle = require("@material/list/dist/mdc.list.css");
const checkboxStyle = require("@material/checkbox/dist/mdc.checkbox.css");
export class OrMwcListChangedEvent extends CustomEvent {
    constructor(items) {
        super(OrMwcListChangedEvent.NAME, {
            detail: items,
            bubbles: true,
            composed: true
        });
    }
}
OrMwcListChangedEvent.NAME = "or-mwc-list-changed";
export var ListType;
(function (ListType) {
    ListType["PLAIN"] = "PLAIN";
    ListType["SELECT"] = "SELECT";
    ListType["RADIO"] = "RADIO";
    ListType["MULTI_CHECKBOX"] = "MULTI_CHECKBOX";
    ListType["MULTI_TICK"] = "MULTI_TICK";
})(ListType || (ListType = {}));
export function createListGroup(lists) {
    return html `
        <div class="mdc-list-group">
            ${lists.map((list) => {
        return html `
                    <h3 class="mdc-list-group__subheader">${list.heading}</h3>
                    ${list.list}
                `;
    })}
        </div>    
    `;
}
export function getListTemplate(type, content, isTwoLine, role, actionHandler) {
    role = role || "listbox";
    switch (type) {
        case ListType.RADIO:
            role = "radiogroup";
            break;
        case ListType.MULTI_CHECKBOX:
            role = "group";
            break;
    }
    return html `
        <ul id="list" class="mdc-list${isTwoLine ? " mdc-list--two-line" : ""}" role="${ifDefined(role)}" @MDCList:action="${(ev) => actionHandler && actionHandler(ev)}" aria-hidden="true" aria-orientation="vertical" tabindex="-1">
            ${content}
        </ul>
    `;
}
export function getItemTemplate(item, index, selectedValues, type, translate, itemClickCallback) {
    if (item === null) {
        // Divider
        return html `<li role="separator" class="mdc-list-divider"></li>`;
    }
    const listItem = item;
    const multiSelect = type === ListType.MULTI_CHECKBOX || type === ListType.MULTI_TICK;
    const value = listItem.value;
    const isSelected = type !== ListType.PLAIN && selectedValues.length > 0 && selectedValues.some((v) => v === value);
    const text = listItem.text !== undefined ? listItem.text : listItem.value;
    const secondaryText = listItem.secondaryText;
    let role = "menuitem";
    let ariaSelected;
    let ariaChecked;
    let tabIndex;
    let textTemplate = ``;
    let leftTemplate = ``;
    let rightTemplate = ``;
    let icon = listItem.icon;
    let selectedClassName = "mdc-list-item--selected";
    translate = translate || item.translate;
    if (multiSelect && type === ListType.MULTI_TICK) {
        icon = isSelected ? "checkbox-marked" : "checkbox-blank-outline";
    }
    if (type === ListType.MULTI_TICK || icon) {
        leftTemplate = html `
                <span class="mdc-list-item__graphic">
                    <or-icon icon="${icon}"></or-icon>
                </span>
            `;
    }
    if (listItem.trailingIcon) {
        rightTemplate = html `
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
            leftTemplate = html `
                    <span class="mdc-list-item__graphic">
                        <div class="mdc-radio">
                            <input class="mdc-radio__native-control" id="radio-item-${index + 1}" type="radio" value="${value}" />
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
            leftTemplate = html `
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
            textTemplate = html `
                    <span class="mdc-list-item__text">
                        <span class="mdc-list-item__primary-text">${translate && !!text ? html `<or-translate value="${text}"></or-translate>` : text}</span>
                        <span class="mdc-list-item__secondary-text">${translate && !!secondaryText ? html `<or-translate value="${secondaryText}"></or-translate>` : secondaryText}</span>
                    </span>
                `;
        }
        else {
            if (type === ListType.RADIO) {
                textTemplate = html `<label class="mdc-list-item__text" for="radio-item-${index + 1}">${translate && !!text ? html `<or-translate value="${text}"></or-translate>` : text}</label>`;
            }
            else {
                textTemplate = html `<span class="mdc-list-item__text" title="${translate && !!text ? i18next.t(text) : text}">${translate && !!text ? html `<or-translate value="${text}"></or-translate>` : text}</span>`;
            }
        }
    }
    return html `
        <li @click="${(e) => { itemClickCallback && itemClickCallback(e, item); }}" style="${listItem.styleMap ? styleMap(listItem.styleMap) : ""}" class="mdc-list-item ${isSelected ? selectedClassName : ""}" role="${ifDefined(role)}" tabindex="${ifDefined(tabIndex)}" aria-checked="${ifDefined(ariaChecked)}" aria-selected="${ifDefined(ariaSelected)}" data-value="${value}">
            <span class="mdc-list-item__ripple"></span>
            ${leftTemplate}
            ${textTemplate}
            ${rightTemplate}
        </li>
    `;
}
// language=CSS
const style = css `
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
let OrMwcList = class OrMwcList extends LitElement {
    static get styles() {
        return [
            css `${unsafeCSS(listStyle)}`,
            css `${unsafeCSS(checkboxStyle)}`,
            style
        ];
    }
    constructor() {
        super();
        this.type = ListType.SELECT;
    }
    disconnectedCallback() {
        super.disconnectedCallback();
        if (this._mdcComponent) {
            this._mdcComponent.destroy();
            this._mdcComponent = undefined;
        }
    }
    shouldUpdate(_changedProperties) {
        if (this._mdcComponent && _changedProperties.has("values")) {
            if (!Util.objectsEqual(this.values, _changedProperties.get("values"))) {
                const vals = this.values ? Array.isArray(this.values) ? this.values : [this.values] : [];
                this.setSelectedItems(this.values && this.listItems ? this.listItems.filter((li) => li && (vals === null || vals === void 0 ? void 0 : vals.includes(li.value))) : undefined);
            }
        }
        return true;
    }
    render() {
        const content = !this.listItems ? html `` : html `${this.listItems.map((listItem, index) => getItemTemplate(listItem, index, (Array.isArray(this.values) ? this.values : this.values ? [this.values] : []), this.type))}`;
        const isTwoLine = this.listItems && this.listItems.some((item) => item && !!item.secondaryText);
        return getListTemplate(this.type, content, isTwoLine, undefined, (ev) => this._onSelected(ev));
    }
    firstUpdated(_changedProperties) {
        super.firstUpdated(_changedProperties);
        if (this._mdcElem) {
            this._mdcComponent = new MDCList(this._mdcElem);
            if (this.type === ListType.SELECT || this.type === ListType.RADIO) {
                this._mdcComponent.singleSelection = true;
            }
        }
    }
    get selectedItems() {
        if (!this._mdcComponent) {
            return [];
        }
        const selectedIndexes = Array.isArray(this._mdcComponent.selectedIndex) ? this._mdcComponent.selectedIndex : [this._mdcComponent.selectedIndex];
        const items = this.listItems ? this.listItems.filter((item) => item !== null) : [];
        return selectedIndexes.map((index) => items[index]);
    }
    setSelectedItems(items) {
        if (!this._mdcComponent || !this.listItems) {
            return;
        }
        if (!items) {
            this._mdcComponent.selectedIndex = -1;
            return;
        }
        const itemArr = (!Array.isArray(items) ? [items] : items).map((item) => typeof (item) === "string" ? item : item.value);
        const listItems = this.listItems.filter((item) => item !== null);
        const indexes = listItems.reduce((indexes, listItem, index) => {
            if (listItem && itemArr.includes(listItem.value)) {
                indexes.push(index);
            }
            return indexes;
        }, []);
        this._mdcComponent.selectedIndex = this.type === ListType.MULTI_CHECKBOX ? indexes : indexes.length >= 1 ? indexes[0] : -1;
    }
    _onSelected(ev) {
        this.values = this.selectedItems.map((item) => item.value);
        ev.stopPropagation();
        this.dispatchEvent(new OrMwcListChangedEvent(this.selectedItems));
    }
};
__decorate([
    property({ type: Array })
], OrMwcList.prototype, "listItems", void 0);
__decorate([
    property({ type: Array })
], OrMwcList.prototype, "values", void 0);
__decorate([
    property({ type: String, attribute: true })
], OrMwcList.prototype, "type", void 0);
__decorate([
    query("#wrapper")
], OrMwcList.prototype, "_wrapperElem", void 0);
__decorate([
    query("#list")
], OrMwcList.prototype, "_mdcElem", void 0);
OrMwcList = __decorate([
    customElement("or-mwc-list")
], OrMwcList);
export { OrMwcList };
//# sourceMappingURL=or-mwc-list.js.map