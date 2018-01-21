alter table ASSET_RULESET
  add column RULES_LANG varchar(255) not null default 'ECMASCRIPT_5_1';

alter table GLOBAL_RULESET
  add column RULES_LANG varchar(255) not null default 'ECMASCRIPT_5_1';

alter table TENANT_RULESET
  add column RULES_LANG varchar(255) not null default 'ECMASCRIPT_5_1';
