/*
 * Copyright 2026, OpenRemote Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
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
