    <#import "template.ftl" as layout>
<@layout.mainLayout active='account'; section>

    <h4>${msg("editAccountHtmlTitle")}</h4>

    <div class="section">
        <span class="subtitle"><span class="required">*</span> ${msg("requiredFields")}</span>

        <form action="${url.accountUrl}" method="post">

            <input type="hidden" id="stateChecker" name="stateChecker" value="${stateChecker}">

            <#if !realm.registrationEmailAsUsername>
                <div class="input-field s12 ${messagesPerField.printIfExists('username','has-error')}">
                    <input type="text" class="validate" id="username" name="username" required
                           <#if !realm.editUsernameAllowed>disabled="disabled"</#if> value="${(account.username!'')}"/>
                    <label for="username">${msg("username")} <#if realm.editUsernameAllowed>
                    <span class="required">*</span></#if></label>
                </div>
            </#if>

            <div class="input-field s12">
                <input type="text" class="validate ${messagesPerField.printIfExists('email','invalid')}" id="email"
                       name="email" autofocus
                       value="${(account.email!'')}" required/>
                <label for="email">${msg("email")} <span class="required">*</span></label>
            </div>

            <div class="input-field s12 ${messagesPerField.printIfExists('firstName','has-error')}">
                <input type="text" class="validate ${messagesPerField.printIfExists('email','invalid')}" id="firstName"
                       name="firstName"
                       value="${(account.firstName!'')}" required/>
                <label for="firstName">${msg("firstName")} <span class="required">*</span></label>
            </div>

            <div class="input-field s12 ${messagesPerField.printIfExists('lastName','has-error')}">
                <input type="text" class="validate ${messagesPerField.printIfExists('email','invalid')}" id="lastName"
                       name="lastName"
                       value="${(account.lastName!'')}" required/>
                <label for="lastName">${msg("lastName")}<span class="required">*</span></label>
            </div>

            <div class="col s12 center-align">
                <button type="submit"
                        class="btn waves-effect waves-light"
                        name="submitAction" value="Save">${msg("doSave")}
                    <i class="material-icons right">send</i>
                </button>
                <button type="submit"
                        class="btn waves-effect waves-light"
                        name="submitAction" value="Cancel" formnovalidate>${msg("doCancel")}
                    <i class="material-icons right">cancel</i>
                </button>
            </div>

            <#if url.referrerURI??>
                <div class="col s12 center-align">
                    <p><a href="${url.referrerURI}">${msg("backToApplication")}/a></p>
                </div>
            </#if>
        </form>
    </div>
</@layout.mainLayout>