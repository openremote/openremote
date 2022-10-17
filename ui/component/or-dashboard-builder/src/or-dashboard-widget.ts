import {DashboardWidget } from "@openremote/model";
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";
import { i18next } from "@openremote/or-translate";
import {css, html, LitElement, TemplateResult } from "lit";
import { customElement, property, query, state } from "lit/decorators.js";
import { when } from "lit/directives/when.js";
import {throttle} from "lodash";
import {style} from "./style";
import {widgetTypes} from "./index";

//language=css
const styling = css`
`

/* ------------------------------------ */

@customElement("or-dashboard-widget")
export class OrDashboardWidget extends LitElement {

    @property({ hasChanged(oldValue, newValue) { return JSON.stringify(oldValue) != JSON.stringify(newValue); }})
    protected readonly widget?: DashboardWidget;

    @property()
    protected readonly editMode?: boolean;

    @property()
    protected readonly realm?: string;

    @property()
    protected loading: boolean = false;

    @state()
    protected error?: string;

    @state()
    protected resizeObserver?: ResizeObserver;

    @query("#widget-container")
    protected widgetContainerElement?: Element;


    static get styles() {
        return [styling, style];
    }

    constructor() {
        super();
    }

    shouldUpdate(changedProperties: Map<PropertyKey, unknown>): boolean {
        const changed = changedProperties;
        changed.delete('resizeObserver');
        return changed.size > 0;
    }

    firstUpdated(_changedProperties: Map<string, any>) {
        this.updateComplete.then(() => {
            const gridItemElement = this.widgetContainerElement;
            if(gridItemElement) {
                this.resizeObserver?.disconnect();
                this.resizeObserver = new ResizeObserver(throttle(() => {
                    const isMinimumSize: boolean = (this.widget?.gridItem?.minPixelW != null && this.widget.gridItem.minPixelH != null &&
                        (this.widget.gridItem.minPixelW < gridItemElement.clientWidth) && (this.widget.gridItem.minPixelH < gridItemElement.clientHeight)
                    );
                    this.error = (isMinimumSize ? undefined : i18next.t('dashboard.widgetTooSmall'));
                }, 200));
                this.resizeObserver.observe(gridItemElement);
            } else {
                console.error("gridItemElement could not be found!");
            }
        });
    }


    protected render() {
        return html`
            <div id="widget-container" style="height: 100%; padding: 8px 16px 8px 16px; display: flex; flex-direction: column; overflow: auto;">
                <div style="display: flex; justify-content: space-between; align-items: center; margin-right: -12px; height: 36px;">
                    <span class="panel-title" style="width: 100%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">${this.widget?.displayName?.toUpperCase()}</span>
                    <div>
                            <!--<or-mwc-input type="${InputType.BUTTON}" outlined label="Period"></or-mwc-input>-->
                            <!--<or-mwc-input type="${InputType.BUTTON}" label="Settings"></or-mwc-input>-->
                            <!--<or-mwc-input type="${InputType.BUTTON}" icon="refresh" @or-mwc-input-changed="${() => { this.requestUpdate(); }}"></or-mwc-input>-->
                    </div>
                </div>
                ${when((!this.error && !this.loading), () => html`
                    ${this.getWidgetContent(this.widget!)}
                `, () => html`
                    ${this.error ? html`${this.error}` : html`${i18next.t('loading')}`}
                `)}
            </div>
        `
    }

    getWidgetContent(widget: DashboardWidget): TemplateResult {
        const _widget = Object.assign({}, widget);
        if(_widget.gridItem) {

            const widgetEntity = widgetTypes.get(widget.widgetTypeId!);
            return widgetEntity!.getWidgetHTML(this.widget!, this.editMode!, this.realm!);

        }
        return html`<span>${i18next.t('error')}!</span>`;
    }
}
