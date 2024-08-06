var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { html, LitElement } from "lit";
import { customElement, property } from "lit/decorators.js";
import { getAssetTypeFromQuery, } from "../../index";
import "@openremote/or-mwc-components/or-mwc-input";
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";
import i18next from "i18next";
import { OrRulesJsonRuleChangedEvent } from "../or-rule-json-viewer";
import { translate } from "@openremote/or-translate";
import { OrMwcDialogOpenedEvent } from "@openremote/or-mwc-components/or-mwc-dialog";
import { OrMapClickedEvent } from "@openremote/or-map";
import "@openremote/or-map";
let OrRuleRadialModal = class OrRuleRadialModal extends translate(i18next)(LitElement) {
    constructor() {
        super();
        this.addEventListener(OrMwcDialogOpenedEvent.NAME, this.initRadialMap);
    }
    initRadialMap() {
        const modal = this.shadowRoot.getElementById('radial-modal');
        if (!modal)
            return;
        const map = modal.shadowRoot.querySelector('.or-map');
        if (map) {
            map.addEventListener(OrMapClickedEvent.NAME, (evt) => {
                const lngLat = evt.detail.lngLat;
                const latElement = modal.shadowRoot.querySelector('.location-lat');
                const lngElement = modal.shadowRoot.querySelector('.location-lng');
                latElement.value = lngLat.lat;
                lngElement.value = lngLat.lng;
                const event = new Event('change');
                latElement.dispatchEvent(event);
                lngElement.dispatchEvent(event);
                this.setValuePredicateProperty('lat', lngLat.lat);
                this.setValuePredicateProperty('lng', lngLat.lng);
            });
            const latElement = modal.shadowRoot.querySelector('.location-lat');
            const lngElement = modal.shadowRoot.querySelector('.location-lng');
            if (lngElement.value && latElement.value) {
                const LngLat = [parseFloat(lngElement.value), parseFloat(latElement.value)];
                map.flyTo(LngLat, 15);
            }
            else {
                map.flyTo();
            }
        }
    }
    getAttributeName(attributePredicate) {
        return attributePredicate && attributePredicate.name ? attributePredicate.name.value : undefined;
    }
    setValuePredicateProperty(propertyName, value) {
        if (!this.attributePredicate)
            return;
        if (!this.attributePredicate.value)
            return;
        const valuePredicate = this.attributePredicate.value;
        valuePredicate[propertyName] = value;
        this.attributePredicate = Object.assign({}, this.attributePredicate);
        this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
        this.requestUpdate();
    }
    renderDialogHTML(value) {
        const dialog = this.shadowRoot.getElementById("radial-modal");
        if (dialog) {
            dialog.content = html `
                <div style="display:grid">
                    <or-map class="or-map" type="VECTOR" style="border: 1px solid #d5d5d5; height: 400px; min-width: 300px; margin-bottom: 20px;">
                        <or-map-marker active color="#FF0000" icon="information" lat="${value.lat}" lng="${value.lng}" radius="${value.radius}"></or-map-marker>
                    </or-map>
                
                    <div class="layout horizontal">
                        <input hidden class="location-lng"  required placeholder=" " type="text" .value="${value && value.lng ? value.lng : null}" />
                        <input hidden class="location-lat" required placeholder=" " type="text" .value="${value && value.lat ? value.lat : null}" />
                    </div>

                    <label>${i18next.t("radiusMin")}</label>
                    <input @change="${(e) => this.setValuePredicateProperty("radius", parseInt(e.target.value))}" style="max-width: calc(50% - 30px);" required placeholder=" " min="100" type="number" .value="${value && value.radius ? value.radius : 100}" />
                </div>`;
        }
    }
    render() {
        if (!this.attributePredicate)
            return html ``;
        if (!this.query)
            return html ``;
        const valuePredicate = this.attributePredicate.value;
        if (!this.assetDescriptor || !valuePredicate) {
            return html ``;
        }
        const attributeName = this.getAttributeName(this.attributePredicate);
        const assetType = getAssetTypeFromQuery(this.query);
        // @ts-ignore
        const value = valuePredicate ? valuePredicate : undefined;
        const radiusPickerModalActions = [
            {
                actionName: "cancel",
                content: html `<or-mwc-input class="button" .type="${InputType.BUTTON}" label="cancel"></or-mwc-input>`,
                action: () => {
                    // Nothing to do here
                }
            },
            {
                actionName: "ok",
                default: true,
                content: html `<or-mwc-input class="button" .type="${InputType.BUTTON}" label="ok"></or-mwc-input>`,
                action: () => {
                }
            }
        ];
        const radialPickerModalOpen = () => {
            const dialog = this.shadowRoot.getElementById("radial-modal");
            if (dialog) {
                dialog.dismissAction = null;
                dialog.open();
                this.renderDialogHTML(value);
            }
        };
        this.renderDialogHTML(value);
        return html `
            <or-mwc-input .type="${InputType.BUTTON}" label="area" @or-mwc-input-changed="${radialPickerModalOpen}"></or-mwc-input>
            <or-mwc-dialog id="radial-modal" heading="area" .actions="${radiusPickerModalActions}"></or-mwc-dialog>
        `;
    }
};
__decorate([
    property({ type: Object })
], OrRuleRadialModal.prototype, "assetDescriptor", void 0);
__decorate([
    property({ type: Object })
], OrRuleRadialModal.prototype, "attributePredicate", void 0);
__decorate([
    property({ type: Object })
], OrRuleRadialModal.prototype, "query", void 0);
OrRuleRadialModal = __decorate([
    customElement("or-rule-radial-modal")
], OrRuleRadialModal);
export { OrRuleRadialModal };
//# sourceMappingURL=or-rule-radial-modal.js.map