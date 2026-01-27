/*
 * Copyright 2026, OpenRemote Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
import {html, TemplateResult, unsafeCSS} from "lit";
import {property, query} from "lit/decorators.js";
import "@openremote/or-asset-tree";
import {i18next} from "@openremote/or-translate";
import {InputType, OrInputChangedEvent, OrMwcInput} from "@openremote/or-mwc-components/or-mwc-input";
import {ListItem, ListType, OrMwcListChangedEvent} from "@openremote/or-mwc-components/or-mwc-list";
import {DefaultColor2, DefaultColor4, DefaultColor5, Util} from "@openremote/core";
import {Attribute, AttributeDescriptor} from "@openremote/model";
import {DialogAction, DialogActionBase, OrMwcDialog} from "@openremote/or-mwc-components/or-mwc-dialog";
import {when} from "lit/directives/when.js";

export abstract class AttributePickerPickedEvent extends CustomEvent<any> {

}

/**
 * @summary Abstract implementation of the Attribute Picker UI. Wraps around OrMwcDialog and provides some utility properties and functions to inherit.
 *
 * @attribute {boolean} multiSelect - Whether selecting multiple attributes is allowed or not.
 * @attribute {boolean} showOnlyDatapointAttrs - Whether only attributes with the 'STORE_DATAPOINT' meta item should be shown.
 * @attribute {boolean} showOnlyRuleStateAttrs - Whether only attributes with the 'RULE_STATE' meta item should be shown.
 *
 * @remarks This class is abstract
 */
export abstract class AttributePicker extends OrMwcDialog {

    protected abstract _setDialogContent(): void;

    protected abstract _setDialogActions(): void;


    @property({type: Boolean})
    public multiSelect?: boolean = false;

    @property({type: Boolean})
    public showOnlyDatapointAttrs?: boolean = false;

    @property({type: Boolean})
    public showOnlyRuleStateAttrs?: boolean = false;

    @query("#add-btn")
    protected addBtn!: OrMwcInput;

    connectedCallback() {
        super.connectedCallback();

        this.heading = i18next.t("selectAttributes");
        this._setDialogContent();
        this._setDialogActions();
        this.dismissAction = null;
        this.styles = this._getStyles();
    }

    public setShowOnlyDatapointAttrs(showOnlyDatapointAttrs: boolean | undefined): this {
        this.showOnlyDatapointAttrs = showOnlyDatapointAttrs;
        return this;
    }

    public setShowOnlyRuleStateAttrs(showOnlyRuleStateAttrs: boolean | undefined): this {
        this.showOnlyRuleStateAttrs = showOnlyRuleStateAttrs;
        return this;
    }

    public setMultiSelect(multiSelect: boolean | undefined): this {
        this.multiSelect = multiSelect;
        return this;
    }

    public setOpen(isOpen: boolean): this {
        super.setOpen(isOpen);
        return this;
    }

    public setHeading(heading: TemplateResult | string | undefined): this {
        super.setHeading(heading);
        return this;
    }

    public setContent(_content: TemplateResult | (() => TemplateResult) | undefined): this {
        throw new Error("Cannot modify attribute picker content");
    }

    public setActions(_actions: DialogAction[] | undefined): this {
        throw new Error("Cannot modify attribute picker actions");
    }

    public setDismissAction(_action: DialogActionBase | null | undefined): this {
        throw new Error("Cannot modify attribute picker dismiss action");
    }

    public setStyles(_styles: string | TemplateResult | undefined): this {
        throw new Error("Cannot modify attribute picker styles");
    }

    public setAvatar(_avatar: boolean | undefined): this {
        throw new Error("Cannot modify attribute picker avatar setting");
    }


    /**
     * Convenient function to update the dialog content manually,
     * since updating the UI is handled different for {@link OrMwcDialog}.
     */
    protected _updateDialogContent() {
        this._setDialogContent();
    }


    /**
     * Convenient function to update the dialog actions manually,
     * since updating the UI is handled different for {@link OrMwcDialog}.
     */
    protected _updateDialogActions() {
        this._setDialogActions();
    }


    /**
     * Function that creates the HTML template for selecting attributes.
     * Currently uses {@link OrMwcList} with or without checkboxes, and uses {@link Util.getAttributeLabel} to formulate the text.
     *
     * @remarks TODO: Move this template into a separate component, such as an "or-attribute-list"
     */
    protected async _getAttributesTemplate(attributes?: Attribute<any>[], descriptors?: AttributeDescriptor[], selectedNames?: string[], multi = false, onSelect?: (attrNames: string[]) => void): Promise<TemplateResult> {
        const length = Math.max((attributes?.length || 0), (descriptors?.length || 0));
        const listItems: ListItem[] = [];
        for(let i = 0; i < length; i++) {
            listItems.push({
                text: Util.getAttributeLabel(attributes?.[i], descriptors?.[i], undefined, true),
                value: (attributes?.[i].name || descriptors?.[i].name),
            });
        }
        return html`
            ${when(multi, () => html`
                <or-mwc-list id="attribute-selector" .type="${ListType.MULTI_CHECKBOX}" .listItems="${listItems}" .values="${selectedNames}"
                             @or-mwc-list-changed="${(ev: OrMwcListChangedEvent) => onSelect?.(ev.detail.map(item => item.value))}"
                </or-mwc-list>
            `, () => html`
                <or-mwc-input id="attribute-selector" .type="${InputType.LIST}" .options="${listItems?.map(item => ([item, item.text]))}"
                              style="display:flex;" .label="${i18next.t("attribute")}"
                              @or-mwc-input-changed="${(ev: OrInputChangedEvent) => onSelect?.([(ev.detail.value as ListItem).value])}"
                ></or-mwc-input>
            `)}
        `;
    }

    /**
     * Simple function that creates the CSS styles for this component
     */
    protected _getStyles(): string {

        // language=css
        return `
            .attributes-header {
                line-height: 48px;
                padding: 0 15px;
                background-color: ${unsafeCSS(DefaultColor2)};
                font-weight: bold;
                border-bottom: 1px solid ${unsafeCSS(DefaultColor2)};
            }
            footer.mdc-dialog__actions {
                border-top: 1px solid ${unsafeCSS(DefaultColor5)};
            }
            #header {
                background-color: ${unsafeCSS(DefaultColor4)} !important;
            }
            #dialog-content {
                padding: 0;
            }
        `

    }
}
