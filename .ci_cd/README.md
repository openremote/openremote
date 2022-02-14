# CI/CD workflow files
The files in this directory are used by the [deploy workflow](../.github/workflows/deploy.yml) which provides a standard
github workflow for deploying this repo on a linux host via SSH.

# `/env` Directory
Environment variable files that will be loaded into the workflow to configure the behaviour of the workflow and are also
passed through to the host and loaded whenever remote commands are executed via SSH on the host.

The naming convention should be as follows:

* `env/.env` - File containing environment variables that will be loaded for any deployment
* `${environment}.env` - File containing environment specific environment variables will be loaded if `environment` input
is set for the workflow run; these will be loaded after the `env` file and so can override any value defined there as well
as being able to add new environment specific values

# `host_init` Directory
Contains scripts/files required to initialise the host ready for running the stack (e.g. download map tiles etc.);
the workflow looks for one of the following bash scripts (in priority order):

* `host_init/${environment}.sh` - Bash script for environment specific host initialisation
* `host_init/init.sh` - Bash script that can be used by any environment as a fallback if no environment specific script
exists

If an initialisation script is found then the entire `host_init` directory will be copied to the host and the script
will be executed; by copying the entire directory additional files can be included and called from the initialisation
script as required.
