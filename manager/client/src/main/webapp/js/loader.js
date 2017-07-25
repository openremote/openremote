window.defaultErrorMessage = "Service not available, please try again later.";

window.startLoading = function() {
    console.log("Start loading...");
    if (!window.document.body.classList.contains("loading")) {
        window.document.body.classList.add("loading");
    }
    // Need this to find out if GWT superdev compilation failed, then abort loading...
    window.clearInterval(window.checkGwtDebugInterval);
    window.checkGwtDebugInterval = window.setInterval(function() {
        if (document.querySelectorAll("a[target='gwt_dev_mode_log']").length > 0) {
            window.document.body.style.background = 'inherit';
            window.document.body.style.opacity = "inherit";
            window.document.body.style.transition = "inherit";
            window.handleLoadComplete();
        }
    }, 500);
};

window.handleLoadError = function(reason) {
    console.log("Load error: " + reason);
    handleLoadComplete();
    window.document.body.innerHTML =
        "<div style='padding:1em;'><h1>Error starting application</h1><h5>Reason: " + reason + "</h5><h3>" + defaultErrorMessage + "</h3></div>";
};

window.handleLoadComplete = function() {
    console.log("Load complete...");
    window.document.body.classList.remove("loading");
    window.checkGwtDebugInterval && window.clearInterval(window.checkGwtDebugInterval);
};

window.load = (function () {
    function _load(tag, rel) {
        return function (url) {
            return new Promise(function (resolve, reject) {
                var element = window.document.createElement(tag);
                var parent = 'body';
                var attr = 'src';

                // Important success and error for the promise
                element.onload = function () {
                    resolve(url);
                };
                element.onerror = function () {
                    reject(url);
                };

                switch (tag) {
                    case 'script':
                        element.async = true;
                        break;
                    case 'link':
                        element.type = 'text/css';
                        element.rel = rel;
                        attr = 'href';
                        parent = 'head';
                }

                element[attr] = url;
                window.document[parent].appendChild(element);
            });
        };
    }

    return {
        css: _load("link", "stylesheet"),
        import: _load("link", "import"),
        js: _load("script"),
        img: _load("img")
    }
})();

window.checkLogoutRedirect = function(pathname) {
    // We listen to onLoad event of iframes in parent window: If the iframe is loading
    // the /auth path (this is how we detect a redirect after session timeout), we logout
    // the parent window and go to the start page of the realm
    if (pathname.endsWith('/auth')) {
        console.log("Session timeout detected (iframe redirected to /auth), triggering Keycloak logout");
        var options = {redirectUri: "/" + keycloak.realm};
        keycloak.logout(options);
    }
};