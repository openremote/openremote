# Deploying EKS load-test stacks

Use this guide to deploy load1 or load2 with `or-eks-cluster` and
`or-eks-stack`, then run a test against its endpoint. The main procedure creates
a cluster and deploys one stack with HTTPS and MQTTS through HAProxy.
Run all commands from the **repository root**.

- [Before you start](#before-you-start)
- [Deploy and verify a stack](#deploy-and-verify-a-stack)
- [Manage a deployment](#manage-a-deployment)
- [Alternative deployments](#alternative-deployments)
- [Profile and deployment reference](#profile-and-deployment-reference)
- [Legacy scripts](#legacy-scripts)
- [Offline validation](#offline-validation)

## Before you start

Complete the prerequisites in the [EKS deployment guide](../kubernetes/README-AWS.md)
and configure your AWS credentials. The commands below use the default
credential chain. For a named profile, set `AWS_PROFILE` or add
`--profile <named-profile>` to the relevant commands. These workflows do not
write credentials.

Follow [load1](load1-eks/README.md) or [load2](load2-eks/README.md) to build and
push the custom Manager image, then set the deployment variables from that
guide:

| Variable                  | Purpose                                                              |
| ------------------------- | -------------------------------------------------------------------- |
| `AWS_REGION`              | Region containing the cluster                                        |
| `CLUSTER_NAME`            | Cluster to create or reuse                                           |
| `STACK_NAME`              | Stack name and Kubernetes namespace                                  |
| `LOAD_HOSTNAME`           | Unique public hostname for the stack                                 |
| `LOAD_PROFILE_DIR`        | Source profile, such as `test/load2-eks/profiles/xlarge`             |
| `LOAD_VALUES_DIR`         | Working values directory, such as `.local/$CLUSTER_NAME/$STACK_NAME` |
| `LOAD_MANAGER_REPOSITORY` | Repository containing the custom Manager image                       |

Ensure the cluster nodes can pull that image, including ECR repository
permissions for cross-account pulls. The supplied clusters use ARM nodes; the
image build examples publish both ARM64 and AMD64 images.

## Deploy and verify a stack

### 1. Prepare the selected profile

The current CLI workflow requires a working copy of the profile's component
values. The commands below copy three files and substitute the Manager image
repository in the fourth. Helm does not expand the
`${LOAD_MANAGER_REPOSITORY}` placeholder itself.

Create the working copy under the git-ignored `.local` directory:

```bash
mkdir -p "$LOAD_VALUES_DIR"
: "${LOAD_MANAGER_REPOSITORY:?Set the custom Manager image repository first}"
cp "$LOAD_PROFILE_DIR"/keycloak.yaml "$LOAD_PROFILE_DIR"/postgresql.yaml \
  "$LOAD_PROFILE_DIR"/proxy.yaml "$LOAD_VALUES_DIR/"
envsubst '${LOAD_MANAGER_REPOSITORY}' < "$LOAD_PROFILE_DIR/manager.yaml" \
  > "$LOAD_VALUES_DIR/manager.yaml"
```

Make any per-stack customisations in this copy before deploying. Rerunning
these commands overwrites that copy, so do not repeat them when updating an
existing deployment unless you intend to replace its customised values.

### 2. Configure DNS and create the cluster

If you already have a compatible cluster, follow
[Use an existing cluster](#use-an-existing-cluster), then continue at step 3.

For a new cluster, choose the DNS zone, domain, role, and a stable owner ID
unique to the cluster. Replace the example values below. `LOAD_HOSTNAME` must
be a strict subdomain of `DNS_DOMAIN` and must not be used by another stack or
cluster.

```bash
export DNS_ZONE_ID=Z0123456789
export DNS_DOMAIN=example.com
export DNS_ROLE_ARN=arn:aws:iam::123456789012:role/load-external-dns
export DNS_OWNER_ID="$(aws sts get-caller-identity --query Account --output text)-$CLUSTER_NAME-$AWS_REGION"
```

Before creating the cluster, provision the DNS role using the
[ExternalDNS IAM bootstrap](../kubernetes/cluster/eks/README.md#cross-account-iam-bootstrap).
Its trust must allow this cluster's IRSA role. If the role requires an external
ID, also add `--external-dns-external-id` to the creation command.

```bash
kubernetes/or-eks-cluster create \
  --name "$CLUSTER_NAME" --region "$AWS_REGION" \
  --config "$LOAD_PROFILE_DIR/cluster.yaml" \
  --external-dns-zone-id "$DNS_ZONE_ID" --external-dns-domain "$DNS_DOMAIN" \
  --external-dns-role-arn "$DNS_ROLE_ARN" --external-dns-owner-id "$DNS_OWNER_ID"
```

### 3. Deploy the stack

```bash
kubernetes/or-eks-stack apply \
  --name "$STACK_NAME" --cluster "$CLUSTER_NAME" \
  --region "$AWS_REGION" \
  --hostname "$LOAD_HOSTNAME" --exposure haproxy --mqtts \
  --values-dir "$LOAD_VALUES_DIR" --timeout 90m
```

Wait for the command to finish. It waits for the stack's NLB, DNS, trusted
HTTPS, and MQTTS. The extended timeout allows the load dataset to initialise;
see [Startup and capacity](#startup-and-capacity) for larger datasets.

### 4. Retrieve credentials and verify the deployment

```bash
kubernetes/or-eks-stack credentials \
  --name "$STACK_NAME" --cluster "$CLUSTER_NAME" \
  --region "$AWS_REGION"
```

Open `https://$LOAD_HOSTNAME/manager` and sign in with the returned administrator
credentials. Before starting the load run:

1. Verify that the expected test users and assets were created.
2. Check browser login and asset creation/editing.
3. Verify an authenticated MQTTS publish with a provisioned test account.

### 5. Run the load scenario

Configure your test client with `MANAGER_HOSTNAME=$LOAD_HOSTNAME` and MQTTS
port **8883**. Plaintext MQTT is not exposed.

Use the clients and scripts documented in [load1](load1/README.md). For load2,
use the scenarios and parameters described in
[load2's running tests section](load2-eks/README.md#running-tests).

## Manage a deployment

### Update configuration or the Manager image

Edit the values in `LOAD_VALUES_DIR`, then repeat the same `or-eks-stack apply`
command used to deploy the stack, including its exposure and MQTTS options.
Ordinary reapply and uninstall/reapply retain data, passwords, and certificate
state.

Prefer a new image tag per build when comparing runs, and update `image.tag`
in the working `manager.yaml`. If you instead push a replacement image using
the same `load1` or `load2` tag, explicitly restart Manager after applying any
configuration changes:

```bash
kubectl --context "$CLUSTER_NAME@$AWS_REGION" --namespace "$STACK_NAME" \
  rollout restart deployment/manager
kubectl --context "$CLUSTER_NAME@$AWS_REGION" --namespace "$STACK_NAME" \
  rollout status deployment/manager --timeout=90m
```

### Inspect the stack

```bash
kubernetes/or-eks-stack status \
  --name "$STACK_NAME" --cluster "$CLUSTER_NAME" \
  --region "$AWS_REGION"
```

Load2 profiles expose JMX internally. To access it from your machine:

```bash
kubectl --context "$CLUSTER_NAME@$AWS_REGION" --namespace "$STACK_NAME" \
  port-forward service/manager-jmx 8085:8085
```

### Reset the test dataset

To rebuild the dataset while keeping the endpoint and credentials:

1. Set `or.setupRunOnRestart: true` in the working `manager.yaml`.
2. Apply the stack and wait for setup to complete.
3. Set the value back to `false` and apply again before running tests.

**While enabled, this setting wipes the stack's database on every Manager
startup.** For a fully fresh stack, destroy it and deploy again; that also
replaces its credentials and certificate state.

### Uninstall a stack or delete its resources

To stop one stack while retaining its data and certificate state:

```bash
kubernetes/or-eks-stack uninstall \
  --name "$STACK_NAME" --cluster "$CLUSTER_NAME" \
  --region "$AWS_REGION"
```

To permanently delete one stack, its data, and its owned external resources:

```bash
kubernetes/or-eks-stack destroy \
  --name "$STACK_NAME" --cluster "$CLUSTER_NAME" --confirm "$STACK_NAME" \
  --region "$AWS_REGION"
```

Only after destroying **every** stack, delete a disposable cluster:

```bash
kubernetes/or-eks-cluster destroy \
  --name "$CLUSTER_NAME" --confirm "$CLUSTER_NAME" \
  --region "$AWS_REGION"
```

Cluster deletion refuses remaining stacks/endpoints. Uninstall alone is not
enough because it retains the stack namespace.

## Alternative deployments

### Use an existing cluster

The cluster must already have managed ExternalDNS configured and the shared
OpenRemote add-ons. Use `or-eks-cluster apply` with that cluster's existing DNS
settings to reconcile them if needed. Neither `apply` nor changing `--config`
resizes an existing node group or upgrades Kubernetes.

Set `CLUSTER_NAME` and `AWS_REGION` for the existing cluster, then refresh its
context instead of creating it:

```bash
kubernetes/or-eks-cluster kubeconfig \
  --name "$CLUSTER_NAME" --region "$AWS_REGION"
```

Continue with [step 3](#3-deploy-the-stack).

### Deploy another stack or cluster

For another stack in the same cluster, select a new `STACK_NAME`,
`LOAD_HOSTNAME`, and `LOAD_VALUES_DIR`. Prepare its values, then follow steps
3–5. Check that the cluster has capacity for both stacks before deploying.

For a separate cluster, also choose a new `CLUSTER_NAME`, update the working
values directory, and configure its DNS role trust and unique owner ID. Follow
the full deployment procedure with the desired profile.

### Use Ingress with ACM certificates

For a **new** stack using ACM termination, replace the apply command in step 3
with:

```bash
export LOAD_MQTTS_HOSTNAME=mqtt-load2.example.com
kubernetes/or-eks-stack apply \
  --name "$STACK_NAME" --cluster "$CLUSTER_NAME" \
  --region "$AWS_REGION" \
  --hostname "$LOAD_HOSTNAME" --exposure ingress \
  --mqtts --mqtts-hostname "$LOAD_MQTTS_HOSTNAME" \
  --values-dir "$LOAD_VALUES_DIR" --timeout 90m
```

Choose an unused MQTTS hostname below the managed DNS domain. For the supplied
MQTT scenarios, set `MANAGER_HOSTNAME` to **`$LOAD_MQTTS_HOSTNAME`**; browser
access still uses `$LOAD_HOSTNAME`. Retrieve credentials and verify the stack
as in steps 4–5, using this separate hostname for MQTTS.

Keep these exposure/MQTTS arguments on every apply. Exposure and hostname
changes require a new stack.

The web endpoint uses a shared ALB and MQTTS a dedicated NLB. The CLI selects
the cluster's shared ACM certificate if configured, otherwise manages
certificates for the stack. Explicit existing certificates are also supported;
see `or-eks-stack --help`.

### Manage DNS and readiness yourself

To deploy without the managed AWS facade, use the same prepared values with
[`or-stack`](../kubernetes/README.md). Follow its documented workflow for DNS,
certificate readiness, and external resource cleanup.

## Profile and deployment reference

### Source profiles and working values

`LOAD_PROFILE_DIR` selects the checked-in cluster template and component
settings. `LOAD_VALUES_DIR` contains the prepared settings for one stack. The
cluster command reads `cluster.yaml` from the source profile; the stack command
reads the working values directory.

`--values-dir` accepts `manager.yaml`, `postgresql.yaml`, `keycloak.yaml`,
`proxy.yaml`, and an optional `or-setup.yaml`. These override the CLI's target
and exposure defaults. The supplied profiles need no `or-setup.yaml`: the CLI
enables NetworkPolicies and generates independent credentials, and each
component owns its dynamically provisioned PVC.

### Startup and capacity

The supplied Manager startup probe allows for some time for the initialisation to execute. If dataset
initialisation takes longer, increase both its allowance in the working
`manager.yaml` and the deployment's `--timeout`.

Manager and Keycloak use `Recreate` updates so a single-node profile does not
need capacity for overlapping old and new Pods. Updates interrupt service.
Profile requests cover one stack; Kubernetes and shared controllers need
capacity too. Size a shared cluster for the combined requests and controller
overhead. Namespace isolation does not eliminate CPU, disk, or network
contention; use separate clusters for independent benchmarks.

## Legacy scripts

The old load1 scripts and load2's standard `eks-setup-load.sh` remain in the
repository, but stop with a message directing users to the CLIs.

The [load2 ACM/NLB experiment](load2-eks/README.md#legacy-acmnlb-multi-proxy-experiment)
remains deployable with `eks-setup-load-acm.sh` and its associated redeployment
and cleanup helpers. Follow the linked instructions and use a dedicated
cluster for that workflow. These helpers must not manage CLI-created stacks.

## Offline validation

From the repository root, run:

```bash
python3 kubernetes/test/load-eks-test
```

This requires Python 3, Bash, Helm, jq, and envsubst. It exercises all seven
profiles through `or-stack` with stubbed Kubernetes commands and real Helm
lint/rendering. It also checks legacy ACM/NLB setup and redeployment for all
six load2 profiles, with AWS commands stubbed. No cluster or AWS credentials
are used.

Actual EKS acceptance still requires deploying the desired profiles and
verifying login, dataset initialisation, MQTTS traffic, persistence, and
independent stack cleanup.
