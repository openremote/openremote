# @openremote/or-rive-renderer
[![NPM Version][npm-image]][npm-url]

Package containing a Web Component, built with [Lit](https://lit.dev), for displaying `.riv` files from the [Rive](https://rive.app) ecosystem.

## Install
```bash
npm i @openremote/or-rive-renderer
yarn add @openremote/or-rive-renderer
```

## Usage
```html
<or-rive-renderer url="animations/my-animation.riv"></or-rive-renderer>
```

A `.riv` file can hold several artboards and state machines. `artboard` picks which one to render and
`stateMachines` names the state machines to run; both default to the ones marked as default in the file. The renderer
resizes with its host element.

```html
<or-rive-renderer url="animations/my-animation.riv" artboard="Dashboard"
                  .stateMachines="${["State Machine 1"]}">
</or-rive-renderer>
```

### Driving the animation
`setValue` writes to a [data binding](https://rive.app/docs/runtimes/web/data-binding) property of the running
animation, which is how live data is fed into it. The type is inferred from the value, and `string`, `boolean`,
`number`, `color` and `enum` are supported; anything else fires the named trigger.

```typescript
const renderer = this.shadowRoot.querySelector("or-rive-renderer");

await renderer.setValue("temperature", 21.5);
await renderer.setValue("statusColor", "#4caf50", "color");
await renderer.setValue("blink");
```

Calls made before the file has loaded are queued and applied once it has, so there is no need to wait for the
animation to be ready.

## License
[GNU AGPL](https://www.gnu.org/licenses/agpl-3.0.en.html)

[npm-image]: https://img.shields.io/npm/v/@openremote/or-rive-renderer.svg
[npm-url]: https://www.npmjs.com/package/@openremote/or-rive-renderer
