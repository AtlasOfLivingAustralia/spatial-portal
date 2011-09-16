\set ON_ERROR_STOP 1
begin;
update layers set keywords='vegetation' where name = 'landcover';
commit;
