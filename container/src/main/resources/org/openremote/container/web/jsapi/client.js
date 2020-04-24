// namespace
openremote.REST = {
    apiURL: null,
    debug: false,
    loglevel: 0,
    antiBrowserCache: false,
    cacheHeaders: []
};

// helper function
openremote.REST.getKeys = function (o) {
    if (o !== Object(o))
        throw new TypeError('openremote.REST.getKeys called on non-object');
    var ret = [], p;
    for (p in o) if (Object.prototype.hasOwnProperty.call(o, p)) ret.push(p);
    return ret;
};

// constructor
openremote.REST.Request = function () {
    openremote.REST.log("Creating new Request");
    this.uri = null;
    this.method = "GET";
    this.username = null;
    this.password = null;
    this.acceptHeader = "*/*";
    this.contentTypeHeader = null;
    this.async = true;
    this.queryParameters = [];
    this.matrixParameters = [];
    this.formParameters = [];
    this.forms = [];
    this.cookies = [];
    this.headers = [];
    this.entity = null;
};

openremote.REST.Request.prototype = {
    execute: function (callback) {
        var request = new XMLHttpRequest();
        var url = this.uri;

        if (openremote.REST.antiBrowserCache == true) {
            request.url = url;
        }

        var restRequest = this;
        for (var i = 0; i < this.matrixParameters.length; i++) {
            url += ";" + openremote.REST.Encoding.encodePathParamName(this.matrixParameters[i][0]);
            url += "=" + openremote.REST.Encoding.encodePathParamValue(this.matrixParameters[i][1]);
        }
        for (var i = 0; i < this.queryParameters.length; i++) {
            if (i == 0)
                url += "?";
            else
                url += "&";
            url += openremote.REST.Encoding.encodeQueryParamNameOrValue(this.queryParameters[i][0]);
            url += "=" + openremote.REST.Encoding.encodeQueryParamNameOrValue(this.queryParameters[i][1]);
        }
        for (var i = 0; i < this.cookies.length; i++) {
            document.cookie = encodeURI(this.cookies[i][0])
                + "=" + encodeURI(this.cookies[i][1]);
        }
        if (this.username && this.password) {
            // TODO Must be async false or Safari will popup 401 dialog
            // TODO Must set dummy URL username or Chrome will popup 401 dialog but that leads to weird behavior in Safari...
            request.open(this.method, url, this.async);
            var credentials = "Basic " + btoa(this.username + ":" + this.password);
            request.setRequestHeader("Authorization", credentials);
        } else {
            request.open(this.method, url, this.async);
        }

        var acceptSet = false;
        var contentTypeSet = false;
        for (var i = 0; i < this.headers.length; i++) {
            if (this.headers[i][0].toLowerCase() == 'accept')
                acceptSet = this.headers[i][1];
            if (this.headers[i][0].toLowerCase() == 'content-type')
                contentTypeSet = this.headers[i][1];
            request.setRequestHeader(openremote.REST.Encoding.encodeHeaderName(this.headers[i][0]),
                openremote.REST.Encoding.encodeHeaderValue(this.headers[i][1]));
        }
        if (!acceptSet)
            request.setRequestHeader('Accept', this.acceptHeader);
        openremote.REST.log("Got form params: " + this.formParameters.length);
        // see if we're sending an entity or a form
        if (this.entity && (this.formParameters.length > 0 || this.forms.length > 0))
            throw "Cannot have both an entity and form parameters";
        // form
        if (this.formParameters.length > 0 || this.forms.length > 0) {
            if (contentTypeSet && contentTypeSet != "application/x-www-form-urlencoded")
                throw "The ContentType that was set by header value (" + contentTypeSet + ") is incompatible with form parameters";
            if (this.contentTypeHeader && this.contentTypeHeader != "application/x-www-form-urlencoded")
                throw "The ContentType that was set with setContentType (" + this.contentTypeHeader + ") is incompatible with form parameters";
            contentTypeSet = "application/x-www-form-urlencoded";
            request.setRequestHeader('Content-Type', contentTypeSet);
        } else if (this.entity && !contentTypeSet && this.contentTypeHeader) {
            // entity
            contentTypeSet = this.contentTypeHeader;
            request.setRequestHeader('Content-Type', this.contentTypeHeader);
        }
        // we use this flag to work around buggy browsers
        var gotReadyStateChangeEvent = false;
        if (callback) {
            request.onreadystatechange = function () {
                gotReadyStateChangeEvent = true;
                openremote.REST._complete(this, callback);
            };
        }
        var data = this.entity;
        if (this.entity) {
            if (this.entity instanceof Element) {
                if (!contentTypeSet || openremote.REST._isXMLMIME(contentTypeSet))
                    data = openremote.REST.serialiseXML(this.entity);
            } else if (this.entity instanceof Document) {
                if (!contentTypeSet || openremote.REST._isXMLMIME(contentTypeSet))
                    data = this.entity;
            } else if (this.entity instanceof Object) {
                if (!contentTypeSet || openremote.REST._isJSONMIME(contentTypeSet))
                    data = JSON.stringify(this.entity);
            }
        } else if (this.formParameters.length > 0) {
            data = '';
            for (var i = 0; i < this.formParameters.length; i++) {
                if (i > 0)
                    data += "&";
                data += openremote.REST.Encoding.encodeFormNameOrValue(this.formParameters[i][0]);
                data += "=" + openremote.REST.Encoding.encodeFormNameOrValue(this.formParameters[i][1]);
            }
        } else if (this.forms.length > 0) {
            data = '';
            for (var i = 0; i < this.forms.length; i++) {
                if (i > 0)
                    data += "&";
                var obj = this.forms[i][1];
                var key = openremote.REST.getKeys(obj)[0];
                data += openremote.REST.Encoding.encodeFormNameOrValue(key);
                data += "=" + openremote.REST.Encoding.encodeFormNameOrValue(obj[key]);
            }
        }
        openremote.REST.log("Content-Type set to " + contentTypeSet);
        openremote.REST.log("Entity set to " + data);
        request.send(data);
        // now if the browser did not follow the specs and did not fire the events while synchronous,
        // handle it manually
        if (!this.async && !gotReadyStateChangeEvent && callback) {
            openremote.REST.log("Working around browser readystatechange bug");
            openremote.REST._complete(request, callback);
        }

        if (openremote.REST.debug == true) {
            openremote.REST.lastRequest = request;
        }

        if (openremote.REST.antiBrowserCache == true && request.status != 304) {
            var _cachedHeaders = {
                "Etag": request.getResponseHeader('Etag'),
                "Last-Modified": request.getResponseHeader('Last-Modified'),
                "entity": request.responseText
            };

            var signature = openremote.REST._generate_cache_signature(url);
            openremote.REST._remove_deprecated_cache_signature(signature);
            openremote.REST._addToArray(openremote.REST.cacheHeaders, signature, _cachedHeaders);
        }
    },
    setAccepts: function (acceptHeader) {
        openremote.REST.log("setAccepts(" + acceptHeader + ")");
        this.acceptHeader = acceptHeader;
    },
    setCredentials: function (username, password) {
        this.password = password;
        this.username = username;
    },
    setEntity: function (entity) {
        openremote.REST.log("setEntity(" + entity + ")");
        this.entity = entity;
    },
    setContentType: function (contentType) {
        openremote.REST.log("setContentType(" + contentType + ")");
        this.contentTypeHeader = contentType;
    },
    setURI: function (uri) {
        openremote.REST.log("setURI(" + uri + ")");
        this.uri = uri;
    },
    setMethod: function (method) {
        openremote.REST.log("setMethod(" + method + ")");
        this.method = method;
    },
    setAsync: function (async) {
        openremote.REST.log("setAsync(" + async + ")");
        this.async = async;
    },
    addCookie: function (name, value) {
        openremote.REST.log("addCookie(" + name + "=" + value + ")");
        openremote.REST._addToArray(this.cookies, name, value);
    },
    addQueryParameter: function (name, value) {
        openremote.REST.log("addQueryParameter(" + name + "=" + value + ")");
        openremote.REST._addToArray(this.queryParameters, name, value);
    },
    addMatrixParameter: function (name, value) {
        openremote.REST.log("addMatrixParameter(" + name + "=" + value + ")");
        openremote.REST._addToArray(this.matrixParameters, name, value);
    },
    addFormParameter: function (name, value) {
        openremote.REST.log("addFormParameter(" + name + "=" + value + ")");
        openremote.REST._addToArray(this.formParameters, name, value);
    },
    addForm: function (name, value) {
        openremote.REST.log("addForm(" + name + "=" + value + ")");
        openremote.REST._addToArray(this.forms, name, value);
    },
    addHeader: function (name, value) {
        openremote.REST.log("addHeader(" + name + "=" + value + ")");
        openremote.REST._addToArray(this.headers, name, value);
    }
};

openremote.REST.log = function (string) {
    if (openremote.REST.loglevel > 0)
        console.log(string);
};

openremote.REST._addToArray = function (array, name, value) {
    // JsInterop method to convert lists to JS Arrays
    value = JsUtil.toJsArray(value);

    if (Array.isArray(value)) {
        for (var i = 0; i < value.length; i++) {
            array.push([name, value[i]]);
        }
    } else {
        array.push([name, value]);
    }
};

openremote.REST._generate_cache_signature = function (url) {
    return url.replace(/\?resteasy_jsapi_anti_cache=\d+/, '');
};

openremote.REST._remove_deprecated_cache_signature = function (signature) {
    for (idx in openremote.REST.cacheHeaders) {
        var _signature = openremote.REST.cacheHeaders[idx][0];
        if (signature == _signature) {
            openremote.REST.cacheHeaders.splice(idx, 1);
        }
    }

};

openremote.REST._get_cache_signature = function (signature) {
    for (idx in openremote.REST.cacheHeaders) {
        var _signature = openremote.REST.cacheHeaders[idx][0];
        if (signature == _signature) {
            return openremote.REST.cacheHeaders[idx];
        }
    }
    return null;
};

openremote.REST._complete = function (request, callback) {
    openremote.REST.log("Request ready state: " + request.readyState);
    if (request.readyState == 4) {
        var entity;
        openremote.REST.log("Request response status: " + request.status);

        if (request.status >= 200 && request.status < 300) {
            request.onreadystatechange = null;
            entity = openremote.REST._readResponseEntity(request);
        } else if (request.status == 400) {
            entity = openremote.REST._readResponseEntity(request);
        }

        if (request.status == 304) {
            entity = openremote.REST._get_cache_signature(openremote.REST._generate_cache_signature(request.url))[1]['entity'];
        }
        openremote.REST.log("Calling callback with: " + entity);
        callback(request.status, request, entity);
    }
};

openremote.REST._readResponseEntity= function (request) {
    /* TODO: we marshall in GWT
    var contentType = request.getResponseHeader("Content-Type");
    if (contentType != null) {
        if (openremote.REST._isXMLMIME(contentType))
            return request.responseXML;
        else if (openremote.REST._isJSONMIME(contentType))
            return JSON.parse(request.responseText);
        else
            return request.responseText;
    } else {
        return request.responseText;
    }
    */
    return request.responseText;
};

openremote.REST._isXMLMIME = function (contentType) {
    return contentType == "text/xml"
        || contentType == "application/xml"
        || (contentType.indexOf("application/") == 0
        && contentType.lastIndexOf("+xml") == (contentType.length - 4));
};

openremote.REST._isJSONMIME = function (contentType) {
    return contentType == "application/json"
        || (contentType.indexOf("application/") == 0
        && contentType.lastIndexOf("+json") == (contentType.length - 5));
};

/* Encoding */

openremote.REST.Encoding = {};

openremote.REST.Encoding.hash = function (a) {
    var ret = {};
    for (var i = 0; i < a.length; i++)
        ret[a[i]] = 1;
    return ret;
};

//
// rules

openremote.REST.Encoding.Alpha = ['a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
    'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'];

openremote.REST.Encoding.Numeric = ['0', '1', '2', '3', '4', '5', '6', '7', '8', '9'];

openremote.REST.Encoding.AlphaNum = [].concat(openremote.REST.Encoding.Alpha, openremote.REST.Encoding.Numeric);

openremote.REST.Encoding.AlphaNumHash = openremote.REST.Encoding.hash(openremote.REST.Encoding.AlphaNum);

/**
 * unreserved = ALPHA / DIGIT / "-" / "." / "_" / "~"
 */
openremote.REST.Encoding.Unreserved = [].concat(openremote.REST.Encoding.AlphaNum, ['-', '.', '_', '~']);

/**
 * gen-delims = ":" / "/" / "?" / "#" / "[" / "]" / "@"
 */
openremote.REST.Encoding.GenDelims = [':', '/', '?', '#', '[', ']', '@'];

/**
 * sub-delims = "!" / "$" / "&" / "'" / "(" / ")" / "*" / "+" / "," / ";" / "="
 */
openremote.REST.Encoding.SubDelims = ['!', '$', '&', '\'', '(', ')', '*', '+', ',', ';', '='];

/**
 * reserved = gen-delims | sub-delims
 */
openremote.REST.Encoding.Reserved = [].concat(openremote.REST.Encoding.GenDelims, openremote.REST.Encoding.SubDelims);

/**
 * pchar = unreserved | escaped | sub-delims | ":" | "@"
 *
 * Note: we don't allow escaped here since we will escape it ourselves, so we don't want to allow them in the
 * unescaped sequences
 */
openremote.REST.Encoding.PChar = [].concat(openremote.REST.Encoding.Unreserved, openremote.REST.Encoding.SubDelims, [':', '@']);

/**
 * path_segment = pchar <without> ";"
 */
openremote.REST.Encoding.PathSegmentHash = openremote.REST.Encoding.hash(openremote.REST.Encoding.PChar);
delete openremote.REST.Encoding.PathSegmentHash[";"];

/**
 * path_param_name = pchar <without> ";" | "="
 */
openremote.REST.Encoding.PathParamHash = openremote.REST.Encoding.hash(openremote.REST.Encoding.PChar);
delete openremote.REST.Encoding.PathParamHash[";"];
delete openremote.REST.Encoding.PathParamHash["="];

/**
 * path_param_value = pchar <without> ";"
 */
openremote.REST.Encoding.PathParamValueHash = openremote.REST.Encoding.hash(openremote.REST.Encoding.PChar);
delete openremote.REST.Encoding.PathParamValueHash[";"];

/**
 * query = pchar / "/" / "?"
 */
openremote.REST.Encoding.QueryHash = openremote.REST.Encoding.hash([].concat(openremote.REST.Encoding.PChar, ['/', '?']));
// deviate from the RFC to disallow separators such as "=", "@" and the famous "+" which is treated as a space
// when decoding
delete openremote.REST.Encoding.QueryHash["="];
delete openremote.REST.Encoding.QueryHash["&"];
delete openremote.REST.Encoding.QueryHash["+"];

/**
 * fragment = pchar / "/" / "?"
 */
openremote.REST.Encoding.FragmentHash = openremote.REST.Encoding.hash([].concat(openremote.REST.Encoding.PChar, ['/', '?']));

// HTTP

openremote.REST.Encoding.HTTPSeparators = ["(", ")", "<", ">", "@"
    , ",", ";", ":", "\\", "\""
    , "/", "[", "]", "?", "="
    , "{", "}", ' ', '\t'];

// This should also hold the CTLs but we never need them
openremote.REST.Encoding.HTTPChar = [];
(function () {
    for (var i = 32; i < 127; i++)
        openremote.REST.Encoding.HTTPChar.push(String.fromCharCode(i));
})();

// CHAR - separators
openremote.REST.Encoding.HTTPToken = openremote.REST.Encoding.hash(openremote.REST.Encoding.HTTPChar);
(function () {
    for (var i = 0; i < openremote.REST.Encoding.HTTPSeparators.length; i++)
        delete openremote.REST.Encoding.HTTPToken[openremote.REST.Encoding.HTTPSeparators[i]];
})();

//
// functions

//see http://www.w3.org/TR/html4/interact/forms.html#h-17.13.4.1
//and http://www.apps.ietf.org/rfc/rfc1738.html#page-4
openremote.REST.Encoding.encodeFormNameOrValue = function (val) {
    return openremote.REST.Encoding.encodeValue(val, openremote.REST.Encoding.AlphaNumHash, true);
};


//see http://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.2
openremote.REST.Encoding.encodeHeaderName = function (val) {
    // token+ from http://www.w3.org/Protocols/rfc2616/rfc2616-sec2.html#sec2

    // There is no way to encode a header name. it is either a valid token or invalid and the
    // XMLHttpRequest will fail (http://www.w3.org/TR/XMLHttpRequest/#the-setrequestheader-method)
    // What we could do here is throw if the value is invalid
    return val;
};

//see http://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.2
openremote.REST.Encoding.encodeHeaderValue = function (val) {
    // *TEXT or combinations of token, separators, and quoted-string from http://www.w3.org/Protocols/rfc2616/rfc2616-sec2.html#sec2
    // FIXME: implement me. Stef has given up, since it involves latin1, quoted strings, MIME encoding (http://www.ietf.org/rfc/rfc2047.txt)
    // which mentions a limit on encoded value of 75 chars, which should be split into several lines. This is mad.
    return val;
};

// see http://www.ietf.org/rfc/rfc3986.txt
openremote.REST.Encoding.encodeQueryParamNameOrValue = function (val) {
    return openremote.REST.Encoding.encodeValue(val, openremote.REST.Encoding.QueryHash);
};

//see http://www.ietf.org/rfc/rfc3986.txt
openremote.REST.Encoding.encodePathSegment = function (val) {
    return openremote.REST.Encoding.encodeValue(val, openremote.REST.Encoding.PathSegmentHash);
};

//see http://www.ietf.org/rfc/rfc3986.txt
openremote.REST.Encoding.encodePathParamName = function (val) {
    return openremote.REST.Encoding.encodeValue(val, openremote.REST.Encoding.PathParamHash);
};

//see http://www.ietf.org/rfc/rfc3986.txt
openremote.REST.Encoding.encodePathParamValue = function (val) {
    return openremote.REST.Encoding.encodeValue(val, openremote.REST.Encoding.PathParamValueHash);
};

openremote.REST.Encoding.encodeValue = function (val, allowed, form) {
    if (typeof val != "string") {
        return val;
    }
    if (val.length == 0) {
        return val;
    }
    var ret = '';
    for (var i = 0; i < val.length; i++) {
        var first = val[i];
        if (allowed[first] == 1) {
            ret = ret.concat(first);
        } else if (form && (first == ' ' || first == '\n')) {
            // special rules for application/x-www-form-urlencoded
            if (first == ' ')
                ret += '+';
            else
                ret += '%0D%0A';
        } else {
            // See http://www.faqs.org/rfcs/rfc2781.html 2.2

            // switch to codepoint
            first = val.charCodeAt(i);
            // utf-16 pair?
            if (first < 0xD800 || first > 0xDFFF) {
                // just a single utf-16 char
                ret = ret.concat(openremote.REST.Encoding.percentUTF8(first));
            } else {
                if (first > 0xDBFF || i + 1 >= val.length)
                    throw "Invalid UTF-16 value: " + val;
                var second = val.charCodeAt(++i);
                if (second < 0xDC00 || second > 0xDFFF)
                    throw "Invalid UTF-16 value: " + val;
                // char = 10 lower bits of first shifted left + 10 lower bits of second
                var c = ((first & 0x3FF) << 10) | (second & 0x3FF);
                // and add this
                c += 0x10000;
                // char is now 32 bit unicode
                ret = ret.concat(openremote.REST.Encoding.percentUTF8(c));
            }
        }
    }
    return ret;
};

// see http://tools.ietf.org/html/rfc3629
openremote.REST.Encoding.percentUTF8 = function (c) {
    if (c < 0x80)
        return openremote.REST.Encoding.percentByte(c);
    if (c < 0x800) {
        var first = 0xC0 | ((c & 0x7C0) >> 6);
        var second = 0x80 | (c & 0x3F);
        return openremote.REST.Encoding.percentByte(first, second);
    }
    if (c < 0x10000) {
        var first = 0xE0 | ((c >> 12) & 0xF);
        var second = 0x80 | ((c >> 6) & 0x3F);
        var third = 0x80 | (c & 0x3F);
        return openremote.REST.Encoding.percentByte(first, second, third);
    }
    if (c < 0x110000) {
        var first = 0xF0 | ((c >> 18) & 0x7);
        var second = 0x80 | ((c >> 12) & 0x3F);
        var third = 0x80 | ((c >> 6) & 0x3F);
        var fourth = 0x80 | (c & 0x3F);
        return openremote.REST.Encoding.percentByte(first, second, third, fourth);
    }
    throw "Invalid character for UTF-8: " + c;
};

openremote.REST.Encoding.percentByte = function () {
    var ret = '';
    for (var i = 0; i < arguments.length; i++) {
        var b = arguments[i];
        if (b >= 0 && b <= 15)
            ret += "%0" + b.toString(16);
        else
            ret += "%" + b.toString(16);
    }
    return ret;
};

openremote.REST.serialiseXML = function (node) {
    if (typeof XMLSerializer != "undefined")
        return (new XMLSerializer()).serializeToString(node);
    else if (node.xml) return node.xml;
    else throw "XML.serialize is not supported or can't serialize " + node;
};
