<#import "template.ftl" as layout>
<@layout.mainLayout active='applications'; section>

    <h4>${msg("applicationsHtmlTitle")}</h4>

    <div class="section">

        <form action="${url.applicationsUrl}" method="post">
            <input type="hidden" id="stateChecker" name="stateChecker" value="${stateChecker}">
            <input type="hidden" id="referrer" name="referrer" value="${stateChecker}">

            <table class="striped">
                <thead>
                <tr>
                    <td>${msg("application")}</td>
                    <td>${msg("availablePermissions")}</td>
                    <td>${msg("grantedPermissions")}</td>
                    <td>${msg("grantedPersonalInfo")}</td>
                    <td>${msg("additionalGrants")}</td>
                    <td>${msg("action")}</td>
                </tr>
                </thead>

                <tbody>
                  <#list applications.applications as application>
                  <tr>
                      <td>
                            <#if application.client.baseUrl??><a href="${application.client.baseUrl}"></#if>
                                <#if application.client.name??>${advancedMsg(application.client.name)}<#else>${application.client.clientId}</#if>
                            <#if application.client.baseUrl??></a></#if>
                      </td>

                      <td>
                            <#list application.realmRolesAvailable as role>
                                <#if role.description??>${advancedMsg(role.description)}<#else>${advancedMsg(role.name)}</#if>
                                <#if role_has_next>, </#if>
                            </#list>
                            <#list application.resourceRolesAvailable?keys as resource>
                                <#if application.realmRolesAvailable?has_content>, </#if>
                                <#list application.resourceRolesAvailable[resource] as clientRole>
                                    <#if clientRole.roleDescription??>${advancedMsg(clientRole.roleDescription)}<#else>${advancedMsg(clientRole.roleName)}</#if>
                                    ${msg("inResource")} <strong><#if clientRole.clientName??>${advancedMsg(clientRole.clientName)}<#else>${clientRole.clientId}</#if></strong>
                                    <#if clientRole_has_next>, </#if>
                                </#list>
                            </#list>
                      </td>

                      <td>
                            <#if application.client.consentRequired>
                                <#list application.realmRolesGranted as role>
                                    <#if role.description??>${advancedMsg(role.description)}<#else>${advancedMsg(role.name)}</#if>
                                    <#if role_has_next>, </#if>
                                </#list>
                                <#list application.resourceRolesGranted?keys as resource>
                                    <#if application.realmRolesGranted?has_content>, </#if>
                                    <#list application.resourceRolesGranted[resource] as clientRole>
                                        <#if clientRole.roleDescription??>${advancedMsg(clientRole.roleDescription)}<#else>${advancedMsg(clientRole.roleName)}</#if>
                                        ${msg("inResource")} <strong><#if clientRole.clientName??>${advancedMsg(clientRole.clientName)}<#else>${clientRole.clientId}</#if></strong>
                                        <#if clientRole_has_next>, </#if>
                                    </#list>
                                </#list>
                            <#else>
                                <strong>${msg("fullAccess")}</strong>
                            </#if>
                      </td>

                      <td>
                            <#if application.client.consentRequired>
                                <#list application.claimsGranted as claim>
                                    ${advancedMsg(claim)}<#if claim_has_next>, </#if>
                                </#list>
                            <#else>
                                <strong>${msg("fullAccess")}</strong>
                            </#if>
                      </td>

                      <td>
                           <#list application.additionalGrants as grant>
                               ${advancedMsg(grant)}<#if grant_has_next>, </#if>
                           </#list>
                      </td>

                      <td>
                            <#if (application.client.consentRequired && application.claimsGranted?has_content) || application.additionalGrants?has_content>
                                <button type="submit" class="btn waves-effect waves-light" id='revoke-${application.client.clientId}' name="clientId" value="${application.client.id}">${msg("revoke")}</button>
                            </#if>
                      </td>
                  </tr>
                  </#list>
                </tbody>
            </table>
        </form>
    </div>

</@layout.mainLayout>