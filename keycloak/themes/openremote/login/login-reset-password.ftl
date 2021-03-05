<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=true displayMessage=!messagesPerField.existsError('username'); section>
    <#if section = "title">
        ${msg("emailForgotTitle")}
    <#elseif section = "header">
        ${msg("emailForgotTitle")}
    <#elseif section = "form">
        <form id="kc-reset-password-form"  action="${url.loginAction}" method="post">
            <div class="row">
                <div class="input-field col s12">
                    <#if auth?has_content && auth.showUsername()>
                        <input type="text" id="username" name="username" class="validate" autofocus value="${auth.attemptedUsername}" aria-invalid="<#if messagesPerField.existsError('username')>true</#if>"/>
                    <#else>
                        <input type="text" id="username" name="username" class="validate" autofocus aria-invalid="<#if messagesPerField.existsError('username')>true</#if>"/>
                    </#if>

                    <#if messagesPerField.existsError('username')>
                        <span id="input-error-username" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                                    ${kcSanitize(messagesPerField.get('username'))?no_esc}
                        </span>
                    </#if>

                    <label for="username" class="${properties.kcLabelClass!}"><#if !realm.loginWithEmailAllowed>${msg("username")}<#elseif !realm.registrationEmailAsUsername>${msg("usernameOrEmail")}<#else>${msg("email")}</#if></label>
                </div>
            </div>

            <div class="col s12 center-align">
                <button class="btn waves-effect waves-light" type="submit" name="login">${msg("doSubmit")}
                    <i class="material-icons right">send</i>
                </button>
            </div>

            <div class="col s12 center-align">
                <p><a href="${url.loginUrl}">${msg("backToLogin")}</a></p>
            </div>
        </form>
    <#elseif section = "info" >
        <div class="layout horizontal center">
            ${msg("emailInstruction")}
        </div>
    </#if>
</@layout.registrationLayout>
