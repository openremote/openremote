import {RuleActionWebhook} from "@openremote/model";
import {InputType, OrInputChangedEvent} from "@openremote/or-mwc-components/or-mwc-input";
import {DialogAction, OrMwcDialog} from "@openremote/or-mwc-components/or-mwc-dialog";
import {i18next} from "@openremote/or-translate";
import {html, LitElement, PropertyValues} from "lit";
import {customElement, property} from "lit/decorators.js";

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

    closeForm(event: OrInputChangedEvent) {
        const dialog: OrMwcDialog = this.shadowRoot!.host as OrMwcDialog;
        dialog.close();
    }

    render() {
        if (!this.action) {
            return html`${i18next.t('errorOccurred')}`;
        }
        const webhookModalActions: DialogAction[] = [
            {
                actionName: "", content: html`
                    <or-mwc-input .type="${InputType.BUTTON}" label="ok"
                                  @or-mwc-input-changed="${this.closeForm}"></or-mwc-input>`
            }
        ];
        const webhookModalOpen = () => {
            const dialog: OrMwcDialog = this.shadowRoot!.getElementById("webhook-modal") as OrMwcDialog;
            if (dialog) {
                dialog.open();
            }
        };
        return html`
            <or-mwc-input type="${InputType.BUTTON}" label="message"
                          @or-mwc-input-changed="${webhookModalOpen}"></or-mwc-input>
            <or-mwc-dialog id="webhook-modal" heading="${this.title}" .actions="${webhookModalActions}"></or-mwc-dialog>
            <slot class="webhook-form-slot"></slot>
        `
    }

}
