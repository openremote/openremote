import {html, LitElement, property, css, query, customElement, PropertyValues} from "lit-element";
import {store} from "../../store";
import {connect} from "pwa-helpers/connect-mixin";
import "@openremote/or-rules";

@customElement("page-rules")
class PageRules extends connect(store)(LitElement)  {

    static get styles() {
        return css`
            :host {
            
            }
            
            or-rules {
                width: 100%;
                height: 100%;
            }
        `;
    }

    protected render() {
        return html`
            <or-rules></or-rules>
        `;
    }
}
