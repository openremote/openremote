
/*
  Removed use of REALM_ID and now just using REALM
 */

-- UPDATE ASSET TABLE

ALTER TABLE ASSET
  DROP CONSTRAINT asset_realm_id_fkey,
  ADD COLUMN REALM VARCHAR(255);

DO $$
  begin
    IF EXISTS(SELECT to_regclass('public.realm')) THEN
      UPDATE openremote.ASSET A SET REALM = R.NAME FROM public.REALM R WHERE A.REALM_ID = R.id;
    END IF;
  end
  $$;

ALTER TABLE ASSET
  ALTER COLUMN REALM SET NOT NULL,
  add foreign key (REALM) references PUBLIC.REALM (NAME),
  DROP COLUMN REALM_ID;


-- UPDATE TENANT RULSET TABLE

ALTER TABLE TENANT_RULESET
  DROP CONSTRAINT tenant_ruleset_realm_id_fkey,
  ADD COLUMN REALM VARCHAR(255);

DO $$
  begin
    IF EXISTS(SELECT to_regclass('public.realm')) THEN
      UPDATE openremote.TENANT_RULESET TR SET REALM = R.NAME FROM public.REALM R WHERE TR.REALM_ID = R.id;
    END IF;
  end
  $$;

ALTER TABLE TENANT_RULESET
  ALTER COLUMN REALM SET NOT NULL,
  add foreign key (REALM) references PUBLIC.REALM (NAME),
  DROP COLUMN REALM_ID;


-- UPDATE USER ASSET TABLE

ALTER TABLE USER_ASSET
  DROP CONSTRAINT user_asset_realm_id_fkey,
  ADD COLUMN REALM VARCHAR(255);

DO $$
  begin
    IF EXISTS(SELECT to_regclass('public.realm')) THEN
      UPDATE openremote.USER_ASSET A SET REALM = R.NAME FROM public.REALM R WHERE A.REALM_ID = R.id;
    END IF;
  end
  $$;

ALTER TABLE USER_ASSET
  ALTER COLUMN REALM SET NOT NULL,
  add foreign key (REALM) references PUBLIC.REALM (NAME) on delete cascade,
  DROP COLUMN REALM_ID;


-- UPDATE NOTIFICATION TABLE
UPDATE openremote.notification N SET target_id = R.NAME FROM public.REALM R WHERE target = 'TENANT' and N.target_id = R.id;