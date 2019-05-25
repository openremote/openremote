# OpenRemote UI Components

Library of UI components that can be used to build web applications that communicate with an OpenRemote Manager.

[Source](https://github.com/openremote/openremote) **·** [Documentation](https://github.com/openremote/openremote/wiki) **·** [Community](https://groups.google.com/forum/#!forum/openremotecommunity) **·** [Issues](https://github.com/openremote/openremote/issues) **·** [Docker Images](https://hub.docker.com/u/openremote/) **·** [OpenRemote Inc.](https://openremote.io)

## Contributing
Please refer to the [Developer Guide](https://github.com/openremote/openremote/wiki/Developer-Guide%3A-Working-on-the-UI) on how to contribute to the UI components.

## Usage
To consume the components in an application simply add the required dependencies to your `package.json` and then import the required component(s) using standard ES6 module syntax; for our own applications we use webpack for bundling (see the existing [apps](../apps) for examples). When using components from a custom project, yarn workspaces can be used to `symlink` the components to the code base allowing for tight development of components within an application.

For usage information on each component refer to the component's `README` file and the TypeDoc generated [documentation]().
