import {css, html, TemplateResult, unsafeCSS, CSSResult} from "lit";
import {customElement, query, state} from "lit/decorators.js";
import manager, {DefaultColor3} from "@openremote/core";
import "@openremote/or-components/or-panel";
import "@openremote/or-translate";
import {Store} from "@reduxjs/toolkit";
import {Page, PageProvider, AppStateKeyed} from "@openremote/or-app";
import {when} from "lit/directives/when.js";
import {until} from "lit/directives/until.js";
import {guard} from "lit/directives/guard.js";
import {i18next} from "@openremote/or-translate";
import {InputType, OrInputChangedEvent, OrMwcInput} from "@openremote/or-mwc-components/or-mwc-input";
import {ClientRole, Role, User, UserAssetLink, UserQuery} from "@openremote/model";
import {showSnackbar} from "@openremote/or-mwc-components/or-mwc-snackbar";

export function pageAccountProvider(store: Store<AppStateKeyed>): PageProvider<AppStateKeyed> {
    return {
        name: "account",
        routes: [
            "/account"
        ],
        pageCreator: () => new PageAccount(store)
    };
}

interface UserModel extends User {
    password?: string;
    loaded?: boolean;
    loading?: boolean;
    previousRoles?: Role[];
    roles?: Role[];
    previousRealmRoles?: Role[];
    realmRoles?: Role[];
    previousAssetLinks?: UserAssetLink[];
    userAssetLinks?: UserAssetLink[];
}

@customElement("page-account")
export class PageAccount extends Page<AppStateKeyed>  {

    protected static _getStyle(): CSSResult {
        return css`
            #wrapper {
                height: 100%;
                width: 100%;
                display: flex;
                flex-direction: column;
            }
            #title {
                padding: 0 20px;
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
            .panel {
                flex: 0;
                width: 100%;
                box-sizing: border-box;
                max-width: 1360px;
                background-color: white;
                border: 1px solid #e5e5e5;
                border-radius: 5px;
                position: relative;
                margin: 0 auto 10px;
                padding: 12px 24px 24px;
                display: flex;
                flex-direction: column;
            }

            .panel-title {
                display: flex;
                text-transform: uppercase;
                font-weight: bolder;
                color: var(--or-app-color3, ${unsafeCSS(DefaultColor3)});
                line-height: 1em;
                margin-bottom: 10px;
                margin-top: 0;
                flex: 0 0 auto;
                letter-spacing: 0.025em;
                align-items: center;
                min-height: 36px;
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
                flex: 1 1 0;
                gap: 24px;
            }

            .column {
                display: flex;
                flex-direction: column;
                margin: 0px;
                flex: 1 1 0;
                gap: 20px;
            }
            
            h5 {
                margin-bottom: 0;
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
                }
            }
        `;
    }

    @state()
    protected _user?: UserModel;

    @state()
    protected _dirty = false;

    @state()
    protected _invalid = false;

    @query("#password")
    protected _passwordElem?: OrMwcInput;

    @query("#repeatPassword")
    protected _repeatPasswordElem?: OrMwcInput;

    static get styles() {
        return this._getStyle();
    }

    get name(): string {
        return "account";
    }

    public stateChanged(_state: AppStateKeyed) {
    }

    protected render(): TemplateResult | void {

        if (!manager.authenticated) {
            return html`
                <or-translate value="notAuthenticated"></or-translate>
            `;
        }

        if (!manager.isKeycloak()) {
            return html`
                <or-translate value="notSupported"></or-translate>
            `;
        }

        const readonly = !manager.hasRole(ClientRole.WRITE_ADMIN);

        return html`
            <div id="wrapper">
                <div id="title">
                    <or-icon icon="account"></or-icon>
                    <or-translate value="account">
                </div>
                <div id="content" class="panel">
                    
                    <p class="panel-title">${i18next.t("user")} ${i18next.t("settings")}</p>
                    
                    <!-- Account settings row -->
                    ${guard([this._user], () => until(
                        this._getAccountRowTemplate(this._user, readonly, (_user, dirty, invalid) => {
                            this._dirty = dirty;
                            this._invalid = invalid;
                        })
                    ))}
                    
                    <!-- Actions row (such as the save button) -->
                    ${when(this._user, () => until(this._getActionsRowTemplate(this._user)))}
                    
                </div>
                
                ${when(manager.isKeycloak(), () => html`
                    <div class="panel">
                        <p class="panel-title">
                            <or-translate value="twoFactorAuth"></or-translate>
                        </p>
                        <div class="row">
                            <div class="column">
                                <or-mwc-input .type="${InputType.BUTTON}" label="${i18next.t('twoFactorConfigure')}" outlined
                                              @or-mwc-input-changed="${() => manager.login({ action: "CONFIGURE_TOTP" })}"
                                ></or-mwc-input>
                            </div>
                        </div>
                    </div>
                `)}
            </div>
        `;
    }

    /**
     * Asynchronous function that returns the account settings template.
     * Will call {@link onchange} when an input field submits a new value.
     */
    protected async _getAccountRowTemplate(user?: UserModel, readonly = true, onchange?: (user: User, dirty: boolean, invalid: boolean) => void): Promise<TemplateResult> {
        const registrationEmailAsUsername = true;
        if(!user) {
            user = await this._getUser();
        }
        return html`
            <div class="row">
                <div class="column">
                    <!-- user details -->
                    <h5>${i18next.t("details")}</h5>
                    <or-mwc-input id="username" class="validate"
                                  .label="${i18next.t("username")}"
                                  .type="${InputType.TEXT}"
                                  ?required="${!registrationEmailAsUsername}"
                                  .disabled="${true}"
                                  minLength="3" maxLength="255" pattern="[A-Za-z0-9\\-_]+"
                                  .validationMessage="${i18next.t("invalidUsername")}"
                                  .value="${user?.username}"
                                  @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
                                      user.username = e.detail.value;
                                      onchange?.(user, true, this._isInvalid());
                                  }}"
                    ></or-mwc-input>
                    <!-- if identity provider is set to use email as username, make it required -->
                    <or-mwc-input id="email" class="validate"
                                  .label="${i18next.t("email")}"
                                  .type="${InputType.EMAIL}"
                                  ?required="${registrationEmailAsUsername}"
                                  ?readonly="${(!!user?.id && registrationEmailAsUsername) || readonly}"
                                  .disabled="${!user || (!!user?.id && registrationEmailAsUsername)}"
                                  pattern="^[\\w\\.\\-]+@([\\w\\-]+\\.)+[\\w]{2,4}$"
                                  .validationMessage="${i18next.t("invalidEmail")}"
                                  .value="${user?.email}"
                                  @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
                                      user.email = e.detail.value;
                                      onchange?.(user, true, this._isInvalid());
                                  }}"
                    ></or-mwc-input>
                    <or-mwc-input id="firstName" class="validate"
                                  .label="${i18next.t("firstName")}"
                                  .type="${InputType.TEXT}"
                                  ?readonly="${readonly}"
                                  .disabled="${!user}"
                                  minLength="5"
                                  .value="${user?.firstName}"
                                  @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
                                      user.firstName = e.detail.value;
                                      onchange?.(user, true, this._isInvalid());
                                  }}"
                    ></or-mwc-input>
                    <or-mwc-input id="surname" class="validate"
                                  .label="${i18next.t("surname")}"
                                  .type="${InputType.TEXT}"
                                  ?readonly="${readonly}"
                                  .disabled="${!user}"
                                  minLength="1"
                                  .value="${user?.lastName}"
                                  @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
                                      user.lastName = e.detail.value;
                                      onchange?.(user, true, this._isInvalid());
                                  }}">
                    </or-mwc-input>
                </div>
                <div class="column">
                    <h5>${i18next.t("password")}</h5>
                    <or-mwc-input id="password" class="validate"
                                  .label="${i18next.t("password")}"
                                  .type="${InputType.PASSWORD}"
                                  ?readonly="${readonly}"
                                  .disabled="${!user}"
                                  min="1"
                                  @or-mwc-input-changed="${(_e: OrInputChangedEvent) => {
                                      const changed = this._onPasswordChanged(user);
                                      onchange?.(user, changed, this._isInvalid());
                                  }}"
                    ></or-mwc-input>
                    <or-mwc-input id="repeatPassword"
                                  .label="${i18next.t("repeatPassword")}"
                                  .type="${InputType.PASSWORD}"
                                  helperPersistent
                                  ?readonly="${readonly}"
                                  .disabled="${!user}"
                                  min="1"
                                  @or-mwc-input-changed="${(_e: OrInputChangedEvent) => {
                                      const changed = this._onPasswordChanged(user);
                                      onchange?.(user, changed, this._isInvalid());
                                  }}"
                    ></or-mwc-input>
                </div>
            </div>
        `;
    }

    /**
     * Asynchronous function that returns the actions template, such as the 'save' button.
     */
    protected async _getActionsRowTemplate(user: UserModel): Promise<TemplateResult> {
        const invalid = this._isInvalid();
        const dirty = this._isDirty();
        return html`
            <div class="row" style="justify-content: end; margin-top: 20px;">
                <or-mwc-input id="savebtn" style="margin: 0;" raised
                              .label="${i18next.t(user.id ? "save" : "create")}"
                              .type="${InputType.BUTTON}"
                              .disabled="${invalid || !dirty}"
                              @or-mwc-input-changed="${(e: OrInputChangedEvent) => this._onSaveBtnClick(e)}"
                ></or-mwc-input>
            </div>
        `;
    }

    /**
     * HTML callback function when any of the password input fields change.
     * Checks whether {@link _passwordElem} and {@link _repeatPasswordElem} are the same,
     * and updates the {@link user} object with the new password.
     */
    protected _onPasswordChanged(user: UserModel): boolean {
        const password = this._passwordElem?.nativeValue;
        const repeatPassword = this._repeatPasswordElem?.nativeValue;
        if(password && repeatPassword) {

            if(password !== repeatPassword) {
                const error = i18next.t("passwordMismatch");
                this._repeatPasswordElem?.setCustomValidity(error);
                user.password = "";

            } else {
                this._repeatPasswordElem?.setCustomValidity(undefined);
                user.password = password;
                return true;
            }
        } else {
            console.warn("Could not update password; some fields are empty;", password, repeatPassword);
        }
        return false;
    }

    /**
     * HTML callback function for when the 'Save' button is clicked.
     * Checks if the input fields are valid, and calls the {@link _updateUser} function.
     */
    protected _onSaveBtnClick(_e: OrInputChangedEvent) {
        if(this._user && !this._isInvalid()) {
            this._updateUser(this._user);
        } else {
            console.warn("The fields are invalid!");
            showSnackbar(undefined, "saveUserFailed");
        }
    }

    /**
     * Function that returns whether the input fields are all valid.
     * Listens for HTML elements with the 'validate' class.
     */
    protected _isInvalid(): boolean {
        const validateArray = this.shadowRoot.querySelectorAll(".validate");
        if(validateArray.length === 0) {
            return true;
        }
        return Array.from(validateArray).filter(e => e instanceof OrMwcInput).some(input => !(input as OrMwcInput).valid);
    }

    /**
     * Function that returns if the input fields are 'dirty'. (if they have been changed by the user)
     */
    protected _isDirty(): boolean {
        return this._dirty;
    }

    /**
     * Function that fetches (and returns) the currently logged in {@link UserModel} from the Manager HTTP API.
     */
    protected async _getUser(): Promise<UserModel> {
        if(!this._user) {
            try {
                const usersResponse = await manager.rest.api.UserResource.query({
                    realmPredicate: {name: manager.getRealm()},
                    usernames: [{predicateType: "string", value: manager.username}],
                    limit: 1
                } as UserQuery);

                if(usersResponse.status < 200 || usersResponse.status > 299) {
                    throw new Error(usersResponse.statusText);
                }
                if(!usersResponse.data || usersResponse.data.length === 0) {
                    throw new Error("No user could be found.");
                }
                this._user = usersResponse.data[0];

            } catch (e) {
                console.error(e);
                showSnackbar(undefined, "errorOccurred");
            }
        }
        return this._user;
    }

    /**
     * Function that updates the {@link UserModel} through the Manager HTTP API.
     * If the password has been changed, it will, after updating the user, also request to reset the password.
     */
    protected async _updateUser(user: UserModel): Promise<void> {
        await manager.rest.api.UserResource.update(manager.getRealm(), user).then(() => {
            if(user.password) {
                const credentials = {value: user.password};
                manager.rest.api.UserResource.resetPassword(manager.getRealm(), user.id, credentials);
            }
            this._dirty = false;
            showSnackbar(undefined, "saveUserSucceeded");

        }).catch(e => {
            console.error(e);
            showSnackbar(undefined, "saveUserFailed");
        });
    }
}
