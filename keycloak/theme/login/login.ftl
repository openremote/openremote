<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=social.displayInfo; section>
    <#if section = "title">
    ${msg("loginTitle",(realm.displayName!''))}
    <#elseif section = "header">
    ${msg("loginTitleHtml",(realm.displayNameHtml!''))}
    <#elseif section = "form">
        <#if realm.password>
        <form class="layout vertical or-Form theme-Form" action="${url.loginAction}" method="post">
            <div class="layout horizontal center or-FormGroup theme-FormGroup">
                <div class="or-FormLabel theme-FormLabel">
                    <label for="username"><#if !realm.registrationEmailAsUsername>${msg("usernameOrEmail")}<#else>${msg("email")}</#if></label>
                </div>

                <div class="or-FormField theme-FormField">
                    <#if usernameEditDisabled??>
                        <input id="username"
                               class="or-FormControl theme-FormControl or-FormInputText theme-FormInputText"
                               name="username" value="${(login.username!'')?html}" type="text" disabled/>
                    <#else>
                        <input id="username"
                               class="or-FormControl theme-FormControl or-FormInputText theme-FormInputText"
                               name="username" value="${(login.username!'')?html}" type="text" autofocus/>
                    </#if>
                </div>
            </div>

            <div class="layout horizontal center or-FormGroup theme-FormGroup">
                <div class="or-FormLabel theme-FormLabel">
                    <label for="password">${msg("password")}</label>
                </div>

                <div class="or-FormField theme-FormField">
                    <input id="password" class="or-FormControl theme-FormControl or-FormInputText theme-FormInputText"
                           name="password" type="password" autocomplete="off"/>
                </div>
            </div>

            <div class="layout horizontal center-center or-FormGroup theme-FormGroup">
                <div>
                    <#if realm.rememberMe && !usernameEditDisabled??>
                        <div class="checkbox">
                            <label>
                                <#if login.rememberMe??>
                                    <input id="rememberMe" name="rememberMe" type="checkbox" tabindex="3"
                                           checked> ${msg("rememberMe")}
                                <#else>
                                    <input id="rememberMe" name="rememberMe" type="checkbox"
                                           tabindex="3"> ${msg("rememberMe")}
                                </#if>
                            </label>
                        </div>
                    </#if>
                    <div>
                        <#if realm.resetPasswordAllowed>
                            <span><a href="${url.loginResetCredentialsUrl}">${msg("doForgotPassword")}</a></span>
                        </#if>
                    </div>
                </div>

                <div class="or-FormField theme-FormField">
                    <input class="or-FormControl theme-FormControl or-FormButtonPrimary theme-FormButtonPrimary or-PushButton theme-PushButton"
                           name="login"
                           type="submit"
                           value="${msg("doLogIn")}"/>
                </div>
            </div>
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
