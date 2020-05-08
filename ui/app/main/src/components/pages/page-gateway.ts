import {css, customElement, html, LitElement} from "lit-element";
import {store} from "../../store";
import {connect} from "pwa-helpers/connect-mixin";
import "../or-gateway-connection-panel/src/index";

@customElement("page-gateway")
class PageGateway extends connect(store)(LitElement)  {
    // language=CSS
    static get styles() {
        return css`
            :host {
                flex: 1;
                width: 100%;
                
                display: flex;
                justify-content: center;            
            }
            
            or-gateway-connection-panel {
                max-width: 1000px;
                margin-top: 40px;
            }
            
            @media only screen and (max-width: 1080px){
                or-gateway-connection-panel {
                    margin: 0;
                }
            }
        `;
    }

    protected render() {
        return html`
                
            <or-gateway-connection-panel></or-gateway-connection-panel>
        `;
    }
}
