-- importOneStatementOnly
create or replace function GET_ASSET_TREE_PATH(ASSET_ID text)
  returns text [] as
$$
begin
  return (with recursive ASSET_TREE(ID, PARENT_ID, PATH) as (
    select A1.ID, A1.PARENT_ID, array [text(A1.ID)] from ASSET A1 where A1.ID = ASSET_ID
    union all
    select A2.ID, A2.PARENT_ID, array_append(AT.PATH, text(A2.ID))
      from ASSET A2, ASSET_TREE AT
      where A2.ID = AT.PARENT_ID and AT.PARENT_ID is not null
  ) select PATH from ASSET_TREE where PARENT_ID is null);
end;
$$
language plpgsql;
