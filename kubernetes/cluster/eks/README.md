# EKS cluster management

`kubernetes/or-eks-cluster` manages the shared EKS cluster, its shared AWS Load
Balancer Controller, and the `openremote-ebs` StorageClass. It does not deploy
or delete OpenRemote stacks, stack namespaces, certificates, DNS records, or
stack data volumes.

Use `kubernetes/or-stack` to install and inspect namespaced OpenRemote stacks
after the shared cluster is ready.

The current implementation uses the existing `kubernetes/cluster.yaml`
`eksctl` configuration. This boundary is intended to remain stable when EKS
infrastructure management moves from `eksctl` to CloudFormation.

## Configuration

The cluster name is deliberately required. Other settings have defaults that
match the existing EKS scripts.

```bash
export OR_EKS_CLUSTER_NAME=openremote-test
export OR_EKS_AWS_REGION=eu-west-1
export OR_EKS_AWS_PROFILE=or
```

Optional settings are:

```bash
export OR_EKS_CONFIG_FILE=/path/to/cluster.yaml
export OR_EKS_KUBE_CONTEXT=openremote-test@eu-west-1
export OR_EKS_LOAD_BALANCER_CONTROLLER_CHART_VERSION=1.14.0
```

Command-line options override environment variables. Run
`kubernetes/or-eks-cluster --help` for the complete command reference.

The script does not write AWS credentials or profiles. Configure the selected
AWS profile before invoking it.

## Lifecycle

Create a cluster and a stable kubeconfig context:

```bash
kubernetes/or-eks-cluster create \
  --name openremote-test \
  --region eu-west-1 \
  --profile or
```

Inspect it:

```bash
kubernetes/or-eks-cluster status --name openremote-test
```

`apply` currently reconciles the AWS Load Balancer Controller, including its
CRDs, and the `openremote-ebs` StorageClass used by OpenRemote stack PVCs. It
intentionally does not attempt to mutate node-group infrastructure; that
reconciliation will be implemented by the future CloudFormation cluster
definition.

```bash
kubernetes/or-eks-cluster apply --name openremote-test
```

Cluster destruction requires an exact-name confirmation and is refused while
Ingresses, LoadBalancer Services, PVCs, or non-system Pods remain:

```bash
kubernetes/or-eks-cluster destroy \
  --name openremote-test \
  --confirm openremote-test
```

Remove OpenRemote stacks and make an explicit data-retention decision before
destroying their cluster. `or-stack uninstall` preserves a stack's namespace
and EBS-backed PVCs, so the cluster destroy preflight continues to block.
`or-stack destroy --confirm <stack-name>` deletes that namespace and its data,
allowing cluster destruction after the backing volumes have been removed.

Kubernetes minor-version upgrades are intentionally outside this first phase.
The current `cluster.yaml` uses a self-managed `nodeGroups` entry, so an upgrade
strategy must be designed separately before the cluster command exposes that
lifecycle operation.

## Persistent storage

The `openremote-ebs` StorageClass uses the standard EBS CSI driver, encrypted
`gp3` volumes, and `WaitForFirstConsumer` binding. Its `Delete` reclaim policy
removes the EBS volume when its PVC is explicitly deleted. The component charts
retain their PVCs during an ordinary Helm uninstall, and cluster destruction is
blocked while any PVC remains.
