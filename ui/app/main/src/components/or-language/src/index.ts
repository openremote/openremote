import {customElement, html, LitElement, property} from "lit-element";
import "@openremote/or-icon";
import manager from "@openremote/core";
import {style} from "./style";

@customElement("or-language")
export class OrLanguage extends LitElement {


    @property({type: Boolean})
    isVisible?: boolean = false;

    static styles = style;

    protected render() {
        return html`
            <or-icon icon="web" @click="${this.toggleMenu}"></or-icon>
            <div class="background-close ${this.isVisible ? "active" : ""}" @click="${this.closeMenu}"></div>
            ${this.isVisible ? html`
                <div class="dropdown-menu">
                      <span @click="${() => this.changeLanguage("en")}"><or-translate value="english"></or-translate></span>
                      <span @click="${() => this.changeLanguage("nl")}"><or-translate value="dutch"></or-translate></span>
                      <span @click="${() => this.changeLanguage("fr")}"><or-translate value="french"></or-translate></span>
                      <span @click="${() => this.changeLanguage("de")}"><or-translate value="german"></or-translate></span>
                      <span @click="${() => this.changeLanguage("es")}"><or-translate value="spanish"></or-translate></span>
                </div>
            ` : ``}
        `;
    }

    changeLanguage(language) {
        manager.language = language;
        this.isVisible = false;
    }

    closeMenu() {
        this.isVisible = false;
    }

    toggleMenu() {
        this.isVisible = !this.isVisible;
    }
}
