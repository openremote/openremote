/*
 * Copyright 2026, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
import {css, html, LitElement, PropertyValues} from 'lit';
import {customElement, property} from 'lit/decorators.js';
import '@openremote/or-icon';
import {style} from './style';
import {Dashboard} from '@openremote/model';
import manager, {Util} from '@openremote/core';
import {ListItem} from '@openremote/or-mwc-components/or-mwc-list';
import '@openremote/or-mwc-components/or-mwc-menu';
import {showOkCancelDialog} from '@openremote/or-mwc-components/or-mwc-dialog';
import {i18next} from '@openremote/or-translate';
import {showSnackbar} from '@openremote/or-mwc-components/or-mwc-snackbar';
import {style as OrAssetTreeStyle} from '@openremote/or-asset-tree';
import {DashboardService, DashboardSizeOption} from './service/dashboard-service';
import {isAxiosError} from '@openremote/rest';
import {when} from "lit/directives/when.js";

// language=css
const treeStyling = css`
    #header-btns {
        display: flex;
        flex-direction: row;
        padding-right: 5px;
    }
    
    #content {
        flex: 1;
        overflow: hidden auto;
    }

    .node-container {
        align-items: center;
        padding-left: 10px;
    }
`;

@customElement('or-dashboard-tree')
export class OrDashboardTree extends LitElement {

    static get styles() {
        return [style, treeStyling, OrAssetTreeStyle];
    }

    @property()
    protected dashboards?: Dashboard[];

    @property()
    protected selected?: Dashboard;

    @property()
    protected readonly realm: string = manager.displayRealm;

    @property() // REQUIRED
    protected readonly userId!: string;

    @property()
    protected readonly readonly = true;

    @property() // Whether the selected dashboard has been changed or not.
    protected readonly hasChanged = false;

    @property()
    protected showControls = true;


    /* --------------- */

    shouldUpdate(changedProperties: PropertyValues) {
        if (changedProperties.size === 1) {

            // Prevent any update since it is not necessary in its current state.
            // However, do update when dashboard is saved (aka when hasChanged is set back to false)
            if (changedProperties.has('hasChanged') && this.hasChanged) {
                return false;
            }
        }
        return super.shouldUpdate(changedProperties);
    }

    updated(changedProperties: PropertyValues) {
        if (!this.dashboards) {
            this.getAllDashboards();
        }
        if (changedProperties.has('dashboards') && changedProperties.get('dashboards') != null) {
            this.dispatchEvent(new CustomEvent('updated', {detail: this.dashboards}));
        }
        if (changedProperties.has('selected')) {
            this.dispatchEvent(new CustomEvent('select', {detail: this.selected}));
        }
    }


    /* ---------------------- */


    // Gets ALL dashboards the user has access to.
    // TODO: Optimize this by querying the database, or limit the JSON that is fetched.
    private async getAllDashboards() {
        return manager.rest.api.DashboardResource.getAllRealmDashboards(this.realm)
            .then(result => {
                this.dashboards = result.data;
            }).catch(reason => {
                console.error(reason);
                showSnackbar(undefined, 'errorOccurred');
            });
    }

    // Selects the dashboard (id).
    // Will emit a "select" event during the lifecycle
    private selectDashboard(id: string | Dashboard | undefined) {
        if (typeof id === 'string') {
            this.selected = this.dashboards?.find(dashboard => dashboard.id === id);
        } else {
            this.selected = id;
        }
    }

    // Creates a dashboard, and adds it onto the list.
    // Dispatch a "created" event, to let parent elements know a new dashboard has been created.
    // It will automatically select the newly created dashboard.
    private createDashboard(size: DashboardSizeOption) {
        DashboardService.create(undefined, size, this.realm).then(d => {
            if (!this.dashboards) {
                this.dashboards = [d] as Dashboard[];
            } else {
                this.dashboards.push(d);
                this.requestUpdate('dashboards');
            }
            this.dispatchEvent(new CustomEvent('created', {detail: {d}}));
            this.selectDashboard(d);

        }).catch(e => {
            console.error(e);
            if (isAxiosError(e) && e.response?.status === 404) {
                showSnackbar(undefined, 'noDashboardFound');
            } else {
                showSnackbar(undefined, 'errorOccurred');
            }
        });
    }

    protected duplicateDashboard(dashboard: Dashboard) {
        if (dashboard?.id !== null) {
            if(this.hasChanged) {
                this.showDiscardChangesModal().then(ok => {
                    if(ok) {
                        this._doDuplicateDashboard(dashboard);
                    }
                })
            } else {
                this._doDuplicateDashboard(dashboard);
            }
        } else {
            console.warn("Tried duplicating a NULL dashboard!");
        }
    }

    protected _doDuplicateDashboard(dashboard: Dashboard) {
        const newDashboard = JSON.parse(JSON.stringify(dashboard)) as Dashboard;
        newDashboard.displayName = (newDashboard.displayName + " copy");
        if(newDashboard.template) {
            newDashboard.template.id = Util.generateUniqueUUID();
        }
        DashboardService.create(newDashboard, undefined, this.realm).then(d => {
            if (!this.dashboards) {
                this.dashboards = [d] as Dashboard[];
            } else {
                this.dashboards.push(d);
                this.requestUpdate('dashboards');
            }
            this.dispatchEvent(new CustomEvent('created', {detail: {d}}));
            this.selectDashboard(d.id);
        }).catch(e => {
            console.error(e);
            if (isAxiosError(e) && e.response?.status === 404) {
                showSnackbar(undefined, 'noDashboardFound');
            } else {
                showSnackbar(undefined, 'errorOccurred');
            }
        });
    }

    private deleteDashboard(dashboard: Dashboard) {
        if (dashboard.id != null) {
            DashboardService.delete(dashboard.id, this.realm).then(() => {
                this.getAllDashboards();
            }).catch(reason => {
                console.error(reason);
                showSnackbar(undefined, 'errorOccurred');
            });
        }
    }

    /* ---------------------- */


    // When a user clicks on a dashboard within the list...
    protected onDashboardClick(dashboardId: string) {
        if (dashboardId !== this.selected?.id) {
            if (this.hasChanged) {
                this.showDiscardChangesModal().then(ok => {
                    if(ok) this.selectDashboard(dashboardId);
                })
            } else {
                this.selectDashboard(dashboardId);
            }
        }
    }

    protected showDiscardChangesModal(): Promise<boolean> {
        return showOkCancelDialog(i18next.t('areYouSure'), i18next.t('confirmContinueDashboardModified'), i18next.t('discard'));
    }

    // Element render method
    // TODO: Move dashboard filtering to separate method.
    protected render() {
        const dashboardItems = this.dashboards?.sort((a, b) => a.displayName ? a.displayName.localeCompare(b.displayName!) : 0)
            .map(d => ({ icon: 'view-dashboard', text: d.displayName, value: d.id} as ListItem)) || []
        return html`
            <div id="menu-header">
                <div id="title-container">
                    <span id="title"><or-translate value="insights"></or-translate></span>
                </div>
                
                <!-- Controls header -->
                ${this.showControls ? html`
                    <div id="header-btns">
                        ${when(this.selected != null, () => html`
                            <or-vaadin-button theme="icon" title=${i18next.t("deselect")} @click=${() => this.selectDashboard(undefined)}>
                                <or-icon icon="close"></or-icon>
                            </or-vaadin-button>
                            ${when(!this.readonly, () => html`
                                <or-vaadin-button theme="icon" class="hideMobile" title=${i18next.t("duplicate")} @click=${() => this.duplicateDashboard(this.selected!)}>
                                    <or-icon icon="content-copy"></or-icon>
                                </or-vaadin-button>
                                <or-vaadin-button theme="icon" title=${i18next.t("delete")} @click=${() => {
                                    if (this.selected != null) {
                                        showOkCancelDialog(i18next.t('areYouSure'), i18next.t('dashboard.deletePermanentWarning', {dashboard: this.selected.displayName}), i18next.t('delete')).then((ok: boolean) => {
                                            if (ok) this.deleteDashboard(this.selected!);
                                        });
                                    }
                                }}>
                                    <or-icon icon="delete"></or-icon>
                                </or-vaadin-button>
                            `)}
                        `)}
                        ${when(!this.readonly, () => html`
                            <or-vaadin-button theme="icon" class="hideMobile" title=${i18next.t("addInsights")} @click=${() => this.createDashboard(DashboardSizeOption.DESKTOP)}>
                                <or-icon icon="plus"></or-icon>
                            </or-vaadin-button>
                        `)}
                    </div>
                ` : undefined}
            </div>
            
            <!-- List of dashboards -->
            <div id="content">
                <div id="list-container" style="overflow: hidden;">
                    <ol id="list">
                        ${dashboardItems.map((listItem: ListItem) => html`
                            <li ?data-selected="${listItem.value === this.selected?.id}" @click="${(_evt: MouseEvent) => this.onDashboardClick(listItem.value)}">
                                <div class="node-container">
                                    <span class="node-name">${listItem.text} </span>
                                </div>
                            </li>
                        `)}
                    </ol>
                </div>
            </div>
        `;
    }
}
