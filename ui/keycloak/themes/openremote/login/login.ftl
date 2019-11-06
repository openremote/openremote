<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=social.displayInfo; section>
    <#if section = "title">
        ${msg("loginTitle",(realm.displayName!''))}
    <#elseif section = "header">
        ${msg("loginTitleHtml",(realm.displayNameHtml!''))}
    <#elseif section = "form">
        <#if realm.password>
        <form action="${url.loginAction}" method="post">

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
                    <input id="password" name="password" type="password" autocomplete="off"/>
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
                            <li><a href="${p.loginUrl}" id="zocial-${p.alias}" class="zocial ${p.providerId}"> <span
                                    class="text">${p.alias}</span></a></li>
                        </#list>
                    </ul>
                </div>
            </#if>
        </#if>
</@layout.registrationLayout>
