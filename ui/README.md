# UI Components & Apps
Here you can find the standard OpenRemote web UI components and apps using a monorepo architecture. The code is divided
 into 3 categories by directory:
 
 * `component` - Base OpenRemote web components (built using Polymer) these are written as ES6 modules
 * `app` - Built-in OpenRemote web applications (applications can be built with whatever frameworks/libraries are desired)
 * `demo` - Demos of each web component (provides a development harness for developers working on the components)
  
## Requirements
* Node JS (>=v10.0)
* Yarn (uses yarn workspaces for developing within a mono repo)



## Components
Components can be developed and tested in isolation (with dependencies on other components and/or public modules as required).

## Apps
Apps bring together components and/or public modules and can be written using any framework/library etc that is
compatible with web components (see [here]([https://custom-elements-everywhere.com/)).

## Demos
Generally a 1-1 mapping between components and demos; they provide a simple harness for the components that can be used during
development and optionally can be deployed to offer working demos. 