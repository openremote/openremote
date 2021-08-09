import { PopupModal } from "../components/popup-modal";
import { html, TemplateResult } from "lit";
import { i18next } from "@openremote/or-translate";

export class ModalService {
    public element!: PopupModal;

    public confirmation(agreeCallback: () => void, header: string = i18next.t("flowEditor", "Flow editor"), question: string = i18next.t("areYouSure", "Are you sure?")) {
        this.element.content = html`<confirmation-dialog 
        .question = "${question}"
        @agreed="${() => { if (agreeCallback) { agreeCallback(); } this.element.close(); }}"
        @disagreed="${this.element.close}"
        ></confirmation-dialog>`;
        this.element.header = header;
        this.element.open();
    }

    public notification(header: string, message: string, buttonText: string = i18next.t("ok", "OK")) {
        this.element.content = html`<notification-dialog 
        .message = "${message}"
        @closed = "${this.element.close}"
        ></notification-dialog>`;
        this.element.header = header;
        this.element.open();
    }

    public anything(header: string, content: TemplateResult){
        this.element.content = content;
        this.element.header = header;
        this.element.open();
    }
}
