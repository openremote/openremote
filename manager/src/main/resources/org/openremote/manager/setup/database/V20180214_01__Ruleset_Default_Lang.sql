-- alter table ASSET_DATAPOINT
--   alter column VALUE DROP NOT NULL;

alter table ASSET_RULESET
  alter column RULES_LANG set default 'GROOVY';

alter table GLOBAL_RULESET
  alter column RULES_LANG set default 'GROOVY';

alter table TENANT_RULESET
  alter column RULES_LANG set default 'GROOVY';
