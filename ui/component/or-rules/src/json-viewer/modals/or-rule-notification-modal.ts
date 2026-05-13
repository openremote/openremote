import {html, LitElement, PropertyValues} from "lit";
import {customElement, property, query} from "lit/decorators.js";
import {
    RuleActionNotification,
    AssetQuery,
} from "@openremote/model";

import "@openremote/or-mwc-components/or-mwc-input";
import {InputType} from "@openremote/or-mwc-components/or-mwc-input";
import {i18next, translate} from "@openremote/or-translate"

import {DialogAction, DialogActionBase, OrMwcDialog, OrMwcDialogOpenedEvent} from "@openremote/or-mwc-components/or-mwc-dialog";
import {OrRuleFormLocalized} from "../forms/or-rule-form-localized";
import {OrRulesJsonRuleChangedEvent} from "../or-rule-json-viewer";

const checkValidity = (form:HTMLElement | null) => {
    if(form) {
        const inputs = form.querySelectorAll('or-mwc-input');
        const elements = Array.prototype.slice.call(inputs);

        return elements.every((element) => {
            if(element.shadowRoot) {
                const input  = element.shadowRoot.querySelector('input, textarea, select') as any

                if(input && input.checkValidity()) {
                    return true;
                } else {
                    if(element._mdcComponent) {
                        element._mdcComponent.valid = false;
                        element._mdcComponent.helperTextContent = 'required';
                    }

                    return false;
                }
            } else {
                return false;
            }
        });
    }
}

export class OrRulesNotificationModalCancelEvent extends CustomEvent<void> {

    public static readonly NAME = "or-rules-notification-modal-cancel";

    constructor() {
        super(OrRulesNotificationModalCancelEvent.NAME, {
            bubbles: true,
            composed: true
        });
    }
}

export class OrRulesNotificationModalOkEvent extends CustomEvent<void> {

    public static readonly NAME = "or-rules-notification-modal-ok";

    constructor() {
        super(OrRulesNotificationModalOkEvent.NAME, {
            bubbles: true,
            composed: true
        });
    }
}

@customElement("or-rule-notification-modal")
export class OrRuleNotificationModal extends translate(i18next)(LitElement) {

    @property({type: Object})
    public action!: RuleActionNotification;

    @property({type: String})
    public title = "message";

    @property({type: Object})
    public query?: AssetQuery;

    @query("#notification-modal")
    protected _orMwcDialog?: OrMwcDialog;
    
    constructor() {
        super();
        this.addEventListener(OrMwcDialogOpenedEvent.NAME, this.initDialog)
    }

    connectedCallback() {
        this.addEventListener(OrRulesJsonRuleChangedEvent.NAME, this._onJsonRuleChanged);
        return super.connectedCallback();
    }

    disconnectedCallback() {
        this.removeEventListener(OrRulesJsonRuleChangedEvent.NAME, this._onJsonRuleChanged);
        return super.disconnectedCallback();
    }

    protected _onJsonRuleChanged() {
        this.validateForm();
    }

    initDialog() {
        if(!this._orMwcDialog) return;
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
        // Possibly, a render is triggered by renderDialogHTML(), so we await the pending update. (if there is any)
        this.getUpdateComplete().finally(() => {
            this.validateForm();
        });
    }

    validateForm() {
        const valid = this.checkForm();
        this._orMwcDialog?.setActions(this._orMwcDialog?.actions?.map(action => {
            if(action.actionName === "ok") {
                action.disabled = !valid;
            }
            return action;
        }));
    }

    checkForm() {
        if (this.shadowRoot) {

            const dialog = this._orMwcDialog;
            const root = dialog?.shadowRoot;
            if(dialog && root) {

                const messageNotification = root.querySelector("or-rule-form-email-message");
                const pushNotification = root.querySelector("or-rule-form-push-notification");
                const localizedNotification = root.querySelector("or-rule-form-localized");

                if(pushNotification?.shadowRoot) {
                    const form = pushNotification.shadowRoot.querySelector('form');
                    return checkValidity(form);
                }
                else if(messageNotification?.shadowRoot) {
                    const form = messageNotification.shadowRoot.querySelector('form');
                    return checkValidity(form);
                }
                else if(localizedNotification?.shadowRoot) {
                    return (localizedNotification as OrRuleFormLocalized).isValid();
                }
            }
        }
    }

    protected render() {

        const onCancel = () => {
            this.dispatchEvent(new OrRulesNotificationModalCancelEvent());
        };

        const onOk = () => {
            this.dispatchEvent(new OrRulesNotificationModalOkEvent());
        };

        const dismissAction: DialogActionBase = {
            actionName: "cancel",
            action: onCancel
        };

        const actions: DialogAction[] = [
            {
                actionName: "cancel",
                content: html`<or-mwc-input class="button" .type="${InputType.BUTTON}" label="cancel"></or-mwc-input>`,
                action: onCancel
            },
            {
                actionName: "ok",
                content: "ok",
                disabled: true, // (by default, can be changed in checkForm())
                action: onOk
            }
        ];

        const styles = html`
            <style>
                .mdc-dialog__actions {
                    justify-content: space-between !important;
                }
            </style>
        `;
       
      
        const notificationPickerModalOpen = () => {
            const dialog: OrMwcDialog = this.shadowRoot!.getElementById("notification-modal") as OrMwcDialog;
            if (dialog) {
                dialog.open();
            }
        };

        return html`
            <or-mwc-input .type="${InputType.BUTTON}" label="message" @or-mwc-input-changed="${notificationPickerModalOpen}"></or-mwc-input>
            <or-mwc-dialog id="notification-modal" .heading="${this.title}" .dismissAction="${dismissAction}" .actions="${actions}" .styles="${styles}"></or-mwc-dialog>
            <slot class="notification-form-slot"></slot>
        `;
    }
}
