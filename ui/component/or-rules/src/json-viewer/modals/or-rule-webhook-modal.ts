import {RuleActionWebhook} from "@openremote/model";
import {InputType, OrInputChangedEvent, OrMwcInput} from "@openremote/or-mwc-components/or-mwc-input";
import {DialogAction, OrMwcDialog, showDialog} from "@openremote/or-mwc-components/or-mwc-dialog";
import {i18next} from "@openremote/or-translate";
import {html, LitElement, PropertyValues} from "lit";
import {customElement, property} from "lit/decorators.js";

const checkValidity = (form: HTMLFormElement | null, dialog: OrMwcDialog) => {
    if (form) {
        const inputs = form.querySelectorAll('or-mwc-input');
        const elements = Array.prototype.slice.call(inputs);

        const valid = elements.every((element: OrMwcInput) => {
            const mdcComponent = (element as any)._mdcComponent;
            if (element.shadowRoot) {
                if (element.type == InputType.SELECT) {
                    return element.checkValidity();
                }
                if (element.type == InputType.BUTTON) {
                    return true;
                }
                const input = element.shadowRoot.querySelector('input, textarea, select') as any

                if (input && input.checkValidity()) {
                    return true
                } else {
                    mdcComponent.valid = false;
                    mdcComponent.helperTextContent = 'required';
                    return false;
                }
            } else {
                return false;
            }
        })
        if (valid) {
            dialog.close();
        }
    }
}

@customElement("or-rule-webhook-modal")
export class OrRuleWebhookModal extends LitElement {

    @property({type: Object})
    protected action!: RuleActionWebhook;

    @property({type: String})
    public title: string = i18next.t('message');

    constructor() {
        super();
    }

    /* ----------------------- */

    firstUpdated(changedProperties: PropertyValues) {
        if (changedProperties.has("action")) {
            this.renderDialogHTML(this.action);
        }
    }

    renderDialogHTML(action: RuleActionWebhook) {
        const dialog: OrMwcDialog = this.shadowRoot!.getElementById("webhook-modal") as OrMwcDialog;
        if (!this.shadowRoot) return

        const slot: HTMLSlotElement | null = this.shadowRoot.querySelector('.webhook-form-slot');
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

    checkForm(event: OrInputChangedEvent) {
        const dialog: OrMwcDialog = this.shadowRoot!.host as OrMwcDialog;
        const component: HTMLElement | null = this.shadowRoot!.querySelector('or-rule-form-webhook');
        const form: HTMLFormElement = component?.shadowRoot!.firstElementChild as HTMLFormElement;
        checkValidity(form, dialog);
        this.requestUpdate();
    }

    render() {
        if (!this.action) {
            return html`${i18next.t('errorOccurred')}`;
        }
        const webhookModalActions: DialogAction[] = [
            {
                actionName: "", content: html`
                    <or-mwc-input .type="${InputType.BUTTON}" .label="${i18next.t("ok")}"
                                  @or-mwc-input-changed="${this.checkForm}"></or-mwc-input>`
            }
        ];
        const webhookModalOpen = () => {
            const dialog: OrMwcDialog = this.shadowRoot!.getElementById("webhook-modal") as OrMwcDialog;
            if (dialog) {
                dialog.open();
            }
        };
        return html`
            <or-mwc-input type="${InputType.BUTTON}" label="${i18next.t('message')}"
                          @or-mwc-input-changed="${webhookModalOpen}"></or-mwc-input>
            <or-mwc-dialog id="webhook-modal" heading="${this.title}" .actions="${webhookModalActions}"></or-mwc-dialog>
            <slot class="webhook-form-slot"></slot>
        `
    }

}
