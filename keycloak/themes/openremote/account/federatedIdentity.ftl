<#import "template.ftl" as layout>
<@layout.mainLayout active='social'; section>


    <h4>${msg("federatedIdentitiesHtmlTitle")}</h4>

    <div class="section">
        <form action="${url.passwordUrl}" method="post">
        <#list federatedIdentity.identities as identity>
            <div class="input-field col s6">
                <input disabled="true" class="form-control" value="${identity.userName!}">
                <label for="${identity.providerId!}" class="control-label">${identity.displayName!}</label>
            </div>
            <div class="input-field col s6">
                <#if identity.connected>
                    <#if federatedIdentity.removeLinkPossible>
                        <a href="${identity.actionUrl}" type="submit" id="remove-${identity.providerId!}"
                           class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}">${msg("doRemove")}</a>
                    </#if>
                <#else>
                    <a href="${identity.actionUrl}" type="submit" id="add-${identity.providerId!}"
                       class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}">${msg("doAdd")}</a>
                </#if>
            </div>
        </#list>
        </form>
    </div>
</@layout.mainLayout>