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
import { css, html, LitElement, TemplateResult, unsafeCSS} from "lit";
import {customElement, property, query} from "lit/decorators.js";
import { MDCDrawer } from "@material/drawer";
import { classMap } from "lit/directives/class-map.js";

const drawerStyle = require("@material/drawer/dist/mdc.drawer.css");

export class OrMwcDrawerChangedEvent extends CustomEvent<boolean> {

  public static readonly NAME = "or-mwc-drawer-changed";

  constructor(value: boolean) {
    super(OrMwcDrawerChangedEvent.NAME, {
      detail: value,
      bubbles: true,
      composed: true
    });
  }
}

@customElement("or-mwc-drawer")
export class OrMwcDrawer extends LitElement {

  public static get styles() {
    return [
      css`${unsafeCSS(drawerStyle)}`,
      css`
      .transparent{
        background: none;
      }`
    ];
  }

  @property({ attribute: false }) public header!: TemplateResult;

  @property({ type: Boolean }) public dismissible: boolean = false;
  @property({ type: Boolean }) public rightSided: boolean = false;
  @property({ type: Boolean }) public transparent: boolean = false;
  @property({ type: Boolean }) public open: boolean = false;

  @property({ attribute: false }) public appContent!: HTMLElement;
  @property({ attribute: false }) public topBar!: HTMLElement;

  @query(".mdc-drawer")
  protected drawerElement!: HTMLElement;

  protected drawer?: MDCDrawer;

  public toggle() {
    this.open = !this.open;
  }

  public disconnectedCallback(): void {
    super.disconnectedCallback();
    if (this.drawer) {
      this.drawer.destroy();
      this.drawer = undefined;
    }
  }

  protected render() {
    const isModal = !this.dismissible;
    const classes = {
      "mdc-drawer--dismissible": this.dismissible,
      "mdc-drawer--modal": isModal,
      "transparent": this.transparent
    };

    return html`
      <aside class="mdc-drawer ${classMap(classes)}" dir="${(this.rightSided ? "rtl" : "ltr")}">
        ${this.header}
        <div class="mdc-drawer__content" dir="ltr">
          <slot></slot>
        </div>
      </aside>`;
  }

  protected updated() {
    if (this.drawer!.open !== this.open) {
      this.drawer!.open = this.open;
    }
  }

  protected dispatchChangedEvent(value: boolean) {
    this.dispatchEvent(new OrMwcDrawerChangedEvent(value));
  }

  protected firstUpdated() {
    this.drawer = MDCDrawer.attachTo(this.drawerElement);
    const openHandler = () => { this.dispatchChangedEvent(true); };
    const closeHandler = () => { this.dispatchChangedEvent(false); };
    this.drawer!.listen("MDCDrawer:opened", openHandler);
    this.drawer!.listen("MDCDrawer:closed", closeHandler);
    if (this.appContent) {
      this.appContent.classList.add("mdc-drawer-app-content");
    }
    if (this.topBar) {
      this.topBar.classList.add("mdc-top-app-bar");
    }
  }
}
