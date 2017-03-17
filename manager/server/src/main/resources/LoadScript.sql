alter table ASSET
  add foreign key (REALM_ID) references REALM (ID);

alter table ASSET_DATAPOINT
  add foreign key (ENTITY_ID) references ASSET (ID) on delete cascade;

alter table TENANT_RULES
  add foreign key (REALM_ID) references REALM (ID);

alter table ASSET_RULES
  add foreign key (ASSET_ID) references ASSET (ID) on delete cascade;

alter table DEVICE_NOTIFICATION_TOKEN
  add foreign key (USER_ID) references USER_ENTITY (ID) on delete cascade;

alter table USER_CONFIGURATION
  add foreign key (USER_ID) references USER_ENTITY (ID) on delete cascade;

alter table USER_ASSET
  add foreign key (USER_ID) references USER_ENTITY (ID) on delete cascade;

alter table USER_ASSET
  add foreign key (ASSET_ID) references ASSET (ID) on delete cascade;
