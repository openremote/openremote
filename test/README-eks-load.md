# Deploying EKS load-test stacks

First follow [load1](load1-eks/README.md) or [load2](load2-eks/README.md) to build
the custom image and set the cluster, stack, hostname, profile, and image
repository variables. All commands here run from the **repository root**.

Use the prerequisites in the [EKS deployment guide](../kubernetes/README-AWS.md).
Configure AWS credentials separately; these workflows do not write credentials.
The commands use the default credential chain. For a named profile, set
`AWS_PROFILE` or add `--profile <named-profile>` to the relevant commands.
The ARM nodes must be able to pull the custom Manager image, including ECR
repository permissions for cross-account pulls. The build examples publish
both ARM64 and AMD64 images. Keep the existing chart image versions for the
other components.

## Prepare per-stack values

Create a private working copy under the git-ignored `.local` directory:

```bash
mkdir -p "$LOAD_VALUES_DIR"
: "${LOAD_MANAGER_REPOSITORY:?Set the custom Manager image repository first}"
cp "$LOAD_PROFILE_DIR"/keycloak.yaml "$LOAD_PROFILE_DIR"/postgresql.yaml \
  "$LOAD_PROFILE_DIR"/proxy.yaml "$LOAD_VALUES_DIR/"
envsubst '${LOAD_MANAGER_REPOSITORY}' < "$LOAD_PROFILE_DIR/manager.yaml" \
  > "$LOAD_VALUES_DIR/manager.yaml"
```

Edit this copy for each run. `--values-dir` reads `manager.yaml`, `postgresql.yaml`,
`keycloak.yaml`, `proxy.yaml`, and an optional `or-setup.yaml`. It layers them over
the CLI's target and exposure settings. No `or-setup.yaml` override is needed:
the CLI enables NetworkPolicies and generates independent credentials, and each
component owns its dynamically provisioned PVC. Do not copy the legacy static
volume settings or AWS volume IDs into the new configuration.

Manager and Keycloak use `Recreate` updates so a single-node profile does not
need capacity for surge Pods. Updates interrupt service. Profile requests cover
one stack; Kubernetes and shared controllers need capacity too. Before placing
multiple stacks on one cluster, size its nodes for the combined requests and
controller overhead. Namespace isolation does not eliminate CPU, disk, or
network contention, so use separate clusters for independent benchmarks.

## Create or reuse a cluster

For a **new cluster**, configure managed ExternalDNS first. The zone, domain,
DNS role, and owner ID are required together. Use a stable owner ID unique to
this cluster across every account/controller that can access the DNS zone:

```bash
export DNS_ZONE_ID=Z0123456789
export DNS_DOMAIN=example.com
export DNS_ROLE_ARN=arn:aws:iam::123456789012:role/load1-external-dns
export DNS_OWNER_ID="$(aws sts get-caller-identity --query Account --output text)-$CLUSTER_NAME-$AWS_REGION"

kubernetes/or-eks-cluster create \
  --name "$CLUSTER_NAME" --region "$AWS_REGION" \
  --config "$LOAD_PROFILE_DIR/cluster.yaml" \
  --external-dns-zone-id "$DNS_ZONE_ID" --external-dns-domain "$DNS_DOMAIN" \
  --external-dns-role-arn "$DNS_ROLE_ARN" --external-dns-owner-id "$DNS_OWNER_ID"
```

Provision the DNS role using the
[ExternalDNS IAM bootstrap](../kubernetes/cluster/eks/README.md#cross-account-iam-bootstrap)
before creation. Its trust must allow this cluster's IRSA role; a second cluster
needs its own permitted role and unique TXT owner. Add
`--external-dns-external-id` if required by your role. The stack hostname must
be a strict subdomain of the configured domain. Use distinct hostnames across
clusters as well as across stacks.

For an **existing compatible cluster**, skip creation and refresh its context:

```bash
kubernetes/or-eks-cluster kubeconfig \
  --name "$CLUSTER_NAME" --region "$AWS_REGION"
```

It must already have the managed ExternalDNS configuration and the shared
OpenRemote add-ons. Use `or-eks-cluster apply` with that cluster's existing DNS
settings to reconcile them if needed. Neither `apply` nor changing
`--config` resizes an existing node group or upgrades Kubernetes.

## Deploy a stack

```bash
kubernetes/or-eks-stack apply \
  --name "$STACK_NAME" --cluster "$CLUSTER_NAME" \
  --region "$AWS_REGION" \
  --hostname "$LOAD_HOSTNAME" --exposure haproxy --mqtts \
  --values-dir "$LOAD_VALUES_DIR" --timeout 90m

kubernetes/or-eks-stack credentials \
  --name "$STACK_NAME" --cluster "$CLUSTER_NAME" \
  --region "$AWS_REGION"
```

The extended timeout allows the load dataset to initialize. The existing
Manager startup probe allows roughly 83 minutes; increase it in your values
copy too if your dataset takes longer. The facade waits for the stack's NLB,
DNS, trusted HTTPS, and MQTTS. Browser access is `https://$LOAD_HOSTNAME/manager`;
the load clients use `MANAGER_HOSTNAME=$LOAD_HOSTNAME` and MQTTS port 8883.
Plaintext MQTT is not exposed. The CLI configures Keycloak's `/auth` issuer and
Manager's exact HTTPS CORS origin.

For a second stack in the same cluster, select a new `STACK_NAME`,
`LOAD_HOSTNAME`, and `LOAD_VALUES_DIR`, prepare its values, then run only the
stack commands. For a separate cluster, also select a new `CLUSTER_NAME` and DNS
owner/IRSA configuration, then create it with the desired profile.

### Ingress alternative

For a **new** stack using ACM termination, replace the apply command with:

```bash
export LOAD_MQTTS_HOSTNAME=mqtt-load2.example.com
kubernetes/or-eks-stack apply \
  --name "$STACK_NAME" --cluster "$CLUSTER_NAME" \
  --region "$AWS_REGION" \
  --hostname "$LOAD_HOSTNAME" --exposure ingress \
  --mqtts --mqtts-hostname "$LOAD_MQTTS_HOSTNAME" \
  --values-dir "$LOAD_VALUES_DIR" --timeout 90m
```

The CLI selects the cluster's shared ACM certificate if configured, otherwise
manages certificates for the stack. Explicit existing certificates are also
supported; see `or-eks-stack --help`. The web endpoint uses a shared ALB and
MQTTS a dedicated NLB. For the supplied MQTT scenarios, set `MANAGER_HOSTNAME`
to **`$LOAD_MQTTS_HOSTNAME`**; browser access still uses `$LOAD_HOSTNAME`.
Keep these exposure/MQTTS arguments on every apply. Exposure and hostname
changes require a new stack.

## Update, inspect, or reset one stack

Repeat the same `or-eks-stack apply` command after editing its values. Ordinary
reapply and uninstall/reapply retain data, passwords, and certificate state.
Pushing a mutable `load1`/`load2` image tag alone does not restart Pods; after
pushing, reapply any configuration changes and explicitly restart Manager:

```bash
kubectl --context "$CLUSTER_NAME@$AWS_REGION" --namespace "$STACK_NAME" \
  rollout restart deployment/manager
kubectl --context "$CLUSTER_NAME@$AWS_REGION" --namespace "$STACK_NAME" \
  rollout status deployment/manager --timeout=90m

kubernetes/or-eks-stack status \
  --name "$STACK_NAME" --cluster "$CLUSTER_NAME" \
  --region "$AWS_REGION"
```

Prefer a new image tag per build when comparing runs. To rebuild a dataset
while keeping the endpoint and credentials, deliberately set
`or.setupRunOnRestart: true` in the copied `manager.yaml`, apply, and wait for
setup to complete. **This wipes the stack's database on every Manager startup
while enabled.** Set it back to `false` and apply again before running tests.
For a fully fresh stack, use destroy below and then apply again. That also
replaces its credentials and certificate state.

Load2 profiles expose JMX internally. For local inspection:

```bash
kubectl --context "$CLUSTER_NAME@$AWS_REGION" --namespace "$STACK_NAME" \
  port-forward service/manager-jmx 8085:8085
```

## Uninstall or destroy

Stop one stack but retain its data and certificate state:

```bash
kubernetes/or-eks-stack uninstall \
  --name "$STACK_NAME" --cluster "$CLUSTER_NAME" \
  --region "$AWS_REGION"
```

Permanently delete one stack, its data, and its owned external resources:

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
enough because it retains the stack namespace. To deploy without the managed
AWS facade (for example, with your own DNS controller), use the same values
with [`or-stack`](../kubernetes/README.md); DNS/certificate readiness and external
cleanup then need the documented low-level workflow.

## Existing legacy installations

This is a fresh namespaced deployment workflow, not an in-place migration of
default-namespace Helm releases or static PVs. Standard legacy setup entry
points now direct you to the CLIs. The load2 ACM/NLB experiment and its helper
scripts remain usable on a dedicated cluster; see its README. Do not use those
helpers to update or clean up a CLI-managed stack.

Deploy a new stack with a new hostname and verify it before retiring the old
installation. If data must be retained, back it up and plan its restore first.
The CLIs do not adopt old namespaces, EBS volumes, manual Route 53 records, or
ACM certificates. Inspect those legacy resources separately; use the
[DNS migration guidance](../kubernetes/cluster/eks/README.md) before reusing a
hostname. Never run the old cleanup procedure against a cluster that now hosts
other stacks.

## Offline validation

Run `python3 kubernetes/test/load-eks-test` to exercise all seven profiles
through `or-stack` with stubbed Kubernetes commands and real Helm lint/rendering.
The test also checks legacy ACM/NLB setup and redeployment for all six load2
profiles, using the same component values with AWS commands stubbed. It requires
Python 3, Bash, Helm, jq, and envsubst; no cluster or AWS
credentials are used. Actual EKS acceptance still requires deploying the desired
profiles and verifying login, dataset initialization, MQTTS traffic, persistence,
and independent stack cleanup.
