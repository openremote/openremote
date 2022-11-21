import {Asset, DashboardWidget } from "@openremote/model";
import {css, html, LitElement, TemplateResult, unsafeCSS } from "lit";
import { customElement, property, state } from "lit/decorators.js";
import { InputType, OrInputChangedEvent } from "@openremote/or-mwc-components/or-mwc-input";
import {style} from './style';
import {DefaultColor5} from "@openremote/core";
import { i18next } from "@openremote/or-translate";
import {widgetTypes} from "./index";

const tableStyle = require("@material/data-table/dist/mdc.data-table.css");

//language=css
const widgetSettingsStyling = css`
    
    /* ------------------------------- */
    .switchMwcInputContainer {
        display: flex;
        align-items: center;
        justify-content: space-between;
    }
    /* ---------------------------- */
    #attribute-list {
        overflow: auto;
        flex: 1 1 0;
        min-height: 150px;
        width: 100%;
        display: flex;
        flex-direction: column;
    }

    .attribute-list-item {
        cursor: pointer;
        display: flex;
        flex-direction: row;
        align-items: center;
        padding: 0;
        min-height: 50px;
    }

    .attribute-list-item-label {
        display: flex;
        flex: 1 1 0;
        line-height: 16px;
        flex-direction: column;
    }

    .attribute-list-item-bullet {
        width: 14px;
        height: 14px;
        border-radius: 7px;
        margin-right: 10px;
    }

    .attribute-list-item .button.delete {
        display: none;
    }

    .attribute-list-item:hover .button.delete {
        display: block;
    }

    /* ---------------------------- */
    .button-clear {
        background: none;
        visibility: hidden;
        color: var(--or-app-color5, ${unsafeCSS(DefaultColor5)});
        --or-icon-fill: var(--or-app-color5, ${unsafeCSS(DefaultColor5)});
        display: inline-block;
        border: none;
        padding: 0;
        cursor: pointer;
    }

    .attribute-list-item:hover .button-clear {
        visibility: visible;
    }

    .button-clear:hover {
        --or-icon-fill: var(--or-app-color4);
    }
    /* ---------------------------- */
`

/* ------------------------------------ */

@customElement("or-dashboard-widgetsettings")
export class OrDashboardWidgetsettings extends LitElement {

    static get styles() {
        return [unsafeCSS(tableStyle), widgetSettingsStyling, style]
    }

    @property({type: Object})
    protected selectedWidget: DashboardWidget | undefined;

    @property()
    protected realm?: string;

    @state() // list of assets that are loaded in the list
    protected loadedAssets: Asset[] | undefined;

    @state()
    protected expandedPanels: string[];

    constructor() {
        super();
        this.expandedPanels = [];
    }

    /* ------------------------------------ */


    // Method to update the Grid. For example after changing a setting.
    forceParentUpdate(changes: Map<string, any>, force: boolean = false) {
        this.requestUpdate();
        this.dispatchEvent(new CustomEvent('update', { detail: { changes: changes, force: force }}));
    }

    protected render() {
        this.updateComplete.then(() => {
            const settingElem = this.shadowRoot?.children[0].children[1];
            settingElem!.addEventListener('updated', (event: any) => {
                this.forceParentUpdate(event.detail.changes, event.detail.force);
            });
        });
        if(this.selectedWidget?.widgetTypeId != null && this.selectedWidget.widgetConfig != null) {
            return this.generateHTML(this.selectedWidget.widgetTypeId, this.selectedWidget.widgetConfig);
        }
        return html`<span>${i18next.t('errorOccurred')}</span>`
    }




    /* ----------------------------------- */

    // UI generation of all settings fields. Depending on the WidgetType it will
    // return different HTML containing different settings.
    generateHTML(widgetTypeId: string, widgetConfig: any): TemplateResult {
        let htmlGeneral: TemplateResult;
        let htmlContent: TemplateResult;
        htmlGeneral = html`
            <div style="padding: 12px;">
                <div>
                    <or-mwc-input .type="${InputType.TEXT}" style="width: 100%;" .value="${this.selectedWidget?.displayName}" label="${i18next.t('name')}" 
                                  @or-mwc-input-changed="${(event: OrInputChangedEvent) => {
                                      this.selectedWidget!.displayName = event.detail.value;
                                      this.forceParentUpdate(new Map<string, any>([['widget', this.selectedWidget]])); }}"
                    ></or-mwc-input>
                </div>
            </div>
        `
        htmlContent = widgetTypes.get(widgetTypeId)!.getSettingsHTML(this.selectedWidget!, this.realm!);

        return html`
            <div>
                ${htmlGeneral}
                ${htmlContent}
            </div>
        `
    }
}
