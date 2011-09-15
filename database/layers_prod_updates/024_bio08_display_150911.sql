\set ON_ERROR_STOP 1
begin;
update layers set displayname='Temperature - wettest quarter mean (Bio08)' where id = 870;
commit;
