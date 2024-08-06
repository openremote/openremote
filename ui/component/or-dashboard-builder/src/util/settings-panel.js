var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    function adopt(value) { return value instanceof P ? value : new P(function (resolve) { resolve(value); }); }
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
import { css, html, LitElement } from "lit";
import { customElement, property } from "lit/decorators.js";
import { until } from "lit/directives/until.js";
import { when } from "lit/directives/when.js";
import { classMap } from "lit/directives/class-map.js";
import { style } from "../style";
const styling = css `
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
let SettingsPanel = class SettingsPanel extends LitElement {
    constructor() {
        super(...arguments);
        this.expanded = false;
    }
    static get styles() {
        return [styling, style];
    }
    render() {
        const contentClasses = {
            "panel-content": true,
            "panel-content--expanded": this.expanded
        };
        return html `
            <div id="panel-wrapper">
                ${until(this.generateHeader(this.expanded, this.displayName), html ``)}
                <div class="${classMap(contentClasses)}">
                    <div style="padding-bottom: 16px;">
                        <slot></slot>
                    </div>
                </div>
            </div>
        `;
    }
    toggle(state) {
        this.expanded = state || !this.expanded;
    }
    generateHeader(expanded, title) {
        return __awaiter(this, void 0, void 0, function* () {
            return html `
            <div id="panel-header" @click="${() => this.toggle()}">
                <div id="panel-chevron">
                    <or-icon icon="${expanded ? 'chevron-down' : 'chevron-right'}"></or-icon>
                </div>
                ${when(title, () => html `
                    <div id="panel-title">
                        <span><or-translate value="${this.displayName}"></or-translate></span>
                    </div>
                `)}
            </div>
        `;
        });
    }
};
__decorate([
    property()
], SettingsPanel.prototype, "expanded", void 0);
__decorate([
    property()
], SettingsPanel.prototype, "displayName", void 0);
SettingsPanel = __decorate([
    customElement("settings-panel")
], SettingsPanel);
export { SettingsPanel };
//# sourceMappingURL=settings-panel.js.map