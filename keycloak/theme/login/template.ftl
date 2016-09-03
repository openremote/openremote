<#macro registrationLayout displayInfo=false displayMessage=true>
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

    <div class="flex layout vertical center-center">

        <div class="layout horizontal center-center" style="margin: 1em;">
            <svg style="pointer-events: none; display: block; width: 4em; height:4em; margin-right: 0.4em;"
                 viewBox="0 0 24 24"
                 preserveAspectRatio="xMidYMid meet">
                <path fill="#C4D600"
                      d="M11.93,21.851c-5.551,0-10.066-4.515-10.066-10.065h2.108c0,4.388,3.57,7.958,7.958,7.958 c4.387,0,7.958-3.57,7.958-7.958c0-4.388-3.57-7.958-7.958-7.958V1.72c5.55,0,10.066,4.517,10.066,10.066 C21.996,17.336,17.48,21.851,11.93,21.851L11.93,21.851z"/>
                <path fill="#4E9D2D"
                      d="M10.406,19.088c-1.95-0.406-3.626-1.549-4.717-3.215s-1.469-3.66-1.062-5.61 c0.407-1.951,1.55-3.626,3.217-4.718c1.667-1.092,3.659-1.469,5.61-1.062c4.027,0.84,6.62,4.799,5.779,8.825l-2.063-0.429 c0.603-2.889-1.257-5.73-4.147-6.333c-1.4-0.292-2.829-0.022-4.025,0.762C7.802,8.091,6.982,9.293,6.69,10.693 c-0.291,1.398-0.021,2.828,0.762,4.024c0.783,1.196,1.985,2.016,3.385,2.307L10.406,19.088L10.406,19.088z"/>
                <path fill="#1D5632"
                      d="M11.936,16.622c-0.082,0-0.164-0.001-0.245-0.004c-1.29-0.065-2.478-0.628-3.346-1.585 c-0.868-0.958-1.31-2.195-1.246-3.487l2.104,0.105c-0.036,0.728,0.214,1.427,0.704,1.967c0.488,0.54,1.16,0.858,1.888,0.894 c0.725,0.033,1.426-0.213,1.966-0.703c0.541-0.489,0.858-1.159,0.895-1.887c0.075-1.503-1.088-2.787-2.591-2.862l0.105-2.104 c2.664,0.132,4.724,2.406,4.592,5.07c-0.064,1.291-0.628,2.478-1.585,3.345C14.28,16.183,13.137,16.622,11.936,16.622L11.936,16.622 z"/>
            </svg>
            <div class="layout vertical">
                <div class="or-Headline1" style="margin: 0;"><#nested "header"></div>
                <div class="or-HeadlineSub" style="margin-left: 0.2em;">Manager Login</div>
            </div>
        </div>

        <#if realm.internationalizationEnabled>
            <div id="kc-locale">
                    <div id="kc-locale-dropdown">
                        <a href="#" id="kc-current-locale-link">${locale.current}</a>
                        <ul>
                            <#list locale.supported as l>
                                <li><a href="${l.url}">${l.label}</a></li>
                            </#list>
                        </ul>
                    </div>
            </div>
        </#if>

        <#if displayMessage && message?has_content>
            <div class="layout horizontal or-FormMessages ${message.type}">
                <#if message.type=='success' ><div class="or-MessagesIcon fa fa-check"></div></#if>
                <#if message.type=='warning' ><div class="or-MessagesIcon fa fa-warning"></div></#if>
                <#if message.type=='error' ><div class="or-MessagesIcon fa fa-warning"></div></#if>
                <#if message.type=='info' ><div class="or-MessagesIcon fa fa-info"></div></#if>
                ${message.summary}
            </div>
        </#if>

        <#nested "form">

        <#if displayInfo>
            <#nested "info">
        </#if>

    </div>

</body>
</html>
</#macro>
