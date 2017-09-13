<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=true; section>
    <#if section = "title">
        ${msg("emailForgotTitle")}
    <#elseif section = "header">
        ${msg("emailForgotTitle")}
    <#elseif section = "form">
        <form id="kc-reset-password-form"  action="${url.loginAction}" method="post">
            <div class="layout horizontal center or-FormGroup">
                <div class="or-FormLabel">
                        <label for="username" class="${properties.kcLabelClass!}"><#if !realm.loginWithEmailAllowed>${msg("username")}<#elseif !realm.registrationEmailAsUsername>${msg("usernameOrEmail")}<#else>${msg("email")}</#if></label>
                </div>
                <div class="or-FormField">
                        <input class="or-FormControl or-FormInputText" type="text" id="username" name="username" autofocus/>
                </div>
            </div>
            <div class="layout horizontal center-center or-FormGroup">
                <span class="or-Hyperlink"><a href="${url.loginUrl}">${msg("backToLogin")}</a></span>
                <div class="or-FormField">
                    <input class="or-FormControl or-FormButtonPrimary or-PushButton" type="submit" value="${msg("doSubmit")}"/>
                </div>
            </div>
        </form>

    <#elseif section = "info" >
        <div class="layout horizontal center" style="margin: 1em;">
            ${msg("emailInstruction")}
        </div>
    </#if>
</@layout.registrationLayout>