create table CUSTOM_ASSET_TYPE (
  NAME         varchar(255)             not null,
  DISPLAY_NAME varchar(255)             not null,
  ICON         varchar(255),
  COLOUR       varchar(255),
  DESCRIPTION  text,
  ENABLED      boolean                  not null,
  ATTRIBUTES   jsonb                    not null,
  VERSION      int8                     not null,
  CREATED_ON   timestamp with time zone not null,
  CREATED_BY   varchar(255),
  UPDATED_ON   timestamp with time zone not null,
  UPDATED_BY   varchar(255),
  primary key (NAME)
);
