/*
  Fixes null issues with helper functions for meta items
 */
CREATE OR REPLACE FUNCTION REMOVE_META(ASSET_ROW asset, ATTRIBUTE_NAME text, VARIADIC META_NAMES text[]) RETURNS boolean AS $$
BEGIN
    UPDATE asset a
    SET attributes = (a.attributes - ATTRIBUTE_NAME) || jsonb_build_object(ATTRIBUTE_NAME, (((a.attributes -> ATTRIBUTE_NAME) - 'meta') || jsonb_build_object('meta', '{}'::jsonb || COALESCE(jsonb_strip_nulls(a.attributes -> ATTRIBUTE_NAME) -> 'meta', '{}'::jsonb) - META_NAMES)))
    WHERE a.ID = ASSET_ROW.ID;
    RETURN FOUND;
END
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION ADD_UPDATE_META(ASSET_ROW asset, ATTRIBUTE_NAME text, META jsonb) RETURNS boolean AS $$
BEGIN
    UPDATE asset a
    SET attributes = (a.attributes - ATTRIBUTE_NAME) || jsonb_build_object(ATTRIBUTE_NAME, (((a.attributes -> ATTRIBUTE_NAME) - 'meta') || jsonb_build_object('meta', '{}'::jsonb || COALESCE(jsonb_strip_nulls(a.attributes -> ATTRIBUTE_NAME) -> 'meta', '{}'::jsonb) || META)))
    WHERE a.id = ASSET_ROW.id;
    RETURN FOUND;
END
$$ LANGUAGE plpgsql;
