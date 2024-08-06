import { html } from "lit";
import { i18next } from "@openremote/or-translate";
export class ModalService {
    confirmation(agreeCallback, header = i18next.t("flowEditor", "Flow editor"), question = i18next.t("areYouSure", "Are you sure?")) {
        this.element.content = html `<confirmation-dialog 
        .question = "${question}"
        @agreed="${() => { if (agreeCallback) {
            agreeCallback();
        } this.element.close(); }}"
        @disagreed="${this.element.close}"
        ></confirmation-dialog>`;
        this.element.header = header;
        this.element.open();
    }
    notification(header, message, buttonText = i18next.t("ok", "OK")) {
        this.element.content = html `<notification-dialog 
        .message = "${message}"
        @closed = "${this.element.close}"
        ></notification-dialog>`;
        this.element.header = header;
        this.element.open();
    }
    anything(header, content) {
        this.element.content = content;
        this.element.header = header;
        this.element.open();
    }
}
//# sourceMappingURL=modal.js.map