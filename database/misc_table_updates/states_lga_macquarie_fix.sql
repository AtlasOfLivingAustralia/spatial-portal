begin;
update aus1
set name_1 = 'Macquarie Island'
where id_1 = 159;

update aus2
set name_1 = 'Macquarie Island', name_2 = 'Macquarie Island'
where id_2 = 2093;
commit;
