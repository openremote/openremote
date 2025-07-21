import { TemplateResult, css, html } from "lit";
import {AttributeAction, AttributesPanel} from "./attributes-panel";
import {Asset, AttributeRef} from "@openremote/model";
import { customElement } from "lit/decorators.js";
import {i18next} from "@openremote/or-translate";
import { when } from "lit/directives/when.js";

@customElement("attributes-chart-panel")
export class AttributesChartPanel extends AttributesPanel {

    static get styles() {
        return [...super.styles, css`
            .attribute-list-item:hover {
                padding-bottom: 8px;
            }
        `];
    }

    protected _getAttributeActionsTemplate(asset: Asset, attributeRef: AttributeRef): TemplateResult {
        return html`
            <div class="attribute-list-item-actions">
                <!-- Remove attribute button -->
                <button class="button-action" title="${i18next.t('delete')}" @click="${() => this.removeWidgetAttribute(attributeRef)}">
                    <or-icon icon="close-circle"></or-icon>
                </button>
            </div>
            ${when(!!this.attributeIconCallback, () => html`
                <div class="attribute-list-item-actions" style="margin-left: 18px; justify-content: start;">
                    <!-- Custom actions defined by callback -->
                    ${this.attributeActionCallback!(attributeRef).map(action => this._getAttributeActionTemplate(action, asset, attributeRef))}
                </div>
            `)}
        `;
    }

    protected _getAttributeActionTemplate(action: AttributeAction, asset: Asset, attributeRef: AttributeRef): TemplateResult {
        // If 'palette', aka 'color palette', show color picker instead.
        if(action.icon === "palette") {
            return html`
                <div style="position: relative;">
                    <input id="chart-color-${attributeRef.id}-${attributeRef.name}" type="color" style="position: absolute; visibility: hidden; height: 24px; width: 24px; padding: 0;" />
                    ${super._getAttributeActionTemplate(action, asset, attributeRef)}
                </div>
            `;
        } else {
            return super._getAttributeActionTemplate(action, asset, attributeRef);
        }
    }
}
