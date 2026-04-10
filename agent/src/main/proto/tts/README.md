# The Things Stack gRPC API Proto Files

This directory contains the Protocol Buffer definitions (`.proto`) required to generate Java client classes for The Things Stack (TTN V3) gRPC API.

## Source Information

* **Upstream Repository:** [https://github.com/TheThingsNetwork/lorawan-stack](https://github.com/TheThingsNetwork/lorawan-stack)
* **Release Version:** `v3.34.3`
* **Source:** Files extracted from the `api/` directory of the official release source.

## Directory List

The following directory was copied directly from `api/`:

* `ttn/lorawan/v3/`

## Third-Party Dependencies (Flattened)

Dependencies located under `api/third_party/` in the upstream repository have been flattened into the local project structure as follows:

| Upstream Source Path (under api/third_party/) | Local Destination Path |
| :--- | :--- |
| `protoc-gen-openapiv2/options` | `protoc-gen-openapiv2/options/` |
| `thethings/flags` | `thethings/flags/` |
| `thethings/json` | `thethings/json/` |
| `validate` | `validate/` |

> [!NOTE]
> The files from **`api/third_party/google/api`** have been moved to the local **`shared/google/api`** directory to align with the project's shared resource structure.

## How to Update

1.  Go to the [The Things Stack Releases](https://github.com/TheThingsNetwork/lorawan-stack/releases) page.
2.  Download the Source Code for the target version.
3.  Extract the `.proto` files from the `api/` folder.
4.  Update the local files according to the flattened mapping table above.
5.  Ensure `shared/google/api` is updated if the upstream Google dependencies have changed.
6.  Re-run the build
