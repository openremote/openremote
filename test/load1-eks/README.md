# Load Tests

Scripts and configuration files to setup an OpenRemote stack running in an EKS cluster appropriate for load testing.  
This includes configuring the cluster to use a more powerful machine
and adapting the values files to configure the memory usage on the different pods.  
For the manager, JVM parameters are used to make use of the extra memory available to the container.  
It also uses a custom manager image (available in the ECR) that includes setup code to create assets and users used during the tests.

Once deployed, running the load tests can be done using the same clients as for load testing a VM,
see `load1` folder for the tools, scripts and documentation.

## Creating load test manager image

There seems to be a problem in the setup gradle build file, and you need to comment out the
```groovy
tasks.register('demoJar', Jar) {
    base {
        archivesName = "openremote-demo-${project.name}"
    }
    from sourceSets.demo.output
}
```
section from `setup/build.gradle` to properly build the load test image.

After that, run
```
./gradlew -PSETUP_JAR=load1 clean installDist

export AWS_DEVELOPERS_ACCOUNT_ID="dev-account-id"
aws ecr get-login-password --region eu-west-1 | docker login --username AWS --password-stdin $AWS_DEVELOPERS_ACCOUNT_ID.dkr.ecr.eu-west-1.amazonaws.com
docker buildx build --push --platform linux/amd64,linux/arm64 -t $AWS_DEVELOPERS_ACCOUNT_ID.dkr.ecr.eu-west-1.amazonaws.com/openremote/manager:load1 manager/build/install/manager/
```
