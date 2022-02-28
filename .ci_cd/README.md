# CI/CD workflow files
The files in this directory are used by the [CI/CD workflow](../.github/workflows/ci_cd.yml) which provides a standard
github workflow for CI/CD tasks in this repo and custom projects/forks on a linux host via SSH.

# `ci_cd.json` File
Configuration file for distribution (pushing openremote/manager docker images) and deploying for this specific
repository; the file is only required if automatic deployments are desired during push/release and/or openremote/manager
docker image tag updates. The file layout is:

```json
{
  // Configure behaviour on push
  "push": {
    // Name of branch to react to (exact match)
    "develop": {
      // Comma separated list of tags to push to openremote/manager docker hub image $version is replaced with release version
      // This can only be used on the main openremote repo (openremote/openremote)
      "distribute": {
        "tags": "develop"
      },
      // Singleton or array of deployments to execute, a deployment consists of (environment and/or managerTag see variables for explanation)
      "deploy": {
        "managerTag": "develop", // Can use #ref or exclude this to build COMMIT specific maanger docker image  
        "environment": "staging"
      }
    },
    "master": {
      "distribute": {
        "tags": "latest"
      }
    }
  },
  // Configure behaviour on release
  "release": {
    // Comma separated list of tags to push to openremote/manager docker hub image $version is replaced with release version
    // This can only be used on the main openremote repo (openremote/openremote)
    "distribute": {
      "tags": "latest,$version"
    },
    // Singleton or array of deployments to execute, a deployment consists of (environment and/or managerTag see variables for explanation)
    "deploy": {
      "managerTag": "latest", // Can use #ref or exclude this to build COMMIT specific maanger docker image
      "environment": "production"
    }
  },
  // Configure behaviour on manager docker image update (must be triggered by schedule event from a custom project or fork)
  "managerDockerPush": {
    // Docker tags to monitor can only be used if not the main openremote repo (openremote/openremote)
    "develop": {
        // Singleton or array of deployments to execute, a deployment consists of (environment and/or managerTag see variables for explanation)
        "deploy": {
            "environment": "staging"
        }      
    }
  }
}
```

# `deploy.sh` Bash script
Bash script that handles the actual deployment to a specific host; environment variables should be already loaded into
the shell or be available in `temp/env` file which will be automatically loaded. If `ssh.env` is found this env file is
also automatically loaded. See [Variables section below](#variables) for available and required values.

## Docker compose file resolution
The docker compose file used for the deployment is resolved as follows; if resolved file does not exist then deployment
will fail:
* `ENV_COMPOSE_FILE` variable
* `profile/${ENVIRONMENT}.yml` file
* `docker-compose.yml` file

# `env` Directory
Environment variable files that will be loaded when deploying.

The naming convention should be as follows:
* `env/.env` - File containing environment variables that will be loaded for all deployments
* `${ENVIRONMENT}.env` - File containing environment specific environment variables will be loaded if `ENVIRONMENT` variable
is set for the workflow run; these will be loaded after the `env` file and so can override any value defined there as well
as being able to add new environment specific values

# `host_init` Directory
Contains scripts/files required to initialise the host ready for running the stack (e.g. download map tiles etc.);
the workflow looks for one of the following bash scripts (in priority order):

* `host_init/${ENVIRONMENT}.sh` - Bash script for environment specific host initialisation
* `host_init/init.sh` - Bash script that can be used by any environment as a fallback if no environment specific script
exists

If an initialisation script is found then the entire `host_init` directory will be copied to the host and the script
will be executed; by copying the entire directory additional files can be included and called from the initialisation
script as required.


# Variables
The following variables are supported by deployments; any additional variables can also be used and these are made
available to the shell where the docker compose stack is brought up; so any variables required in the docker compose
profile can be specified in any of the places variables are loaded from (github secrets, inputs, and/or env files).

* `ENVIRONMENT` - Used to control which env file(s), docker compose file and github secrets to use for deployment.
* `MANAGER_TAG` - The docker tag to pull for the manager image (if not specified)
* `CLEAN_INSTALL` - Indicates if the or_postgresql-data volume should be removed before starting the stack
* `COMMIT` - Which commit or branch to checkout and use on this repo (default: branch/SHA on which workflow is executed)
* `HOST` - FQDN for the host (default: env/secrets.HOST)
* `SSH_USER` - SSH username (default: root)
* `SSH_KEY` - SSH private key (this is written to `ssh.key` file and loaded by `deploy.sh` for SSH/SCP commands)
* `SSH_PASSWORD` - SSH password (alternative to private key authentication)
* `SSH_PORT` - SSH port (default: 22)
* `SETUP_ADMIN_PASSWORD` - Admin password to be set on deployment; sets the SETUP_ADMIN_PASSWORD env variable
* `ENV_COMPOSE_FILE` - The full path to the docker compose file to use for the deployment

## Github secrets
Variables are also loaded from Github secrets using the '_{ENVIRONMENT}_' prefix naming convention (note it doesn't use
github environment secrets as these only seem to be supported for public repositories); any secrets that begin with `_`
will only be loaded if they begin with `_{ENVIRONMENT}_` and this prefix will be removed, all other secrets will be
loaded independent of the `ENVIRONMENT` variable.
