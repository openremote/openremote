import {customElement, html, css, LitElement, property} from "lit-element";
import "@openremote/or-icon";
import i18next from "i18next";
import manager from "@openremote/core";
import {MenuItem} from "@openremote/or-mwc-components/dist/or-mwc-menu";
import {getContentWithMenuTemplate} from "@openremote/or-mwc-components/dist/or-mwc-menu";

// language=CSS
const style = css`
    :host {
        position: relative;
    }
    
    .or-language-container {
        display: flex;
        height: 50px;
        align-items: center;
    }
`;
interface languageOptions {
    [key: string]: string;
}

export const languageOptions: languageOptions = {
    "en": "english",
    "nl": "dutch",
    "fr": "french",
    "de": "german",
    "es": "spanish"
};

function getLanguageMenu(): MenuItem[] {

    return Object.entries(languageOptions).map(([key, value]) => {
        return {
            text: i18next.t(value),
            value: key
        };
    });
}
// TODO remove because deprecated?
@customElement("or-language")
export class OrLanguage extends LitElement {


    @property({type: Boolean})
    isVisible?: boolean = false;

    @property({type: String})
    language: string = manager.language;

    static styles = style;

    firstUpdated() {
        this.language = manager.language ? manager.language : "en";
    }

    protected render() {
        return html`
            ${getContentWithMenuTemplate(
                html`
                    <div class="or-language-container">
                        <or-icon icon="web"></or-icon>
                        <span style="margin-left: 10px; text-transform: uppercase;">${this.language}</span>
                    </div>
                `,
                getLanguageMenu(),
                this.language,
                (values: string | string[]) => this.changeLanguage(values as string))}
        `;
    }

    changeLanguage(language: string) {
        manager.language = language;
        this.language = language;
        this.isVisible = false;
    }
}
