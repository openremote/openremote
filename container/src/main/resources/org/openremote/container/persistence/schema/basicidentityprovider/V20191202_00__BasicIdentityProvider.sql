create table PUBLIC.REALM (
  ID                        varchar(36)           not null primary key,
  NAME                      varchar(255) unique   not null,
  ENABLED                   boolean default false not null,
  RESET_PASSWORD_ALLOWED    boolean default true not null,
  DUPLICATE_EMAILS_ALLOWED  boolean default false not null,
  REMEMBER_ME               boolean default false not null,
  REGISTRATION_ALLOWED      boolean default false not null,
  REG_EMAIL_AS_USERNAME     boolean default false not null,
  VERIFY_EMAIL              boolean default false not null,
  LOGIN_WITH_EMAIL_ALLOWED  boolean default false not null,
  LOGIN_THEME               varchar(255) default null,
  ACCOUNT_THEME             varchar(255) default null,
  ADMIN_THEME               varchar(255) default null,
  EMAIL_THEME               varchar(255) default null,
  ACCESS_TOKEN_LIFESPAN     integer default null,
  NOT_BEFORE integer
);

create table PUBLIC.KEYCLOAK_ROLE (
  ID                        varchar(36) not null primary key,
  REALM_ID varchar(36)      not null references PUBLIC.REALM,
  CLIENT                    varchar(255) default null,
  NAME                      varchar(255) not null,
  DESCRIPTION               varchar(255) default null
);

create table PUBLIC.REALM_ATTRIBUTE (
  NAME     varchar(255) not null,
  VALUE    varchar(255),
  REALM_ID varchar(36)  not null references PUBLIC.REALM,
  primary key (NAME, REALM_ID)
);

create table PUBLIC.USER_ENTITY (
  ID         varchar(36)           not null primary key,
  REALM_ID   varchar(255)          not null,
  USERNAME   varchar(255)          not null,
  PASSWORD   varchar(255)          not null,
  FIRST_NAME varchar(255),
  LAST_NAME  varchar(255),
  EMAIL      varchar(255),
  ENABLED    boolean default false not null,
  constraint USER_ENTITY_UNIQUE unique (REALM_ID, USERNAME)
);
