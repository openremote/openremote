import { UserModel } from "../../../src/pages/page-users";
import { Role } from "@openremote/model";

export const roles: Record<string, Role> = {
  Custom: {},
};

export type Usernames = "admin" | "smartcity";
export const users: Record<Usernames, UserModel & { username: Usernames; password: string }> = {
  admin: {
    username: "admin",
    password: "secret",
  },
  smartcity: {
    enabled: true,
    username: "smartcity",
    password: "smartcity",
    realm: "smartcity",
    roles: ["read", "write"],
    previousRoles: [],
    realmRoles: [],
    previousRealmRoles: [],
    userAssetLinks: [],
    serviceAccount: false,
  },
};
