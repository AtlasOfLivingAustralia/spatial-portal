begin;
update layers set scale = '0.1 deg' where type = 'Environmental';
commit;
