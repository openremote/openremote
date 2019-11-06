<#import "template.ftl" as layout>
<@layout.mainLayout active='totp'; section>

    <h4>${msg("authenticatorTitle")}</h4>

    <div class="section">
        <#if totp.enabled>
            <table class="table table-bordered table-striped">
                <thead
                <tr>
                    <th colspan="2">${msg("configureAuthenticators")}</th>
                </tr>
                </thead>
                <tbody>
                <tr>
                    <td class="provider">${msg("mobile")}</td>
                    <td class="action">
                        <a id="remove-mobile" href="${url.totpRemoveUrl}"><i class="pficon pficon-delete"></i></a>
                    </td>
                </tr>
                </tbody>
            </table>
        <#else>
            <hr/>

            <ol>
                <li>
                    <p>${msg("totpStep1")}</p>
                </li>
                <li>
                    <p>${msg("totpStep2")}</p>
                    <p class="center-align"><img src="data:image/png;base64, ${totp.totpSecretQrCode}" alt="Figure: Barcode"></p>
                    <p class="center-align"><span class="code">${totp.totpSecretEncoded}</span></p>
                </li>
                <li>
                    <p>${msg("totpStep3")}</p>
                </li>
            </ol>

            <hr/>

            <form action="${url.totpUrl}" method="post">

                <div class="row">
                    <input type="hidden" id="stateChecker" name="stateChecker" value="${stateChecker}">

                    <div class="input-field col s12">
                        <input type="text" class="form-control" id="totp" name="totp" autocomplete="off" autofocus
                               autocomplete="off">
                        <input type="hidden" id="totpSecret" name="totpSecret" value="${totp.totpSecret}"/>
                        <label for="totp" class="control-label">${msg("authenticatorCode")}</label>
                    </div>
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

            </form>
        </#if>
    </div>
</@layout.mainLayout>