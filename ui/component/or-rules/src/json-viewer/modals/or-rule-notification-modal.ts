import {customElement, html, LitElement, property, PropertyValues} from "lit-element";
import {
    RuleActionNotification,
    AssetQuery,
} from "@openremote/model";

import "@openremote/or-input";
import {InputType} from "@openremote/or-input";
import i18next from "i18next";
import {translate} from "@openremote/or-translate";

import {DialogAction, OrMwcDialog, OrMwcDialogOpenedEvent} from "@openremote/or-mwc-components/dist/or-mwc-dialog";

@customElement("or-rule-notification-modal")
export class OrRuleNotificationModal extends translate(i18next)(LitElement) {

    @property({type: Object, attribute: false})
    public action!: RuleActionNotification;

    @property({type: String})
    public title = "message";
    
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
        if(!this.shadowRoot) return

        const slot:HTMLSlotElement|null = this.shadowRoot.querySelector('.notification-form-slot');
        if (dialog && slot) {
            let container = document.createElement("div");
            slot.assignedNodes({flatten: true}).forEach((child) => {
                if (child instanceof HTMLElement) {
                    container.appendChild(child);
                }
            });
            dialog.dialogContent = html`${container}`;
            this.requestUpdate();
        }
    }

    firstUpdated(changedProperties: PropertyValues){
        if(changedProperties.has("action")){
            this.renderDialogHTML(this.action);
        }
    }

    protected render() {
        if(!this.action) return html``;
        
        const notificationPickerModalActions: DialogAction[] = [
            {
                actionName: "cancel",
                content: html`<or-input class="button" .type="${InputType.BUTTON}" .label="${i18next.t("cancel")}"></or-input>`,
                action: () => {
                    // Nothing to do here
                }
            },
            {
                actionName: "ok",
                content: html`<or-input class="button" .type="${InputType.BUTTON}" .label="${i18next.t("ok")}"></or-input>`,
                action: () => {
                }
            }
        ];
       
      
        const notificationPickerModalOpen = () => {
            const dialog: OrMwcDialog = this.shadowRoot!.getElementById("notification-modal") as OrMwcDialog;
            if (dialog) {
                dialog.open();
            }
        };

        return html`
            <or-input .type="${InputType.BUTTON}" .label="${i18next.t("message")}" @click="${notificationPickerModalOpen}"></or-input>
            <or-mwc-dialog id="notification-modal" dialogTitle="${this.title}" .dialogActions="${notificationPickerModalActions}"></or-mwc-dialog>
            <slot class="notification-form-slot"></slot>
        `
    }
}