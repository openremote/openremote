create table ALARM (
  ID              int8                     not null,
  REALM           varchar(255)             not null,
  TITLE           varchar(255)             not null,
  CONTENT         varchar(4096),
  SEVERITY        varchar(15)              not null,
  STATUS          varchar(15)              not null,
  SOURCE          varchar(50)              not null,
  SOURCE_ID       varchar(43)              not null,
  CREATED_ON      timestamp with time zone not null,
  ACKNOWLEDGED_ON timestamp with time zone,
  LAST_MODIFIED   timestamp with time zone not null,
  ASSIGNEE_ID     varchar(255),
  primary key (ID)
);

create table ALARM_ASSET_LINK (
  SENTALARM_ID    int8                     not null,
  REALM           varchar(255)             not null,
  ASSET_ID        varchar(22)              not null,
  CREATED_ON      timestamp with time zone not null,
  primary key (ASSET_ID, REALM, SENTALARM_ID)
);

alter table ALARM
  add foreign key (ASSIGNEE_ID) references PUBLIC.USER_ENTITY (ID) on delete cascade;

alter table ALARM_ASSET_LINK
  add foreign key (SENTALARM_ID) references ALARM (ID) on delete cascade;

alter table ALARM_ASSET_LINK
  add foreign key (ASSET_ID) references ASSET (ID) on delete cascade;

alter table ALARM_ASSET_LINK
  add foreign key (REALM) references PUBLIC.REALM (NAME) on delete cascade;
