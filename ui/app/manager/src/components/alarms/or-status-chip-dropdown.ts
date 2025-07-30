import { css, html, LitElement } from "lit";
import { customElement, property } from "lit/decorators.js";
import { classMap } from "lit/directives/class-map.js";
import { i18next } from "@openremote/or-translate";
import { AlarmStatus } from "@openremote/model";
import { InputType, OrInputChangedEvent } from "@openremote/or-mwc-components/or-mwc-input";

import "@openremote/or-mwc-components/or-mwc-input";

/**
 * Interface describing the detail object passed with the custom event.
 */
export interface StatusChangeDetail {
    status: AlarmStatus;
}

/**
 * Custom event fired when the alarm status is changed by the user.
 *
 * Event name: `status-change`
 * Bubbles: true
 * Composed: true
 */
export class StatusChangeEvent extends CustomEvent<StatusChangeDetail> {
    constructor(detail: StatusChangeDetail) {
        super('status-change', {
            detail,
            bubbles: true,
            composed: true
        });
    }
}

/**
 * Custom dropdown component for selecting an alarm status.
 *
 * Displays a dropdown using `or-mwc-input` with predefined `AlarmStatus` values
 * translated via i18next, and dispatches a `StatusChangeEvent` when a new status is selected.
 */
@customElement("or-status-chip-dropdown")
export class OrStatusChipDropdown extends LitElement {

    /**
     * Currently selected alarm status.
     */
    @property({ type: String }) status?: AlarmStatus;

    /**
     * Ordered list of possible alarm statuses for the dropdown.
     */
    private readonly statusOrder: AlarmStatus[] = [
        AlarmStatus.OPEN,
        AlarmStatus.ACKNOWLEDGED,
        AlarmStatus.IN_PROGRESS,
        AlarmStatus.RESOLVED,
        AlarmStatus.CLOSED
    ];

    static styles = css`
    `;

    /**
     * Renders the dropdown using the `or-mwc-input` component.
     */
    render() {
        return html`
            <or-mwc-input
                style="width: 200px;"
                .type="${InputType.SELECT}"
                .value="${i18next.t(this._getTranslatedStatus(this.status))}"
                .options="${this.statusOrder.map(s => i18next.t(this._getTranslatedStatus(s)))}"
                @click=${(e: Event) => e.stopPropagation()}
                @or-mwc-input-changed="${this._onStatusChanged}"
            ></or-mwc-input>
        `;
    }

    /**
     * Handler for the `or-mwc-input-changed` event.
     *
     * Compares the selected translated value to known statuses and fires a `StatusChangeEvent`
     * if a different status was selected.
     */
    private _onStatusChanged(e: OrInputChangedEvent) {
        const selectedStatus = this.statusOrder.find(s =>
            i18next.t(this._getTranslatedStatus(s)) === e.detail.value
        );

        if (selectedStatus && selectedStatus !== this.status) {
            this.dispatchEvent(new StatusChangeEvent({ status: selectedStatus }));
        }
    }

    /**
     * Returns the translation key string for a given `AlarmStatus`.
     *
     * @param status The alarm status
     * @returns A translation key string like `alarm.status_OPEN` or `"error"` if undefined
     */
    private _getTranslatedStatus(status?: AlarmStatus): string {
        return status ? `alarm.status_${status}` : "error";
    }

}
