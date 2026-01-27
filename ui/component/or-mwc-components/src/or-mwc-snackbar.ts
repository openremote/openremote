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
import {css, html, LitElement, PropertyValues, unsafeCSS} from "lit";
import {customElement, property, query} from "lit/decorators.js";
import {MDCSnackbar, MDCSnackbarCloseEvent} from "@material/snackbar";

const drawerStyle = require("@material/snackbar/dist/mdc.snackbar.css");

export interface OrMwcSnackbarChangedEventDetail {
    opened: boolean,
    closeReason?: string
}

export class OrMwcSnackbarChangedEvent extends CustomEvent<OrMwcSnackbarChangedEventDetail> {

    public static readonly NAME = "or-mwc-snackbar-changed";

    constructor(value: OrMwcSnackbarChangedEventDetail) {
        super(OrMwcSnackbarChangedEvent.NAME, {
            detail: value,
            bubbles: true,
            composed: true
        });
    }
}

declare global {
    export interface HTMLElementEventMap {
        [OrMwcSnackbarChangedEvent.NAME]: OrMwcSnackbarChangedEvent;
    }
}

export function showSnackbar(hostElement: HTMLElement | undefined, text: string, buttonText?: string, buttonAction?: () => void): OrMwcSnackbar {
    if (!hostElement) {
        hostElement = OrMwcSnackbar.DialogHostElement || document.body;
    }

    const snackbar = new OrMwcSnackbar();
    snackbar.text = text;
    snackbar.buttonText = buttonText;
    snackbar.buttonAction = buttonAction;
    snackbar.isOpen = true;
    snackbar.addEventListener(OrMwcSnackbarChangedEvent.NAME, (ev: OrMwcSnackbarChangedEvent) => {
        ev.stopPropagation();
        if (!ev.detail.opened) {
            window.setTimeout(() => {
                if (snackbar.parentElement) {
                    snackbar.parentElement.removeChild(snackbar);
                }
            }, 0);
        }
    });
    hostElement.append(snackbar);
    return snackbar;
}

@customElement("or-mwc-snackbar")
export class OrMwcSnackbar extends LitElement {

    /**
     * Can be set by apps to control where in the DOM snackbars are added
     */
    public static DialogHostElement: HTMLElement;

    public static get styles() {
        return [
            css`${unsafeCSS(drawerStyle)}`,
            css`
      `
        ];
    }

    @property({type: String, attribute: false})
    public text!: string;

    @property({type: String})
    public buttonText?: string;

    @property({type: Object, attribute: false})
    public buttonAction?: () => void;

    @property({type: Number})
    public timeout?: number;

    @property({type: Boolean})
    public _open: boolean = false;

    @query("#mdc-snackbar")
    protected _mdcElem!: HTMLElement;

    protected _mdcComponent?: MDCSnackbar;

    public get isOpen() {
        return this._mdcComponent ? this._mdcComponent.isOpen : false;
    }

    public set isOpen(isOpen: boolean) {
        this._open = true;
    }

    public open() {
        if (this._mdcElem && !this._mdcComponent) {
            this._mdcComponent = new MDCSnackbar(this._mdcElem);
            this._mdcComponent.timeoutMs = this.timeout || 4000;
        }
        if (this._mdcComponent) {
            this._mdcComponent.open();
        }
    }

    public close(action?: string) {
        if (this._mdcComponent) {
            this._mdcComponent.close(action);
        }
    }

    public disconnectedCallback(): void {
        super.disconnectedCallback();
        if (this._mdcComponent) {
            this._mdcComponent.destroy();
            this._mdcComponent = undefined;
        }
    }

    protected render() {
        return html`
            <div id="mdc-snackbar" class="mdc-snackbar" @MDCSnackbar:opened="${() => this.onOpen()}"
                 @MDCSnackbar:closed="${(ev: MDCSnackbarCloseEvent) => this.onClose(ev.detail.reason)}">
                <div class="mdc-snackbar__surface" role="status" aria-relevant="additions">
                    <div class="mdc-snackbar__label" aria-atomic="false">
                        <or-translate value="${this.text}"></or-translate>
                    </div>
                    ${!this.buttonText ? html`` : html`
                        <div class="mdc-snackbar__actions" aria-atomic="true">
                            <or-mwc-input type="button" class="mdc-button mdc-snackbar__action" label="${this.buttonText}">                                
                            </or-mwc-input>
                        </div>
                    `}
                </div>
            </div>
        `;
    }

    protected updated(_changedProperties: PropertyValues) {
        super.updated(_changedProperties);
        if (_changedProperties.has("_open") && this._open) {
            this.open();
        }
    }

    protected onClose(reason: string | undefined) {
        if (this.buttonAction) {
            this.buttonAction();
        }
        this.dispatchChangedEvent({opened: false, closeReason: reason});
    }

    protected onOpen() {
        this.dispatchChangedEvent({opened: true});
    }

    protected dispatchChangedEvent(detail: OrMwcSnackbarChangedEventDetail) {
        this.dispatchEvent(new OrMwcSnackbarChangedEvent(detail));
    }
}
