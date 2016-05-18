window.defaultErrorMessage = "Service not available, please try again later.";

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