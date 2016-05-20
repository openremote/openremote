<#macro mainLayout active>
<!DOCTYPE html>
<html>
<head>

    <meta http-equiv="content-type" content="text/html; charset=UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">
    <meta name="robots" content="noindex, nofollow">

    <#if properties.meta?has_content>
        <#list properties.meta?split(' ') as meta>
            <meta name="${meta?split('==')[0]}" content="${meta?split('==')[1]}"/>
        </#list>
    </#if>

    <link rel="icon" type="image/png" href="/static/img/favicon.png" />

    <script src="/static//js/loader.js"></script>

    <title>OpenRemote Manager</title>

    <link rel="icon" type="image/png" href="/static/img/favicon.png" />

    <script>
        document.addEventListener("DOMContentLoaded", function (event) {
            Promise.all([
                load.js("/static/bower_components/webcomponentsjs/webcomponents-lite.min.js"),
                load.import("/static/bower_components/iron-flex-layout/iron-flex-layout-classes.html"),
                load.css("/static/bower_components/font-awesome/css/font-awesome.css"),

                load.import("/static/css/style.html"),
                load.import("/static/css/theme.html")
            ]).then(function () {
                console.log("Auxiliary application resources loaded, web components will initialize...");
            }).catch(function () {
                alert("Error loading application resources. " + defaultErrorMessage);
            });
        });
    </script>
</head>
<body class="layout vertical">

    <div class="flex layout horizontal">
        <div class="or-SidebarContent theme-SidebarContent">
            <ul class="or-UnorderedList theme-UnorderedList">
                <li><div class="or-Hyperlink theme-Hyperlink <#if active=='account'>active</#if>"><a href="${url.accountUrl}">${msg("account")}</a></div></li>
                <#if features.passwordUpdateSupported><li><div class="or-Hyperlink theme-Hyperlink <#if active=='password'>active</#if>"><a href="${url.passwordUrl}">${msg("password")}</a></div></li></#if>
            </ul>
        </div>
        <div class="or-MainContent theme-MainContent">
            <#if message?has_content>
                <div class="layout horizontal or-FormMessages theme-FormMessages ${message.type}">
                    <#if message.type=='success' ><div class="or-MessagesIcon theme-MessagesIcon fa fa-check"></div></#if>
                    <#if message.type=='error' ><div class="or-MessagesIcon theme-MessagesIcon fa fa-warning"></div></#if>
                    ${message.summary}
                </div>
            </#if>

            <#nested "content">
        </div>
    </div>

</body>
</html>
</#macro>