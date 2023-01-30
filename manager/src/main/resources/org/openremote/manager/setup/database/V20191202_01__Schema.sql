/*
  ############################# EXTENSIONS #############################
 */
create extension if not exists POSTGIS;
create extension if not exists POSTGIS_TOPOLOGY;
create extension if not exists ltree;
/*
  ############################# SEQUENCES #############################
 */
create sequence OPENREMOTE_SEQUENCE
  start 1000
  increment 1;

/*
  ############################# TABLES #############################
 */

create table ASSET (
  ID                 varchar(22)              not null,
  ATTRIBUTES         jsonb,
  CREATED_ON         timestamp with time zone not null,
  NAME               varchar(1023)            not null,
  PARENT_ID          varchar(22),
  PATH               ltree,
  REALM              varchar(255)             not null,
  TYPE               varchar(500)             not null,
  ACCESS_PUBLIC_READ boolean                  not null,
  VERSION            int8                     not null,
  primary key (ID),
  check (ID != PARENT_ID)
);

create table ASSET_DATAPOINT (
  TIMESTAMP      timestamp                  not null,
  ENTITY_ID      varchar(22)                not null,
  ATTRIBUTE_NAME varchar(255)               not null,
  VALUE          jsonb                      not null,
  primary key (TIMESTAMP, ENTITY_ID, ATTRIBUTE_NAME)
);

create table GLOBAL_RULESET (
  ID            int8                     not null,
  CREATED_ON    timestamp with time zone not null,
  ENABLED       boolean                  not null,
  LAST_MODIFIED timestamp with time zone not null,
  NAME          varchar(255)             not null,
  RULES         text                     not null,
  RULES_LANG    varchar(255)             not null,
  VERSION       int8                     not null,
  META          jsonb,
  primary key (ID)
);

create table ASSET_RULESET (
  ID                    int8                     not null,
  CREATED_ON            timestamp with time zone not null,
  ENABLED               boolean                  not null,
  LAST_MODIFIED         timestamp with time zone not null,
  NAME                  varchar(255)             not null,
  RULES                 text                     not null,
  RULES_LANG            varchar(255)             not null default 'GROOVY',
  VERSION               int8                     not null,
  ASSET_ID              char(22)                 not null,
  ACCESS_PUBLIC_READ    boolean                  not null default false,
  META                  jsonb,
  primary key (ID)
);

create table TENANT_RULESET (
  ID                    int8                     not null,
  CREATED_ON            timestamp with time zone not null,
  ENABLED               boolean                  not null,
  LAST_MODIFIED         timestamp with time zone not null,
  NAME                  varchar(255)             not null,
  RULES                 text                     not null,
  RULES_LANG            varchar(255)             not null default 'GROOVY',
  VERSION               int8                     not null,
  REALM                 varchar(255)             not null,
  ACCESS_PUBLIC_READ    boolean                  not null default false,
  META                  jsonb,
  primary key (ID)
);

create table USER_ASSET_LINK (
  ASSET_ID   char(22)                 not null,
  REALM      varchar(255)             not null,
  USER_ID    varchar(36)              not null,
  CREATED_ON timestamp with time zone not null,
  primary key (ASSET_ID, REALM, USER_ID)
);

create table NOTIFICATION (
  ID              int8                     not null,
  NAME            varchar(255),
  TYPE            varchar(50)              not null,
  TARGET          varchar(50)              not null,
  TARGET_ID       varchar(255)              not null,
  SOURCE          varchar(50)              not null,
  SOURCE_ID       varchar(43),
  MESSAGE         jsonb,
  ERROR           varchar(4096),
  SENT_ON         timestamp with time zone not null,
  DELIVERED_ON    timestamp with time zone,
  ACKNOWLEDGED_ON timestamp with time zone,
  ACKNOWLEDGEMENT varchar(255),
  primary key (ID)
);

create table SYSLOG_EVENT (
  ID          int8         not null,
  TIMESTAMP   timestamp with time zone not null,
  CATEGORY    varchar(255) not null,
  LEVEL       int4         not null,
  MESSAGE     varchar(131072),
  SUBCATEGORY varchar(1024),
  primary key (ID)
);

create table ASSET_PREDICTED_DATAPOINT (
  TIMESTAMP      timestamp                  not null,
  ENTITY_ID      varchar(36)                not null,
  ATTRIBUTE_NAME varchar(255)               not null,
  VALUE          jsonb                      not null,
  primary key (TIMESTAMP, ENTITY_ID, ATTRIBUTE_NAME)
);

create table GATEWAY_CONNECTION (
    LOCAL_REALM        varchar(255)             not null,
    REALM              varchar(255)             not null,
    HOST               varchar(255)             not null,
    PORT               int8                     null,
    CLIENT_ID          varchar(36)              not null,
    CLIENT_SECRET      varchar(36)              not null,
    SECURED            boolean                  null,
    DISABLED           boolean                  not null default false,
    primary key (LOCAL_REALM)
);

create table PROVISIONING_CONFIG (
  ID                    int8                     not null,
  CREATED_ON            timestamp with time zone not null,
  DISABLED              boolean                  not null default false,
  LAST_MODIFIED         timestamp with time zone not null,
  NAME                  varchar(255)             not null,
  TYPE                  varchar(100)             not null,
  ROLES                 varchar(255)[]           null,
  REALM                 varchar(255)             not null,
  ASSET_TEMPLATE        text                     null,
  RESTRICTED_USER       boolean                  not null default false,
  DATA                  jsonb,
  primary key (ID)
);

/*
  ############################# FUNCTIONS #############################
 */
CREATE OR REPLACE FUNCTION update_asset_parent_info() RETURNS TRIGGER AS $$
DECLARE
    ppath ltree;
BEGIN
    IF NEW.parent_id IS NULL THEN
        NEW.path = NEW.id::ltree;
    ELSEIF TG_OP = 'INSERT' OR OLD.parent_id IS NULL OR OLD.parent_id != NEW.parent_id THEN
        SELECT A.path || NEW.id::text INTO ppath FROM ASSET A WHERE id = NEW.parent_id;
        IF ppath IS NULL THEN
            RAISE EXCEPTION 'Invalid parent_id %', NEW.parent_id;
        END IF;
        NEW.path = ppath;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

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
  ############################# TRIGGERS #############################
 */

CREATE TRIGGER asset_parent_info_tgr
    BEFORE INSERT OR UPDATE ON ASSET
    FOR EACH ROW EXECUTE PROCEDURE update_asset_parent_info();

/*
  ############################# CONSTRAINTS #############################
 */

alter table ASSET
  add foreign key (PARENT_ID) references ASSET (ID);

alter table ASSET
  add foreign key (REALM) references PUBLIC.REALM (NAME);

alter table ASSET_DATAPOINT
  add foreign key (ENTITY_ID) references ASSET (ID) on delete cascade;

alter table TENANT_RULESET
  add foreign key (REALM) references PUBLIC.REALM (NAME);

alter table ASSET_RULESET
  add foreign key (ASSET_ID) references ASSET (ID) on delete cascade;

alter table USER_ASSET_LINK
  add foreign key (USER_ID) references PUBLIC.USER_ENTITY (ID) on delete cascade;

alter table USER_ASSET_LINK
  add foreign key (ASSET_ID) references ASSET (ID) on delete cascade;

alter table USER_ASSET_LINK
  add foreign key (REALM) references PUBLIC.REALM (NAME) on delete cascade;

alter table PROVISIONING_CONFIG
    add foreign key (REALM) references PUBLIC.REALM (NAME);

/*
  ############################# INDICES #############################
 */

CREATE INDEX SECTION_PARENT_PATH_IDX ON ASSET USING GIST (path);
CREATE INDEX SECTION_PARENT_ID_IDX ON ASSET (parent_id);
