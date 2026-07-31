# @openremote/core
[![NPM Version][npm-image]][npm-url]

ES6 modules for connecting to an OpenRemote Manager as well as utilities for performing common tasks.

The default export is a singleton of type `Manager` that can be used to connect to an OpenRemote Manager, initiate
authentication and download common resources (translation files, icons, etc). Everything is initiated by calling
the `init` method, what tasks are performed during initialisation is determined by the `ManagerConfig` passed to the
`init`, the tasks include the following:

* Check the manager exists and is accessible (calls the `api/master/info` endpoint)
* Initialise authentication and perform login redirect (if requested in the `ManagerConfig`)
* Download `mdi` iconset (if requested in the `ManagerConfig` - if not specified iconset will be downloaded)
* Initialise REST API client (`@openremote/rest`) - Sets a timeout of 20s and will also add a request interceptor to
add required `Authorization` header for authentication
* Initialise console (the console is the device used to render the application desktop, Android or iOS device)
* Download built in OpenRemote translation files
* Download Asset Model Descriptors

## Install
```bash
npm i @openremote/core
yarn add @openremote/core 
```

## Usage
For a full list of properties, methods and options refer to the TypeDoc generated [documentation]().

Initialisation is done by calling the `init` method which returns a Promise that is fulfilled with a `boolean` indicating
whether initialisation was successful or not.

Initialisation usage example:

```typescript
import openremote from "@openremote/core";
import {Auth} from "@openremote/model";

openremote.init({
    managerUrl: "http://localhost:8080",
    keycloakUrl: "http://localhost:8080/auth",
    auth: Auth.KEYCLOAK,
    autoLogin: false,
    realm: "building",
    configureTranslationsOptions: (options) => {
        options.lng = "nl"; // Change initial language to dutch rather than english
    }
}).then((success) => {
    if (success) {
        // Load the app
    } else {
        // Something has gone wrong
    }
});
```

Anything left out of the `ManagerConfig` is defaulted by `normaliseConfig`; notably `managerUrl` falls back to the
host serving the app, `realm` to `master` and `auth` to `Auth.KEYCLOAK`.

The singleton emits `OREvent` values for lifecycle changes, which can be observed with `addListener`:

```typescript
openremote.addListener((event) => {
    if (event === OREvent.OFFLINE) {
        // Manager connection lost
    }
});
```

### Asset Mixin (`@openremote/core/asset-mixin`)
Exports a `subscribe` function/mixin that connects a component to the event bus and keeps its subscriptions in step
with the assets or attributes it is interested in. Set `assetIds` to receive both asset and attribute events for those
assets, or `attributeRefs` to receive attribute events for individual attributes only. Both are re-subscribed
automatically when reassigned, and unsubscribed when the element is disconnected.

```typescript
class AssetComponent extends subscribe(openremote)(LitElement) {

    @property({type: String})
    public assetId?: string;

    public willUpdate(changedProps: PropertyValues) {
        if (changedProps.has("assetId")) {
            this.assetIds = this.assetId ? [this.assetId] : undefined;
        }
    }

    // Called for every event received on the current subscriptions
    public _onEvent(event: SharedEvent) {
        if (event.eventType === "attribute") {
            // An attribute value changed
        }
    }

    // Called when the event provider connects and disconnects
    public onEventsConnect() {}
    public onEventsDisconnect() {}

    // Write an attribute; the event must be for a subscribed asset
    protected doSendEvent(event: AttributeEvent) {
        this._sendEvent(event);
    }
}
```

### Events (`@openremote/core/event`)
Provides infrastructure for connecting to the OpenRemote Manager client event bus; by default an `EventProvider` is
initialised by the `Manager` during the initialisation process and can be accessed from `openremote.events` but it is
also possible to instantiate an `EventProvider` manually. `ManagerConfig.eventProviderType` selects between the
WebSocket and polling implementations.

### Util (`@openremote/core/util`)
Various utility methods for common tasks.


## Supported Browsers
The last 2 versions of all modern browsers are supported, including Chrome, Safari, Opera, Firefox, Edge.


## License
[GNU AGPL](https://www.gnu.org/licenses/agpl-3.0.en.html)

[npm-image]: https://img.shields.io/npm/v/@openremote/core.svg
[npm-url]: https://www.npmjs.com/package/@openremote/core
