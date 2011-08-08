--SET CLIENT_ENCODING TO 'UTF8';

/*
side-by-side comparison of the dev to [live] layers index

you need dblink installed in the pgsql server you try to run this query on
 - note: it doesn't need to be on the spatial db servers - dblink is 'client' logic

values in cells are as follows:
 - = : values are the same (value omitted for clarity ^*)
 - '' : values are blank (value omitted for clarity ^*)
 - null : value is null (in one or both tables)
 - dev_val [live_val] : values differ and are shown side by side

^* dev_id, [live_id] and name will always show a value for usability

if dev_id or [live_id] is null, then that layer id was not found in the corresponding layer index

bk - 20110728: converted to linked db comparison
*/

select 

-- shown regardless of update or not
(lu.id) as dev_id
, (l.id) as live_id
, case when ((l.name is null and lu.name is null) or (l.name::text = lu.name::text)) then lu.name else coalesce(lu.name::text, 'null') || ' [' || coalesce(l.name::text, 'null') || ']' end as "name"
, case when ((l.source is null and lu.source is null) or (l.source = lu.source)) then lu.source else coalesce(lu.source::text, 'null') || ' [' || coalesce(l.source, 'null') || ']' end as source
--, case when ((l.classification1 is null and lu.classification1 is null) or (l.classification1 = lu.classification1)) then lu.classification1 else coalesce(lu.classification1, 'null') || ' [' || coalesce(l.classification1, 'null') || ']' end as clasf1
--, case when ((l.classification2 is null and lu.classification2 is null) or (l.classification2 = lu.classification2)) then lu.classification2 else coalesce(lu.classification2, 'null') || ' [' || coalesce(l.classification2, 'null') || ']' end as clasf2
, case when (l.classification1 = '' and lu.classification1 = '') then '\'\'' when (l.classification1 is null and lu.classification1 is null) then 'null' when (l.classification1::text = lu.classification1::text) then l.classification1 else coalesce(lu.classification1::text, 'null') || ' [' || coalesce(l.classification1::text, 'null') || ']' end as classification1
, case when (l.classification2 = '' and lu.classification2 = '') then '\'\'' when (l.classification2 is null and lu.classification2 is null) then 'null' when (l.classification2::text = lu.classification2::text) then l.classification2 else coalesce(lu.classification2::text, 'null') || ' [' || coalesce(l.classification2::text, 'null') || ']' end as classification2

-- possible updates or blank if no update
, case when (l.description = '' and lu.description = '') then '\'\'' when (l.description is null and lu.description is null) then 'null' when (l.description::text = lu.description::text) then '=' else coalesce(lu.description::text, 'null') || ' [' || coalesce(l.description::text, 'null') || ']' end as description
, case when (l."type" = '' and lu."type" = '') then '\'\'' when (l."type" is null and lu."type" is null) then 'null' when (l."type"::text = lu."type"::text) then '=' else coalesce(lu."type"::text, 'null') || ' [' || coalesce(l."type"::text, 'null') || ']' end as "type"
, case when (l.path = '' and lu.path = '') then '\'\'' when (l.path is null and lu.path is null) then 'null' when (l.path::text = lu.path::text) then '=' else coalesce(lu.path::text, 'null') || ' [' || coalesce(l.path::text, 'null') || ']' end as path
, case when (l.extents is null and lu.extents is null) then 'null' when (l.extents::text = lu.extents::text) then '=' else coalesce(lu.extents::text, 'null') || ' [' || coalesce(l.extents::text, 'null') || ']' end as extents
, case when (l.minlatitude is null and lu.minlatitude is null) then 'null' when (l.minlatitude::text = lu.minlatitude::text) then '=' else coalesce(lu.minlatitude::text, 'null') || ' [' || coalesce(l.minlatitude::text, 'null') || ']' end as minlatitude
, case when (l.minlongitude is null and lu.minlongitude is null) then 'null' when (l.minlongitude::text = lu.minlongitude::text) then '=' else coalesce(lu.minlongitude::text, 'null') || ' [' || coalesce(l.minlongitude::text, 'null') || ']' end as minlongitude
, case when (l.maxlatitude is null and lu.maxlatitude is null) then 'null' when (l.maxlatitude::text = lu.maxlatitude::text) then '=' else coalesce(lu.maxlatitude::text, 'null') || ' [' || coalesce(l.maxlatitude::text, 'null') || ']' end as maxlatitude
, case when (l.maxlongitude is null and lu.maxlongitude is null) then 'null' when (l.maxlongitude::text = lu.maxlongitude::text) then '=' else coalesce(lu.maxlongitude::text, 'null') || ' [' || coalesce(l.maxlongitude::text, 'null') || ']' end as maxlongitude

, case when (l.enabled is null and lu.enabled is null) then 'null' when (l.enabled::text = lu.enabled::text) then '=' else coalesce(lu.enabled::text, 'null') || ' [' || coalesce(l.enabled::text, 'null') || ']' end as enabled
, case when (l.displayname = '' and lu.displayname = '') then '\'\'' when (l.displayname is null and lu.displayname is null) then 'null' when (l.displayname::text = lu.displayname::text) then '=' else coalesce(lu.displayname::text, 'null') || ' [' || coalesce(l.displayname::text, 'null') || ']' end as displayname
, case when (l.displaypath = '' and lu.displaypath = '') then '\'\'' when (l.displaypath is null and lu.displaypath is null) then 'null' when (l.displaypath::text = lu.displaypath::text) then '=' else coalesce(lu.displaypath::text, 'null') || ' [' || coalesce(l.displaypath::text, 'null') || ']' end as displaypath
, case when (l.scale = '' and lu.scale = '') then '\'\'' when (l.scale is null and lu.scale is null) then 'null' when (l.scale::text = lu.scale::text) then '=' else coalesce(lu.scale::text, 'null') || ' [' || coalesce(l.scale::text, 'null') || ']' end as scale
, case when (l.environmentalvaluemin = '' and lu.environmentalvaluemin = '') then '\'\'' when (l.environmentalvaluemin is null and lu.environmentalvaluemin is null) then 'null' when (l.environmentalvaluemin::text = lu.environmentalvaluemin::text) then '=' else coalesce(lu.environmentalvaluemin::text, 'null') || ' [' || coalesce(l.environmentalvaluemin::text, 'null') || ']' end as environmentalvaluemin
, case when (l.environmentalvaluemax = '' and lu.environmentalvaluemax = '') then '\'\'' when (l.environmentalvaluemax is null and lu.environmentalvaluemax is null) then 'null' when (l.environmentalvaluemax::text = lu.environmentalvaluemax::text) then '=' else coalesce(lu.environmentalvaluemax::text, 'null') || ' [' || coalesce(l.environmentalvaluemax::text, 'null') || ']' end as environmentalvaluemax
, case when (l.environmentalvalueunits = '' and lu.environmentalvalueunits = '') then '\'\'' when (l.environmentalvalueunits is null and lu.environmentalvalueunits is null) then 'null' when (l.environmentalvalueunits::text = lu.environmentalvalueunits::text) then '=' else coalesce(lu.environmentalvalueunits::text, 'null') || ' [' || coalesce(l.environmentalvalueunits::text, 'null') || ']' end as environmentalvalueunits
, case when (l.lookuptablepath = '' and lu.lookuptablepath = '') then '\'\'' when (l.lookuptablepath is null and lu.lookuptablepath is null) then 'null' when (l.lookuptablepath::text = lu.lookuptablepath::text) then '=' else coalesce(lu.lookuptablepath::text, 'null') || ' [' || coalesce(l.lookuptablepath::text, 'null') || ']' end as lookuptablepath
, case when (l.metadatapath = '' and lu.metadatapath = '') then '\'\'' when (l.metadatapath is null and lu.metadatapath is null) then 'null' when (l.metadatapath::text = lu.metadatapath::text) then '=' else coalesce(lu.metadatapath::text, 'null') || ' [' || coalesce(l.metadatapath::text, 'null') || ']' end as metadatapath
, case when (l.mdDateSt = '' and lu.mdDateSt = '') then '\'\'' when (l.mdDateSt is null and lu.mdDateSt is null) then 'null' when (l.mdDateSt::text = lu.mdDateSt::text) then '=' else coalesce(lu.mdDateSt::text, 'null') || ' [' || coalesce(l.mdDateSt::text, 'null') || ']' end as mdDateSt
, case when (l.Citation_date = '' and lu.Citation_date = '') then '\'\'' when (l.Citation_date is null and lu.Citation_date is null) then 'null' when (l.Citation_date::text = lu.Citation_date::text) then '=' else coalesce(lu.Citation_date::text, 'null') || ' [' || coalesce(l.Citation_date::text, 'null') || ']' end as Citation_date
, case when (l.dataLang = '' and lu.dataLang = '') then '\'\'' when (l.dataLang is null and lu.dataLang is null) then 'null' when (l.dataLang::text = lu.dataLang::text) then '=' else coalesce(lu.dataLang::text, 'null') || ' [' || coalesce(l.dataLang::text, 'null') || ']' end as dataLang
, case when (l.mdHrLv = '' and lu.mdHrLv = '') then '\'\'' when (l.mdHrLv is null and lu.mdHrLv is null) then 'null' when (l.mdHrLv::text = lu.mdHrLv::text) then '=' else coalesce(lu.mdHrLv::text, 'null') || ' [' || coalesce(l.mdHrLv::text, 'null') || ']' end as mdHrLv
, case when (l.RespParty_role = '' and lu.RespParty_role = '') then '\'\'' when (l.RespParty_role is null and lu.RespParty_role is null) then 'null' when (l.RespParty_role::text = lu.RespParty_role::text) then '=' else coalesce(lu.RespParty_role::text, 'null') || ' [' || coalesce(l.RespParty_role::text, 'null') || ']' end as RespParty_role
, case when (l.licence_level is null and lu.licence_level is null) then 'null' when (l.licence_level::text = lu.licence_level::text) then '=' else coalesce(lu.licence_level::text, 'null') || ' [' || coalesce(l.licence_level::text, 'null') || ']' end as licence_level
, case when (l.licence_link = '' and lu.licence_link = '') then '\'\'' when (l.licence_link is null and lu.licence_link is null) then 'null' when (l.licence_link::text = lu.licence_link::text) then '=' else coalesce(lu.licence_link::text, 'null') || ' [' || coalesce(l.licence_link::text, 'null') || ']' end as licence_link

, case when (l.licence_notes = '' and lu.licence_notes = '') then '\'\'' when (l.licence_notes is null and lu.licence_notes is null) then 'null' when (l.licence_notes::text = lu.licence_notes::text) then '=' else coalesce( replace(lu.licence_notes::text,'\n','\\n'), 'null' )  || ' [' || coalesce( replace(l.licence_notes::text,'\n','\\n'), 'null' ) || ']' end as licence_notes
, case when (l.notes = '' and lu.notes = '') then '\'\'' when (l.notes is null and lu.notes is null) then 'null' when (l.notes::text = lu.notes::text) then '=' else coalesce( replace(lu.notes::text,'\n','\\n'), 'null' ) || ' [' || coalesce( replace(l.notes::text,'\n','\\n'), 'null' ) || ']' end as notes


-- new fields
, case when (l.source_link = '' and lu.source_link = '') then '\'\'' when (l.source_link is null and lu.source_link is null) then 'null' when (l.source_link::text = lu.source_link::text) then '=' else coalesce(lu.source_link::text, 'null') || ' [' || coalesce(l.source_link::text, 'null') || ']' end as source_link
--, case when (lu.source_link is null) then '' else coalesce(lu.source_link::text, 'null') || ' [null]' end as source_link

/******** SPATIALDB DEV ************/
/* linked tables in separate db's - lu 'layers updated' as the layers in dev [ie, potentially newer] */
from dblink('dbname=spatialdb port=25431 user=postgres password=postgres',
            'SELECT * FROM public.layers')
AS lu(
  id bigint,
  "name" character varying(150),
  description text,
  "type" character varying(20),
  source character varying(150),
  path character varying(500),
  extents character varying(100),
  minlatitude numeric(18,5),
  minlongitude numeric(18,5),
  maxlatitude numeric(18,5),
  maxlongitude numeric(18,5),
  notes text,
  enabled boolean,
  displayname character varying(150),
  displaypath character varying(500),
  scale character varying(20),
  environmentalvaluemin character varying(30),
  environmentalvaluemax character varying(30),
  environmentalvalueunits character varying(150),
  lookuptablepath character varying(300),
  metadatapath character varying(300),
  classification1 character varying(150),
  classification2 character varying(150),
  uid character varying(50),
  mddatest character varying(30),
  citation_date character varying(30),
  datalang character varying(5),
  mdhrlv character varying(5),
  respparty_role character varying(30),
  licence_level smallint,
  licence_link character varying(300),
  licence_notes character varying(1024),
  source_link character varying(300)
)

/******** SPATIALDB LIVE ************/
/* linked tables in separate db's - l 'layers' as the layers in live [ie, current] */
full outer join dblink('dbname=spatialdb port=25432 user=postgres password=postgres',
            'SELECT * FROM public.layers')
AS l(
  id bigint,
  "name" character varying(150),
  description text,
  "type" character varying(20),
  source character varying(150),
  path character varying(500),
  extents character varying(100),
  minlatitude numeric(18,5),
  minlongitude numeric(18,5),
  maxlatitude numeric(18,5),
  maxlongitude numeric(18,5),
  notes text,
  enabled boolean,
  displayname character varying(150),
  displaypath character varying(500),
  scale character varying(20),
  environmentalvaluemin character varying(30),
  environmentalvaluemax character varying(30),
  environmentalvalueunits character varying(150),
  lookuptablepath character varying(300),
  metadatapath character varying(300),
  classification1 character varying(150),
  classification2 character varying(150),
  uid character varying(50),
  mddatest character varying(30),
  citation_date character varying(30),
  datalang character varying(5),
  mdhrlv character varying(5),
  respparty_role character varying(30),
  licence_level smallint,
  licence_link character varying(300),
  licence_notes character varying(1024),
  source_link character varying(300)
)

on 
(l.id::int = lu.id::int) --or 
--(l.name = lu.name)

order by 
lu.id desc
--classification1, classification2, displayname
;


/*
select 
--substr(lu."name", (length(lu."name")-1), 2)
l.id
,coalesce(lu.licence_level,-1) || ' [' || coalesce((l.licence_level)::text,'null') || ']' as licence_level
,lu.licence_notes || ' [' || l.licence_notes || ']' as licence_notes
,lu.licence_link || ' [' || l.licence_link || ']' as licence_link
, lu.*
*/
--, lu.*

/*
,lu.licence_level || ' [' || l.licence_level || ']' as licence_level
,lu.licence_notes || ' [' || l.licence_notes || ']' as licence_notes
,lu.licence_link || ' [' || l.licence_link || ']' as licence_link
*/
/*
from 		layers_20101019_update lu
right join	layers l on l.id::int = lu.id::int

where lower(lu."name") like 'bio%'

order by (regexp_matches(lu."name", '[0-9]+'))[1]::int
*/

/*
SELECT p.*
FROM dblink('dbname=spatialdb_dev port=25431 user=postgres password=postgres',
            'SELECT * FROM public.layers limit 10')
AS 
p(
  id bigint,
  "name" character varying(150),
  description text,
  "type" character varying(20),
  source character varying(150),
  path character varying(500),
  extents character varying(100),
  minlatitude numeric(18,5),
  minlongitude numeric(18,5),
  maxlatitude numeric(18,5),
  maxlongitude numeric(18,5),
  notes text,
  enabled boolean,
  displayname character varying(150),
  displaypath character varying(500),
  scale character varying(20),
  environmentalvaluemin character varying(30),
  environmentalvaluemax character varying(30),
  environmentalvalueunits character varying(150),
  lookuptablepath character varying(300),
  metadatapath character varying(300),
  classification1 character varying(150),
  classification2 character varying(150),
  uid character varying(50),
  mddatest character varying(30),
  citation_date character varying(30),
  datalang character varying(5),
  mdhrlv character varying(5),
  respparty_role character varying(30),
  licence_level smallint,
  licence_link character varying(300),
  licence_notes character varying(1024),
  source_link character varying(300)
);
*/

/*
SELECT p.*
FROM dblink('dbname=spatialdb port=25432 user=postgres password=postgres',
            'SELECT * FROM public.layers limit 10')
AS 
p(
  id bigint,
  "name" character varying(150),
  description text,
  "type" character varying(20),
  source character varying(150),
  path character varying(500),
  extents character varying(100),
  minlatitude numeric(18,5),
  minlongitude numeric(18,5),
  maxlatitude numeric(18,5),
  maxlongitude numeric(18,5),
  notes text,
  enabled boolean,
  displayname character varying(150),
  displaypath character varying(500),
  scale character varying(20),
  environmentalvaluemin character varying(30),
  environmentalvaluemax character varying(30),
  environmentalvalueunits character varying(150),
  lookuptablepath character varying(300),
  metadatapath character varying(300),
  classification1 character varying(150),
  classification2 character varying(150),
  uid character varying(50),
  mddatest character varying(30),
  citation_date character varying(30),
  datalang character varying(5),
  mdhrlv character varying(5),
  respparty_role character varying(30),
  licence_level smallint,
  licence_link character varying(300),
  licence_notes character varying(1024),
  source_link character varying(300)
);
*/