# @openremote/theme
[![NPM Version][npm-image]][npm-url]

This is a package that contains CSS files used for styling OpenRemote components and apps.
Defined CSS variables like `--or-color-primary` and `--or-color-text-primary` can be used to customize the look and feel of the OpenRemote components.
Currently, only 1 theme ("default") is provided, but can be expanded upon in the future.
Every theme is expected to have a light and dark counterpart.

## Install
```bash
npm i @openremote/theme
yarn add @openremote/theme
```

## Usage

### Using CSS
```css
@import "@openremote/theme/default.css";
```
### Using JavaScript / TypeScript:
Note: using the `<style>` tag is an example here. Please check the documentation of your JavaScript bundler for proper installation of CSS files.
```typescript
import themeCss from "@openremote/theme/default.css";

// Apply theme to the app
const style = document.createElement("style");
style.id = "orDefaultTheme";
style.textContent = themeCss;
document.head.appendChild(style);
```

## License
[GNU AGPL](https://www.gnu.org/licenses/agpl-3.0.en.html)

[npm-image]: https://img.shields.io/npm/v/@openremote/theme.svg
[npm-url]: https://npmjs.org/package/@openremote/theme
