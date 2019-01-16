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

    <link rel="icon" type="image/png" href="${url.resourcesPath}/img/favicon.png"/>
    <link type=text/css rel="stylesheet" href="${url.resourcesPath}/css/materialize.min.css" media="screen,projection"/>
    <link rel="stylesheet" href="${url.resourcesPath}/css/styles.css"/>

    <script type="text/javascript">
        document.addEventListener('DOMContentLoaded', function () {
            var elems = document.querySelectorAll('.sidenav');
            M.Sidenav.init(elems);
        });
    </script>
</head>

<body>

<header>
    <ul id="menu" class="sidenav sidenav-fixed">
        <li class="<#if active=='account'>active</#if>"><a href="${url.accountUrl}">${msg("account")}</a></li>
                <#if features.passwordUpdateSupported>
                    <li class="<#if active=='password'>active</#if>"><a href="${url.passwordUrl}">${msg("password")}</a>
                    </li></#if>
        <li class="<#if active=='totp'>active</#if>"><a href="${url.totpUrl}">${msg("authenticator")}</a></li>
        <#if features.identityFederation>
            <li class="<#if active=='social'>active</#if>"><a href="${url.socialUrl}">${msg("federatedIdentity")}</a></li>
        </#if>
        <li class="<#if active=='sessions'>active</#if>"><a href="${url.sessionsUrl}">${msg("sessions")}</a></li>
        <li class="<#if active=='applications'>active</#if>"><a
                href="${url.applicationsUrl}">${msg("applications")}</a></li>
                <#if features.log>
                    <li class="<#if active=='log'>active</#if>"><a href="${url.logUrl}">${msg("log")}</a></li></#if>
    </ul>
    <a href="#" data-target="menu" class="top-nav sidenav-trigger full hide-on-large-only green-text darken-3"><i class="material-icons">menu</i></a>
</header>

<main>
    <div class="container">
        <#if message?has_content>
            <div class="section">
                <div class="card-panel">
                    <#if message.type=='success' ><i class="material-icons green-text">check_circle</i><span
                            class="green-text">${message.summary}</span></#if>
                    <#if message.type=='error' ><i class="material-icons red-text">error</i><span
                            class="red-text">${message.summary}</span></#if>
                </div>
            </div>
        </#if>

        <div class="col s12">
            <#nested "content">
        </div>

    </div>
</main>

<script type="text/javascript" src="${url.resourcesPath}/js/materialize.min.js"></script>
</body>
</html>
</#macro>