window.authenticate = function (successCallback, errorCallback) {
    Promise.all([
        load.js("/auth/js/keycloak.min.js")
    ]).then(function (e) {
        console.log("Keycloak available...");
        startKeycloakAuthentication(successCallback, errorCallback);
    }).catch(function () {
        console.log("Keycloak not available, falling back to basic authentication...");
        startBasicAuthentication(successCallback, errorCallback);
    })
};

window.startKeycloakAuthentication = function (successCallback, errorCallback) {
    var keycloakConfig = {
        url: '/auth',
        realm: window.location.pathname.split('/')[1],
        clientId: "openremote"
    };
    window.keycloak = Keycloak(keycloakConfig);
    window.keycloak.init({onLoad: 'login-required'})
        .success(function (authenticated) {
            if (authenticated) {
                console.log("Keycloak authentication successful...");
                successCallback();
            } else {
                console.log("Keycloak authentication failed, forcing login...");
                window.keycloak.login();
            }
        })
        .error(errorCallback);
};

window.startBasicAuthentication = function (successCallback, errorCallback) {
    Promise.all([
        load.js("/static/bower_components/webcomponentsjs/webcomponents-lite.min.js"),
        load.import("/static/bower_components/iron-flex-layout/iron-flex-layout-classes.html"),
        load.css("/static/bower_components/font-awesome/css/font-awesome.css"),

        load.import("/static/css/style.html"),
        load.import("/static/css/theme.html")
    ]).then(function () {
        console.log("Resources loaded for basic authentication...");
        setTimeout(handleLoadComplete, 0);
        var authForm = renderBasicAuthForm();
        document.body.appendChild(authForm);
        // Override submit button handler
        document.querySelector("#basicAuthForm [name='login']").onclick = function() {
            var username = document.querySelector("#basicAuthForm [name='username']").value;
            var password = document.querySelector("#basicAuthForm [name='password']").value;
            performBasicAuthentication(
                username,
                password,
                function (e) {
                    if (e === true) {
                        console.log("Basic authentication successful...");
                        // Application can grab the credentials from window for its own requests
                        window.basicAuthUsername = username;
                        window.basicAuthPassword = password;
                        document.body.removeChild(authForm);
                        startLoading();
                        successCallback();
                    } else {
                        console.log("Basic authentication failed, trying again");
                        document.querySelector("#basicAuthForm .basicAuthMessage").style.display = "inherit";
                        document.querySelector("#basicAuthForm .basicAuthMessageText").innerText = "Login failed, invalid username or password.";
                    }
                },
                function (e) {
                    alert("Error sending authentication to service, please try again later.");
                    console.dir(e);
                }
            );
            return false;
        }
    }).catch(errorCallback);
};

window.renderBasicAuthForm = function () {
    var authForm = document.createElement("div");
    authForm.classList.add("flex", "layout", "vertical");
    authForm.id = "basicAuthForm";
    authForm.innerHTML =
        '<div class="flex layout vertical center-center">' +
        '<div class="layout horizontal center-center" style="margin: 1em;">' +
        '<a href="https://www.openremote.com/">' +
        '<svg style="pointer-events: none; display: block; width: 4em; height:4em; margin-right: 0.4em;" viewBox="0 0 24 24" preserveAspectRatio="xMidYMid meet">' +
        '<path fill="#C4D600" d="M11.93,21.851c-5.551,0-10.066-4.515-10.066-10.065h2.108c0,4.388,3.57,7.958,7.958,7.958 c4.387,0,7.958-3.57,7.958-7.958c0-4.388-3.57-7.958-7.958-7.958V1.72c5.55,0,10.066,4.517,10.066,10.066 C21.996,17.336,17.48,21.851,11.93,21.851L11.93,21.851z"/>' +
        '<path fill="#4E9D2D" d="M10.406,19.088c-1.95-0.406-3.626-1.549-4.717-3.215s-1.469-3.66-1.062-5.61 c0.407-1.951,1.55-3.626,3.217-4.718c1.667-1.092,3.659-1.469,5.61-1.062c4.027,0.84,6.62,4.799,5.779,8.825l-2.063-0.429 c0.603-2.889-1.257-5.73-4.147-6.333c-1.4-0.292-2.829-0.022-4.025,0.762C7.802,8.091,6.982,9.293,6.69,10.693 c-0.291,1.398-0.021,2.828,0.762,4.024c0.783,1.196,1.985,2.016,3.385,2.307L10.406,19.088L10.406,19.088z"/>' +
        '<path fill="#1D5632" d="M11.936,16.622c-0.082,0-0.164-0.001-0.245-0.004c-1.29-0.065-2.478-0.628-3.346-1.585 c-0.868-0.958-1.31-2.195-1.246-3.487l2.104,0.105c-0.036,0.728,0.214,1.427,0.704,1.967c0.488,0.54,1.16,0.858,1.888,0.894 c0.725,0.033,1.426-0.213,1.966-0.703c0.541-0.489,0.858-1.159,0.895-1.887c0.075-1.503-1.088-2.787-2.591-2.862l0.105-2.104 c2.664,0.132,4.724,2.406,4.592,5.07c-0.064,1.291-0.628,2.478-1.585,3.345C14.28,16.183,13.137,16.622,11.936,16.622L11.936,16.622 z"/>' +
        '</svg>' +
        '</a>' +
        '<div class="layout vertical">' +
        '<div class="or-HeadlineText" style="margin: 0; white-space: nowrap;"><span>Master</span></div>' +
        '<div class="or-HeadlineSub" style="margin-left: 0.2em;">OpenRemote Login</div></div>' +
        '</div>' +
        '<div class="basicAuthMessage layout horizontal center or-FormMessages error" style="display: none;">' +
        '<div class="or-MessagesIcon fa fa-warning"></div>' +
        '<span class="basicAuthMessageText"></span>' +
        '</div>' +
        '<form class="layout vertical or-Form">' +
        '<div class="layout horizontal center or-FormGroup">' +
        '<div class="or-FormLabel">' +
        '<label for="username">Username or email</label>' +
        '</div>' +
        '<div class="or-FormField">' +
        '<input autocomplete="off" autocapitalize="off" class="or-FormControl or-FormInputText" name="username" value="" type="text" autofocus/>' +
        '</div>' +
        '</div>' +
        '<div class="layout horizontal center or-FormGroup">' +
        '<div class="or-FormLabel">' +
        '<label for="password">Password</label>' +
        '</div>' +
        '<div class="or-FormField">' +
        '<input class="or-FormControl or-FormInputText" name="password" type="password" autocomplete="off"/>' +
        '</div>' +
        '</div>' +
        '<div class="layout horizontal center-center or-FormGroup">' +
        '<div>' +
        '<div>' +
        '</div>' +
        '</div>' +
        '<div class="or-FormField">' +
        '<input class="or-FormControl or-FormButtonPrimary or-PushButton" name="login" type="submit" value="Log in"/>' +
        '</div>' +
        '</div>' +
        '</form>' +
        '</div>';
    return authForm;
};

window.performBasicAuthentication = function (username, password, successCallback, errorCallback) {
    // Send a request with credentials to a resource that we know returns 200 or 401
    try {
        var x = new XMLHttpRequest();

        // TODO You all suck
        var isSafari = /constructor/i.test(window.HTMLElement) || (function (p) { return p.toString() === "[object SafariRemoteNotification]"; })(!window['safari'] || safari.pushNotification);
        var isChrome = !!window.chrome && !!window.chrome.webstore;
        if (isSafari) {
            // Must be async false or Safari will popup 401 dialog
            x.open("GET", "/master/asset/user/current", false);
        } else if (isChrome) {
            // Must set dummy URL username or Chrome will popup 401 dialog
            x.open("GET", "/master/asset/user/current", true, "dummy");
        } else {
            x.open("GET", "/master/asset/user/current", true);
        }

        var credentials = "Basic " + btoa(username + ":" + password);
        x.setRequestHeader("Authorization", credentials);

        x.onreadystatechange = function () {
            if (x.readyState > 3) {
                if (successCallback && x.status === 200) {
                    successCallback(true);
                } else if (successCallback && x.status === 401) {
                    successCallback(false);
                } else if (errorCallback) {
                    errorCallback(x);
                }
            }
        };
        x.send()
    } catch (e) {
        if (errorCallback) {
            errorCallback(e);
        }
    }
};
