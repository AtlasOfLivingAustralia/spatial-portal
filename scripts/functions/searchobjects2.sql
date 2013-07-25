-- Function: searchobjects2(text, integer)

-- DROP FUNCTION searchobjects2(text, integer);

CREATE OR REPLACE FUNCTION searchobjects2(q text, lim integer)
  RETURNS SETOF searchobjectstype AS
$BODY$
DECLARE
    strresult text
    found integer;
    r RECORD;
    s RECORD;
BEGIN
    strresult := '';
    found := 0;
    FOR r IN SELECT id FROM obj_names WHERE name LIKE q LOOP
        FOR s IN SELECT pid, id, name, o.desc ,fid, fname FROM objects o WHERE name_id=r.id LOOP
         RETURN NEXT s;
         found := found + 1;
         IF found >= lim THEN
             RETURN;
         END IF;
     END LOOP;
    END LOOP;

    RETURN;
END;
$BODY$
  LANGUAGE plpgsql IMMUTABLE SECURITY DEFINER
  COST 10
  ROWS 1000;
ALTER FUNCTION searchobjects2(text, integer)
  OWNER TO postgres;
