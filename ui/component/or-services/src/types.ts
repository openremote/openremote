import { TreeNode } from "@openremote/or-tree-menu";
import { Microservice } from "@openremote/model";

export enum MicroserviceStatusIcon {
  AVAILABLE = "play",
  UNAVAILABLE = "alert-octagon",
}

export enum MicroserviceStatusColor {
  AVAILABLE = "iconfill-gray",
  UNAVAILABLE = "iconfill-red",
}

export interface ServiceTreeNode extends TreeNode {
  service?: Microservice;
} 