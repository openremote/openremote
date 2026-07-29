# OpenRemote AGENTS.md file

## DB update scripts

Flyway is used to automate execution of DB update scripts.  
When naming scripts, use \<date>_\<time> as version number, date being YYYYMMDD and time being HHmm in UTC e.g. `V20260611_0755__Changes.sql` (the `V` prefix is required by Flyway and must be uppercase)

## Dev environment
The backend is written in Java, we target JDK 21, use modern language features up to that version during implementation.  
Gradle is used as the build system; run `./gradlew clean installDist` for a full clean build.  
In sandboxed environments, add `--offline` (requires dependencies to already be cached).

## Running tests

When running integration tests, part of the stack (PostgreSQL and Keycloak) must be running. Start it with `mkdir -pm 777 tmp && docker compose -f profile/dev-testing.yml -p openremote up -d --no-build`.  
Running `./gradlew clean` deletes the root `tmp/` directory that is mounted into PostgreSQL (see `profile/dev-testing.yml`), so recreate it and restart the stack before running tests again.

## REST resources

### Endpoint roles

Annotate every endpoint with the resource role of its domain, e.g. `read:alarms` or `write:notifications`. Use `read:admin` or `write:admin` only where the domain has no resource role, or where admin-only access is a deliberate decision. The admin roles are standalone, so holding `write:admin` does not grant `write:notifications`.

### Access control

Access control belongs in the resource implementation, not in the service. Extend `ManagerWebResource` and use its checks rather than reaching for the identity provider: `throwIfNotRealmActiveAndAccessible` and `throwIfRestrictedUser` reject with a 403, and `isRealmActiveAndAccessible` and `isRestrictedUser` are there for conditions that need more than a rejection. Resolve the entity first and return 404 when it is missing, then require access to its realm, and apply restricted user rules in the same place. Service methods take already authorised input and state that in their javadoc with "Callers are responsible for enforcing realm authorization." Alarms and notifications are the reference for this shape; equivalent endpoints across the two must enforce the same way. Flows that never pass through a resource, such as notifications published to the message broker, authorise inside the service instead.

## UI

### Generated model types

`ui/component/model/src/model.ts` is generated from the Java backend by typescript-generator. Do not edit it by hand. When a TypeScript type mirrors a backend class, import it from `@openremote/model` instead of redeclaring a local interface. Regenerate it from the backend rather than patching the output.

### Base element

Own Lit components extend `OrElement` from `@openremote/or-element` instead of `LitElement` (also through mixins, e.g. `translate(i18next)(OrElement)`). It applies the shared shadow-DOM styling automatically. Vaadin wrappers are exempt; they extend their Vaadin base and are themed via Lumo.

### Adding a new component package

New packages under `ui/component/` are picked up automatically by the yarn workspace and by Gradle (any dir with a `build.gradle`). The rsbuild apps are not automatic: add the package to the `@openremote/*` alias maps in `ui/app/manager/rsbuild.config.ts` and `ui/app/storybook/rsbuild.config.ts`, which resolve workspace packages to their `src` dirs. A missing entry fails the build with "Module not found" for any file importing the package.

### Adding a new Vaadin component

Vaadin-based components live in `ui/component/or-vaadin-components/src/` as thin wrappers that extend a Vaadin class and re-register it under an `or-vaadin-<name>` tag, e.g. `export class OrVaadinX extends (X as new () => X & LitElement)` with `@customElement("or-vaadin-x")`. Keep all `@vaadin/*` dependency versions aligned.

#### Styling the component's shadow-DOM internals

Pick the approach by how much of the base component you need to keep. The base + Lumo styles are keyed off the element's `static get is()`, so a wrapper that does *not* override `is` still reports the base tag (e.g. `vaadin-checkbox-group`) and inherits the full base + Lumo theme.

**Small tweaks to an existing component (preferred default).** Do NOT override `is`, since that detaches the element from the base + Lumo styles keyed off `is` and strips the inherited theme/layout entirely. Do NOT use `static get styles()` — ThemableMixin injects those *before* Lumo, so Lumo wins; and for wrapper classes that don't override `is`, ThemableMixin may not associate the child's `static get styles()` with the finalized element at all. Instead apply styles in one of these ways:
- **External `::part()` rule (preferred for part properties).** Add the rule in `ui/component/theme/src/components/or-vaadin-<name>.css` (imported from that dir's `index.css`), keyed on the real tag name: `or-vaadin-<tag>::part(<part>) { … }`. This is a plain browser CSS feature — the global stylesheet pierces the shadow DOM boundary through the exposed part with no ThemableMixin involvement. It scopes cleanly to that tag only and is invisible to the `is`-based lookup.
- **Lumo `@media` module injection (for rules that must live inside the shadow DOM).** Use this only when `::part()` is insufficient — e.g. `:host` pseudo-class selectors, `::slotted`, or pseudo-elements that cannot be reached from outside. Define the rules in a named `@media <module-name>` block in the theme CSS file and add that name to `--_lumo-vaadin-<tag>-inject-modules`. The injector stamps those rules into the shadow DOM *after* Lumo, so they always win.
- **Slotted native inputs (`<textarea>`, `<input>`) are light DOM, not shadow DOM.** Style them with a normal descendant rule from the surrounding tree (`or-vaadin-<tag> textarea { … }`) or with inline styles from the wrapper class; both win over the base component's `::slotted()` rules in the cascade.

**A component that fully owns its visuals via `registerStyles()`.** Only when the component re-implements the look from scratch (so it has no inherited theme to preserve) override `is` and register styles from its own module; do NOT use `static get styles()`. Vaadin's `ThemableMixin.finalizeStyles()` injects `static` styles *before* the Lumo theme injector, so the theme overrides them and nothing appears to apply; styles registered via `registerStyles()` are injected *after* the injector, so they always win. Recipe (see `or-vaadin-toggle.ts`):
- Call `registerStyles("or-vaadin-<tag>", css\`...\`)` from `@vaadin/vaadin-themable-mixin/register-styles.js` at module top (before the element is finalized).
- Override `static get is() { return "or-vaadin-<tag>"; }`. The base class reports its own tag (e.g. `vaadin-checkbox`), and `registerStyles`/`getStylesForThis()` key off `is`; without this the styles either don't match or leak onto every instance of the base component. (This same override is what detaches the element from the base theme, which is acceptable here because the component re-styles everything itself.)
- Out-specify base rules with `:host(...)` prefixes (e.g. the checkbox base hides its marker with `:host(:not([checked])) [part='checkbox']::after { opacity: 0 }`).
- Give every themed CSS var a hard-coded fallback (e.g. `var(--lumo-primary-color, #47a942)`) so the component also looks correct standalone, with no theme loaded.

#### Registering an input type in the input pipeline

To make an input type usable through the input pipeline, register it in `or-vaadin-input.ts` (`TEMPLATES` map + a `getXTemplate`, and `nativeValue` if it is a boolean that exposes `checked` instead of `value`) and update `value-input-provider.ts` (boolean/checked types use the `checked` attribute, not `value`).

### Storybook

Component docs are MDX under `ui/app/storybook/docs/components/or-vaadin-<name>.mdx`; the stories are under `ui/component/or-vaadin-components/stories/or-vaadin-<name>.stories.ts`. To add one, copy an existing pair (e.g. `or-vaadin-checkbox`), keeping the relative story import in the MDX and the `ComponentDocs` block.

Stories call `getORStorybookHelpers(tagName)` and `setCustomElementsManifest(customElements)` using the `../custom-elements.json` manifest. That manifest is generated by `npm run analyze`, so a newly added component will render but show no args/argTypes/description until the manifest is regenerated.

Storybook covers any component under `ui/component/` (e.g. `or-chart`, `or-map`, `or-mwc-components`, `or-tree-menu`, `or-vaadin-components`). For each component package the stories live in that package's own `stories/` dir (`ui/component/<pkg>/stories/<name>.stories.ts`), and the human-readable docs are MDX under `ui/app/storybook/docs/components/<name>.mdx`. To add a component, copy an existing pair from the same package, keeping the relative story import in the MDX and the `ComponentDocs` block.

Stories call `getORStorybookHelpers(tagName)` (from `ui/component/storybook-utils.js`) and `setCustomElementsManifest(customElements)` off the package's own `../custom-elements.json`. That manifest is generated per-package by `npm run analyze` (cem), so a newly added component will render but show no args/argTypes/description until you regenerate its package's manifest. `custom-elements.json` (and `custom-elements-jsx.d.ts`) are generated and untracked.

### Writing UI tests

- **Test Naming:** `test.describe` blocks describe a feature. Tests should be named starting with "should ...".
- **Test Structure:** Keep tests flat by default. Omit top-level `test.describe` blocks. If grouping is needed, target a specific feature (e.g., filtering notifications) rather than a parent concept like the whole page. Example: `test.describe("Filter Notifications", ...)` instead of `test.describe("Notifications", ...)`.

#### Fixtures

- **Avoid Redundant Actions:** Do not create methods in fixtures that simply wrap native Playwright actions (e.g., `click()`, `getByRole()`, `expect()`). Use native Playwright actions directly in the tests where possible.
- **Provide Locators:** Provide fixture methods for locators with non-standard or complex paths (e.g., reliant on specific DOM structures) so others can reuse the correct locators across tests.

#### App tests

- **Compilation:** Run `./gradlew clean installDist` after making changes to the UI source code to ensure they are applied before testing.
- **Location:** Define tests in `ui/app/<app-name>/test/`. Define fixtures in `ui/app/<app-name>/test/fixtures/`.
- **Comments:** Add scenario comments above tests (`@given`, `@when`, `@then`, `@and`) based on the acceptance criteria.
- **Auth State:** Select correct `storageState` for the task to be tested. Use `adminStatePath` for master/admin tasks. Use `userStatePath` for regular realm user tasks.

#### Component testing

- **Location:** Define tests in `ui/component/<component-name>/test/`. Define fixtures in `ui/component/<component-name>/test/fixtures/`.

Component tests use Playwright component testing (`@sand4rt/experimental-ct-web`). They live in each package's `test/*.test.ts`, import `{ ct, expect }` from `@openremote/test`, and `mount(ComponentClass, { props, slots, on })`. Run them with `npm test` in the package (which does `tsc -b && playwright test`). CI runs `./gradlew -p ui/component npmTest`, which only executes packages that register an `npmTest` task, so when adding the first test to a package also register `npmTest` (and `npmTestUI`) in its `build.gradle` file; copy the tasks from a sibling package. Prefer web-first, role-based assertions (`getByRole("checkbox", { name }).toBeChecked()`, `toHaveCount(...)`) over poking at JS properties (`toHaveJSProperty`) or internal locators. Some important quirks to know about:

- **Custom elements used as slotted/appended children must be eagerly registered.** Playwright CT turns each imported component into a *lazy* dynamic import that only runs when that component is `mount()`ed, so a child element that is never mounted itself (e.g. `or-vaadin-toggle` slotted inside `or-vaadin-toggle-group`) never gets `customElements.define`d and stays an inert, unupgraded tag that appends to the DOM but does not render. Declare such components in the test itself via `mount(..., { hooksConfig: { components: [OrVaadinToggle] } })`; the `beforeMount` hook in `ui/test/playwright/index.js` resolves their import refs, which runs their modules and registers them before the mount.
- **Use only one `mount()` call per test.** Multiple mounts do not resolve to separate locator paths and can hang until the test times out on a strict mode violation.
- **Imports referenced outside a `mount()` call are evaluated in Node, where browser-only modules crash.** Any such usage (a props-building helper, a spread like `...StandardRenderers`) makes Node evaluate the imported module chain, which crashes on the `require("*.css")` calls in `or-mwc-components` (`SyntaxError: Unexpected token '.'`). Pass non-serializable values such as renderer arrays as bare identifiers inline in the `mount()` props. The inverse also holds: imported plain *data* referenced inside `mount()` is mistaken for a component and replaced by an import-ref object, so extract the needed values to a local variable outside the call first (`const id = importedAsset.id`).
- **The `on` handler receives `event.detail`, not the event** (`listener(event.detail)`). This is inherent to `@sand4rt/experimental-ct-web`, which expects components to emit `CustomEvent`s and forwards only their `detail`, while components may also emit native events (like `change`) that carry no detail. So assert event values via events whose detail carries them; for Vaadin fields that is the `<prop>-changed` notify event (detail `{ value }`), while the native `change` event can only be counted. To await a single event's value, use the `shared.promiseEventDispatch()` fixture helper.
- **Vaadin fires an initial `<prop>-changed` at mount** (notify-on-first-commit), so a freshly mounted toggle emits `checked-changed(false)` before any interaction. Assert on the user-driven transitions (e.g. the last two values), not exact-array equality, and prove "emits nothing" with `not.toContain(true)` rather than `toEqual([])`.
- **Vaadin `<prop>-changed` events do not bubble or compose**, so a listener on a parent/group will not catch a child field's event; attach it to the field itself.
- **The default slot must be an array of single-element strings.** The CT runner builds each slot via `createContextualFragment(str).firstChild`, so a single string containing multiple elements silently keeps only the first; pass `slots: { default: ["<a>…</a>", "<b>…</b>"] }`.
- **Vaadin's `theme` is attribute-only.** `ThemePropertyMixin` derives a read-only `_theme` from the `theme` *attribute*; there is no writable reflecting `theme` property. So `mount(..., { props: { theme: "vertical" } })` sets an ignored JS property and the `:host([theme~='vertical'])` styles never apply (e.g. a "vertical" checkbox-group stays horizontal). Set it as an attribute instead: `await component.evaluate((el) => el.setAttribute("theme", "vertical"))`. (In Storybook/Lit templates `theme="vertical"` is already a real attribute, so it works there.)
- The `on` handler type is `Record<string, Function>`, so typed handler params are fine.
- **Mounted component sources must be JavaScript.** The CT bundle compiles `.ts` against `ui/test/tsconfig.json` (`rootDir` `ui/test`), so a TS component file anywhere else fails with TS6059. Mount components from the package's built lib, or write test-only fixture elements as plain `.js` (no decorators; register with `customElements.define`).
