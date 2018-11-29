<#import "template.ftl" as layout>
<@layout.mainLayout active='log'; section>

    <h4>${msg("accountLogHtmlTitle")}</h4>


    <div class="section">
        <table class="striped">
            <thead>
            <tr>
                <td>${msg("date")}</td>
                <td>${msg("event")}</td>
                <td>${msg("ip")}</td>
                <td>${msg("client")}</td>
                <td>${msg("details")}</td>
            </tr>
            </thead>

            <tbody>
                <#list log.events as event>
                    <tr>
                        <td>${event.date?datetime}</td>
                        <td>${event.event}</td>
                        <td>${event.ipAddress}</td>
                        <td>${event.client!}</td>
                        <td><#list event.details as detail>${detail.key} = ${detail.value} <#if detail_has_next>
                            , </#if></#list></td>
                    </tr>
                </#list>
            </tbody>

        </table>
    </div>
</@layout.mainLayout>