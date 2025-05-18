// @ts-check
// This file is injected into the registry as text, no dependencies are allowed.

/** @typedef {import('@playwright/experimental-ct-core/types/component').ObjectComponent} ObjectComponent */
/** @typedef {new (...args: any[]) => HTMLElement} FrameworkComponent */

const __pwListeners = new Map();

/**
 * @param {HTMLElement} webComponent
 */
function __pwUpdateProps(webComponent, props = {}) {
  for (const [key, value] of Object.entries(props)) webComponent[key] = value;
}

/**
 * @param {HTMLElement} webComponent
 */
function __pwRemoveEvents(webComponent, events = {}) {
  for (const [key] of Object.entries(events)) {
    webComponent.removeEventListener(key, __pwListeners.get(key));
    __pwListeners.delete(key);
  }
}

/**
 * @param {HTMLElement} webComponent
 */
function __pwUpdateEvents(webComponent, events = {}) {
  for (const [key, listener] of Object.entries(events)) {
    const fn = (event) => listener(/** @type {CustomEvent} */ (event).detail);
    webComponent.addEventListener(key, fn);
    __pwListeners.set(key, fn);
  }
}

/**
 * @param {HTMLElement} webComponent
 */
function __pwUpdateSlots(webComponent, slots = {}) {
  for (const [key, value] of Object.entries(slots)) {
    let slotElements;
    if (typeof value !== "object") slotElements = [__pwCreateSlot(value)];

    if (Array.isArray(value)) slotElements = value.map(__pwCreateSlot);

    if (!slotElements) throw new Error(`Invalid slot with name: \`${key}\` supplied to \`mount()\``);

    for (const slotElement of slotElements) {
      if (!slotElement) throw new Error(`Invalid slot with name: \`${key}\` supplied to \`mount()\``);

      if (key === "default") {
        webComponent.appendChild(slotElement);
        continue;
      }

      if (slotElement.nodeName === "#text") {
        throw new Error(
          `Invalid slot with name: \`${key}\` supplied to \`mount()\`, expected \`HTMLElement\` but received \`TextNode\`.`
        );
      }

      slotElement.slot = key;
      webComponent.appendChild(slotElement);
    }
  }
}

/**
 * @param {any} value
 * @return {?HTMLElement}
 */
function __pwCreateSlot(value) {
  return /** @type {?HTMLElement} */ (document.createRange().createContextualFragment(value).firstChild);
}

/**
 * @param {ObjectComponent} component
 */
function __pwCreateComponent(component) {
  const webComponent = new component.type();
  __pwUpdateProps(webComponent, component.props);
  __pwUpdateSlots(webComponent, component.slots);
  __pwUpdateEvents(webComponent, component.on);
  return webComponent;
}

window.playwrightMount = async (component, rootElement, hooksConfig) => {
  if (component.__pw_type === "jsx") throw new Error("JSX mount notation is not supported");

  for (const hook of window.__pw_hooks_before_mount || []) await hook({ hooksConfig, App: component.type });

  const webComponent = __pwCreateComponent(component);

  rootElement.appendChild(webComponent);

  for (const hook of window.__pw_hooks_after_mount || []) await hook({ hooksConfig, instance: webComponent });
};

window.playwrightUpdate = async (rootElement, component) => {
  if (component.__pw_type === "jsx") throw new Error("JSX mount notation is not supported");

  const webComponent = /** @type {?HTMLElement} */ (rootElement.firstChild);
  if (!webComponent) throw new Error("Component was not mounted");

  __pwUpdateProps(webComponent, component.props);
  __pwUpdateSlots(webComponent, component.slots);
  __pwRemoveEvents(webComponent, component.on);
  __pwUpdateEvents(webComponent, component.on);
};

window.playwrightUnmount = async (rootElement) => {
  rootElement.replaceChildren();
};

// REMOVE

export function isImportRef(value) {
  return typeof value === "object" && value && value.__pw_type === "importRef";
}

export class ImportRegistry {
  _registry = new Map();

  initialize(components) {
    for (const [name, value] of Object.entries(components)) this._registry.set(name, value);
  }

  async resolveImportRef(importRef) {
    const importFunction = this._registry.get(importRef.id);
    if (!importFunction)
      throw new Error(
        `Unregistered component: ${importRef.id}. Following components are registered: ${[...this._registry.keys()]}`
      );
    let importedObject = await importFunction();
    if (!importedObject) throw new Error(`Could not resolve component: ${importRef.id}.`);
    if (importRef.property) {
      importedObject = importedObject[importRef.property];
      if (!importedObject) throw new Error(`Could not instantiate component: ${importRef.id}.${importRef.property}.`);
    }
    return importedObject;
  }
}

function isFunctionRef(value) {
  return value && typeof value === "object" && value.__pw_type === "function";
}

export function wrapObject(value, callbacks) {
  return transformObject(value, (v) => {
    if (typeof v === "function") {
      const ordinal = callbacks.length;
      callbacks.push(v);
      const result = {
        __pw_type: "function",
        ordinal,
      };
      return { result };
    }
  });
}

export async function unwrapObject(value) {
  return transformObjectAsync(value, async (v) => {
    if (isFunctionRef(v)) {
      const result = (...args) => {
        window.__ctDispatchFunction(v.ordinal, args);
      };
      return { result };
    }
    if (isImportRef(v)) return { result: await window.__pwRegistry.resolveImportRef(v) };
  });
}

export function transformObject(value, mapping) {
  const result = mapping(value);
  if (result) return result.result;
  if (value === null || typeof value !== "object") return value;
  if (value instanceof Date || value instanceof RegExp || value instanceof URL) return value;
  if (Array.isArray(value)) {
    const result = [];
    for (const item of value) result.push(transformObject(item, mapping));
    return result;
  }
  if (value?.__pw_type === "jsx" && typeof value.type === "function") {
    throw new Error(
      [
        `Component "${value.type.name}" cannot be mounted.`,
        `Most likely, this component is defined in the test file. Create a test story instead.`,
        `For more information, see https://playwright.dev/docs/test-components#test-stories.`,
      ].join("\n")
    );
  }
  const result2 = {};
  for (const [key, prop] of Object.entries(value)) result2[key] = transformObject(prop, mapping);
  return result2;
}

export async function transformObjectAsync(value, mapping) {
  const result = await mapping(value);
  if (result) return result.result;
  if (value === null || typeof value !== "object") return value;
  if (value instanceof Date || value instanceof RegExp || value instanceof URL) return value;
  if (Array.isArray(value)) {
    const result = [];
    for (const item of value) result.push(await transformObjectAsync(item, mapping));
    return result;
  }
  const result2 = {};
  for (const [key, prop] of Object.entries(value)) result2[key] = await transformObjectAsync(prop, mapping);
  return result2;
}

window.__pwRegistry = new ImportRegistry();
window.__pwUnwrapObject = unwrapObject;
window.__pwTransformObject = transformObject;
