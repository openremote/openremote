<#import "template.ftl" as layout>
<@layout.mainLayout active='password'; section>

<form action="${url.passwordUrl}" class="flex layout vertical or-Form" method="post">

    <div class="flex or-MainContent">

        <div class="or-Headline">
            <span class="or-Icon fa fa-key"></span>
            <span class="or-HeadlineText">${msg("changePasswordHtmlTitle")}</span>
        </div>

        <input type="text" readonly value="this is not a login form" style="display: none;">
        <input type="password" readonly value="this is not a login form" style="display: none;">

        <#if password.passwordSet>
            <div class="layout horizontal center or-FormGroup">
                <div class="or-FormLabel">
                    <label for="password">${msg("password")}</label>
                </div>

                <div class="or-FormField">
                    <input type="password" class="or-FormControl or-FormInputText" id="password" name="password"
                           autofocus autocomplete="off">
                </div>
            </div>
        </#if>

        <input type="hidden" id="stateChecker" name="stateChecker" value="${stateChecker?html}">

        <div class="layout horizontal center or-FormGroup">
            <div class="or-FormLabel">
                <label for="password-new">${msg("passwordNew")}</label>
            </div>

            <div class="or-FormField">
                <input type="password" class="or-FormControl or-FormInputText" id="password-new" name="password-new"
                       autocomplete="off">
            </div>
        </div>

        <div class="layout horizontal center or-FormGroup">
            <div class="or-FormLabel">
                <label for="password-confirm" class="two-lines">${msg("passwordConfirm")}</label>
            </div>

            <div class="or-FormField">
                <input type="password" class="or-FormControl or-FormInputText" id="password-confirm"
                       name="password-confirm" autocomplete="off">
            </div>
        </div>

    </div>

    <div class="flex-none or-MainContent">
        <div class="layout horizontal or-FormGroup">
            <div class="or-FormField">
                <button type="submit" class="or-FormControl or-FormButtonPrimary or-PushButton"
                        name="submitAction" value="Save">
                    <span class="or-Icon fa fa-save"></span><span class="html-face">${msg("doSave")}</span>
                </button>
            </div>
        </div>
    </div>

</form>

</@layout.mainLayout>