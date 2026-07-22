alter table SYSLOG_EVENT add column REALM varchar(255);

create index SYSLOG_EVENT_REALM_TIMESTAMP_IDX on SYSLOG_EVENT (REALM, TIMESTAMP desc);
