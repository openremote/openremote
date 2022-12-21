/*
  Some helper functions for performing CRUD operations on attributes and/or meta items
 */
CREATE OR REPLACE FUNCTION REMOVE_ATTRIBUTES(ASSET_ROW asset, VARIADIC ATTRIBUTE_NAMES text[]) RETURNS boolean AS $$
BEGIN
    UPDATE asset a
    SET attributes = a.attributes - ATTRIBUTE_NAMES
    WHERE a.ID = ASSET_ROW.ID;
    RETURN FOUND;
END
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION ADD_ATTRIBUTE(ASSET_ROW asset, ATTRIBUTE_NAME text, ATTRIBUTE_TYPE text, VALUE jsonb, TIME_STAMP timestamp with time zone, META jsonb) RETURNS boolean AS $$
BEGIN
    UPDATE asset a
    SET attributes = a.attributes || (
        SELECT jsonb_build_object(ATTRIBUTE_NAME, jsonb_build_object(
                'name', ATTRIBUTE_NAME,
                'type', ATTRIBUTE_TYPE,
                'value', VALUE,
                'timestamp', (extract(epoch from TIME_STAMP AT TIME ZONE 'UTC') * 1000)::bigint,
                'meta', '{}'::jsonb || coalesce(META, '{}'::jsonb))))
    WHERE a.id = ASSET_ROW.id;
    RETURN FOUND;
END
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION REMOVE_META(ASSET_ROW asset, ATTRIBUTE_NAME text, VARIADIC META_NAMES text[]) RETURNS boolean AS $$
BEGIN
    UPDATE asset a
    SET attributes = (a.attributes - ATTRIBUTE_NAME) || jsonb_build_object(ATTRIBUTE_NAME, (((a.attributes -> ATTRIBUTE_NAME) - 'meta') || jsonb_build_object('meta', (a.attributes -> ATTRIBUTE_NAME -> 'meta' || '{}'::jsonb) - META_NAMES)))
    WHERE a.ID = ASSET_ROW.ID;
    RETURN FOUND;
END
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION ADD_UPDATE_META(ASSET_ROW asset, ATTRIBUTE_NAME text, META jsonb) RETURNS boolean AS $$
BEGIN
    UPDATE asset a
    SET attributes = (a.attributes - ATTRIBUTE_NAME) || jsonb_build_object(ATTRIBUTE_NAME, (((a.attributes -> ATTRIBUTE_NAME) - 'meta') || jsonb_build_object('meta', (a.attributes -> ATTRIBUTE_NAME -> 'meta') || META || '{}'::jsonb)))
    WHERE a.id = ASSET_ROW.id;
    RETURN FOUND;
END
$$ LANGUAGE plpgsql;
