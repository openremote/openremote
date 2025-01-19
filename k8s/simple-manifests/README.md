# Simple manifests

This folder contains all manifest files required to start an OR stack within kubernetes.
It has been tested under Docker Desktop on macOS.

## TL;DR

Edit the *-pv.yaml files and update the spec.hostPath.path entry to point to a folder on your local machine.
Under macOS, it needs to be located under your home folder (/Users/xxx/...)

You can then apply all the files at once, using `ls -1 or*.yaml | xargs -L 1 kubectl apply -f `

## Explanation of the different manifest files

TODO
