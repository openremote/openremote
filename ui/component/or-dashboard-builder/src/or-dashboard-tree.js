var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    function adopt(value) { return value instanceof P ? value : new P(function (resolve) { resolve(value); }); }
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
import { css, html, LitElement } from 'lit';
import { customElement, property } from 'lit/decorators.js';
import { InputType } from '@openremote/or-mwc-components/or-mwc-input';
import '@openremote/or-icon';
import { style } from './style';
import manager from '@openremote/core';
import '@openremote/or-mwc-components/or-mwc-menu';
import { showOkCancelDialog } from '@openremote/or-mwc-components/or-mwc-dialog';
import { i18next } from '@openremote/or-translate';
import { showSnackbar } from '@openremote/or-mwc-components/or-mwc-snackbar';
import { style as OrAssetTreeStyle } from '@openremote/or-asset-tree';
import { DashboardService, DashboardSizeOption } from './service/dashboard-service';
import { isAxiosError } from '@openremote/rest';
// language=css
const treeStyling = css `
    #header-btns {
        display: flex;
        flex-direction: row;
        padding-right: 5px;
    }

    .node-container {
        align-items: center;
        padding-left: 10px;
    }
`;
let OrDashboardTree = class OrDashboardTree extends LitElement {
    constructor() {
        super(...arguments);
        this.realm = manager.displayRealm;
        this.readonly = true;
        this.hasChanged = false;
        this.showControls = true;
    }
    static get styles() {
        return [style, treeStyling, OrAssetTreeStyle];
    }
    /* --------------- */
    shouldUpdate(changedProperties) {
        if (changedProperties.size === 1) {
            // Prevent any update since it is not necessary in its current state.
            // However, do update when dashboard is saved (aka when hasChanged is set back to false)
            if (changedProperties.has('hasChanged') && this.hasChanged) {
                return false;
            }
        }
        return super.shouldUpdate(changedProperties);
    }
    updated(changedProperties) {
        if (!this.dashboards) {
            this.getAllDashboards();
        }
        if (changedProperties.has('dashboards') && changedProperties.get('dashboards') != null) {
            this.dispatchEvent(new CustomEvent('updated', { detail: this.dashboards }));
        }
        if (changedProperties.has('selected')) {
            this.dispatchEvent(new CustomEvent('select', { detail: this.selected }));
        }
    }
    /* ---------------------- */
    // Gets ALL dashboards the user has access to.
    // TODO: Optimize this by querying the database, or limit the JSON that is fetched.
    getAllDashboards() {
        return __awaiter(this, void 0, void 0, function* () {
            return manager.rest.api.DashboardResource.getAllRealmDashboards(this.realm)
                .then(result => {
                this.dashboards = result.data;
            }).catch(reason => {
                console.error(reason);
                showSnackbar(undefined, 'errorOccurred');
            });
        });
    }
    // Selects the dashboard (id).
    // Will emit a "select" event during the lifecycle
    selectDashboard(id) {
        var _a;
        if (typeof id === 'string') {
            this.selected = (_a = this.dashboards) === null || _a === void 0 ? void 0 : _a.find(dashboard => dashboard.id === id);
        }
        else {
            this.selected = id;
        }
    }
    // Creates a dashboard, and adds it onto the list.
    // Dispatch a "created" event, to let parent elements know a new dashboard has been created.
    // It will automatically select the newly created dashboard.
    createDashboard(size) {
        DashboardService.create(undefined, size, this.realm).then(d => {
            if (!this.dashboards) {
                this.dashboards = [d];
            }
            else {
                this.dashboards.push(d);
                this.requestUpdate('dashboards');
            }
            this.dispatchEvent(new CustomEvent('created', { detail: { d } }));
            this.selectDashboard(d);
        }).catch(e => {
            var _a;
            console.error(e);
            if (isAxiosError(e) && ((_a = e.response) === null || _a === void 0 ? void 0 : _a.status) === 404) {
                showSnackbar(undefined, 'noDashboardFound');
            }
            else {
                showSnackbar(undefined, 'errorOccurred');
            }
        });
    }
    duplicateDashboard(dashboard) {
        if ((dashboard === null || dashboard === void 0 ? void 0 : dashboard.id) !== null) {
            if (this.hasChanged) {
                this.showDiscardChangesModal().then(ok => {
                    if (ok) {
                        this._doDuplicateDashboard(dashboard);
                    }
                });
            }
            else {
                this._doDuplicateDashboard(dashboard);
            }
        }
        else {
            console.warn("Tried duplicating a NULL dashboard!");
        }
    }
    _doDuplicateDashboard(dashboard) {
        const newDashboard = JSON.parse(JSON.stringify(dashboard));
        newDashboard.displayName = (newDashboard.displayName + ' copy');
        DashboardService.create(newDashboard, undefined, this.realm).then(d => {
            if (!this.dashboards) {
                this.dashboards = [d];
            }
            else {
                this.dashboards.push(d);
                this.requestUpdate('dashboards');
            }
            this.dispatchEvent(new CustomEvent('created', { detail: { d } }));
            this.selectDashboard(d.id);
        }).catch(e => {
            var _a;
            console.error(e);
            if (isAxiosError(e) && ((_a = e.response) === null || _a === void 0 ? void 0 : _a.status) === 404) {
                showSnackbar(undefined, 'noDashboardFound');
            }
            else {
                showSnackbar(undefined, 'errorOccurred');
            }
        });
    }
    deleteDashboard(dashboard) {
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
    onDashboardClick(dashboardId) {
        var _a;
        if (dashboardId !== ((_a = this.selected) === null || _a === void 0 ? void 0 : _a.id)) {
            if (this.hasChanged) {
                this.showDiscardChangesModal().then(ok => {
                    if (ok)
                        this.selectDashboard(dashboardId);
                });
            }
            else {
                this.selectDashboard(dashboardId);
            }
        }
    }
    showDiscardChangesModal() {
        return showOkCancelDialog(i18next.t('areYouSure'), i18next.t('confirmContinueDashboardModified'), i18next.t('discard'));
    }
    // Element render method
    // TODO: Move dashboard filtering to separate method.
    render() {
        var _a;
        const dashboardItems = [];
        if (this.dashboards && this.dashboards.length > 0) {
            if (this.userId) {
                const myDashboards = [];
                const otherDashboards = [];
                (_a = this.dashboards) === null || _a === void 0 ? void 0 : _a.forEach(d => {
                    (d.ownerId === this.userId) ? myDashboards.push(d) : otherDashboards.push(d);
                });
                if (myDashboards.length > 0) {
                    const items = [];
                    myDashboards.sort((a, b) => a.displayName ? a.displayName.localeCompare(b.displayName) : 0).forEach(d => {
                        items.push({ icon: 'view-dashboard', text: d.displayName, value: d.id });
                    });
                    dashboardItems.push(items);
                }
                if (otherDashboards.length > 0) {
                    const items = [];
                    otherDashboards.sort((a, b) => a.displayName ? a.displayName.localeCompare(b.displayName) : 0).forEach(d => {
                        items.push({ icon: 'view-dashboard', text: d.displayName, value: d.id });
                    });
                    dashboardItems.push(items);
                }
            }
        }
        return html `
            <div id="menu-header">
                <div id="title-container">
                    <span id="title"><or-translate value="insights"></or-translate></span>
                </div>
                
                <!-- Controls header -->
                ${this.showControls ? html `
                    <div id="header-btns">
                        ${this.selected != null ? html `
                            <or-mwc-input type="${InputType.BUTTON}" icon="close" @or-mwc-input-changed="${() => {
            this.selectDashboard(undefined);
        }}"></or-mwc-input>
                            ${!this.readonly ? html `
                                <or-mwc-input type="${InputType.BUTTON}" class="hideMobile" icon="content-copy"
                                              @or-mwc-input-changed="${() => {
            this.duplicateDashboard(this.selected);
        }}"
                                ></or-mwc-input>
                                <or-mwc-input type="${InputType.BUTTON}" icon="delete" @or-mwc-input-changed="${() => {
            if (this.selected != null) {
                showOkCancelDialog(i18next.t('areYouSure'), i18next.t('dashboard.deletePermanentWarning', { dashboard: this.selected.displayName }), i18next.t('delete')).then((ok) => {
                    if (ok) {
                        this.deleteDashboard(this.selected);
                    }
                });
            }
        }}"></or-mwc-input>
                            ` : undefined}
                        ` : undefined}
                        ${!this.readonly ? html `
                            <or-mwc-input type="${InputType.BUTTON}" class="hideMobile" icon="plus"
                                          @or-mwc-input-changed="${() => {
            this.createDashboard(DashboardSizeOption.DESKTOP);
        }}"
                            ></or-mwc-input>
                        ` : undefined}
                    </div>
                ` : undefined}
            </div>
            
            <!-- List of dashboards -->
            <div id="content">
                <div style="padding-top: 8px;">
                    ${dashboardItems.map((items, index) => {
            return (items != null && items.length > 0) ? html `
                            <div style="padding: 8px 0;">
                                <span style="font-weight: 500; padding-left: 14px; color: #000000;">
                                    <or-translate value="${(index === 0 ? 'dashboard.myDashboards' : 'dashboard.createdByOthers')}"></or-translate>
                                 </span>
                                <div id="list-container" style="overflow: hidden;">
                                    <ol id="list">
                                        ${items.map((listItem) => {
                var _a;
                return html `
                                                <li ?data-selected="${listItem.value === ((_a = this.selected) === null || _a === void 0 ? void 0 : _a.id)}" @click="${(_evt) => {
                    this.onDashboardClick(listItem.value);
                }}">
                                                    <div class="node-container">
                                                        <span class="node-name">${listItem.text} </span>
                                                    </div>
                                                </li>
                                            `;
            })}
                                    </ol>
                                </div>
                            </div>
                        ` : undefined;
        })}
                </div>
            </div>
        `;
    }
};
__decorate([
    property()
], OrDashboardTree.prototype, "dashboards", void 0);
__decorate([
    property()
], OrDashboardTree.prototype, "selected", void 0);
__decorate([
    property()
], OrDashboardTree.prototype, "realm", void 0);
__decorate([
    property() // REQUIRED
], OrDashboardTree.prototype, "userId", void 0);
__decorate([
    property()
], OrDashboardTree.prototype, "readonly", void 0);
__decorate([
    property() // Whether the selected dashboard has been changed or not.
], OrDashboardTree.prototype, "hasChanged", void 0);
__decorate([
    property()
], OrDashboardTree.prototype, "showControls", void 0);
OrDashboardTree = __decorate([
    customElement('or-dashboard-tree')
], OrDashboardTree);
export { OrDashboardTree };
//# sourceMappingURL=or-dashboard-tree.js.map