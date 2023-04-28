import { css, html } from "lit";
import { customElement, property, state } from "lit/decorators.js";
import "@openremote/or-alarm-viewer";
import { ViewerConfig } from "@openremote/or-alarm-viewer";
import { Page, PageProvider } from "@openremote/or-app";
import { AppStateKeyed } from "@openremote/or-app";
import { Store } from "@reduxjs/toolkit";
import { AlarmAssetLink, AlarmUserLink, SentAlarm, ClientRole, UserQuery  } from "@openremote/model";
import { manager } from "@openremote/core";
import i18next from "i18next";
import { Asset, User } from "@openremote/model";
import { showSnackbar } from "@openremote/or-mwc-components/or-mwc-snackbar";
import { GenericAxiosResponse, isAxiosError } from "@openremote/rest";

export interface PageAlarmsConfig {
  viewer?: ViewerConfig;
}

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
  alarmUserLinks?: AlarmUserLink[]; 
}

@customElement("page-alarms")
export class PageAlarms extends Page<AppStateKeyed> {
  static get styles() {
    // language=CSS
    return css`
      :host {
        flex: 1;
        width: 100%;
      }

      or-alarm-viewer {
        width: 100%;
      }
    `;
  }

  @property()
  public config?: PageAlarmsConfig;

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

  constructor(store: Store<AppStateKeyed>) {
    super(store);
  }

  public stateChanged(state: AppStateKeyed) {}

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

  protected render() {
    return html` <or-alarm-viewer .config="${this.config?.viewer}"></or-alarm-viewer> `;
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
}
