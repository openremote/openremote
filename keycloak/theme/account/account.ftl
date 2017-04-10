<#import "template.ftl" as layout>
<@layout.mainLayout active='account'; section>

<div class="layout vertical">

    <div class="or-Headline1">${msg("editAccountHtmlTtile")}</div>
    <div class="or-HeadlineSub"><span class="required">*</span> ${msg("requiredFields")}</div>

    <form action="${url.accountUrl}" class="layout vertical or-Form" method="post">

        <input type="hidden" id="stateChecker" name="stateChecker" value="${stateChecker?html}">

        <div class="layout horizontal center or-FormGroup ${messagesPerField.printIfExists('username','error')}">
            <div class="or-FormLabel">
                <label for="username">${msg("username")}</label>
                <#if realm.editUsernameAllowed><span class="required">*</span></#if>
            </div>

            <div class="or-FormField">
                <input type="text" class="or-FormControl or-FormInputText" id="username" name="username"
                       <#if !realm.editUsernameAllowed>disabled="disabled"</#if> value="${(account.username!'')?html}"/>
            </div>
        </div>

        <div class="layout horizontal center or-FormGroup ${messagesPerField.printIfExists('email','error')}">
            <div class="or-FormLabel">
                <label for="email">${msg("email")}</label>
                <span class="required">*</span>
            </div>

            <div class="or-FormField">
                <input type="text" class="or-FormControl or-FormInputText" size="50" id="email" name="email" autofocus
                       value="${(account.email!'')?html}"/>
            </div>
        </div>

        <div class="layout horizontal center or-FormGroup ${messagesPerField.printIfExists('firstName','error')}">
            <div class="or-FormLabel">
                <label for="firstName">${msg("firstName")}</label> <span class="required">*</span>
            </div>

            <div class="or-FormField">
                <input type="text" class="or-FormControl or-FormInputText" size="50" id="firstName" name="firstName"
                       value="${(account.firstName!'')?html}"/>
            </div>
        </div>

        <div class="layout horizontal center or-FormGroup ${messagesPerField.printIfExists('lastName','error')}">
            <div class="or-FormLabel">
                <label for="lastName">${msg("lastName")}</label> <span class="required">*</span>
            </div>

            <div class="or-FormField">
                <input type="text" class="or-FormControl or-FormInputText" size="50" id="lastName" name="lastName"
                       value="${(account.lastName!'')?html}"/>
            </div>
        </div>

        <div class="layout horizontal or-FormGroup">
            <div class="or-FormField">
                <#if url.referrerURI??><a href="${url.referrerURI}">${msg("backToApplication")}/a></#if>
                <button type="submit"
                        class="or-FormControl or-FormButtonPrimary or-PushButton"
                        name="submitAction" value="Save">
                    <span class="or-PushButtonIcon fa fa-save"></span><span class="html-face">${msg("doSave")}</span></button>
                <button type="submit"
                        class="or-FormControl or-FormButton or-PushButton"
                        name="submitAction" value="Cancel">
                    <span class="or-PushButtonIcon fa fa-close"></span><span class="html-face">${msg("doCancel")}</span></button>
            </div>
        </div>
    </form>
</div>

</@layout.mainLayout>