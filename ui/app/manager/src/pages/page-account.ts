import {css, html, TemplateResult, unsafeCSS, CSSResult} from "lit";
import {customElement, query, state} from "lit/decorators.js";
import manager, {DefaultColor3} from "@openremote/core";
import "@openremote/or-components/or-panel";
import "@openremote/or-translate";
import {Store} from "@reduxjs/toolkit";
import {Page, PageProvider, AppStateKeyed} from "@openremote/or-app";
import {when} from "lit/directives/when.js";
import {until} from "lit/directives/until.js";
import {map} from 'lit/directives/map.js';
import {guard} from "lit/directives/guard.js";
import {i18next} from "@openremote/or-translate";
import {InputType, OrInputChangedEvent, OrMwcInput} from "@openremote/or-mwc-components/or-mwc-input";
import {ClientRole, Credential, Role, User, UserAssetLink, UserQuery} from "@openremote/model";
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
export class PageAccount extends Page<AppStateKeyed> {

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

    @state()
    protected _passwordPolicy: string[] = [];

    @query("#new-password")
    protected _passwordElem?: OrMwcInput;

    @query("#new-repeatPassword")
    protected _repeatPasswordElem?: OrMwcInput;

    static get styles() {
        return this._getStyle();
    }

    get name(): string {
        return "account";
    }

    public stateChanged(_state: AppStateKeyed) {
    }

    public connectedCallback() {
        super.connectedCallback();
        this._getPasswordPolicy();
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

        const readonly = !manager.hasRole(ClientRole.WRITE_USER);

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
                                <or-mwc-input .type="${InputType.BUTTON}" label="${i18next.t('twoFactorConfigure')}"
                                              outlined
                                              @or-mwc-input-changed="${() => manager.login({action: "CONFIGURE_TOTP"})}"
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
        const realmResponse = await manager.rest.api.RealmResource.get(manager.displayRealm);
        const registrationEmailAsUsername = realmResponse.data.registrationEmailAsUsername;
        if (!user) {
            user = await this._getUser();
        }
        return html`
            <div class="row">
                <div class="column">
                    <!-- user details -->
                    <h5>${i18next.t("details")}</h5>
                    <or-mwc-input id="new-username" class="validate"
                                  .label="${i18next.t("username")}"
                                  .type="${InputType.TEXT}"
                                  ?required="${!registrationEmailAsUsername}"
                                  .disabled="${true}"
                                  minLength="3" maxLength="255" pattern="[A-Za-z0-9\\-_+@\\.ßçʊÇʊ]+"
                                  .validationMessage="${i18next.t("invalidUsername")}"
                                  .value="${user?.username}" autocomplete="false"
                                  @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
                                      user.username = e.detail.value;
                                      onchange?.(user, true, this._isInvalid());
                                  }}"
                    ></or-mwc-input>
                    <!-- if identity provider is set to use email as username, make it required -->
                    <or-mwc-input id="new-email" class="validate"
                                  .label="${i18next.t("email")}"
                                  .type="${InputType.EMAIL}"
                                  ?required="${registrationEmailAsUsername}"
                                  ?readonly="${(!!user?.id && registrationEmailAsUsername) || readonly}"
                                  .disabled="${!user || (!!user?.id && registrationEmailAsUsername)}"
                                  pattern="^[\\w\\.\\-]+@([\\w\\-]+\\.)+[\\w]{2,4}$"
                                  .validationMessage="${i18next.t("invalidEmail")}"
                                  .value="${user?.email}" autocomplete="false"
                                  @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
                                      user.email = e.detail.value;
                                      onchange?.(user, true, this._isInvalid());
                                  }}"
                    ></or-mwc-input>
                    <or-mwc-input id="new-firstName" class="validate"
                                  .label="${i18next.t("firstName")}"
                                  .type="${InputType.TEXT}"
                                  ?readonly="${readonly}"
                                  .disabled="${!user}"
                                  minLength="5" autocomplete="false"
                                  .value="${user?.firstName}"
                                  @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
                                      user.firstName = e.detail.value;
                                      onchange?.(user, true, this._isInvalid());
                                  }}"
                    ></or-mwc-input>
                    <or-mwc-input id="new-surname" class="validate"
                                  .label="${i18next.t("surname")}"
                                  .type="${InputType.TEXT}"
                                  ?readonly="${readonly}"
                                  .disabled="${!user}"
                                  minLength="1" autocomplete="false"
                                  .value="${user?.lastName}"
                                  @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
                                      user.lastName = e.detail.value;
                                      onchange?.(user, true, this._isInvalid());
                                  }}">
                    </or-mwc-input>
                </div>
                <div class="column">
                    <h5>${i18next.t("password")}</h5>
                    ${registrationEmailAsUsername ? html`
                        <!-- Reset password button when email as username is configured -->
                        <or-mwc-input id="reset-password" raised
                                      .label="${i18next.t("resetPassword")}"
                                      .type="${InputType.BUTTON}"
                                      .disabled="${!user.email}"
                                      @or-mwc-input-changed="${(e: OrInputChangedEvent) => this._onResetPasswordBtnClick(e)}"
                        ></or-mwc-input>
                    ` : html`
                        <!-- Direct password input fields when email as username not configured -->
                        <or-mwc-input id="new-password" class="validate"
                                      .label="${i18next.t("password")}"
                                      .type="${InputType.PASSWORD}"
                                      ?readonly="${readonly}"
                                      .disabled="${!user}"
                                      min="1" autocomplete="false"
                                      @or-mwc-input-changed="${(_e: OrInputChangedEvent) => {
                                          const changed = this._onPasswordChanged(user);
                                          onchange?.(user, changed, this._isInvalid());
                                      }}"
                        ></or-mwc-input>
                        <or-mwc-input id="new-repeatPassword"
                                      .label="${i18next.t("repeatPassword")}"
                                      .type="${InputType.PASSWORD}"
                                      helperPersistent
                                      ?readonly="${readonly}"
                                      .disabled="${!user}"
                                      min="1" autocomplete="false"
                                      @or-mwc-input-changed="${(_e: OrInputChangedEvent) => {
                                          const changed = this._onPasswordChanged(user);
                                          onchange?.(user, changed, this._isInvalid());
                                      }}"
                        ></or-mwc-input>
                        ${when(this._passwordPolicy, () => until(this._getPasswordPolicyTemplate(user, this._passwordPolicy)))}
                    `}
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
        if (this._user && !this._isInvalid()) {
            this._updateUser(this._user);
        } else {
            console.warn("The fields are invalid!");
            showSnackbar(undefined, "saveUserFailed");
        }
    }

    /**
     * HTML callback function for when the 'Reset Password' button is clicked.
     */
    protected _onResetPasswordBtnClick(_e: OrInputChangedEvent) {
        if (this._user && !this._isInvalid()) {

            manager.rest.api.UserResource.requestPasswordResetCurrent()
                .then(() => showSnackbar(undefined, i18next.t("resetPasswordConfirmation")))
                .catch((reason) => showSnackbar(undefined, i18next.t("errorOccurred")));
        }
    }

    /**
     * Function that returns whether the input fields are all valid.
     * Listens for HTML elements with the 'validate' class.
     */
    protected _isInvalid(): boolean {
        const validateArray = this.shadowRoot.querySelectorAll(".validate");
        if (validateArray.length === 0) {
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
        if (!this._user) {
            try {
                const usersResponse = await manager.rest.api.UserResource.getCurrent();

                if (usersResponse.status < 200 || usersResponse.status > 299) {
                    throw new Error(usersResponse.statusText);
                }
                if (!usersResponse.data) {
                    throw new Error("No user could be found.");
                }
                this._user = usersResponse.data;

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
        try {
            await manager.rest.api.UserResource.updateCurrent(user);
            
            if (user.password) {
                const credentials = {value: user.password} as Credential;
                try {
                    await manager.rest.api.UserResource.updatePasswordCurrent(credentials);
                    showSnackbar(undefined, "saveUserSucceeded");
                } catch (e) {
                    showSnackbar(undefined, "saveUserFailed");
                    return;
                }
            } else {
                showSnackbar(undefined, "saveUserSucceeded");
            }
            
            // Update the stored user with the saved version and reset dirty flag
            this._user = {...user};
            this._dirty = false;
        } catch (e) {
            console.error("Failed to update user:", e);
            showSnackbar(undefined, "saveUserFailed");
        }
    }

    /**
     * Function that formats the password policy from the currently authenticated realm.
     */
    protected async _getPasswordPolicy(): Promise<void> {
        await manager.rest.api.RealmResource.get(manager.getRealm()).then((response) => {
            this._passwordPolicy = response.data.passwordPolicy ?? this._passwordPolicy;
            })
        }

    /**
     * Function that formats the password policy into a displayable html format.
     */
    protected async _getPasswordPolicyTemplate(user: UserModel, passwordPolicy = this._passwordPolicy): Promise<TemplateResult> {
        const policyMap = new Map(passwordPolicy.map(policyStr => {
            const name = policyStr.split("(")[0];
            const value = policyStr.split("(")[1]?.split(")")[0];
            return [name, value];
        }));
        const policies = Array.from(policyMap.keys());
        const policyTexts: TemplateResult[] = [];

        // Minimum / maximum length warning
        if (policies.includes("length") && policies.includes("maxLength")) {
            policyTexts.push(html`
                <or-translate value="password-policy-invalid-length" .options="${{
                    0: policyMap.get("length"),
                    1: policyMap.get("maxLength")
                }}"></or-translate>`);
        } else if (policies.includes("length")) {
            policyTexts.push(html`
                <or-translate value="password-policy-invalid-length-too-short"
                              .options="${{0: policyMap.get("length")}}"></or-translate>`);
        } else if (policies.includes("maxLength")) {
            policyTexts.push(html`
                <or-translate value="password-policy-invalid-length-too-long"
                              .options="${{0: policyMap.get("maxLength")}}"></or-translate>`);
        }

        // Special characters
        if (policies.includes("specialChars")) {
            const value = policyMap.get("specialChars");
            const translation = value === "1" ? "password-policy-special-chars-single" : "password-policy-special-chars";
            policyTexts.push(html`
                <or-translate value="${translation}" .options="${{0: value}}"></or-translate>`);
        }

        // Digits/numbers
        if (policies.includes("digits")) {
            const value = policyMap.get("digits");
            const translation = value === "1" ? "password-policy-digits-single" : "password-policy-digits";
            policyTexts.push(html`
                <or-translate value="${translation}" .options="${{0: value}}"></or-translate>`);
        }

        // Uppercase / lowercase letters
        if (policies.includes("upperCase")) {
            const value = policyMap.get("upperCase");
            const translation = value === "1" ? "password-policy-uppercase-single" : "password-policy-uppercase";
            policyTexts.push(html`
                <or-translate value="${translation}" .options="${{0: value}}"></or-translate>`);
        }

        // Warn for recently used passwords
        if (policies.includes("passwordHistory")) {
            policyTexts.push(html`
                <or-translate value="password-policy-recently-used"></or-translate>`);
        }

        // Cannot be username and/or email
        if (policies.includes("notUsername") && policies.includes("notEmail")) {
            policyTexts.push(html`
                <or-translate value="password-policy-not-email-username"></or-translate>`);
        } else if (policies.includes("notUsername")) {
            policyTexts.push(html`
                <or-translate value="password-policy-not-username"></or-translate>`);
        } else if (policies.includes("notEmail")) {
            policyTexts.push(html`
                <or-translate value="password-policy-not-email"></or-translate>`);
        }

        return html`
            <ul>
                ${map(policyTexts, text => html`
                    <li>${text}</li>`)}
            </ul>
        `;
    }
}
