import {html, LitElement, PropertyValues} from "lit";
import {customElement, property} from "lit/decorators.js";
import {
    RuleActionNotification,
    AssetQuery,
} from "@openremote/model";

import "@openremote/or-mwc-components/or-mwc-input";
import {InputType} from "@openremote/or-mwc-components/or-mwc-input";
import i18next from "i18next";
import {translate} from "@openremote/or-translate";

import {DialogAction, OrMwcDialog, OrMwcDialogOpenedEvent} from "@openremote/or-mwc-components/or-mwc-dialog";
const checkValidity = (form:HTMLElement | null, dialog:OrMwcDialog) => {
    if(form) {
        const inputs = form.querySelectorAll('or-mwc-input');
        const elements = Array.prototype.slice.call(inputs);

        const valid = elements.every((element) => {
            if(element.shadowRoot) {
                const input  = element.shadowRoot.querySelector('input, textarea, select') as any

                if(input && input.checkValidity()) {
                    return true
                } else {
                    element._mdcComponent.valid = false;
                    element._mdcComponent.helperTextContent = 'required';

                    return false;
                }
            } else {
                return false;
            }
        })
        if(valid) {
            dialog.close();
        }
    }
}
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
            dialog.content = html`${container}`;
            dialog.dismissAction = null;
            this.requestUpdate();
        }
    }

    firstUpdated(changedProperties: PropertyValues){
        if(changedProperties.has("action")){
            this.renderDialogHTML(this.action);
        }
    }

    checkForm() {
        const dialog: OrMwcDialog = this.shadowRoot!.host as OrMwcDialog;

        if (this.shadowRoot) {
            const messageNotification = this.shadowRoot.querySelector('or-rule-form-email-message');
            const pushNotification = this.shadowRoot.querySelector('or-rule-form-push-notification');

            if(pushNotification && pushNotification.shadowRoot) {
                const form = pushNotification.shadowRoot.querySelector('form');
                return checkValidity(form, dialog);
            }
            else if(messageNotification && messageNotification.shadowRoot) {
                const form = messageNotification.shadowRoot.querySelector('form');
                return checkValidity(form, dialog);
            }
        }
    }

    protected render() {
        if(!this.action) return html``;
        
        const notificationPickerModalActions: DialogAction[] = [
            {
                actionName: "cancel",
                content: html`<or-mwc-input class="button" .type="${InputType.BUTTON}" label="cancel"></or-mwc-input>`,
                action: (dialog) => {
                  
                }
            },
            {
                actionName: "",
                content: html`<or-mwc-input class="button" .type="${InputType.BUTTON}" label="ok" @or-mwc-input-changed="${this.checkForm}"></or-mwc-input>`
            }
        ];
       
      
        const notificationPickerModalOpen = () => {
            const dialog: OrMwcDialog = this.shadowRoot!.getElementById("notification-modal") as OrMwcDialog;
            if (dialog) {
                dialog.open();
            }
        };

        return html`
            <or-mwc-input .type="${InputType.BUTTON}" label="message" @or-mwc-input-changed="${notificationPickerModalOpen}"></or-mwc-input>
            <or-mwc-dialog id="notification-modal" heading="${this.title}" .actions="${notificationPickerModalActions}"></or-mwc-dialog>
            <slot class="notification-form-slot"></slot>
        `
    }
}
