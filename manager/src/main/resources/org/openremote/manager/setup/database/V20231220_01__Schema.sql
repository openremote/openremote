CREATE TABLE ASSET_ANOMALY (
                               TIMESTAMP      timestamp                  not null,
                               ENTITY_ID      varchar(36)                not null,
                               ATTRIBUTE_NAME varchar(255)               not null,
                               METHOD_NAME    varchar(255)               not null,
                               ALARM_ID       int8                       null,
                               primary key (TIMESTAMP, ENTITY_ID, ATTRIBUTE_NAME, METHOD_NAME)
);
SELECT public.create_hypertable('asset_anomaly', 'timestamp', if_not_exists := true, migrate_data := true);
