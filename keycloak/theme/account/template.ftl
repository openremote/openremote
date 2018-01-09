<#macro mainLayout active>
<!DOCTYPE html>
<html>
<head>
    <title>OpenRemote Account</title>

    <meta http-equiv="content-type" content="text/html; charset=UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">
    <meta name="robots" content="noindex, nofollow">

    <#if properties.meta?has_content>
        <#list properties.meta?split(' ') as meta>
            <meta name="${meta?split('==')[0]}" content="${meta?split('==')[1]}"/>
        </#list>
    </#if>

    <link rel="icon" type="image/png" href="/static/img/favicon.png"/>

    <script src="/static/bower_components/webcomponentsjs/webcomponents-lite.js"></script>

    <link rel="import" href="/static/src/or-app/or-app.html">

    <style type="text/css">
        or-app > * {
            visibility: hidden;
        }
    </style>

</head>

<body class="layout horizontal">

<or-app>

    <div class="flex layout horizontal">
        <div class="layout vertical or-SecondaryNav">
            <div class="or-SecondaryNavItem <#if active=='account'>active</#if>"><div class="fa fa-user" style="width: 1em; margin-right:0.4em;"></div><a href="${url.accountUrl}">${msg("account")}</a></div>
        <#if features.passwordUpdateSupported><div class="or-SecondaryNavItem <#if active=='password'>active</#if>"><div class="fa fa-key" style="width: 1em; margin-right:0.4em;"></div><a href="${url.passwordUrl}">${msg("password")}</a></div></#if>
        </div>

        <div class="flex layout vertical">

            <div class="layout horizontal center end-justified or-SecondaryNav">
            </div>

        <#if message?has_content>
            <div style="max-width: 30em;" class="layout horizontal or-FormMessages ${message.type}">
                <#if message.type=='success' ><div class="or-MessagesIcon fa fa-check"></div></#if>
                <#if message.type=='error' ><div class="or-MessagesIcon fa fa-warning"></div></#if>
                ${message.summary}
            </div>
        </#if>

        <#nested "content">
        </div>
    </div>

</or-app>

</body>
</html>
</#macro>