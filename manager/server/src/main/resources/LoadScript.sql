alter table REALM_RULES
  add foreign key (REALM) references REALM (NAME) on update cascade on delete cascade;

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
