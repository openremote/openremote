create table DASHBOARD (
  ID                 varchar(22)              not null,
  CREATED_ON         timestamp with time zone not null,
  REALM              varchar(255)             not null,
  VERSION            int8                     not null,
  OWNER_ID           varchar(255)             not null,
  VIEW_ACCESS        int8                     not null,
  EDIT_ACCESS        int8                     not null,
  DISPLAY_NAME       varchar(255)             not null,
  TEMPLATE           jsonb                    not null,
  primary key (ID)
);

create table ALARM (
  ID                 int8                     not null,
  REALM              varchar(255)             not null,
  TITLE              varchar(255),
  CONTENT            varchar(4096),
  SEVERITY           varchar(15)              not null,
  STATUS             varchar(15)              not null,
  SOURCE             varchar(50)              not null,
  SOURCE_ID          varchar(43)              not null,
  CREATED_ON         timestamp with time zone not null,
  ACKNOWLEDGED_ON    timestamp with time zone,
  primary key (ID)
);

create table ALARM_USER_LINK (
  ALARM_ID   varchar(22)              not null,
  REALM      varchar(255)             not null,
  USER_ID    varchar(36)              not null,
  CREATED_ON timestamp with time zone not null,
  primary key (USER_ID, REALM, ALARM_ID)
);

create table ALARM_ASSET_LINK (
  ASSET_ID   varchar(22)              not null,
  REALM      varchar(255)             not null,
  ALARM_ID   varchar(36)              not null,
  CREATED_ON timestamp with time zone not null,
  primary key (ASSET_ID, REALM, ALARM_ID)
);
