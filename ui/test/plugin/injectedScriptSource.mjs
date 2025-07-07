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
