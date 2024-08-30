import {css, html, LitElement, TemplateResult } from "lit";
import { customElement, property } from "lit/decorators.js";
import { until } from "lit/directives/until.js";
import { when } from "lit/directives/when.js";
import { classMap } from "lit/directives/class-map.js";
import {style} from "../style";

const styling = css`
  :host {
    height: auto !important;
  }
  
  #panel-wrapper {
    border-top: 1px solid #E0E0E0;
    border-bottom: 1px solid #E0E0E0;
  }

  #panel-header {
    display: flex;
    align-items: center;
    cursor: pointer;
    padding: 12px;
    gap: 8px;
  }

  #panel-title {
    line-height: 100%;
    font-weight: 700;
  }

  .panel-content {
    padding: 0 16px;
    max-height: 0;
    overflow: hidden;
    transition: max-height 0.2s cubic-bezier(0.4, 0.0, 0.2, 1) 0s, visibility 0s 0.2s; /* expanded -> collapsed */
  }

  .panel-content--expanded {
    max-height: 100vh;
    overflow: visible;
    transition: max-height 0.25s cubic-bezier(0.4, 0.0, 0.2, 1) 0s; /* collapsed -> expanded */
  }
`;

@customElement("settings-panel")
export class SettingsPanel extends LitElement {

    @property()
    protected expanded: boolean = false;

    @property()
    protected displayName?: string;

    static get styles() {
        return [styling, style]
    }

    protected render(): TemplateResult {
        const contentClasses = {
            "panel-content": true,
            "panel-content--expanded": this.expanded
        }
        return html`
            <div id="panel-wrapper">
                ${until(this.generateHeader(this.expanded, this.displayName), html``)}
                <div class="${classMap(contentClasses)}">
                    <div style="padding-bottom: 16px;">
                        <slot></slot>
                    </div>
                </div>
            </div>
        `;
    }

    public toggle(state?: boolean) {
        this.expanded = state || !this.expanded
    }

    protected async generateHeader(expanded: boolean, title?: string): Promise<TemplateResult> {
        return html`
            <div id="panel-header" @click="${() => this.toggle()}">
                <div id="panel-chevron">
                    <or-icon icon="${expanded ? 'chevron-down' : 'chevron-right'}"></or-icon>
                </div>
                ${when(title, () => html`
                    <div id="panel-title">
                        <span><or-translate value="${this.displayName}"></or-translate></span>
                    </div>
                `)}
            </div>
        `
    }
}
