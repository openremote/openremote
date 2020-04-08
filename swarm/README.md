## Introduction

Swarm mode is a built in orchestrator which comes bundled with Docker. Although, currently industry standard 
orchestrator is Kubernetes by Google, using swarm has its merits:
- it covers 20% features which covers 80% of what people need
- it is easier to learn therefore less expensive to introduce
- learning Kubernetes after Swarm is painless
- Kubernetes is either a full man job or an expensive tool, e.g. [Rancher](https://rancher.com/) costs ~100k/year for
 support
- Kubernetes being a Google product can be discontinued at any time, 
see [Google graveyard](https://killedbygoogle.com/)

### Swarm commands primer
```docker stack deploy --compose-file swarm/swarm-docker-compose.yml demo```

```docker stack ls```

```docker stack rm <stack_name>```

```docker service ls```

```docker service ps <service_name>```

```docker service logs <service_name> [--follow]```

Deploy on Kubernetes (works only on builtin Kubernetes in Docker Desktop)

```docker stack deploy --orchestrator kubernetes --compose-file swarm/kube-docker-compose.yml demo```


### Swarm advantages:
- no manual deployments
- automatic repair in case of node crash
- possible rollback in case of wrong build
- secrets management
- scalable logs

### Swarm disadvantages
- multi-node swarm is usual more expensive than a single machine. However, the nodes can be lighter than with the 
single machine deployment.

In swarm mode each service shouldn't depend on any other services. The orchestrator can make services go on and off on 
random. Each service should cope with this. Although we have still depends_on entry in the compose file, swarm mode 
does not support condition entry. The depends_on influences only the order of starting containers but not wait till 
container is ready. Therefore, the depending container should resolve the problem itself. An example would be a startup
code of the manager where it waits for the keycloak.

Compose files v2 are generally used for development and image build, they should not be used in production. 
Compose files v3 are for swarm deployment and some development options are ignored, e.g. like 'build' - 
swarm uses only prebuilt images.

## Persistent data/volumes

We using a trick to populate volumes with data by one container and use them by another one. This generally does not 
work in the swarm mode because we cannot guarantee that both containers would be spin up in the same node.

## Secrets

TODO

## Uninterrupted updates

In the v3 docker-compose file include in the deploy section
```
  update_config:
    order: start-first 
```
It results in spinning up a new container when the old one is running, switching over when the new one is ready and 
destroying the old one.

However, it does not work on our current OVH server, does work on AWS though. The reason is, that with this 
configuration and because we are updating all images at once there is not enough resources to deploy double stack 
and hot switch them. We can either use a bigger OVH VM or configure a multi-node swarm. We can also configure that 
only a single service updates it's image at the time, this should be done from begin. However, to do it right we 
should split github repo into separate repos, each one with a single service, i.e. openremote/proxy, 
openremote/postgresql, openremote/keycloak and openremote/manager. 

## Deploying swarm on AWS (UI)

- ASW console in Services go to CloudFormation
- press Create Stack
- Specify template -> Upload a template file -> Chose file (browse and select swarm/docker-swarm-template.yml from the 
openremote repo)
- Press next.
Default openremote swarm has 3 swarm managers nodes and 0 workers. Each node is a t3.small instance (~0.028 EUR/h)
- Enter stack name, e.g. docker
- In Swarm Properties select SSH key which you want to use for access. Note, if you don't specify a key here then
deploying the CloudFormation stack will fail. If you don't have already a key in a given region, go to EC2 console
and create a new key pair in a region that you want deploy the swarm.
- press next
- add any tags if you need
- press next
- review stack creation parameters
- tick 'I acknowledge that AWS CloudFormation might create IAM resources.'
- Press create stack and wait till stack is complete.
- After the docker swarm stack is created go to the stack Outputs tab and see the summary. Here you can copy 
DefaultDNSTarget field to use in CNAME DNS record, or ALIAS if you are using AWS Route53 service.
- Press Managers link which should open EC2 console with 3 swarm manager nodes, here you can see their public IPv4 
addresses, which can be used for ssh access with the private key you've used in the stack creation configuration.
- TODO letsencrypt does not work with this setup as it requires IPv6 address for identification. The default AWS EC2 
setup is IPv4 only and extra effort should be made to change the template to include IPv6 configuration. However, 
AWS offer a free SSL certification service which should be used instead. This is outside the scope of this document 
though.
- to deploy openremote stack:
- copy stack to one of the manager nodes:

```scp swarm/aws-swarm-docker-compose.yml <Manager-Public-IP>:```
- deploy docker stack:

```ssh docker@<Manager-Public-IP> IDENTITY_NETWORK_HOST=DNS-enrty docker stack deploy --compose-file aws-swarm-docker-compose.yml aws_demo```

you should see something like this:
```
Creating network aws_demo_public
Creating network aws_demo_private
Creating service aws_demo_keycloak
Creating service aws_demo_postgresql
Creating service aws_demo_manager
```
You can check deployment with:

```ssh docker@<Manager-Public-IP> docker service ls```

Deployment can take few minutes as images are fetched from the docker hub repository.

After it you should be able to access it http://DNS-entry:8080/manager

If you want automatic images update you can use a project like disrupt https://github.com/BlameButton/disrupt

Example of functional AWS swarm:

```
> ssh <Manager-Public-IP> docker service ls
ID                  NAME                  MODE                REPLICAS            IMAGE                             PORTS
v5t56il41f3e        aws_demo_keycloak     replicated          1/1                 openremote/keycloak:latest        *:8081->8080/tcp
vinzzbpfgoe1        aws_demo_manager      replicated          1/1                 openremote/manager-swarm:latest   *:8080->8080/tcp
0a9bg5ssfrxu        aws_demo_postgresql   replicated          1/1                 openremote/postgresql:latest      
slip9ccrin6p        disrupt_disrupt       replicated          1/1                 blamebutton/disrupt:latest        
```
When region used for the deployment has 3 availability zones (like Ireland or Frankfurt), the the swarm should create 
each node in a different AZ. You should also see in the CloudFormation outputs

ZoneAvailabilityComment	This region has at least 3 Availability Zones (AZ). This is ideal to ensure a fully functional 
Swarm in case you lose an AZ.

Note that swarm stack cost money, in the above configuration about 2$ per day, 60$ per month, therefore destroy the 
stack when you don't need it:
- in the CloudFormation screen select the docker stack
- in the top right row select Delete button.




