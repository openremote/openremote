/*
  ############################# EXTENSIONS #############################
 */
create extension if not exists POSTGIS;
create extension if not exists POSTGIS_TOPOLOGY;

/*
  ############################# SEQUENCES #############################
 */
create sequence OPENREMOTE_SEQUENCE
  start 1000
  increment 1;

/*
  ############################# TABLES #############################
 */

create table ALERT_NOTIFICATION (
  ID              int8                     not null,
  ACTIONS         jsonb,
  APP_URL         varchar(255)             not null,
  CREATED_ON      timestamp with time zone not null,
  DELIVERY_STATUS varchar(255),
  MESSAGE         varchar(255)             not null,
  TITLE           varchar(255)             not null,
  USER_ID         varchar(255),
  primary key (ID)
);

create table ASSET (
  ID          varchar(43)              not null,
  ATTRIBUTES  jsonb,
  CREATED_ON  timestamp with time zone not null,
  NAME        varchar(1023)            not null,
  PARENT_ID   varchar(36),
  REALM_ID    varchar(255)             not null,
  ASSET_TYPE  varchar(255)             not null,
  OBJ_VERSION int8                     not null,
  LOCATION    geometry,
  primary key (ID),
  check (ID != PARENT_ID)
);

create table ASSET_DATAPOINT (
  VALUE          jsonb        not null,
  TIMESTAMP      int8         not null,
  ENTITY_ID      varchar(36)  not null,
  ATTRIBUTE_NAME varchar(255) not null,
  primary key (VALUE, TIMESTAMP, ENTITY_ID, ATTRIBUTE_NAME)
);

create table ASSET_RULESET (
  ID                int8                     not null,
  CREATED_ON        timestamp with time zone not null,
  ENABLED           boolean                  not null,
  LAST_MODIFIED     timestamp with time zone not null,
  NAME              varchar(255)             not null,
  RULES             text                     not null,
  TEMPLATE_ASSET_ID varchar(255),
  OBJ_VERSION       int8                     not null,
  ASSET_ID          varchar(255)             not null,
  primary key (ID)
);

create table DEVICE_NOTIFICATION_TOKEN (
  DEVICE_ID   varchar(255)             not null,
  USER_ID     varchar(36)              not null,
  DEVICE_TYPE varchar(255),
  TOKEN       varchar(4096)            not null,
  UPDATED_ON  timestamp with time zone not null,
  primary key (DEVICE_ID, USER_ID)
);

create table GLOBAL_RULESET (
  ID                int8                     not null,
  CREATED_ON        timestamp with time zone not null,
  ENABLED           boolean                  not null,
  LAST_MODIFIED     timestamp with time zone not null,
  NAME              varchar(255)             not null,
  RULES             text                     not null,
  TEMPLATE_ASSET_ID varchar(255),
  OBJ_VERSION       int8                     not null,
  primary key (ID)
);

create table SYSLOG_EVENT (
  ID          int8         not null,
  TIMESTAMP   int8         not null,
  CATEGORY    varchar(255) not null,
  LEVEL       int4         not null,
  MESSAGE     varchar(131072),
  SUBCATEGORY varchar(1024),
  primary key (ID)
);

create table TENANT_RULESET (
  ID                int8                     not null,
  CREATED_ON        timestamp with time zone not null,
  ENABLED           boolean                  not null,
  LAST_MODIFIED     timestamp with time zone not null,
  NAME              varchar(255)             not null,
  RULES             text                     not null,
  TEMPLATE_ASSET_ID varchar(255),
  OBJ_VERSION       int8                     not null,
  REALM_ID          varchar(255)             not null,
  primary key (ID)
);

create table USER_ASSET (
  ASSET_ID   varchar(36)              not null,
  REALM_ID   varchar(36)              not null,
  USER_ID    varchar(36)              not null,
  CREATED_ON timestamp with time zone not null,
  primary key (ASSET_ID, REALM_ID, USER_ID)
);

create table USER_CONFIGURATION (
  USER_ID    varchar(36) not null,
  RESTRICTED boolean     not null,
  primary key (USER_ID)
);

/*
  ############################# FUNCTIONS #############################
 */
create or replace function GET_ASSET_TREE_PATH(ASSET_ID text)
  returns text [] as
$$
begin
  return (with recursive ASSET_TREE(ID, PARENT_ID, PATH) as (
    select
      A1.ID,
      A1.PARENT_ID,
      array [text(A1.ID)]
    from ASSET A1
    where A1.ID = ASSET_ID
    union all
    select
      A2.ID,
      A2.PARENT_ID,
      array_append(AT.PATH, text(A2.ID))
    from ASSET A2, ASSET_TREE AT
    where A2.ID = AT.PARENT_ID and AT.PARENT_ID is not null
  ) select PATH
    from ASSET_TREE
    where PARENT_ID is null);
end;
$$
language plpgsql;

/*
  ############################# CONSTRAINTS #############################
 */

alter table ASSET
  add foreign key (PARENT_ID) references ASSET (ID);

alter table ASSET
  add foreign key (REALM_ID) references PUBLIC.REALM (ID);

alter table ASSET_DATAPOINT
  add foreign key (ENTITY_ID) references ASSET (ID) on delete cascade;

alter table TENANT_RULESET
  add foreign key (REALM_ID) references PUBLIC.REALM (ID);

alter table ASSET_RULESET
  add foreign key (ASSET_ID) references ASSET (ID) on delete cascade;

alter table DEVICE_NOTIFICATION_TOKEN
  add foreign key (USER_ID) references PUBLIC.USER_ENTITY (ID) on delete cascade;

alter table USER_CONFIGURATION
  add foreign key (USER_ID) references PUBLIC.USER_ENTITY (ID) on delete cascade;

alter table USER_ASSET
  add foreign key (USER_ID) references PUBLIC.USER_ENTITY (ID) on delete cascade;

alter table USER_ASSET
  add foreign key (ASSET_ID) references ASSET (ID) on delete cascade;

alter table USER_ASSET
  add foreign key (REALM_ID) references PUBLIC.REALM (ID) on delete cascade;
