<#import "template.ftl" as layout>
<@layout.mainLayout active='account'; section>

<div class="layout vertical">

    <div class="or-Header1 theme-Header1">${msg("editAccountHtmlTtile")}</div>
    <div class="or-HeaderSub theme-HeaderSub"><span class="required">*</span> ${msg("requiredFields")}</div>

    <form action="${url.accountUrl}" class="layout vertical or-Form theme-Form" method="post">

        <input type="hidden" id="stateChecker" name="stateChecker" value="${stateChecker?html}">

        <div class="layout horizontal center or-FormGroup theme-FormGroup ${messagesPerField.printIfExists('username','error')}">
            <div class="or-FormLabel theme-FormLabel">
                <label for="username">${msg("username")}</label>
                <#if realm.editUsernameAllowed><span class="required">*</span></#if>
            </div>

            <div class="or-FormField theme-FormField">
                <input type="text" class="or-FormControl theme-FormControl or-FormInputText theme-FormInputText" id="username" name="username"
                       <#if !realm.editUsernameAllowed>disabled="disabled"</#if> value="${(account.username!'')?html}"/>
            </div>
        </div>

        <div class="layout horizontal center or-FormGroup theme-FormGroup ${messagesPerField.printIfExists('email','error')}">
            <div class="or-FormLabel theme-FormLabel">
                <label for="email">${msg("email")}</label>
                <span class="required">*</span>
            </div>

            <div class="or-FormField theme-FormField">
                <input type="text" class="or-FormControl theme-FormControl or-FormInputText theme-FormInputText" size="50" id="email" name="email" autofocus
                       value="${(account.email!'')?html}"/>
            </div>
        </div>

        <div class="layout horizontal center or-FormGroup theme-FormGroup ${messagesPerField.printIfExists('firstName','error')}">
            <div class="or-FormLabel theme-FormLabel">
                <label for="firstName">${msg("firstName")}</label> <span class="required">*</span>
            </div>

            <div class="or-FormField theme-FormField">
                <input type="text" class="or-FormControl theme-FormControl or-FormInputText theme-FormInputText" size="50" id="firstName" name="firstName"
                       value="${(account.firstName!'')?html}"/>
            </div>
        </div>

        <div class="layout horizontal center or-FormGroup theme-FormGroup ${messagesPerField.printIfExists('lastName','error')}">
            <div class="or-FormLabel theme-FormLabel">
                <label for="lastName">${msg("lastName")}</label> <span class="required">*</span>
            </div>

            <div class="or-FormField theme-FormField">
                <input type="text" class="or-FormControl theme-FormControl or-FormInputText theme-FormInputText" size="50" id="lastName" name="lastName"
                       value="${(account.lastName!'')?html}"/>
            </div>
        </div>

        <div class="layout horizontal center-center or-FormGroup theme-FormGroup">
            <div class="or-FormField theme-FormField">
                <#if url.referrerURI??><a href="${url.referrerURI}">${msg("backToApplication")}/a></#if>
                <button type="submit"
                        class="or-FormControl theme-FormControl or-FormButtonPrimary theme-FormButtonPrimary or-PushButton theme-PushButton"
                        name="submitAction" value="Save">
                    <span class="or-PushButtonIcon fa fa-save"></span>${msg("doSave")}</button>
                <button type="submit"
                        class="or-FormControl theme-FormControl or-FormButton theme-FormButton or-PushButton theme-PushButton"
                        name="submitAction" value="Cancel">
                    <span class="or-PushButtonIcon fa fa-close"></span>${msg("doCancel")}</button>
            </div>
        </div>
    </form>
</div>

</@layout.mainLayout>