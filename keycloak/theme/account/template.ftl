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

    <link rel="icon" type="image/png" href="/static/img/favicon.png" />

    <!-- Promise API polyfill on IE 11  -->
    <script src="/static/3rdparty/es6-promise.js"></script>

    <script src="/static//js/loader.js"></script>
    <link rel="stylesheet" type="text/css" href="/static/css/loader.css" />

    <link rel="import" href="/static/css/style.html">
    <link rel="import" href="/static/css/theme.html">
    <link rel="import" href="/ui/colors.html">
    <custom-style>
        <style include="global-style global-theme colors"></style>
    </custom-style>

    <script>
        document.addEventListener("DOMContentLoaded", function (event) {
            setTimeout(function() {
                Promise.all([
                    load.js("/static/bower_components/webcomponentsjs/webcomponents-loader.js"),
                    load.css("/static/bower_components/font-awesome/css/font-awesome.css"),
                ]).then(function () {
                    console.log("Application resources loaded, starting...");
                    handleLoadComplete();
                }).catch(handleLoadError);
            }, 0);
        });
    </script>
</head>
<body class="layout horizontal loading">

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

</body>
</html>
</#macro>