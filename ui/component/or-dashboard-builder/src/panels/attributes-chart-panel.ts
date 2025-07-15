import { TemplateResult, css, html } from "lit";
import {AttributeAction, AttributesPanel} from "./attributes-panel";
import {Asset, AttributeRef} from "@openremote/model";
import { customElement } from "lit/decorators.js";

@customElement("attributes-chart-panel")
export class AttributesChartPanel extends AttributesPanel {

    static get styles() {
        return [...super.styles, css`
            .attribute-list-item:hover {
                padding-bottom: 8px;
            }
        `];
    }

    protected _getAttributeActionTemplate(action: AttributeAction, asset: Asset, attributeRef: AttributeRef): TemplateResult {
        // If 'palette', aka 'color palette', show color picker instead.
        if(action.icon === "palette") {
            return html`
                <input id="chart-color-${attributeRef.id}-${attributeRef.name}" type="color" style="visibility: hidden; width: 0; padding: 0;" />
                ${super._getAttributeActionTemplate(action, asset, attributeRef)}
            `;
        } else {
            return super._getAttributeActionTemplate(action, asset, attributeRef);
        }
    }
}
