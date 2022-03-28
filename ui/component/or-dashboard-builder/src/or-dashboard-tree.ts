import {css, html, LitElement, unsafeCSS } from "lit";
import { customElement, state } from "lit/decorators.js";
import {InputType} from '@openremote/or-mwc-components/or-mwc-input';
import "@openremote/or-icon";
import {style} from "./style";
import 'gridstack/dist/h5/gridstack-dd-native';

//language=css
const treeStyling = css`
    #content-item {
        padding: 16px;
    }
`

@customElement("or-dashboard-tree")
export class OrDashboardTree extends LitElement {

    // Importing Styles; the unsafe GridStack css, and all custom css
    static get styles() {
        return [style, treeStyling];
    }

    protected render() {
        return html`
            <div id="menu-header">
                <div id="title-container">
                    <span id="title">Dashboards</span>
                </div>
                <div>
                    <or-mwc-input type="${InputType.BUTTON}" icon="delete"></or-mwc-input>
                    <or-mwc-input type="${InputType.BUTTON}" icon="plus"></or-mwc-input>
                </div>
            </div>
            <div id="content">
                <div id="content-item">
                    <span>Dashboard 1</span>
                </div>
                <div id="content-item">
                    <span>Dashboard 2</span>
                </div>
                <div id="content-item">
                    <span>Dashboard 3</span>
                </div>
            </div>
        `
    }
}
