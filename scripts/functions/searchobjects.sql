-- Function: searchobjects(text, integer)

-- DROP FUNCTION searchobjects(text, integer);

CREATE OR REPLACE FUNCTION searchobjects(q text, lim integer)
  RETURNS SETOF searchobjectstype AS
$BODY$
DECLARE
    q2 text;
    strresult text;
    found integer;
    r RECORD;
    s RECORD;
BEGIN
    strresult := '';
    found := 0;
    IF position('%' in q) > 0 THEN 
        q2 := substring(q from 2 for length(q)-2);
    ELSE
	q2 := q;
    END IF;
    FOR r IN SELECT id FROM obj_names WHERE position(q2 in name) > 0 order by position(q2 in name), name LIMIT lim LOOP
        FOR s IN SELECT o.pid as pid, o.id as id, o.name as name, o.desc as desc, o.fid as fid, f.name as fieldname FROM objects o, fields f WHERE o.fid = f.id and o.name_id=r.id LOOP
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
ALTER FUNCTION searchobjects(text, integer)
  OWNER TO postgres;
