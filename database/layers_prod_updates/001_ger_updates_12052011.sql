/* update to other tables for gazeteer area selection */

//ALTER TABLE ger_analysis_boundary_v01
//ADD COLUMN ala_id character varying(150) DEFAULT 'GERI Analysis Boundary v01';

//ALTER TABLE ger_geri_boundary_v102_australia
//ADD COLUMN ala_id character varying(150) DEFAULT 'GERI Boundary v102';

//ALTER TABLE ger_hunter
//ADD COLUMN ala_id character varying(150) DEFAULT 'GER Hunter';

//ALTER TABLE ger_hunter_analysis_mask
//ADD COLUMN ala_id character varying(150) DEFAULT 'GER Hunter Analysis Mask';

//ALTER TABLE ger_upper_hunter_focus_area_v2
//ADD COLUMN ala_id character varying(150) DEFAULT 'GER Upper Hunter Focus Area';

//ALTER TABLE ger_kosciuszko_to_coast
//ADD COLUMN ala_id character varying(150) DEFAULT 'GER Kosciuszko to Coast';

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

--SELECT id, "name", description, "type", source, path, extents, minlatitude, 
--       minlongitude, maxlatitude, maxlongitude, notes, enabled, displayname, 
--       displaypath, scale, environmentalvaluemin, environmentalvaluemax, 
--       environmentalvalueunits, lookuptablepath, metadatapath, classification1, 
--       classification2, uid, mddatest, citation_date, datalang, mdhrlv, 
--       respparty_role, licence_level, licence_link, licence_notes, source_link
--  FROM layers

--where name like 'ger_%'
--description like '%land%' or displayname like 'Mining%'

--order by classification2 asc, displayname asc, classification1 asc;

