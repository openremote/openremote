# Load Tests

Scripts and configuration files to setup an OpenRemote stack running in an EKS cluster appropriate for load testing.  
This includes configuring the cluster to use a more powerful machine
and adapting the values files to configure the memory usage on the different pods.  
For the manager, JVM parameters are used to make use of the extra memory available to the container.  
It also uses a custom manager image (available in the ECR) that includes setup code to create assets and users used during the tests.

Once deployed, running the load tests can be done using the same clients as for load testing a VM,
see `load1` folder for the tools, scripts and documentation.

## Creating load test manager image

Build from the repository root using the checkout you want to test. The current
setup build supports `SETUP_JAR=load1` directly; no Gradle file edits are needed.
The load profile uses the custom `load1` tag, so upgrading the Manager chart's
`appVersion` does not upgrade this image. Rebuild and push it for each Manager
version you want to test:

```
./gradlew -PSETUP_JAR=load1 clean installDist

# The Manager Dockerfile copies lib, but not deployment/manager/extensions.
# Include the custom setup JAR on the image's application classpath.
cp manager/build/install/manager/deployment/manager/extensions/openremote-load1-setup-*.jar \
    manager/build/install/manager/lib/

export AWS_DEVELOPERS_ACCOUNT_ID="dev-account-id"
aws ecr get-login-password --region eu-west-1 | docker login --username AWS --password-stdin $AWS_DEVELOPERS_ACCOUNT_ID.dkr.ecr.eu-west-1.amazonaws.com
docker buildx build --push --platform linux/amd64,linux/arm64 -t $AWS_DEVELOPERS_ACCOUNT_ID.dkr.ecr.eu-west-1.amazonaws.com/openremote/manager:load1 manager/build/install/manager/
```

The profile uses `image.pullPolicy: Always` so new Manager Pods pull the current
`load1` image. Pushing the tag does not restart existing Pods. Keycloak and
PostgreSQL use the image versions in their Kubernetes charts.

The JAR copy above is required: without it, the image can initialize Keycloak
and allow login, but the custom load-test setup provider is absent and no test
users or assets are created. During a clean initialization, Manager logs should
identify `org.openremote.setup.load1.SetupTasks` as a custom setup provider.

## Authentication configuration

Run `bash ./eks-setup-load.sh` from `test/load1-eks` after configuring
`eks-common.sh` and building the custom image. The script sets Keycloak's
`KC_HOSTNAME` to `https://$FQDN/auth` so internal discovery and browser tokens
use the same issuer. Manager keeps the bare hostname and receives
`OR_WEBSERVER_ALLOWED_ORIGINS=https://$FQDN`: CORS requires a scheme and must
not include the `/auth` path.

Before starting a load run, verify browser login, creation and editing of an
asset, and an authenticated MQTT publish using a provisioned test account.
