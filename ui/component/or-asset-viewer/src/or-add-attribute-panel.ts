/*
 * Copyright 2025, OpenRemote Inc.
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
import {html, LitElement, css, PropertyValues} from "lit";
import {customElement, property, query} from "lit/decorators.js";
import {InputType, OrMwcInput, OrInputChangedEvent} from "@openremote/or-mwc-components/or-mwc-input";
import i18next from "i18next";
import {Asset, Attribute, AssetModelUtil} from "@openremote/model";
import {Util} from "@openremote/core";

export class OrAddAttributePanelAttributeChangedEvent extends CustomEvent<Attribute<any>> {

    public static readonly NAME = "or-add-attribute-panel-attribute-changed";

    constructor(attribute: Attribute<any>) {
        super(OrAddAttributePanelAttributeChangedEvent.NAME, {
            bubbles: true,
            composed: false,
            detail: attribute
        });
    }
}
declare global {
    export interface HTMLElementEventMap {
        [OrAddAttributePanelAttributeChangedEvent.NAME]: OrAddAttributePanelAttributeChangedEvent;
    }
}

@customElement("or-add-attribute-panel")
export class OrAddAttributePanel extends LitElement {

    @property({attribute: false})
    protected asset!: Asset;

    @property({attribute: false})
    protected attribute: Attribute<any> = {};

    @property()
    protected isCustom = false;

    @property()
    public isArray = false;

    @property()
    public arrayDimensions = 1;

    @query("#array-input")
    protected arrayInput!: OrMwcInput;

    protected customAttribute = true;
    protected attributeTypes?: [string, string][];
    protected attributeValueTypes?: [string, string][];
    protected arrayRegex = /\[\]/g;

    public static get styles() {
        return css`                        
            or-mwc-input {
                width: 300px;
                display: block;
                padding: 10px 20px;
            }`;
    }

    protected shouldUpdate(_changedProperties: PropertyValues) {
        if (_changedProperties.has("asset")) {
            this.attributeTypes = undefined;
            this.attributeValueTypes = undefined;


            this.attributeTypes = (AssetModelUtil.getAssetTypeInfo(this.asset.type!)?.attributeDescriptors || [])
                .filter((descriptor) => !this.asset.attributes![descriptor.name!])
                .sort(Util.sortByString((descriptor) => descriptor.name!))
                .map((descriptor) => {
                    return [
                        descriptor.name!,
                        Util.getAttributeLabel(undefined, descriptor, this.asset.type, false)
                    ]
                });
            this.attributeTypes = [["@custom", i18next.t("custom")], ...this.attributeTypes];

            this.attributeValueTypes = (AssetModelUtil.getAssetTypeInfo(this.asset.type!)?.valueDescriptors || [])
                .sort(Util.sortByString((descriptor) => descriptor[0]))
                .filter(descriptorName => {
                    const valueDescriptor = AssetModelUtil.getValueDescriptor(descriptorName);
                    return !valueDescriptor || !valueDescriptor.metaUseOnly;
                })
                .map((descriptor) => {
                    return [
                        descriptor,
                        Util.getValueDescriptorLabel(descriptor)
                    ];
                });
        }


        return super.shouldUpdate(_changedProperties);
    }

    protected render() {

        if (!this.attributeTypes || !this.attributeValueTypes) {
            return;
        }

        return html`
            <div id="attribute-creator">
                <or-mwc-input .type="${InputType.SELECT}" .options="${this.attributeTypes}" .label="${i18next.t("type")}" .value="${"@custom"}" @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this.onTypeChanged(ev.detail.value)}"></or-mwc-input>
                <or-mwc-input id="name-input" .type="${InputType.TEXT}" ?disabled="${!this.customAttribute}" .value="${(this.attribute && this.attribute.name) ? this.attribute.name : undefined}" .label="${i18next.t("name")}" pattern="\\w+" required @keyup="${(ev: KeyboardEvent) => this.onNameChanged((ev.target as OrMwcInput).currentValue)}" @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this.onNameChanged(ev.detail.value)}"></or-mwc-input>
                <or-mwc-input id="type-input" .type="${InputType.SELECT}" ?disabled="${!this.customAttribute}" .value="${this.attribute && this.attribute.type ? this.attribute.type.replace(this.arrayRegex, "") : undefined}" .options="${this.attributeValueTypes}" .label="${i18next.t("valueType")}" @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this.onValueTypeChanged(ev.detail.value)}"></or-mwc-input>
                <or-mwc-input id="array-checkbox" .type="${InputType.CHECKBOX}" ?disabled="${!this.customAttribute}" .value="${this.isArray}" .label="${i18next.t("array")}" @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this.onArrayChanged(ev.detail.value, 1)}"></or-mwc-input>
                <or-mwc-input id="array-input" .type="${InputType.NUMBER}" ?disabled="${!this.isArray || !this.customAttribute}" .value="${this.arrayDimensions}" min="1" max="2" .label="${i18next.t("arrayDimensions")}" @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this.onArrayChanged(true, ev.detail.value)}"></or-mwc-input>
            </div>
        `;
    }

    protected onTypeChanged(name: string) {
        if (name === "@custom") {
            this.customAttribute = true;
            this.attribute = {
                meta: {}
            };
        } else {
            this.customAttribute = false;
            const descriptor = AssetModelUtil.getAttributeDescriptor(name, this.asset.type!)!;
            this.attribute = {};
            this.attribute.name = descriptor.name;
            this.attribute.type = descriptor.type;
            if (descriptor.meta) {
                this.attribute.meta = JSON.parse(JSON.stringify(descriptor.meta));
            } else {
                this.attribute.meta = {};
            }
        }

        this.dispatchEvent(new OrAddAttributePanelAttributeChangedEvent(this.attribute));
    };

    protected onNameChanged(name: string) {
        this.attribute.name = name;
        this.dispatchEvent(new OrAddAttributePanelAttributeChangedEvent(this.attribute));
    };

    protected onValueTypeChanged(valueType: string) {
        this.attribute.type = valueType;
        this.updateAttributeType();
    };

    protected onArrayChanged(array: boolean, dimensions: number) {
        this.isArray = array;
        this.arrayDimensions = dimensions;
        this.updateAttributeType();
    }

    protected updateAttributeType() {
        if (!this.attribute || !this.attribute.type) {
            return;
        }

        let type = this.attribute.type.replace(this.arrayRegex, "");
        if (this.isArray) {
            for (let i=1; i<=this.arrayDimensions; i++) {
                type += "[]";
            }
        }
        this.attribute.type = type;
        this.dispatchEvent(new OrAddAttributePanelAttributeChangedEvent(this.attribute));
    }
}
