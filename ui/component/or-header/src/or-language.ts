import {customElement, html, css, LitElement, property} from "lit-element";
import "@openremote/or-icon";
import manager from "@openremote/core";

// language=CSS
const style = css`
    :host {
        position: relative;
    }
    
    .dropdown-menu {
        position: absolute;
        top: 100%;
        box-shadow: rgba(0, 0, 0, 0.3) 0 5px 5px -2px;
        width: 120px;
        background-color: white;
        right: 0;
    }
    
    span {
        display: block;
        height: 40px;
        line-height: 40px;
    }
    
    .background-close {
        display: none;
        position: fixed;
        width: 100vw;
        height: 100vh;
        top: 0;
        left: 0;
        z-index: 0;
    }
    
    .background-close.active {
        display: block;
    }
`;

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

    changeLanguage(language: string) {
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
