import {DashboardWidget} from "@openremote/model";
import {TemplateResult} from "lit";

export interface OrWidgetConfig {

}

export interface OrWidgetEntity {
    DISPLAY_NAME: string,
    DISPLAY_MDI_ICON: string; // https://materialdesignicons.com;
    MIN_COLUMN_WIDTH: number;
    MIN_PIXEL_WIDTH: number;
    MIN_PIXEL_HEIGHT: number;

    getDefaultConfig: (widget: DashboardWidget) => OrWidgetConfig;

    getWidgetHTML: (widget: DashboardWidget, editMode: boolean, realm: string) => TemplateResult;
    getSettingsHTML: (widget: DashboardWidget, realm: string) => TemplateResult;
}

/*export abstract class OrBaseWidget extends ReactiveElement {

    @property({type: Object}) set widget(widget: DashboardWidget) {
        console.error(JSON.stringify(this._widget) == JSON.stringify(widget));
        if(JSON.stringify(this._widget) != JSON.stringify(widget)) {
            this._widget = widget;
            this.doUpdate(new Map<string, any>([["widget", widget]]));
        }
    };
    private _widget?: DashboardWidget;
    get widget() { return this._widget!; }


    @property() set editMode(editMode: boolean) {
        if(this._editMode != editMode) {
            this._editMode = editMode;
            this.doUpdate(new Map<string, any>([["editMode", editMode]]));
        }
    }
    private _editMode: boolean = false;
    get editMode() { return this._editMode; }


    @property({type: String}) set realm(realm: string) {
        if(this._realm != realm) {
            this._realm = realm;
            this.doUpdate(new Map<string, any>([["realm", realm]]));
        }
    }
    private _realm?: string;
    get realm() { return this._realm!; }



    public readonly DISPLAY_NAME: string = "Base Widget";
    public readonly DISPLAY_MDI_ICON: string = "help-circle"; // https://materialdesignicons.com;
    public readonly MIN_COLUMN_WIDTH: number = 2;
    public readonly MIN_PIXEL_WIDTH: number = 200;
    public readonly MIN_PIXEL_HEIGHT: number = 200;

    constructor() {
        super();
        if (!this.realm) {
            this.realm = manager.displayRealm;
        }
    }

    /!* ------------------ *!/

    public getDefaultConfig(): OrBaseWidgetConfig {
        return {};
    }

    public getWidgetHTML(widget: DashboardWidget, editMode: boolean, realm: string): TemplateResult {
        console.error("Getting widgetHTML of or-base-widget!");
        this.widget = widget;
        this.editMode = editMode;
        this.realm = realm;
        return html`<span>Base Widget content</span>`
    }

    public getSettingsHTML(widget: DashboardWidget): TemplateResult {
        return html`<span>Base Widget settings</span>`;
    }

    // Fetching the assets according to the AttributeRef[] input in DashboardWidget if required.
    protected async fetchAssets(config: OrBaseWidgetConfig | any): Promise<Asset[] | undefined> {
        if(config.attributeRefs) {
            console.error("Fetching assets in OrBaseWidget!");
            let assets: Asset[] = [];
            await manager.rest.api.AssetResource.queryAssets({
                ids: config.attributeRefs?.map((x: AttributeRef) => {
                    return x.id;
                }) as string[]
            }).then(response => {
                assets = response.data;
            }).catch((reason) => {
                console.error(reason);
                showSnackbar(undefined, i18next.t('errorOccurred'));
            });
            return assets;
        }
    }

    protected doUpdate(changes: Map<string, any>) { }

    protected dispatchUpdate() {
        console.error("Dispatching update!");
        this.dispatchEvent(new CustomEvent('updated'));
    }



    /!* -------------------------- *!/
}*/
