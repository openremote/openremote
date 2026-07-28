# @openremote/or-app  \<or-app\>
[![NPM Version][npm-image]][npm-url]

Web Component that deals with state management, pages and login flows for OpenRemote apps.

## Install
```bash
npm i @openremote/or-app
yarn add @openremote/or-app
```

## Usage
For a full list of properties, methods and options refer to the TypeDoc generated [documentation]().

`or-app` is constructed with a Redux store rather than being placed in markup; it initialises the `Manager` from
`managerConfig`, renders the header, and routes between pages. The store must include the `appReducer` under the `app`
key.

```typescript
import {combineReducers, configureStore} from "@reduxjs/toolkit";
import {appReducer, OrApp} from "@openremote/or-app";
import "@openremote/or-app";

const store = configureStore({
    reducer: combineReducers({app: appReducer})
});

const orApp = new OrApp(store);

orApp.managerConfig = {
    managerUrl: "http://localhost:8080",
    auth: Auth.KEYCLOAK,
    autoLogin: true
};

orApp.appConfig = {
    pages: [pageMapProvider(store), pageAssetsProvider(store)],
    realms: {
        default: {
            appTitle: "My App",
            header: {
                mainMenu: [{icon: "map", text: "map", href: "map"}],
                secondaryMenu: [{icon: "logout", text: "logout", action: () => orApp.logout()}]
            }
        }
    }
};

document.body.appendChild(orApp);
```

Use `appConfigProvider` when the configuration depends on the authenticated user, for example to show pages only to
super users. It is called with the initialised `Manager`, and only if `appConfig` has not been set.

```typescript
orApp.appConfigProvider = (manager) => ({
    pages: manager.isSuperUser() ? allPages : userPages
});
```

### Pages
A page is a `Page` subclass paired with a `PageProvider` that names it and declares its routes. `or-app` calls
`pageCreator` on every navigation, so a page is rebuilt rather than reused, and stays subscribed to the store while
mounted.

```typescript
export function pageExampleProvider(store: Store<AppStateKeyed>): PageProvider<AppStateKeyed> {
    return {
        name: "example",
        routes: ["example", "example/:id"],
        allowOffline: false,
        pageCreator: () => new PageExample(store)
    };
}

@customElement("page-example")
export class PageExample extends Page<AppStateKeyed> {

    get name() {
        return "example";
    }

    stateChanged(state: AppStateKeyed) {
        // Called on every store update
    }
}
```

Set `allowOffline` to `true` on pages that remain usable without a manager connection; all others are replaced by the
offline page, which can be overridden with `AppConfig.offlinePage`.

### Realm configuration
`AppConfig.realms` maps a realm name to its title, logos, favicon, language, header and styles. The `default` entry is
merged with the current realm's entry, so a realm only has to declare what it changes. `superUserHeader` replaces the
resulting header for super users.

A header item navigates through `href` or runs an `action`. Its `roles` field hides it from users who lack the
required roles, given as role names, a map of client to role names, or a predicate.

```typescript
const item: HeaderItem = {
    icon: "account-group",
    text: "users",
    href: "users",
    roles: ["read:users"]
};
```

## Supported Browsers
The last 2 versions of all modern browsers are supported, including Chrome, Safari, Opera, Firefox, Edge. In addition,
Internet Explorer 11 is also supported.


## License
[GNU AGPLv3](https://www.gnu.org/licenses/agpl-3.0.en.html)

[npm-image]: https://img.shields.io/npm/v/@openremote/or-app.svg
[npm-url]: https://www.npmjs.com/package/@openremote/or-app
