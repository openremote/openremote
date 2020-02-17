import { customElement, html, LitElement, property, css, PropertyValues } from "lit-element";
import "@openremote/or-icon";
import manager, { EventCallback, OREvent } from "@openremote/core";
import { MenuItem } from "@openremote/or-mwc-components/dist/or-mwc-menu";
import { Tenant } from "@openremote/model";
import { getContentWithMenuTemplate } from "@openremote/or-mwc-components/dist/or-mwc-menu";

// language=CSS
const style = css`
    :host {
        position: relative;
    }

    .or-realm-picker-container {
        display: flex;
        height: 50px;
        align-items: center;
    }
`;

@customElement("or-realm-picker")
export class OrRealmPicker extends LitElement {
    public static styles = style;

    protected _initCallback?: EventCallback;

    @property({ attribute: false })
    private realms: Tenant[] = [];

    protected firstUpdated(_changedProperties: PropertyValues): void {
        super.firstUpdated(_changedProperties);

        if (!manager.ready) {
            // Defer until openremote is initialised
            this._initCallback = (initEvent: OREvent) => {
                if (initEvent === OREvent.READY) {
                    this.loadRealms();
                    manager.removeListener(this._initCallback!);
                }
            };
            manager.addListener(this._initCallback);
        } else {
            this.loadRealms();
        }
    }

    protected render() {
        if (!manager.isSuperUser() || this.realms.length === 0) {
            return html``;
        }

        return html`
        ${getContentWithMenuTemplate(
            html`
                <div class="or-realm-picker-container">
                    <or-icon icon="home"></or-icon>
                    <span style="margin-left: 10px;">${manager.displayRealm}</span>
                </div>
            `,
            this.getMenuItems(),
            manager.displayRealm,
            (values: string | string[]) => this.changeRealm(values as string))}
    `;
    }

    private getMenuItems(): MenuItem[] {
        return this.realms.map((r) => {
            return {
                text: r.displayName || "invalid",
                value: r.realm || "invalid"
            };
        });
    }

    private changeRealm(displayRealm: string) {
        manager.displayRealm = displayRealm;
        this.requestUpdate();
    }

    private loadRealms() {
        if (manager.isSuperUser()) {
            manager.rest.api.TenantResource.getAll().then((response) => {
                this.realms = response.data;
            });
        }
    }
}
