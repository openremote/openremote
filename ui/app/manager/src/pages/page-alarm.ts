import { css, html, PropertyValues, TemplateResult, unsafeCSS } from "lit";
import { customElement, property, state } from "lit/decorators.js";
import manager, { DefaultColor3, DefaultColor4 } from "@openremote/core";
import "@openremote/or-components/or-panel";
import "@openremote/or-translate";
import { Store } from "@reduxjs/toolkit";
import { AppStateKeyed, Page, PageProvider, router } from "@openremote/or-app";
import {
  AlarmSeverity,
  AlarmStatus,
  ClientRole,
  Role,
  SentAlarm,
  UserQuery,
  Asset,
  User,
  AlarmAssetLink,
  AlarmUserLink,
} from "@openremote/model";
import { i18next } from "@openremote/or-translate";
import { InputType, OrInputChangedEvent, OrMwcInput } from "@openremote/or-mwc-components/or-mwc-input";
import { OrMwcDialog, showOkCancelDialog, showDialog } from "@openremote/or-mwc-components/or-mwc-dialog";
import { showSnackbar } from "@openremote/or-mwc-components/or-mwc-snackbar";
import { GenericAxiosResponse, isAxiosError } from "@openremote/rest";
import { getAlarmsRoute } from "../routes";
import { when } from "lit/directives/when.js";
import { until } from "lit/directives/until.js";
import { OrMwcTableRowClickEvent, TableColumn, TableRow } from "@openremote/or-mwc-components/or-mwc-table";
import { OrAssetTreeRequestSelectionEvent } from "@openremote/or-asset-tree";

const tableStyle = require("@material/data-table/dist/mdc.data-table.css");

export function pageAlarmsProvider(store: Store<AppStateKeyed>): PageProvider<AppStateKeyed> {
  return {
    name: "alarms",
    routes: ["alarms", "alarms/:id"],
    pageCreator: () => {
      return new PageAlarms(store);
    },
  };
}

interface AlarmModel extends SentAlarm {
  loaded?: boolean;
  loading?: boolean;
  alarmAssetLinks?: AlarmAssetLink[];
  alarmUserLinks?: AlarmUserLink[]; // To change data type
}

@customElement("page-alarms")
export class PageAlarms extends Page<AppStateKeyed> {
  static get styles() {
    // language=CSS
    return [
      unsafeCSS(tableStyle),
      css`
                <!-- #table-container {
                    height: 100%;
                    overflow: auto;
                    width: 70vw;
                }

                #table {
                    width: 100%;
                    margin-bottom: 10px;
                }

                #table > table {
                    width: 100%;
                    table-layout: fixed;
                }

                #table th, #table td {
                    word-wrap: break-word;
                    white-space: pre-wrap;
                }

                .mdc-data-table__cell[title="HIGH"] {
                    color: red;
                }
                  
                td.mdc-data-table__cell.mdc-data-table__cell--clickable span[value="LOW"] {
                    color: green; !important
                }
                  
                .mdc-data-table__cell[title="MEDIUM"] {
                    color: orange;
                } -->

                /* CSS styles for the data table */
.data-table {
  overflow-x: auto;
  max-width: 100%;
}

.data-table table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th,
.data-table td {
  padding: 12px 16px;
  text-align: left;
  border-bottom: 1px solid #ddd;
}

.data-table th {
  background-color: #f8f8f8;
}


                #wrapper {
                    height: 100%;
                    width: 100%;
                    display: flex;
                    flex-direction: column;
                    overflow: auto;
                }

                #title {
                    padding: 0;
                    font-size: 18px;
                    font-weight: bold;
                    width: calc(100% - 40px);
                    max-width: 1360px;
                    margin: 20px auto;
                    align-items: center;
                    display: flex;
                    color: var(--or-app-color3, ${unsafeCSS(DefaultColor3)});
                }

                #title or-icon {
                    margin-right: 10px;
                    margin-left: 14px;
                }

                #content {
                    width: 100%;
                }

                .panel {
                    width: calc(100% - 90px);
                    max-width: 1310px;
                    background-color: white;
                    border: 1px solid #e5e5e5;
                    border-radius: 5px;
                    position: relative;
                    margin: 0 auto 10px;
                    padding: 12px 24px 24px;
                }

                .panel-title {
                    text-transform: uppercase;
                    font-weight: bolder;
                    color: var(--or-app-color3, ${unsafeCSS(DefaultColor3)});
                    line-height: 1em;
                    margin-bottom: 10px;
                    margin-top: 0;
                    flex: 0 0 auto;
                    letter-spacing: 0.025em;
                    display: flex;
                    align-items: center;
                    min-height: 36px;
                }

                or-mwc-input {
                    margin-bottom: 20px;
                }

                or-icon {
                    vertical-align: middle;
                    --or-icon-width: 20px;
                    --or-icon-height: 20px;
                    margin-right: 2px;
                    margin-left: -5px;
                }

                .row {
                    display: flex;
                    flex-direction: row;
                    margin: 10px 0;
                    flex: 1 1 0;
                    gap: 24px;
                }
                
                .column {
                    display: flex;
                    flex-direction: column;
                    margin: 0px;
                    flex: 1 1 0;
                }

                .hidden {
                    display: none;
                }

                .breadcrumb-container {
                    padding: 0 20px;
                    width: calc(100% - 40px);
                    max-width: 1360px;
                    margin: 12px auto 0;
                    display: flex;
                    align-items: center;
                }

                .breadcrumb-clickable {
                    cursor: pointer;
                    color: ${unsafeCSS(DefaultColor4)}
                }

                .breadcrumb-arrow {
                    margin: 0 5px -3px 5px;
                    --or-icon-width: 16px;
                    --or-icon-height: 16px;
                }

                @media screen and (max-width: 768px) {
                    #title {
                        padding: 0;
                        width: 100%;
                    }

                    .row {
                        display: block;
                        flex-direction: column;
                        gap: 0;
                    }

                    .panel {
                        border-radius: 0;
                        border-left: 0px;
                        border-right: 0px;
                        width: calc(100% - 48px);
                    }
                }
            `,
    ];
  }

  @property()
  public realm?: string;
  @property()
  public alarmId?: string;
  @property()
  public creationState?: {
    alarmModel: AlarmModel;
  };

  @state()
  protected _alarms: AlarmModel[] = [];
  @state()
  protected _linkedAssets: Asset[] = [];
  @state()
  protected _linkedUsers: User[] = [];
  @state()
  protected _loadedUsers: User[] = [];

  protected _loading: boolean = false;
  protected _assign: boolean = false;
  protected _userId?: string;

  @state()
  protected _loadAlarmsPromise?: Promise<any>;

  @state()
  protected _saveAlarmPromise?: Promise<any>;

  @state()
  protected _loadUsersPromise?: Promise<any>;

  get name(): string {
    return "alarm.alarm_plural";
  }

  public shouldUpdate(changedProperties: PropertyValues): boolean {
    if (changedProperties.has("realm") && changedProperties.get("realm") != undefined) {
      this.reset();
      this.loadAlarms();
    }
    if (changedProperties.has("alarmId")) {
      this._updateRoute();
    }
    return super.shouldUpdate(changedProperties);
  }

  public connectedCallback() {
    super.connectedCallback();
    this.loadAlarms();
  }

  public disconnectedCallback() {
    super.disconnectedCallback();
  }

  protected responseAndStateOK(
    stateChecker: () => boolean,
    response: GenericAxiosResponse<any>,
    errorMsg: string
  ): boolean {
    if (!stateChecker()) {
      return false;
    }

    if (!response.data) {
      showSnackbar(undefined, errorMsg, i18next.t("dismiss"));
      console.error(errorMsg + ": response = " + response.statusText);
      return false;
    }

    return true;
  }

  protected async loadAlarms(): Promise<void> {
    this._loadAlarmsPromise = this.fetchAlarms();
    this._loadAlarmsPromise.then(() => {
      this._loadAlarmsPromise = undefined;
    });
    return this._loadAlarmsPromise;
  }

  protected async fetchAlarms(): Promise<void> {
    if (!this.realm || this._loading || !this.isConnected) {
      return;
    }

    this._loading = true;

    this._alarms = [];
    this._linkedUsers = [];
    this._linkedAssets = [];
    this._loadedUsers = [];

    if (!manager.authenticated || !manager.hasRole(ClientRole.READ_ALARMS)) {
      console.warn("Not authenticated or insufficient access");
      return;
    }

    // After async op check that the response still matches current state and that the component is still loaded in the UI
    const stateChecker = () => {
      return this.getState().app.realm === this.realm && this.isConnected;
    };

    const usersResponse = await manager.rest.api.UserResource.query({
      realmPredicate: { name: manager.displayRealm },
    } as UserQuery);

    if (!this.responseAndStateOK(stateChecker, usersResponse, i18next.t("loadFailedUsers"))) {
      return;
    }

    const roleResponse = await manager.rest.api.UserResource.getRoles(manager.displayRealm);

    if (!this.responseAndStateOK(stateChecker, roleResponse, i18next.t("loadFailedRoles"))) {
      return;
    }

    const alarmResponse = await manager.rest.api.AlarmResource.getAlarms(null);

    if (!this.responseAndStateOK(stateChecker, alarmResponse, i18next.t("TODO"))) {
      return;
    }

    this._alarms = alarmResponse.data;
    this._loadedUsers = usersResponse.data.filter((user) => !user.serviceAccount);
    this._loading = false;
  }

  protected async _createUpdateAlarm(alarm: AlarmModel, action: "update" | "create") {
    if (!alarm.title || !alarm.content) {
      return;
    }

    if (alarm.content === "" || alarm.title === "") {
      // Means a validation failure shouldn't get here
      return;
    }

    const isUpdate = !!alarm.id;
    if (!isUpdate) {
      alarm.realm = manager.getRealm();
    }

    try {
      action == "update"
        ? await manager.rest.api.AlarmResource.updateAlarm(alarm.id, alarm)
        : await manager.rest.api.AlarmResource.createAlarm(alarm);
    } catch (e) {
      if (isAxiosError(e)) {
        console.error(
          (isUpdate ? "save alarm failed" : "create alarm failed") + ": response = " + e.response.statusText
        );

        if (e.response.status === 400) {
          showSnackbar(
            undefined,
            i18next.t(isUpdate ? "alarm.saveAlarmFailed" : "alarm.createAlarmFailed"),
            i18next.t("dismiss")
          );
        } else if (e.response.status === 403) {
          showSnackbar(undefined, i18next.t("alarm.alarmAlreadyExists"));
        }
      }
      throw e; // Throw exception anyhow to handle individual cases
    } finally {
      await this.loadAlarms();
    }
  }

  private _deleteAlarm(alarm) {
    showOkCancelDialog(i18next.t("delete"), i18next.t("alarm.deleteAlarmConfirm"), i18next.t("delete")).then((ok) => {
      if (ok) {
        this.doDelete(alarm);
      }
    });
  }

  private doDelete(alarm) {
    // manager.rest.api.UserResource.delete(manager.displayRealm, user.id).then(response => {
    //     if (user.serviceAccount) {
    //         this._serviceUsers = [...this._serviceUsers.filter(u => u.id !== user.id)];
    //         this.reset();
    //     } else {
    //         this._users = [...this._users.filter(u => u.id !== user.id)];
    //         this.reset();
    //     }
    // })
  }

  protected render(): TemplateResult | void {
    if (!manager.authenticated) {
      return html` <or-translate value="notAuthenticated"></or-translate> `;
    }

    const readonly = !manager.hasRole(ClientRole.WRITE_ALARMS);

    // Content of Alarm Table
    const alarmTableColumns: TableColumn[] = [
      { title: i18next.t("createdOn") },
      { title: i18next.t("alarm.title") },
      { title: i18next.t("alarm.content") },
      { title: i18next.t("alarm.severity") },
      { title: i18next.t("status") },
    ];

    const activeAlarmTableRows: TableRow[] = this._alarms.map((alarm) => {
      return {
        content: [new Date(alarm.createdOn).toLocaleString(), alarm.title, alarm.content, alarm.severity, alarm.status],
        clickable: true,
      };
    });

    // Configuration
    const tableConfig = {
      columnFilter: [],
      stickyFirstColumn: false,
      pagination: {
        enable: true,
      },
    };

    const index: number | undefined = this.alarmId
      ? this._alarms.findIndex((alarm) => alarm.id.toString() == this.alarmId)
      : undefined;

    return html`
      <div id="table-container" style="max-width: 90%; margin: auto;">
        <!-- Breadcrumb on top of the page-->
        ${when(
          this.alarmId && index != undefined,
          () => html`
            <div class="breadcrumb-container">
              <span class="breadcrumb-clickable" @click="${() => this.reset()}"
                >${i18next.t("alarm.alarm_plural")}</span
              >
              <or-icon class="breadcrumb-arrow" icon="chevron-right"></or-icon>
              <span style="margin-left: 2px;"
                >${index != undefined ? this._alarms[index]?.title : i18next.t("alarm.creatingAlarm")}</span
              >
            </div>
          `
        )}

        <div id="title" style="justify-content: space-between;">
          <div>
            <or-icon icon="alert-outline"></or-icon>
            <span>
              ${this.alarmId && index != undefined ? this._alarms[index]?.title : i18next.t("alarm.alarm_plural")}
            </span>
          </div>
          <div class="${this.creationState ? "hidden" : "panel-title"} style="justify-content: flex-end;">
            <or-mwc-input
              style="margin: 0;"
              type="${InputType.BUTTON}"
              icon="plus"
              label="${i18next.t("add")} ${i18next.t("alarm.")}"
              @or-mwc-input-changed="${() => (this.creationState = { alarmModel: this.getNewAlarmModel() })}"
            ></or-mwc-input>
          </div>
        </div>

        <!-- Alarm Specific page -->
        ${when(
          (this.alarmId && index != undefined) || this.creationState,
          () => html`
            ${when(this._alarms[index] != undefined || this.creationState, () => {
              const alarm: AlarmModel = index != undefined ? this._alarms[index] : this.creationState.alarmModel;
              return html`
                <div id="content" class="panel">
                  <p class="panel-title">${i18next.t("alarm.")} ${i18next.t("settings")}</p>
                  ${this.getSingleAlarmView(alarm, readonly || this._loadAlarmsPromise != undefined)}
                </div>
              `;
            })}
          `,
          () =>
            html`<!-- List of Alarms page -->
              <div class="panel">${this._getTable()}</div> `
        )}
      </div>
    `;
  }

  protected async getAlarmsTable(
    columns: TemplateResult | TableColumn[] | string[],
    rows: TemplateResult | TableRow[] | string[][],
    config: any,
    onRowClick: (event: OrMwcTableRowClickEvent) => void,
    type: string
  ): Promise<TemplateResult> {
    switch (type) {
      case "alarm":
        if (this._loadAlarmsPromise) {
          await this._loadAlarmsPromise;
        }
        break;
      case "user":
        if (this._loadUsersPromise) {
          await this._loadUsersPromise;
        }
        break;
      default:
        break;
    }

    return html`
      <or-mwc-table
        .columns="${columns instanceof Array ? columns : undefined}"
        .columnsTemplate="${!(columns instanceof Array) ? columns : undefined}"
        .rows="${rows instanceof Array ? rows : undefined}"
        .rowsTemplate="${!(rows instanceof Array) ? rows : undefined}"
        .config="${config}"
        @or-mwc-table-row-click="${rows instanceof Array ? onRowClick : undefined}"
      ></or-mwc-table>
    `;
  }

  protected getNewAlarmModel(): AlarmModel {
    return {
      alarmAssetLinks: [],
      alarmUserLinks: [],
      loaded: true,
    };
  }

  protected async loadAlarm(alarm: AlarmModel) {
    if (alarm.loaded) {
      return;
    }
    const stateChecker = () => {
      return this.getState().app.realm === this.realm && this.isConnected;
    };

    const alarmAssetLinksResponse = await manager.rest.api.AlarmResource.getAssetLinks(alarm.id, alarm.realm);
    if (!this.responseAndStateOK(stateChecker, alarmAssetLinksResponse, i18next.t("loadFailedUsers"))) {
      return;
    }

    const alarmUserLinksResponse = await manager.rest.api.AlarmResource.getUserLinks(alarm.id, alarm.realm);
    if (!this.responseAndStateOK(stateChecker, alarmUserLinksResponse, i18next.t("loadFailedUsers"))) {
      return;
    }

    alarm.alarmUserLinks = alarmUserLinksResponse.data;
    alarm.alarmAssetLinks = alarmAssetLinksResponse.data;
    alarm.loaded = true;
    alarm.loading = false;

    // Update the dom
    this.requestUpdate();
  }

  protected _openAssetSelector(ev: MouseEvent, alarm: AlarmModel, readonly: boolean) {
    const openBtn = ev.target as OrMwcInput;
    openBtn.disabled = true;

    const dialog = showDialog(
      new OrMwcDialog()
        .setHeading(i18next.t("linkedAssets"))
        .setContent(
          html`
            <or-asset-tree
              id="chart-asset-tree"
              readonly="true"
              .selectedIds="${alarm.alarmAssetLinks?.map((al) => al.id.assetId)}"
              .showSortBtn="${false}"
              expandNodes
              checkboxes
              @or-asset-tree-request-selection="${(e: OrAssetTreeRequestSelectionEvent) => {
                e.detail.allow = false;
              }}"
            ></or-asset-tree>
          `
        )
        .setActions([
          {
            default: true,
            actionName: "cancel",
            content: i18next.t("cancel"),
            action: () => {
              openBtn.disabled = false;
            },
          },
        ])
        .setDismissAction({
          actionName: "cancel",
          action: () => {
            openBtn.disabled = false;
          },
        })
    );
  }

  protected onAlarmChanged(e: OrInputChangedEvent | OrMwcInput) {
    // Don't have form-associated custom element support in lit at time of writing which would be the way to go here
    const formElement = e instanceof OrInputChangedEvent ? (e.target as HTMLElement).parentElement : e.parentElement;
    const saveBtn = this.shadowRoot.getElementById("savebtn") as OrMwcInput;

    if (formElement) {
      const saveDisabled = Array.from(formElement.children)
        .filter((e) => e instanceof OrMwcInput)
        .some((input) => !(input as OrMwcInput).valid);
      saveBtn.disabled = saveDisabled;
    }
  }

  protected _onRowClick(id: number) {
    this.alarmId = id.toString();
    this.requestUpdate();
  }

  protected _assignClick() {
    this._assign = !this._assign;
    this.requestUpdate();
  }

  protected async _assignUser(alarm: AlarmModel) {
    if (!this._userId || !this.alarmId) {
      return;
    }
    try {
      await manager.rest.api.AlarmResource.assignUser(alarm.id, this._userId);
    } catch (e) {
      if (isAxiosError(e)) {
        console.error("save alarm failed" + ": response = " + e.response.statusText);

        if (e.response.status === 400) {
          showSnackbar(undefined, i18next.t("alarm.saveAlarmFailed"), i18next.t("dismiss"));
        }
      }
      throw e; // Throw exception anyhow to handle individual cases
    } finally {
      await this.loadAlarm(alarm);
    }
  }

  protected getSingleAlarmView(alarm: AlarmModel, readonly: boolean = true): TemplateResult {
    return html`
      ${when(
        alarm.loaded,
        () => {
          return this.getSingleAlarmTemplate(alarm, readonly);
        },
        () => {
          const getTemplate = async () => {
            await this.loadAlarm(alarm);
            return this.getSingleAlarmTemplate(alarm, readonly);
          };
          const content: Promise<TemplateResult> = getTemplate();
          return html` ${until(content, html`${i18next.t("loading")}`)} `;
        }
      )}
    `;
  }

  protected getSingleAlarmTemplate(alarm: AlarmModel, readonly: boolean = true): TemplateResult {
    // Configuration
    const tableConfig = {
      columnFilter: [],
      stickyFirstColumn: false,
    };

    // Content of User Table
    const userTableColumns: TableColumn[] = [{ title: i18next.t("username") }];

    const linkedUserTableRows: TableRow[] = this._linkedUsers.map((user) => {
      return {
        content: [user.username],
        clickable: true,
      };
    });

    return html`
            <div class="row">
                <div class="column">
                    <or-mwc-input ?readonly="${true}"
                                  .label="${i18next.t("createdOn")}"
                                  .type="${InputType.DATETIME}" 
                                  .value="${new Date(alarm.createdOn)}"
                                  }}"></or-mwc-input>
                    
                    <h5>${i18next.t("details")}</h5>
                    <!-- alarm details -->
                    <or-mwc-input ?readonly="${readonly}"
                                  class="${false ? "hidden" : ""}"
                                  .label="${i18next.t("alarm.title")}"
                                  .type="${InputType.TEXT}" 
                                  .value="${alarm.title}"
                                  @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
                                    alarm.title = e.detail.value;
                                    this.onAlarmChanged(e);
                                  }}"></or-mwc-input>
                    <or-mwc-input ?readonly="${readonly}"
                                  class="${false ? "hidden" : ""}"
                                  .label="${i18next.t("alarm.content")}"
                                  .type="${InputType.TEXTAREA}" 
                                  .value="${alarm.content}"
                                  @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
                                    alarm.content = e.detail.value;
                                    this.onAlarmChanged(e);
                                  }}"></or-mwc-input>
                    <or-mwc-input ?readonly="${readonly}"
                                  class="${false ? "hidden" : ""}"
                                  .label="${i18next.t("alarm.severity")}"
                                  .type="${InputType.SELECT}"
                                  .options="${[AlarmSeverity.LOW, AlarmSeverity.MEDIUM, AlarmSeverity.HIGH]}"
                                  .value="${alarm.severity}"
                                  @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
                                    alarm.severity = e.detail.value;
                                    this.onAlarmChanged(e);
                                  }}"></or-mwc-input>
                    <or-mwc-input ?readonly="${readonly}"
                                  class="${false ? "hidden" : ""}"
                                  .label="${i18next.t("alarm.status")}"
                                  .type="${InputType.SELECT}"
                                  .options="${[
                                    AlarmStatus.ACTIVE,
                                    AlarmStatus.ACKNOWLEDGED,
                                    AlarmStatus.INACTIVE,
                                    AlarmStatus.RESOLVED,
                                  ]}"
                                  .value="${alarm.status}"
                                  @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
                                    alarm.status = e.detail.value;
                                    this.onAlarmChanged(e);
                                  }}"></or-mwc-input>
                </div>
                
                <div class="${this.creationState ? "hidden" : "column"}">
                    <div class="${!this._assign ? "hidden" : ""}">
                        <or-mwc-input type="${InputType.SELECT}" 
                                    .options="${
                                      this._loadedUsers ? this._loadedUsers.map((user) => user.username) : undefined
                                    }"
                                    .label="${i18next.t("user_plural")}"
                                    .value="${this._loadedUsers.map((user) => user.username)[0]}"
                                    @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
                                      this._userId = this._loadedUsers.find(
                                        (user) => user.username == e.detail.value
                                      ).id;
                                      this.requestUpdate;
                                    }}" 
                                    ?hidden="${!this._assign}">
                        </or-mwc-input>
                        <or-mwc-input raised outlined style="margin-left: 4px;"
                                    .type="${InputType.BUTTON}"
                                    .label="${i18next.t("confirm")}"
                                    @or-mwc-input-changed="${(ev: MouseEvent) => this._assignUser(alarm)}">
                        </or-mwc-input>
                    </div>
                    <div id="content" class="panel">
                        <div class="panel-title" style="justify-content: space-between;">
                            <p>${i18next.t("alarm.linkedUsers")}</p>
                            <or-mwc-input style="margin: 0;" type="${InputType.BUTTON}" icon="plus"
                                          label="${i18next.t("alarm.assignUser")}"
                                          @or-mwc-input-changed="${() => this._assignClick()}"></or-mwc-input>
                        </div>
                        ${until(
                          this.getAlarmsTable(
                            userTableColumns,
                            linkedUserTableRows,
                            tableConfig,
                            (ev) => {
                              this.alarmId = this._alarms[ev.detail.index].id.toString();
                            },
                            "user"
                          ),
                          html`${i18next.t("loading")}`
                        )}
                    </div>

                    <div>
                        <span style="margin: 0px auto 10px;">${i18next.t("linkedAssets")}:</span>
                        <or-mwc-input outlined ?disabled="${readonly}" style="margin-left: 4px;"
                                      .type="${InputType.BUTTON}"
                                      .label="${i18next.t("selectRestrictedAssets", {
                                        number: alarm.alarmAssetLinks?.length,
                                      })}"
                                      @or-mwc-input-changed="${(ev: MouseEvent) =>
                                        this._openAssetSelector(ev, alarm, readonly)}"></or-mwc-input>
                    </div>
                </div>

            </div>

                <!-- Bottom controls (save/update and delete button) -->
                ${when(
                  !(readonly && !this._saveAlarmPromise),
                  () => html`
                    <div class="row" style="margin-bottom: 0; justify-content: space-between;">
                      <div style="display: flex; align-items: center; gap: 16px; margin: 0 0 0 auto;">
                        <or-mwc-input
                          id="savebtn"
                          style="margin: 0;"
                          raised
                          ?disabled="${readonly}"
                          .label="${i18next.t(alarm.id ? "save" : "create")}"
                          .type="${InputType.BUTTON}"
                          @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
                            let error: { status?: number; text: string };
                            this._saveAlarmPromise = this._createUpdateAlarm(alarm, alarm.id ? "update" : "create")
                              .then(() => {
                                showSnackbar(undefined, i18next.t("alarm.saveAlarmSucceeded"));
                                this.reset();
                              })
                              .catch((ex) => {
                                if (isAxiosError(ex)) {
                                  error = {
                                    status: ex.response.status,
                                    text:
                                      ex.response.status == 403
                                        ? i18next.t("alarm.alarmAlreadyExists")
                                        : i18next.t("errorOccurred"),
                                  };
                                }
                              })
                              .finally(() => {
                                this._saveAlarmPromise = undefined;
                              });
                          }}"
                        >
                        </or-mwc-input>
                      </div>
                    </div>
                  `
                )}
        `;
  }

  protected _getTable(): TemplateResult {
    return html`
      <!-- HTML structure for the data table -->
      <div class="data-table">
        <table id="alarm-table">
          <thead>
            <tr>
              <th>${i18next.t("createdOn")}</th>
              <th>${i18next.t("alarm.severity")}</th>
              <th>${i18next.t("alarm.status")}</th>
              <th>${i18next.t("alarm.title")}</th>
              <th>${i18next.t("alarm.content")}</th>
            </tr>
          </thead>
          <tbody>
            ${this._alarms.map((ev) => {
              return html`
                <tr @click="">
                  <td>${new Date(ev.createdOn).toLocaleString()}</td>
                  <td>${ev.severity}</td>
                  <td>${ev.status}</td>
                  <td>${ev.title}</td>
                  <td>${ev.content}</td>
                </tr>
              `;
            })}
          </tbody>
        </table>
      </div>
    `;
  }

  // Function to populate the table with data
  protected _populateTable(dataList: AlarmModel[]) {
    // Get the table body element
    const tableBody = document.getElementById("tableBody");

    dataList.forEach((data) => {
      // Create a table row
      const row = document.createElement("tr");

      // Create table cells for each data item
      const createdOnCell = document.createElement("td");
      createdOnCell.textContent = new Date(data.createdOn).toLocaleString();
      row.appendChild(createdOnCell);

      const severityCell = document.createElement("td");
      severityCell.textContent = data.severity;
      row.appendChild(severityCell);

      const statusCell = document.createElement("td");
      statusCell.textContent = data.status;
      row.appendChild(statusCell);

      const titleCell = document.createElement("td");
      titleCell.textContent = data.title;
      row.appendChild(titleCell);

      const contentCell = document.createElement("td");
      contentCell.textContent = data.content;
      row.appendChild(contentCell);

      // Add a click event listener to the row
      row.addEventListener("click", () => {
        this.alarmId = this._alarms[row.rowIndex].id.toString();
      });

      // Append the row to the table body
      tableBody.appendChild(row);
    });
  }

  // Reset selected alarm and go back to the alarm overview
  protected reset() {
    this.alarmId = undefined;
    this.creationState = undefined;
    this._assign = false;
  }

  public stateChanged(state: AppStateKeyed) {
    if (state.app.page == "alarms") {
      this.realm = state.app.realm;
      this.alarmId = state.app.params && state.app.params.id ? state.app.params.id : undefined;
    }
  }

  protected _updateRoute(silent: boolean = false) {
    router.navigate(getAlarmsRoute(this.alarmId), {
      callHooks: !silent,
      callHandler: !silent,
    });
  }
}
