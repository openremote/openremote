# @openremote/rest
[![NPM Version][npm-image]][npm-url]
[![Linux Build][travis-image]][travis-url]
[![Test Coverage][coveralls-image]][coveralls-url]

ES6 modules for connecting to an OpenRemote Manager as well as utilities for performing common tasks.

The default export is a singleton of type `RestApi` that can be used to communicate with the OpenRemote Manager REST API.
It uses an [axios](https://github.com/axios/axios) client to perform the requests and it contains strongly typed
definitions for each OpenRemote Manager REST API endpoint (JAX-RS resource). 



## Install
```bash
npm i @openremote/rest
yarn add @openremote/rest 
```

## Usage
For a full list of properties, methods and options refer to the TypeDoc generated [documentation]().

If used in conjunction with `@openremote/core` and the `Manager` `init` method has been called then the default export
will be ready to use, the endpoints can be accessed via the `RestApi` `api` property and each JAX-RS resource defined
in the OpenRemote Manager is also defined with the same name in the `RestApi` object.

```$typescript
import openremote from "@openremote/core";
import rest from "@openremote/rest";

openremote.init({
    ...
}).then((success) => {
    if (success) {
        let assetQuery = ...;
        let response = await rest.api.AssetResource.queryAssets(assetQuery);
        let assets = response.data;
        
        // Do something with the assets
    } else {
        // Something has gone wrong
    }
});
```

It is possible to add additional request interceptors by calling the `addRequestInterceptor` method, it is also possible
to access the `AxiosInstance` by calling the `axiosInstance` property.

It is also possible to instantiate the `RestApi` object on demand but note you will need to ensure the Authorization
header is correctly set if calling secure endpoints on the OpenRemote Manager REST API.

```$typescript
import {RestApi} from "@openremote/rest";

let rest = new RestApi();
rest.setTimeout(10000);
rest.addRequestInterceptor(...);
rest.initialise();
```


## Supported Browsers
The last 2 versions of all modern browsers are supported, including Chrome, Safari, Opera, Firefox, Edge. In addition,
Internet Explorer 11 is also supported.


## License
[GNU AGPL](https://www.gnu.org/licenses/agpl-3.0.en.html)

[npm-image]: https://img.shields.io/npm/v/live-xxx.svg
[npm-url]: https://npmjs.org/package/@openremote/core
[travis-image]: https://img.shields.io/travis/live-js/live-xxx/master.svg
[travis-url]: https://travis-ci.org/live-js/live-xxx
[coveralls-image]: https://img.shields.io/coveralls/live-js/live-xxx/master.svg
[coveralls-url]: https://coveralls.io/r/live-js/live-xxx?branch=master