/*
 * Drop existing function and trigger
 */
DROP FUNCTION update_asset_parent_info cascade;

/*
 * Fix update_asset_parent_info so descendant paths are updated when an asset is re-parented
 */
CREATE FUNCTION update_asset_parent_info() RETURNS TRIGGER AS $$
DECLARE
    ppath ltree;
    cnode ltree;
BEGIN
    IF TG_OP = 'INSERT' THEN
        IF NEW.parent_id IS NULL THEN
            NEW.path = NEW.id::ltree;
        ELSE
            SELECT ltree_addtext(path, NEW.id) INTO NEW.path FROM asset WHERE id = NEW.parent_id;
        END IF;
    -- If UPDATE, when a row's parent changes, we also need to also patch the path of all of the children.
    ELSEIF TG_OP = 'UPDATE' AND NEW.parent_id IS DISTINCT FROM OLD.parent_id THEN
        -- we want to move the paths such that all descendants of NEW.id gets their path patched.
        cnode = NEW.id::ltree;
        SELECT A.path INTO ppath FROM ASSET A WHERE id = NEW.parent_id;
        IF index(ppath, cnode) > -1 THEN
            RAISE EXCEPTION 'Circular reference. Tried to set parent to % for row %, but % is already in parent path %',
                NEW.parent_id, NEW.id, NEW.id, ppath;
        ELSEIF ppath IS NULL AND NEW.parent_id IS NULL THEN
           NEW.path = cnode;
        ELSEIF ppath IS NULL THEN
            RAISE EXCEPTION 'Invalid parent_id %', NEW.parent_id;
        ELSE
            NEW.path = ppath || cnode;
        END IF;
        UPDATE asset
            SET path = NEW.path || subpath(path, nlevel(OLD.path))
            WHERE path <@ OLD.path AND id <> OLD.id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

/*
 * Array reverse utility function
 */
CREATE OR REPLACE FUNCTION array_reverse(anyarray) RETURNS anyarray AS $$
SELECT ARRAY(
           SELECT $1[i]
    FROM generate_subscripts($1,1) AS s(i)
    ORDER BY i DESC
);
$$ LANGUAGE 'sql' STRICT IMMUTABLE;

/*
 * Update all existing asset paths to ensure they are accurate
 */
UPDATE ASSET SET PATH = text2ltree(array_to_string(array_reverse(GET_ASSET_TREE_PATH(id)), '.'));

/*
 * Reinsert function
 */
CREATE TRIGGER asset_parent_info_tgr
    BEFORE INSERT OR UPDATE ON ASSET
                         FOR EACH ROW EXECUTE PROCEDURE update_asset_parent_info();
