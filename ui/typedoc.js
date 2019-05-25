module.exports = {
    "mode": "modules",
    "out": "docs",
    "exclude": "test",
    "theme": "minimal",
    "ignoreCompilerErrors": true,
    "excludePrivate": true,
    "excludeProtected": true,
    "excludeNotExported": true,
    "target": "es6",
    "moduleResolution": "node",
    "preserveConstEnums": true,
    "stripInternal": true,
    "suppressExcessPropertyErrors": true,
    "suppressImplicitAnyIndexErrors": true,
    "module": "esNext",
    "external-modulemap": ".*node_modules\/(@openremote\/[^\/]+)\/.*"
}