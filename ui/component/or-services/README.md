# @openremote/or-services

OpenRemote services related UI components and utilities.

## Components

### OrServiceTree

A custom tree component for displaying and managing microservices.

**Properties:**
- `services?: Microservice[]` - Array of microservices to display
- `selectedService?: Microservice` - Currently selected service
- `readonly?: boolean` - Whether the tree is in readonly mode

**Events:**
- `service-selected` - Fired when a service is selected
- `refresh-services` - Fired when the refresh button is clicked

**Usage:**
```typescript
import "@openremote/or-services";

// In your template
<or-service-tree
  .services="${services}"
  .selectedService="${selectedService}"
  @service-selected="${onServiceSelected}"
  @refresh-services="${onRefreshServices}"
></or-service-tree>
```

## Types

### MicroserviceStatusIcon
Enum for service status icons:
- `AVAILABLE = "play"`
- `UNAVAILABLE = "alert-octagon"`

### MicroserviceStatusColor
Enum for service status colors:
- `AVAILABLE = "iconfill-gray"`
- `UNAVAILABLE = "iconfill-red"`

### ServiceTreeNode
Interface extending TreeNode with optional service property:
```typescript
interface ServiceTreeNode extends TreeNode {
  service?: Microservice;
}
```

## Utilities

### consolidateServices(services: Microservice[]): Microservice[]
Consolidates services by serviceId, preferring AVAILABLE over UNAVAILABLE status.

### getServiceUrlPath(service: Microservice, realmName: string, isSuperUser: boolean): string
Generates the iframe URL path for a service, handling realm parameter replacement.

## Dependencies

This package depends on:
- `@openremote/core`
- `@openremote/model`
- `@openremote/or-tree-menu`
- `@openremote/or-mwc-components`
- `@openremote/or-translate`
- `@openremote/or-icon`