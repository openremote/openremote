<#import "template.ftl" as layout>
<@layout.mainLayout active='sessions'; section>

    <h4>${msg("sessionsHtmlTitle")}</h4>

    <div class="section">

        <table class="striped">
            <thead>
            <tr>
                <td>${msg("ip")}</td>
                <td>${msg("started")}</td>
                <td>${msg("lastAccess")}</td>
                <td>${msg("expires")}</td>
                <td>${msg("clients")}</td>
            </tr>
            </thead>

            <tbody>
                <#list sessions.sessions as session>
                    <tr>
                        <td>${session.ipAddress}</td>
                        <td>${session.started?datetime}</td>
                        <td>${session.lastAccess?datetime}</td>
                        <td>${session.expires?datetime}</td>
                        <td>
                            <#list session.clients as client>
                                ${client}<br/>
                            </#list>
                        </td>
                    </tr>
                </#list>
            </tbody>

        </table>

    </div>

    <a id="logout-all-sessions" href="${url.sessionsLogoutUrl}">${msg("doLogOutAllSessions")}</a>

</@layout.mainLayout>