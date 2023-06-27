import {DashboardWidget } from "@openremote/model";
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

        // Update config if some values in the spec are not set.
        // Useful for when migrations have taken place.
        if(this.widget) {
            const widgetType = widgetTypes.get(this.widget.widgetTypeId!);
            if(widgetType) {
                this.widget.widgetConfig = widgetType.verifyConfigSpec(this.widget);
            }
        }

        const changed = changedProperties;
        changed.delete('resizeObserver');
        return changed.size > 0;
    }

    disconnectedCallback() {
        super.disconnectedCallback()
        this.resizeObserver?.disconnect();
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
            <div id="widget-container" style="height: calc(100% - 16px); padding: 8px 16px 8px 16px; display: flex; flex-direction: column;">
                <div style="flex: 0 0 36px; display: flex; justify-content: space-between; align-items: center;">
                    <span class="panel-title" style="width: 100%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">${this.widget?.displayName?.toUpperCase()}</span>
                </div>
                <div style="flex: 1; max-height: calc(100% - 36px);">
                    ${when((!this.error && !this.loading), () => html`
                        ${this.getWidgetContent(this.widget!)}
                    `, () => html`
                        ${this.error ? html`${this.error}` : html`${i18next.t('loading')}`}
                    `)}
                </div>
            </div>
        `
    }

    getWidgetContent(widget: DashboardWidget): TemplateResult {
        if(widget.gridItem) {
            const widgetEntity = widgetTypes.get(widget.widgetTypeId!);
            return widgetEntity!.getWidgetHTML(widget, this.editMode!, this.realm!);
        }
        return html`<span>${i18next.t('error')}!</span>`;
    }
}
