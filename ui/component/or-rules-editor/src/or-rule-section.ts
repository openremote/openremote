import {html, property, LitElement, customElement} from "lit-element";
import {ruleSectionStyle} from "./style";

@customElement("or-rule-section")
class OrRuleSection extends LitElement {

    @property({type: String})
    public heading?: string;

    static get styles() {
        return ruleSectionStyle;
    }

    public render() {
        return html`
            <div>
                <strong>${this.heading}</strong>                    
                <slot></slot>
            </div>
        `;
    }
}