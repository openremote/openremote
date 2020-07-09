# GitLab CI/CD pipeline

There is .gitlab-ci.yml configuration file which is used to build/test and deploy stack in the GitLab enviroment.
If one imports main repo the pipeline can be started.

## Build docker images

You can skip this stage by adding **skip-build** in the git commit message.

## Test the stack

The second major step in the CI/CD pipeline are functional tests. When extending
the functionality, e.g. by adding a new protocol, tests covering its behaviour should
be included to the main branch and executed during each commit and check whether no
legacy functionality is broken. Sometimes this is intended and in this case rgresssion
tests should be adjusted to the new situation.

In case that no functionaity is changed, e.g. in case of documentation updates, this stage can
be skipped by adding **skip-test** in the git commit message.

### Inspect failed test report output

When tests fail and break the pipeline, test reports are stored as artifacts. On the right side of the failed pipleine there is a link
where the reports can be downloaded.

## Deploy

You can skip this stage by adding **skip-deploy** in the git commit message.

# Deploing OpenRemote stack in the cloud

## Deployment on AWS

We will use the [AWS CLI](https://aws.amazon.com/cli/) to create the necessary resources on AWS side.
Following commands should be executed from openremote/gitlab-ci subdirectory.

### Prepare the environment

You will need to do this only once per AWS account.

Enter the following commands in your terminal:

```bash
# Configure the AWS CLI with your AWS region (supported eu-west-1<Ireland>)
aws configure

# Create SSH keys (it's a pity but AWS EC2 does not accept other type than rsa keys here)
ssh-keygen -f openremote -t rsa -b 4096  -C "your_email@example.com"

# Import key pair to AWS
aws ec2 import-key-pair --key-name openremote --public-key-material fileb://openremote.pub

```

### Deploy EC2 instance with Openremote stack

After this command it will take some time till the stack is created and initialized (about 10-30 mins).

```bash
# Create stack
aws cloudformation create-stack --stack-name openremote --template-body file://aws-cloudformation.template.yml

# Get stack endpoint
aws cloudformation describe-stacks --stack-name openremote --query "Stacks[0].Outputs[?OutputKey=='InstanceIP'].OutputValue" --output text
export OR_ENDPOINT=$(aws cloudformation describe-stacks --stack-name openremote --query "Stacks[0].Outputs[?OutputKey=='InstanceIP'].OutputValue" --output text)
```

Now you should be able to access the openremote v3 server at the URL $OR_ENDPOINT environment variable.

### Assing custom domain to the endpoint

TODO

### Test the deployment (optional)

```bash
curl --insecure -L https://$OR_ENDPOINT/api/master/model/asset/descriptors
```

### Add/remove assets (optional)

TODO

### Delete EC2 instance

The openremote stack costs money. You can delete is with

```bash
aws cloudformation delete-stack --stack-name openremote
```

## Deployment on Alibaba

TODO

## Deployment on Azure

TODO

## Deployment on GCP

TODO

## Deployment on OVH

TODO
