create table ALERT (
  ID                 int8              not null,
  TITLE              varchar(255),
  CONTENT            varchar(4096),
  TRIGGER            varchar(50)       not null,
  TRIGGER_ID         varchar(50)       not null,
  SEVERITY           varchar(50)       not null,
  STATUS             varchar(10)       not null,
  primary key (ID)
);
