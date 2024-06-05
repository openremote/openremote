import {html, LitElement, PropertyValues} from "lit";
import {customElement, property} from "lit/decorators.js";
import {
    RuleActionAlarm,
    AssetQuery,
    AlarmSeverity
} from "@openremote/model";

import "@openremote/or-mwc-components/or-mwc-input";
import {InputType, OrInputChangedEvent} from "@openremote/or-mwc-components/or-mwc-input";
import i18next from "i18next";
import {translate} from "@openremote/or-translate";

import {DialogAction, OrMwcDialog, OrMwcDialogOpenedEvent} from "@openremote/or-mwc-components/or-mwc-dialog";
import {OrRulesJsonRuleChangedEvent} from "../or-rule-json-viewer";
const checkValidity = (form:HTMLElement | null, dialog:OrMwcDialog) => {
    if(form) {
        const inputs = form.querySelectorAll('or-mwc-input');
        const elements = Array.prototype.slice.call(inputs);

        const valid = elements.every((element) => {
            if(element.shadowRoot) {
                const input  = element.shadowRoot.querySelector('input, textarea') as any
                
                if(element.type === InputType.SELECT) {
                    return true;
                }

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
@customElement("or-rule-alarm-modal")
export class OrRuleAlarmModal extends translate(i18next)(LitElement) {

    @property({type: Object, attribute: false})
    public action!: RuleActionAlarm;

    @property({type: String})
    public title = "settings";

    @property({type: Object})
    public query?: AssetQuery;

    constructor() {
        super();
        this.addEventListener(OrMwcDialogOpenedEvent.NAME, this.initDialog)
    }

    initDialog() {
        const modal = this.shadowRoot!.getElementById('alarm-modal');
        if(!modal) return;
    }

    renderDialogHTML(action:RuleActionAlarm) {
        const dialog: OrMwcDialog = this.shadowRoot!.getElementById("alarm-modal") as OrMwcDialog;
        if(!this.shadowRoot) return

        const slot:HTMLSlotElement|null = this.shadowRoot.querySelector('.alarm-form-slot');
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
            const alarmConfig = this.shadowRoot.querySelector('or-rule-form-alarm');

            if(alarmConfig && alarmConfig.shadowRoot) {
                const form = alarmConfig.shadowRoot.querySelector('form');
                return checkValidity(form, dialog);
            }
        }
    }

    protected render() {
        if(!this.action) return html``;

        const alarmPickerModalActions: DialogAction[] = [
            {
                actionName: "cancel",
                content: html`<or-mwc-input class="button" .type="${InputType.BUTTON}" .label="${i18next.t("cancel")}"></or-mwc-input>`,
                action: (dialog) => {

                }
            },
            {
                actionName: "",
                content: html`<or-mwc-input class="button" .type="${InputType.BUTTON}" .label="${i18next.t("ok")}" @or-mwc-input-changed="${this.checkForm}"></or-mwc-input>`
            }
        ];


        const alarmPickerModalOpen = () => {
            const dialog: OrMwcDialog = this.shadowRoot!.getElementById("alarm-modal") as OrMwcDialog;
            if (dialog) {
                dialog.open();
            }
        };

        return html`
            <or-mwc-input style="width: 200px" .type="${InputType.SELECT}" .value="${this.action.alarm?.severity}" .label="${i18next.t("alarm.severity")}" .options="${[AlarmSeverity.LOW, AlarmSeverity.MEDIUM, AlarmSeverity.HIGH]}" @or-mwc-input-changed="${(e: OrInputChangedEvent) => this.setActionAlarmSeverity(e.detail.value)}"></or-mwc-input>
            <or-mwc-input .type="${InputType.BUTTON}" .label="${i18next.t("settings")}" @or-mwc-input-changed="${alarmPickerModalOpen}"></or-mwc-input>
            <or-mwc-dialog id="alarm-modal" heading="${this.title}" .actions="${alarmPickerModalActions}"></or-mwc-dialog>
            <slot class="alarm-form-slot"></slot>
        `
    }

    protected setActionAlarmSeverity(value: string | undefined) {
        if(value && this.action.alarm){
            const alarm:any = this.action.alarm;
            alarm.severity = value;
            this.action.alarm = {...alarm};
        }

        this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
        this.requestUpdate();
    }
}
