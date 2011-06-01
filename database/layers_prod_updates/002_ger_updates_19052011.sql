begin;
update layers set classification1 = 'Area Management' where name like 'ger_%';
update layers set classification2 = 'Great Eastern Ranges' where name like 'ger_%';
commit;
