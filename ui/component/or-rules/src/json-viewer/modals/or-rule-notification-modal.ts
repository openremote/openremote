import {css, customElement, html, LitElement, property, PropertyValues} from "lit-element";
import {
    Asset,
    AssetDescriptor,
    AssetQueryMatch,
    AssetQueryOperator as AQO,
    RuleActionNotification,
    AssetQuery,
    RadialGeofencePredicate
} from "@openremote/model";
import {
    AssetQueryOperator,
    AssetTypeAttributeName,
    getAssetIdsFromQuery,
    getAssetTypeFromQuery,
    RulesConfig
} from "../../index";
import {OrSelectChangedEvent} from "@openremote/or-select";
import "@openremote/or-input";
import {InputType, OrInputChangedEvent} from "@openremote/or-input";
import {getAttributeValueTemplate} from "@openremote/or-attribute-input";
import manager, {AssetModelUtil, Util} from "@openremote/core";
import i18next from "i18next";
import {OrRulesJsonRuleChangedEvent} from "../or-rule-json-viewer";
import {translate} from "@openremote/or-translate";

import {DialogAction, OrMwcDialog, OrMwcDialogOpenedEvent} from "@openremote/or-mwc-components/dist/or-mwc-dialog";
import {OrMap, OrMapClickedEvent} from '@openremote/or-map';
import '@openremote/or-map/dist/markers/or-map-marker';

@customElement("or-rule-notification-modal")
export class OrRuleNotificationModal extends translate(i18next)(LitElement) {

    @property({type: Object, attribute: false})
    public action!: RuleActionNotification;


    @property({type: Object})
    public query?: AssetQuery;
    
    constructor() {
        super();
        this.addEventListener(OrMwcDialogOpenedEvent.NAME, this.initDialog)
    }

    initDialog() {
        const modal = this.shadowRoot!.getElementById('notification-modal');
        if(!modal) return;
    }

    renderDialogHTML(action:RuleActionNotification) {
        const dialog: OrMwcDialog = this.shadowRoot!.getElementById("notification-modal") as OrMwcDialog;
        const slot = this.shadowRoot?.getElementById('notification-form-slot');
        if (dialog) {
            dialog.dialogContent = html`${slot}`;
        }
    }

    protected render() {
        if(!this.action) return html``;


        const notificationPickerModalActions: DialogAction[] = [
            {
                actionName: "ok",
                default: true,
                content: html`<or-input class="button" .type="${InputType.BUTTON}" .label="${i18next.t("ok")}"></or-input>`,
                action: () => {
                }
            },
            {
                actionName: "cancel",
                content: html`<or-input class="button" .type="${InputType.BUTTON}" .label="${i18next.t("cancel")}"></or-input>`,
                action: () => {
                    // Nothing to do here
                }
            },
        ];
       
      
        const notificationPickerModalOpen = () => {
            const dialog: OrMwcDialog = this.shadowRoot!.getElementById("notification-modal") as OrMwcDialog;
            if (dialog) {
                dialog.open();
            }
        };

        return html`
            <or-input .type="${InputType.BUTTON}" .label="${i18next.t("message")}" @click="${notificationPickerModalOpen}"></or-input>
            <or-mwc-dialog id="notification-modal" dialogTitle="message" .dialogActions="${notificationPickerModalActions}">
                <slot id="notification-form-slot"></slot>
            </or-mwc-dialog>
        `
    }
}