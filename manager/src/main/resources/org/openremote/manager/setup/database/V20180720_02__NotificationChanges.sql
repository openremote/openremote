
/*
  New notification infrastructure, rather than migrate existing notifications the entire table is dropped and recreated
 */

drop table ALERT_NOTIFICATION cascade;
drop table DEVICE_NOTIFICATION_TOKEN cascade;

create table NOTIFICATION (
  ID              int8                     not null,
  NAME            varchar(255),
  TYPE            varchar(50)              not null,
  TARGET          varchar(50)              not null,
  TARGET_ID       varchar(43)              not null,
  SOURCE          varchar(50)              not null,
  SOURCE_ID       varchar(43),
  MESSAGE         jsonb,
  ERROR           varchar(255),
  SENT_ON         timestamp with time zone not null,
  DELIVERED_ON    timestamp with time zone,
  ACKNOWLEDGED_ON timestamp with time zone,
  ACKNOWLEDGEMENT varchar(255),
  primary key (ID)
);