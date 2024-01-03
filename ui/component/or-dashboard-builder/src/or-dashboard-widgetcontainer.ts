import {i18next} from "@openremote/or-translate";
import {html, LitElement, PropertyValues} from "lit";
import {customElement, property, query, state} from "lit/decorators.js";
import {when} from "lit/directives/when.js";
import {throttle} from "lodash";
import {style} from "./style";
import {DashboardWidget} from "@openremote/model";
import {OrWidget, WidgetManifest} from "./util/or-widget";
import {WidgetService} from "./service/widget-service";
import {WidgetConfig} from "./util/widget-config";

/* ------------------------------------ */

const elemTagName = "or-dashboard-widget-container"

@customElement(elemTagName)
export class OrDashboardWidgetContainer extends LitElement {

    static tagName = elemTagName;

    @property()
    protected readonly widget!: DashboardWidget;

    @property()
    protected readonly editMode!: boolean;

    @property()
    protected loading: boolean = false;

    @state()
    protected orWidget?: OrWidget

    @state()
    protected error?: string; // untranslated error messages

    @query("#widget-container")
    protected containerElem?: Element;

    protected resizeObserver?: ResizeObserver;
    protected manifest?: WidgetManifest;


    static get styles() {
        return [style];
    }

    disconnectedCallback() {
        super.disconnectedCallback()
        this.resizeObserver?.disconnect();
    }

    shouldUpdate(changedProps: Map<PropertyKey, unknown>): boolean {
        const changed = changedProps;

        // Update config if some values in the spec are not set.
        // Useful for when migrations have taken place.
        if (this.widget) {
            const manifest = WidgetService.getManifest(this.widget.widgetTypeId!);
            if (manifest) {
                this.widget.widgetConfig = WidgetService.correctToConfigSpec(manifest, this.widget.widgetConfig);
            }
        }

        // Only update widget if certain properties of widget has changed.
        // For example, when the 'gridItem' field changes, no update is needed since it doesn't apply here.
        if (changedProps.has('widget') && this.widget) {
            const oldVal = changedProps.get('widget') as DashboardWidget | undefined;
            const idChanged = oldVal?.id !== this.widget?.id;
            const nameChanged = oldVal?.displayName !== this.widget?.displayName;
            const configChanged = JSON.stringify(oldVal?.widgetConfig) !== JSON.stringify(this.widget?.widgetConfig);
            if (!(idChanged || nameChanged || configChanged)) {
                changed.delete('widget');
            }
        }

        return (changed.size === 0 ? false : super.shouldUpdate(changedProps));
    }

    willUpdate(changedProps: Map<string, any>) {
        super.willUpdate(changedProps);

        if (!this.manifest && this.widget) {
            this.manifest = WidgetService.getManifest(this.widget.widgetTypeId!);
        }

        // Create widget
        if (changedProps.has("widget") && this.widget) {
            this.initializeWidgetElem(this.manifest!, this.widget.widgetConfig);
        }
    }

    firstUpdated(changedProps: PropertyValues) {
        super.firstUpdated(changedProps);

        if (this.orWidget) {
            const containerElem = this.containerElem;
            if (containerElem) {
                this.resizeObserver?.disconnect();
                this.resizeObserver = new ResizeObserver(throttle(() => {
                    const minWidth = this.manifest!.minPixelWidth || 0;
                    const minHeight = this.manifest!.minPixelHeight || 0;
                    const isMinimumSize: boolean = (minWidth < containerElem.clientWidth) && (minHeight < containerElem.clientHeight);
                    this.error = (isMinimumSize ? undefined : "dashboard.widgetTooSmall");
                }, 200));
                this.resizeObserver.observe(containerElem);
            } else {
                console.error("gridItemElement could not be found!");
            }
        }
    }

    protected initializeWidgetElem(manifest: WidgetManifest, config: WidgetConfig) {
        console.debug(`Initialising ${manifest.displayName} widget..`);
        if (this.orWidget) {
            this.orWidget.remove();
        }
        this.orWidget = manifest.getContentHtml(config);
        this.orWidget.getDisplayName = () => this.widget.displayName;
        this.orWidget.getEditMode = () => this.editMode
        this.orWidget.getWidgetLocation = () => ({
            x: this.widget.gridItem?.x,
            y: this.widget.gridItem?.y,
            w: this.widget.gridItem?.w,
            h: this.widget.gridItem?.h
        });
    }

    protected render() {
        const showHeader = !!this.widget.displayName;
        return html`
            <div id="widget-container" style="height: calc(100% - 16px); padding: 8px 16px 8px 16px; display: flex; flex-direction: column;">

                <!-- Container title -->
                ${when(showHeader, () => html`
                    <div style="flex: 0 0 36px; display: flex; justify-content: space-between; align-items: center;">
                        <span class="panel-title" style="width: 100%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">
                            ${this.widget.displayName?.toUpperCase()}
                        </span>
                    </div>
                `)}

                <!-- Content -->
                <div style="flex: 1; max-height: ${showHeader ? 'calc(100% - 36px)' : '100%'};">
                    ${when((!this.error && !this.loading), () => html`
                        ${this.orWidget}
                    `, () => html`<or-translate value="${this.error ? this.error : "loading"}"></or-translate>`)}
                </div>
            </div>
        `
    }

    public refreshContent(force: boolean) {
        this.orWidget?.refreshContent(force);
    }
}
