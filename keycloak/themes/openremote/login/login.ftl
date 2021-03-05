<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('username','password') displayInfo=realm.password && realm.registrationAllowed && !registrationDisabled??; section>
    <#if section = "title">
        ${msg("loginTitle",(realm.displayName!''))}
    <#elseif section = "header">
        ${msg("loginTitleHtml",(realm.displayNameHtml!''))}
    <#elseif section = "form">
        <#if realm.password>
        <form onsubmit="login.disabled = true; return true;" action="${url.loginAction}" method="post">
            <div class="row">
                <div class="input-field col s12">
                    <#if usernameEditDisabled??>
                        <input id="username"
                               autofocus
                               autocomplete="off"
                               autocapitalize="off"
                               name="username" value="${(login.username!'')}" type="text" disabled/>
                    <#else>
                        <input id="username"
                               autofocus
                               autocomplete="off"
                               autocapitalize="off"
                               required
                               class="validate"
                               name="username" value="${(login.username!'')}" type="text"/>
                    </#if>
                    <label for="username"><#if !realm.registrationEmailAsUsername>${msg("usernameOrEmail")}<#else>${msg("email")}</#if></label>
                </div>

                <div class="input-field col s12">
                    <input id="password" name="password" type="password" autocomplete="off" aria-invalid="<#if messagesPerField.existsError('username','password')>true</#if>" />
                    <label for="password">${msg("password")}</label>
                </div>

                <#if realm.rememberMe && !usernameEditDisabled??>
                    <div class="input-field col s12">
                        <div>
                            <label>
                                <#if login.rememberMe??>
                                    <input id="rememberMe" name="rememberMe" type="checkbox" tabindex="3"
                                           checked/><span>${msg("rememberMe")}</span>
                                <#else>
                                    <input id="rememberMe" name="rememberMe" type="checkbox"
                                           tabindex="3"/><span>${msg("rememberMe")}</span>
                                </#if>
                            </label>
                        </div>
                    </div>
                </#if>
            </div>

            <div class="col s12 center-align">
                <button class="btn waves-effect waves-light" type="submit" name="login">${msg("doLogIn")}
                    <i class="material-icons right">send</i>
                </button>
            </div>

            <#if realm.resetPasswordAllowed>
                <div class="col s12 center-align">
                    <p><a href="${url.loginResetCredentialsUrl}">${msg("doForgotPassword")}</a></p>
                </div>
            </#if>
        </form>
        </#if>
    <#elseif section = "info" >

        <#if realm.password && realm.registrationAllowed && !usernameEditDisabled??>
            <div id="kc-registration">
                <span>${msg("noAccount")} <a href="${url.registrationUrl}">${msg("doRegister")}</a></span>
            </div>
        </#if>


        <#if realm.password && social.providers??>
            <div id="kc-social-providers">
                <ul>
                    <#list social.providers as p>
                        <a id="social-${p.alias}" class="${properties.kcFormSocialAccountListButtonClass!} <#if social.providers?size gt 3>${properties.kcFormSocialAccountGridItem!}</#if>"
                           type="button" href="${p.loginUrl}">
                            <#if p.iconClasses?has_content>
                                <i class="${properties.kcCommonLogoIdP!} ${p.iconClasses!}" aria-hidden="true"></i>
                                <span class="${properties.kcFormSocialAccountNameClass!} kc-social-icon-text">${p.displayName!}</span>
                            <#else>
                                <span class="${properties.kcFormSocialAccountNameClass!}">${p.displayName!}</span>
                            </#if>
                        </a>
                    </#list>
                </ul>
            </div>
        </#if>
    </#if>
</@layout.registrationLayout>
