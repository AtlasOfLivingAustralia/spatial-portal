\set ON_ERROR_STOP 1
begin;
update layers set enabled = TRUE where id in (704,705);
commit;
