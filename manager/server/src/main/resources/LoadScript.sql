alter table REALM_RULES add foreign key(REALM) references REALM(NAME) on update cascade on delete cascade;
alter table ASSET_RULES add foreign key(ASSET_ID) references ASSET(ID) on delete cascade;
