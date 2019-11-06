<#import "template.ftl" as layout>
<@layout.mainLayout active='password'; section>

<h4>${msg("changePasswordHtmlTitle")}</h4>

<div class="section">
    <form action="${url.passwordUrl}" method="post">

        <input type="text" readonly value="this is not a login form" style="display: none;">
        <input type="password" readonly value="this is not a login form" style="display: none;">
        <input type="hidden" id="stateChecker" name="stateChecker" value="${stateChecker}">

        <div class="row">
        <#if password.passwordSet>
            <div class="input-field s12">
                <input type="password" class="validate" id="password" name="password"
                       autofocus autocomplete="off" required>
                <label for="password">${msg("password")} <span class="required">*</span></label>
            </div>
        </#if>

            <div class="input-field s12">
                <input type="password" class="validate" id="password-new" name="password-new"
                       autocomplete="off" required>
                <label for="password-new">${msg("passwordNew")} <span class="required">*</span></label>
            </div>

            <div class="input-field s12">
                <input type="password" class="validate" id="password-confirm"
                       name="password-confirm" autocomplete="off" required>
                <label for="password-confirm" class="two-lines">${msg("passwordConfirm")} <span
                        class="required">*</span></label>
            </div>

        </div>

        <div class="col s12 center-align">
            <button class="btn waves-effect waves-light" type="submit" name="login"
                    value="Save">${msg("doSave")}
                <i class="material-icons right">send</i>
            </button>
        </div>

    </form>
</div>
</@layout.mainLayout>