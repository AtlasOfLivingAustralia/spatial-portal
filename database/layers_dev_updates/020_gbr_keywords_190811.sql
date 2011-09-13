\set ON_ERROR_STOP 1
begin;
update layers set keywords='bathymetry, depth' where id=924;
commit;
