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

## Deployment on AWS through the web UI

This is slower prodcedure and meant as en excercise for AWS novice users. Advanced users should use [**aws-cli**](#awscli) method described in the next section.

- Sign in into [AWS web console](https://console.aws.amazon.com/console/home?nc2=h_ct&src=header-signin)
- Select region to eu-west-1 (Ireland). Currently the cloudformation template supports only this region. *Other regions can be added when
  necessary*.
- Go to [EC2 KeyPairs](https://eu-west-1.console.aws.amazon.com/ec2/v2/home?region=eu-west-1#KeyPairs:)
  + create or import key pair with name **openremote**  
  if you create a new key pair don't forget to download it after creation
- Go to [CloudFormation conslole](https://eu-west-1.console.aws.amazon.com/cloudformation/home?region=eu-west-1)
  + The top right corner -> Create stack -> With new resources (standard)
  + On the Create stack page select *Upload a template file*
  + *Chose file* and select the openremote/gitlab-ci/aws-cloudformation.template.yml, press *Next*
  + Enter *Stack name*, e.g. **openremote3**  
  On *Parameters* check if KeyName has a correct value, default is **openremote** which we used for key generation, press *Next*
  + On *Configure stack options* press *Next*
  + On *Review* page you can check all parameters and press *Create stack*  
  You should be on a page where the stack is in CREATE_IN_PROGRESS status.
  In less than a mniute the stack should be in CREATE_COMPLETE state  
  Press *Outputs* tab where you should find *The instance public IP*. Copy it to web browser. You will need to wait about 7 minutes before the
  instance initializes itself with OpenRemote containers.

- After you are done with the stack please for to the [Cloudformation](https://eu-west-1.console.aws.amazon.com/cloudformation/home) dashboard and
delete it. **This will remove all resources and prevent from generating unnecessary costs**.

## <a name="awscli"></a>Deployment on AWS with CLI (advanced)

We will use the [AWS CLI](https://aws.amazon.com/cli/) to create the necessary resources on AWS side.
Following commands should be executed from openremote/gitlab-ci subdirectory.

We are presenting 3 scenarios:
1. Deploy OpenRemote stack with domain hosted on AWS
2. Deploy OpenRemote stack without domain
3. Deploy OpenRemote stack with domain hosted anywhere

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

### Scenario 1 - Deploy OpenRemote stack with domain hosted on AWS

In this scenario we have an AWS account with Route53 hosted zone equal to DomainName input parameter. This is the fastest way to create a full
functional OpenRemote stack with valid SSL key. Within less than 10 minutes you can create a full functional deployment with valid URL.

```bash
# Create stack
aws cloudformation create-stack --stack-name OpenRemote --template-body file://gitlab-ci/aws-cloudformation.template.yml --parameters  ParameterKey=DomainName,ParameterValue=developers.openremote.io ParameterKey=HostedZone,ParameterValue=true ParameterKey=HostName,ParameterValue=myhost 

```

Wait about 7 minutes.

Check the stack:
```bash
# Check the stack status. If it is different than CREATE_COMPLETE the you can find more info in the AWS console
# Services -> CloudFormation -> Stacks -> OpenRemote -> Events tab
aws cloudformation describe-stacks --stack-name OpenRemote --query "Stacks[0].StackStatus" 

# Get the URL of the stack (it should be DomainName.HostName i.e. myhost.developers.openremote.io in this example)
export URL=$(aws cloudformation describe-stacks --stack-name OpenRemote --query "Stacks[0].Outputs[?OutputKey=='PublicUrl'].OutputValue" --output text)

# Check if it gives valid response
curl -L -I $URL

# The output should look like this:
HTTP/1.1 302 Found
content-length: 0
location: https://myhost.developers.openremote.io/
cache-control: no-cache

HTTP/1.1 302 Found
location: http://myhost.developers.openremote.io/main
content-length: 0
date: Sat, 18 Jul 2020 10:41:15 GMT

HTTP/1.1 302 Found
content-length: 0
location: https://myhost.developers.openremote.io/main
cache-control: no-cache

HTTP/1.1 302 Found
location: /main/
content-type: text/html;charset=UTF-8
content-length: 64
date: Sat, 18 Jul 2020 10:41:15 GMT

HTTP/1.1 200 OK
expires: Sat, 18 Jul 2020 22:41:15 GMT
cache-control: public,max-age=43200,must-revalidate
pragma: 
accept-ranges: bytes
date: Sat, 18 Jul 2020 10:41:15 GMT
etag: W/"index.html-1595005800000"
last-modified: Fri, 17 Jul 2020 17:10:00 GMT
content-type: text/html
content-length: 3627
```

### Scenario 2 - Deploy OpenRemote stack without domain

In  this scenario we are deploing stack without valid domain. It will use public IP dynamically assigned by AWS.

To achieve this we create the stack with empty DomainName and HostName set to localhost.

```bash
# Create stack
aws cloudformation create-stack --stack-name OpenRemote --template-body file://gitlab-ci/aws-cloudformation.template.yml --parameters  ParameterKey=DomainName,ParameterValue= ParameterKey=HostName,ParameterValue=localhost 
```

Wait about 7 minutes.

Check the stack:
```bash
# Check the stack status. If it is different than CREATE_COMPLETE the you can find more info in the AWS console
# Services -> CloudFormation -> Stacks -> OpenRemote -> Events tab
aws cloudformation describe-stacks --stack-name OpenRemote --query "Stacks[0].StackStatus" 

# Get public IP of the stack
export IP=$(aws cloudformation describe-stacks --stack-name OpenRemote --query "Stacks[0].Outputs[?OutputKey=='InstanceIP'].OutputValue" --output text)

# Check if it gives valid response
curl -L -I $IP

# The output should look like this:
HTTP/1.1 302 Found
content-length: 0
location: https://3.250.36.158/
cache-control: no-cache

HTTP/1.1 302 Found
location: http://3.250.36.158/main
content-length: 0
date: Sat, 18 Jul 2020 09:12:44 GMT

HTTP/1.1 302 Found
content-length: 0
location: https://3.250.36.158/main
cache-control: no-cache

HTTP/1.1 302 Found
location: /main/
content-type: text/html;charset=UTF-8
content-length: 64
date: Sat, 18 Jul 2020 09:12:44 GMT

HTTP/1.1 200 OK
expires: Sat, 18 Jul 2020 21:12:44 GMT
cache-control: public,max-age=43200,must-revalidate
pragma: 
accept-ranges: bytes
date: Sat, 18 Jul 2020 09:12:44 GMT
etag: W/"index.html-1595005800000"
last-modified: Fri, 17 Jul 2020 17:10:00 GMT
content-type: text/html
content-length: 3627
```

Of course you can point a browser to this endpoint but you have to accept invalid SSL certificate. Sometimes you need to change browser settings to allow for this.

### Scenario 3 - Deploy OpenRemote stack with domain hosted anywhere

In this scenario we are hosting our DNS records outside of AWS, or on AWS but on a different account. It is possible to create a cloudformation template
which would work automatically with the second case by using IAM roles, however this is beyond this tutorial. Please conntact OpenRemote if this is your case.

This is the most time consuming options as it can take up to 24 hours to propagate the DNS record if you are using a slow provider. [AWS](https://aws.amazon.com/) and [CloudFlare](https://www.cloudflare.com/) do this
within few seconds, therefore in dynamic environments we advise to use one of them.

```bash
# Create stack
aws cloudformation create-stack --stack-name OpenRemote --template-body file://gitlab-ci/aws-cloudformation.template.yml --parameters  ParameterKey=DomainName,ParameterValue=mydomain.com ParameterKey=HostName,ParameterValue=example 
```

Wait about 7 minutes.

Check the stack:
```bash
# Check the stack status. If it is different than CREATE_COMPLETE the you can find more info in the AWS console
# Services -> CloudFormation -> Stacks -> OpenRemote -> Events tab
aws cloudformation describe-stacks --stack-name OpenRemote --query "Stacks[0].StackStatus" 

# Get public IP and URL of the stack
export IP=$(aws cloudformation describe-stacks --stack-name OpenRemote --query "Stacks[0].Outputs[?OutputKey=='InstanceIP'].OutputValue" --output text)
export URL=$(aws cloudformation describe-stacks --stack-name OpenRemote --query "Stacks[0].Outputs[?OutputKey=='PublicUrl'].OutputValue" --output text)

# Show public IP and URL
# It should produce something like 34.244.50.94 example.mydomain.com
echo $IP $URL
```

With the data of the last command go to your DNS dashboard and create an A record example.mydomain.com with coresponding IP address.

Wait up to 24 hours for DNS propagation.

```bash
# Check if DNS is updated
ping $URL
# you should get response like
PING example.mydomain.com (34.244.50.94): 56 data bytes
64 bytes from 34.244.50.94: icmp_seq=0 ttl=41 time=29.731 ms
64 bytes from 34.244.50.94: icmp_seq=1 ttl=41 time=37.164 ms

# Update SSL ceriticate
ssh -i ~/.ssh/openremote ubuntu@$URL docker start openremote_proxy_1

# Check if it gives valid response
curl -L -I $URL

HTTP/1.1 302 Found
content-length: 0
location: https://example.mydomain.com/
cache-control: no-cache

HTTP/1.1 302 Found
location: http://example.mydomain.com/main
content-length: 0
date: Sat, 18 Jul 2020 12:20:54 GMT

HTTP/1.1 302 Found
content-length: 0
location: https://example.mydomain.com/main
cache-control: no-cache

HTTP/1.1 302 Found
location: /main/
content-type: text/html;charset=UTF-8
content-length: 64
date: Sat, 18 Jul 2020 12:20:54 GMT

HTTP/1.1 200 OK
expires: Sun, 19 Jul 2020 00:20:54 GMT
cache-control: public,max-age=43200,must-revalidate
pragma: 
accept-ranges: bytes
date: Sat, 18 Jul 2020 12:20:54 GMT
etag: W/"index.html-1595005800000"
last-modified: Fri, 17 Jul 2020 17:10:00 GMT
content-type: text/html
content-length: 3627
```

### Add/remove assets (optional)

TODO

### **IMPORTANT** Delete unused stack

When you are done with the stack don't forget to delete it. Depending on the EC2 instance selected leaving it running incures [costs](https://ec2instances.info/?region=eu-west-1). The default one **t3a.medium** in eu-west-1 region (Ireland) currently costs $0.040800 hourly and when reserved $0.025700 hourly.

```bash
# Delete unused stack
aws cloudformation delete-stack --stack-name OpenRemote 
```

## Deployment on Alibaba

TODO

## Deployment on Azure

TODO

## Deployment on GCP

TODO

## Deployment on OVH

TODO
