<#import "template.ftl" as layout>
<@layout.mainLayout active='password'; section>


<div class="layout vertical">

    <div class="or-Headline1 theme-Headline1">${msg("changePasswordHtmlTitle")}</div>
    <div class="or-HeadlineSub theme-HeadlineSub">${msg("allFieldsRequired")}</div>

    <form action="${url.passwordUrl}" class="layout vertical or-Form theme-Form" method="post">

        <input type="text" readonly value="this is not a login form" style="display: none;">
        <input type="password" readonly value="this is not a login form" style="display: none;">

        <#if password.passwordSet>
            <div class="layout horizontal center or-FormGroup theme-FormGroup">
                <div class="or-FormLabel theme-FormLabel">
                    <label for="password">${msg("password")}</label>
                </div>

                <div class="or-FormField theme-FormField">
                    <input type="password" class="or-FormControl theme-FormControl or-FormInputText theme-FormInputText" id="password" name="password" autofocus autocomplete="off">
                </div>
            </div>
        </#if>

        <input type="hidden" id="stateChecker" name="stateChecker" value="${stateChecker?html}">

        <div class="layout horizontal center or-FormGroup theme-FormGroup">
            <div class="or-FormLabel theme-FormLabel">
                <label for="password-new">${msg("passwordNew")}</label>
            </div>

            <div class="or-FormField theme-FormField">
                <input type="password" class="or-FormControl theme-FormControl or-FormInputText theme-FormInputText" id="password-new" name="password-new" autocomplete="off">
            </div>
        </div>

        <div class="layout horizontal center or-FormGroup theme-FormGroup">
            <div class="or-FormLabel theme-FormLabel">
                <label for="password-confirm" class="two-lines">${msg("passwordConfirm")}</label>
            </div>

            <div class="or-FormField theme-FormField">
                <input type="password" class="or-FormControl theme-FormControl or-FormInputText theme-FormInputText" id="password-confirm" name="password-confirm" autocomplete="off">
            </div>
        </div>

        <div class="layout horizontal center-center or-FormGroup theme-FormGroup">
            <div class="or-FormField theme-FormField">
                <button type="submit" class="or-FormControl theme-FormControl or-FormButtonPrimary theme-FormButtonPrimary or-PushButton theme-PushButton"
                        name="submitAction" value="Save">
                    <span class="or-PushButtonIcon fa fa-save"></span>${msg("doSave")}</button>
            </div>
        </div>
    </form>
</div>

</@layout.mainLayout>