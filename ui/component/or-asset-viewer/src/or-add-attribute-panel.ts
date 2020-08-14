import {customElement, html, LitElement, property, css} from "lit-element";
import {InputType, OrInput, OrInputChangedEvent} from "@openremote/or-input";
import i18next from "i18next";
import {Asset, Attribute, AttributeValueDescriptor} from "@openremote/model";
import {AssetModelUtil, Util} from "@openremote/core";
import "@openremote/or-input";

export class OrAddAttributePanelAttributeChangedEvent extends CustomEvent<Attribute> {

    public static readonly NAME = "or-add-attribute-panel-attribute-changed";

    constructor(attribute: Attribute) {
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
    protected attribute: Attribute = {};

    public static get styles() {
        return css`                        
            #attribute-creator > * {
                padding: 20px;                        
            }

            #attribute-creator > * or-input {
                width: 300px;
            }

            .hidden {
                visibility: hidden;
            }`;
    }

    protected render() {

        let attributeTypes: [string, string][] = AssetModelUtil.getAttributeDescriptors()
            .filter((descriptor) => !this.asset.attributes![descriptor.attributeName!])
            .sort(Util.sortByString((descriptor) => descriptor.attributeName!))
            .map((descriptor) => {
                return [
                    descriptor.attributeName!,
                    Util.getAttributeLabel(undefined, descriptor, undefined, false)
                ]
            });
        attributeTypes = [["@custom", i18next.t("custom")], ...attributeTypes];

        const attributeValueTypes: [string, string][] = AssetModelUtil.getAttributeValueDescriptors()
            .sort(Util.sortByString((descriptor) => descriptor.name!))
            .map((descriptor) => {
                return [descriptor.name!, descriptor.name!];
            });

        return html`
            <div id="attribute-creator">
                <div>
                    <or-input .type="${InputType.SELECT}" .options="${attributeTypes}" .label="${i18next.t("type")}" @or-input-changed="${(ev: OrInputChangedEvent) => this.onTypeChanged(ev.detail.value)}"></or-input>
                </div>
                <div id="name-input" class="hidden">
                    <or-input .type="${InputType.TEXT}" .label="${i18next.t("name")}" pattern="\\w+" required @keyup="${(ev: KeyboardEvent) => this.onNameChanged((ev.target as OrInput).currentValue)}"  @or-input-changed="${(ev: OrInputChangedEvent) => this.onNameChanged(ev.detail.value)}"></or-input>
                </div>
                <div id="type-input" class="hidden">
                    <or-input .type="${InputType.SELECT}" .options="${attributeValueTypes}" .label="${i18next.t("valueType")}" @or-input-changed="${(ev: OrInputChangedEvent) => this.onValueTypeChanged(ev.detail.value)}"></or-input>
                </div>
            </div>
        `;
    }

    protected onTypeChanged(name: string) {
        const nameInput = this.shadowRoot!.getElementById("name-input");
        const typeInput = this.shadowRoot!.getElementById("type-input");

        if (name === "@custom") {
            nameInput!.classList.remove("hidden");
            typeInput!.classList.remove("hidden");
            this.attribute = {
                meta: []
            };
        } else {
            nameInput!.classList.add("hidden");
            typeInput!.classList.add("hidden");
            const descriptor = AssetModelUtil.getAttributeDescriptor(name)!;
            this.attribute = {};
            this.attribute.name = descriptor.attributeName;
            this.attribute.value = descriptor.initialValue;
            this.attribute.type = descriptor.valueDescriptor!.name as AttributeValueDescriptor; // TODO: Fix once asset model works properly
            if (descriptor.metaItemDescriptors) {
                this.attribute.meta = descriptor.metaItemDescriptors.map((descriptor) => {
                    return {
                        name: descriptor.urn,
                        value: descriptor.initialValue
                    }
                });
            } else {
                this.attribute.meta = [];
            }
        }

        this.dispatchEvent(new OrAddAttributePanelAttributeChangedEvent(this.attribute));
    };

    protected onNameChanged(name: string) {
        this.attribute.name = name;
        this.dispatchEvent(new OrAddAttributePanelAttributeChangedEvent(this.attribute));
    };

    protected onValueTypeChanged(valueType: string) {
        // @ts-ignore
        this.attribute.type = valueType;
        this.dispatchEvent(new OrAddAttributePanelAttributeChangedEvent(this.attribute));
    };
}
