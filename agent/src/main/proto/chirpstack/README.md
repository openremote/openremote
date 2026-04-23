# ChirpStack gRPC API Proto Files

This directory contains the Protocol Buffer definitions (`.proto`) required to generate Java client classes for the ChirpStack gRPC API.

## Source Information

* **Upstream Repository:** [https://github.com/chirpstack/chirpstack](https://github.com/chirpstack/chirpstack)
* **ChirpStack Version:** `v4.14.1`
* **Source Path:** `api/proto/`

## Directory List

The following directories were copied from the source:

* `api/`
* `common/`
* `gw/`
* `integration/`
* `internal/`
* `stream/`

> [!NOTE]
> The files located in `api/proto/google/api` in the upstream repository must be copied to the local `shared/google/api` directory to ensure successful compilation.

## How to Update

1.  Identify the desired version tag on the [ChirpStack Releases](https://github.com/chirpstack/chirpstack/releases) page.
2.  Download the folders listed above from the corresponding version branch.
3.  Replace the local `.proto` files.
4.  Ensure the `shared/google/api` files are updated if the upstream dependencies have changed.
5.  Re-run the build
