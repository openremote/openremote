import {css, customElement, html, LitElement} from "lit-element";
import {store} from "../../store";
import {connect} from "pwa-helpers/connect-mixin";
import "@openremote/or-log-viewer";
import {ViewerConfig} from "@openremote/or-log-viewer";
import {SyslogCategory} from "@openremote/model";

@customElement("page-logs")
class PageLogs extends connect(store)(LitElement)  {

    static get styles() {
        return css`
            :host {
                flex: 1;
                width: 100%;            
            }
            
            or-log-viewer {
                width: 100%;
            }
        `;
    }

    protected config: ViewerConfig = {
    };

    protected render() {
        return html`
            <or-log-viewer .config="${this.config}"></or-log-viewer>
        `;
    }
}
