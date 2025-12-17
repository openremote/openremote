// CSS modules
type CSSModuleClasses = { readonly [key: string]: string };

declare module "*.module.css" {
    const classes: CSSModuleClasses;
    export default classes;
}

// CSS
declare module "*.css" {
    /**
     * @deprecated Use `import style from './style.css?inline'` instead.
     */
    const css: string;
    export default css;
}
