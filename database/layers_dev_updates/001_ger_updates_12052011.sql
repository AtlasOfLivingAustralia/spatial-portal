begin;

/* update to layers metadata table */

update layers
set enabled = true
where name like 'ger_%';

update layers
set classification1 = 'Great Eastern Ranges Initiative'
where name like 'ger_%';

-- GERI LAYERS --

update layers
set 
classification2 = ''
, displayname = 'GERI Boundary'
where id in (904);

update layers
set 
classification2 = 'GERI Boundary Analysis'
where id in (902);

-- BORDER RANGES LAYERS --

update layers
set 
classification2 = 'Partnerships: Border Ranges'
, displayname = 'Border Ranges'
where id in (903);

-- HUNTER LAYERS --

update layers
set 
classification2 = 'Partnerships: Hunter'
, displayname = 'Hunter'
where id in (905);

update layers
set 
classification2 = 'Partnerships: Hunter'
where id in (906, 907, 913);


-- K2C LAYERS --

update layers
set 
classification2 = 'Partnerships: Kosciuszko to Coast'
, displayname = 'K2C'
where id in (909);

update layers
set 
classification2 = 'Partnerships: Kosciuszko to Coast'
where id in (908);


-- S2S LAYERS --

update layers
set 
classification2 = 'Partnerships: Slopes to Summit'
, displayname = 'S2S'
where id in (912);

update layers
set 
classification2 = 'Partnerships: Slopes to Summit'
where id in (910, 911);

commit;
