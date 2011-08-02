-- /* bk - 20110801
-- updates the following:
-- ne_world - natural earth country boundaries - add source & source-link
-- dld_DLCMv1_Class - "The National Dynamic Land Cover Dataset [null]" - full metadata record
-- lith_geologicalunitpolygons1m - licence_level to 3 (cc no restrictions)
-- australian_coral_ecoregions - add source & metadata links, licence_level to 3 (cc no restrictions)
-- gbr_gbr100 - great barrier reef 100m dem - add xxx
-- */

--select * into layers_20110801_copy from layers

begin transaction;

--

update layers set
source =           'ABARES'

where source in ('BRS', 'ABARE') and (path like '%MCAS%');

--

update layers set
--name =                    'ne_world'
citation_date =           '2009-12-03'
,respparty_role =          'custodian'
,licence_level =           3

where id =                      922;

--

update layers set
--name =                    'dld_DLCMv1_Class'
--http://www.ga.gov.au/ausgeonews/ausgeonews200812/landcover.jsp
mddatest =                '2011'
,citation_date =           '2011'
,respparty_role =          'custodian'
,licence_level =           0
,licence_link =            ''
,licence_notes =           E'No permission to distribute derivative works at this stage - draft dataset'
,source_link =             'http://www.ga.gov.au/'

where id =                      918;

--

update layers set
--name =                    'lith_geologicalunitpolygons1m'
metadatapath =            'http://www.ga.gov.au/meta/ANZCW0703013575.xml'
,mddatest =                '2010-06-23'
,citation_date =           '2010'
,respparty_role =          'custodian'
,licence_level =           3
,source_link =             'http://www.ga.gov.au/meta/ANZCW0703013575.html'

where id =                      923;

--

update layers set
--name =                    'australian_coral_ecoregions'
description =             'Australian Coral Ecoregions'
,source =                  'Coral Reef Research - J.E.N. Veron'
,notes =                   E'Australian Coral Ecoregions:\n - Arnhem Land\n - Central & North GBR\n - Keppel Islands & Capricorn Bunker Reefs, Souther GBR\n - Darwin\n - Rowley Shoals\n - Ningaloo Reef & NW Western Australia\n - SW West Australia\n - Recherche Archipelago\n - Houtman Abrolhos Islands\n - Direction Bank\n - Ashmore Reef\n - Scott Reef\n - Christmas Island\n - Cocos Keeling\n - Arafura Sea Gulf of Carpenteria\n - Shark Bay\n - Kimberley Coast\n - Elizabeth & Middleton Reefs\n - Lord Howe Island\n - Solitary Islands\n - Moreton Bay\n - Pompey & Swain Reefs, South-east GBR\n - Torres Strait & far Northern GBR\n - South-east Australia\n - Coral Sea'
--,enabled =                 true
,displayname =             'Australian Coral Ecoregions'
,metadatapath =            ''
,mddatest =                '2011-08-01'
,citation_date =           '2011-01-20'
,respparty_role =          'author'
,licence_level =           2
,licence_link =            'http://creativecommons.org.au/learn-more/licences'
,licence_notes =           E'Attribution: J. Veron Coral Reef Research\nLicence: Creative Commons Attribution-Non-Commercial 3.0 Australia'
,source_link =             'http://coralreefresearch.com'

where id =                      917;

--

update layers set
--name =                    'gbr_gbr100'
source =                  'Deepreef Explorer'
,notes =                   E'This dataset provides the new high-resolution bathymetry model of the Great Barrier Reef and Coral Sea, called gbr100. The 100 metre-resolution gridded bathymetry dataset covers an area of about 3 million square km, from the Gulf of Papua to northern New South Wales, and easterly into the deep Coral Sea.'
,metadatapath =            'http://www.deepreef.org/publications/reports/67-3dgbr-final-report.html'
,mddatest =                '2011-08-01'
,citation_date =           '2010-08'
,datalang =                'eng'
,mdhrlv =                  ''
,respparty_role =          'author'
,licence_level =           3
,licence_link =            'http://creativecommons.org/licenses/by/3.0/au/deed.en'
,licence_notes =           E'Attribution: © www.deepreef.org\nLicence: CC-BY-3.0 Creative Commons Attribution 3.0 Australia\n\nIf using the data for publications, please quote the following reference:\n\nBeaman, R.J., 2010. Project 3DGBR: A high-resolution depth model for the Great Barrier Reef and Coral Sea. Marine and Tropical Sciences Research Facility (MTSRF) Project 2.5i.1a Final Report, MTSRF, Cairns, Australia, pp. 13 plus Appendix 1.'
,source_link =             'http://www.deepreef.org/bathymetry/65-3dgbr-bathy.html'

where id =                      924;

--

/*
select 

-- shown regardless of update or not
(lu.id) as dev_id
, (l.id) as live_id
, case when ((l.name is null and lu.name is null) or (l.name::text = lu.name::text)) then lu.name else coalesce(lu.name::text, 'null') || ' [' || coalesce(l.name::text, 'null') || ']' end as "name"
, case when ((l.source is null and lu.source is null) or (l.source = lu.source)) then lu.source else coalesce(lu.source::text, 'null') || ' [' || coalesce(l.source, 'null') || ']' end as source
--, case when ((l.classification1 is null and lu.classification1 is null) or (l.classification1 = lu.classification1)) then lu.classification1 else coalesce(lu.classification1, 'null') || ' [' || coalesce(l.classification1, 'null') || ']' end as clasf1
--, case when ((l.classification2 is null and lu.classification2 is null) or (l.classification2 = lu.classification2)) then lu.classification2 else coalesce(lu.classification2, 'null') || ' [' || coalesce(l.classification2, 'null') || ']' end as clasf2
, case when (l.classification1 = '' and lu.classification1 = '') then E'''''' when (l.classification1 is null and lu.classification1 is null) then 'null' when (l.classification1::text = lu.classification1::text) then l.classification1 else coalesce(lu.classification1::text, 'null') || ' [' || coalesce(l.classification1::text, 'null') || ']' end as classification1
, case when (l.classification2 = '' and lu.classification2 = '') then E'''''' when (l.classification2 is null and lu.classification2 is null) then 'null' when (l.classification2::text = lu.classification2::text) then l.classification2 else coalesce(lu.classification2::text, 'null') || ' [' || coalesce(l.classification2::text, 'null') || ']' end as classification2

-- possible updates or blank if no update
, case when (l.description = '' and lu.description = '') then E'''''' when (l.description is null and lu.description is null) then 'null' when (l.description::text = lu.description::text) then '=' else coalesce(lu.description::text, 'null') || ' [' || coalesce(l.description::text, 'null') || ']' end as description
, case when (l."type" = '' and lu."type" = '') then E'''''' when (l."type" is null and lu."type" is null) then 'null' when (l."type"::text = lu."type"::text) then '=' else coalesce(lu."type"::text, 'null') || ' [' || coalesce(l."type"::text, 'null') || ']' end as "type"
, case when (l.path = '' and lu.path = '') then E'''''' when (l.path is null and lu.path is null) then 'null' when (l.path::text = lu.path::text) then '=' else coalesce(lu.path::text, 'null') || ' [' || coalesce(l.path::text, 'null') || ']' end as path
, case when (l.extents is null and lu.extents is null) then 'null' when (l.extents::text = lu.extents::text) then '=' else coalesce(lu.extents::text, 'null') || ' [' || coalesce(l.extents::text, 'null') || ']' end as extents
, case when (l.minlatitude is null and lu.minlatitude is null) then 'null' when (l.minlatitude::text = lu.minlatitude::text) then '=' else coalesce(lu.minlatitude::text, 'null') || ' [' || coalesce(l.minlatitude::text, 'null') || ']' end as minlatitude
, case when (l.minlongitude is null and lu.minlongitude is null) then 'null' when (l.minlongitude::text = lu.minlongitude::text) then '=' else coalesce(lu.minlongitude::text, 'null') || ' [' || coalesce(l.minlongitude::text, 'null') || ']' end as minlongitude
, case when (l.maxlatitude is null and lu.maxlatitude is null) then 'null' when (l.maxlatitude::text = lu.maxlatitude::text) then '=' else coalesce(lu.maxlatitude::text, 'null') || ' [' || coalesce(l.maxlatitude::text, 'null') || ']' end as maxlatitude
, case when (l.maxlongitude is null and lu.maxlongitude is null) then 'null' when (l.maxlongitude::text = lu.maxlongitude::text) then '=' else coalesce(lu.maxlongitude::text, 'null') || ' [' || coalesce(l.maxlongitude::text, 'null') || ']' end as maxlongitude

, case when (l.enabled is null and lu.enabled is null) then 'null' when (l.enabled::text = lu.enabled::text) then '=' else coalesce(lu.enabled::text, 'null') || ' [' || coalesce(l.enabled::text, 'null') || ']' end as enabled
, case when (l.displayname = '' and lu.displayname = '') then E'''''' when (l.displayname is null and lu.displayname is null) then 'null' when (l.displayname::text = lu.displayname::text) then '=' else coalesce(lu.displayname::text, 'null') || ' [' || coalesce(l.displayname::text, 'null') || ']' end as displayname
, case when (l.displaypath = '' and lu.displaypath = '') then E'''''' when (l.displaypath is null and lu.displaypath is null) then 'null' when (l.displaypath::text = lu.displaypath::text) then '=' else coalesce(lu.displaypath::text, 'null') || ' [' || coalesce(l.displaypath::text, 'null') || ']' end as displaypath
, case when (l.scale = '' and lu.scale = '') then E'''''' when (l.scale is null and lu.scale is null) then 'null' when (l.scale::text = lu.scale::text) then '=' else coalesce(lu.scale::text, 'null') || ' [' || coalesce(l.scale::text, 'null') || ']' end as scale
, case when (l.environmentalvaluemin = '' and lu.environmentalvaluemin = '') then E'''''' when (l.environmentalvaluemin is null and lu.environmentalvaluemin is null) then 'null' when (l.environmentalvaluemin::text = lu.environmentalvaluemin::text) then '=' else coalesce(lu.environmentalvaluemin::text, 'null') || ' [' || coalesce(l.environmentalvaluemin::text, 'null') || ']' end as environmentalvaluemin
, case when (l.environmentalvaluemax = '' and lu.environmentalvaluemax = '') then E'''''' when (l.environmentalvaluemax is null and lu.environmentalvaluemax is null) then 'null' when (l.environmentalvaluemax::text = lu.environmentalvaluemax::text) then '=' else coalesce(lu.environmentalvaluemax::text, 'null') || ' [' || coalesce(l.environmentalvaluemax::text, 'null') || ']' end as environmentalvaluemax
, case when (l.environmentalvalueunits = '' and lu.environmentalvalueunits = '') then E'''''' when (l.environmentalvalueunits is null and lu.environmentalvalueunits is null) then 'null' when (l.environmentalvalueunits::text = lu.environmentalvalueunits::text) then '=' else coalesce(lu.environmentalvalueunits::text, 'null') || ' [' || coalesce(l.environmentalvalueunits::text, 'null') || ']' end as environmentalvalueunits
, case when (l.lookuptablepath = '' and lu.lookuptablepath = '') then E'''''' when (l.lookuptablepath is null and lu.lookuptablepath is null) then 'null' when (l.lookuptablepath::text = lu.lookuptablepath::text) then '=' else coalesce(lu.lookuptablepath::text, 'null') || ' [' || coalesce(l.lookuptablepath::text, 'null') || ']' end as lookuptablepath
, case when (l.metadatapath = '' and lu.metadatapath = '') then E'''''' when (l.metadatapath is null and lu.metadatapath is null) then 'null' when (l.metadatapath::text = lu.metadatapath::text) then '=' else coalesce(lu.metadatapath::text, 'null') || ' [' || coalesce(l.metadatapath::text, 'null') || ']' end as metadatapath
, case when (l.mdDateSt = '' and lu.mdDateSt = '') then E'''''' when (l.mdDateSt is null and lu.mdDateSt is null) then 'null' when (l.mdDateSt::text = lu.mdDateSt::text) then '=' else coalesce(lu.mdDateSt::text, 'null') || ' [' || coalesce(l.mdDateSt::text, 'null') || ']' end as mdDateSt
, case when (l.Citation_date = '' and lu.Citation_date = '') then E'''''' when (l.Citation_date is null and lu.Citation_date is null) then 'null' when (l.Citation_date::text = lu.Citation_date::text) then '=' else coalesce(lu.Citation_date::text, 'null') || ' [' || coalesce(l.Citation_date::text, 'null') || ']' end as Citation_date
, case when (l.dataLang = '' and lu.dataLang = '') then E'''''' when (l.dataLang is null and lu.dataLang is null) then 'null' when (l.dataLang::text = lu.dataLang::text) then '=' else coalesce(lu.dataLang::text, 'null') || ' [' || coalesce(l.dataLang::text, 'null') || ']' end as dataLang
, case when (l.mdHrLv = '' and lu.mdHrLv = '') then E'''''' when (l.mdHrLv is null and lu.mdHrLv is null) then 'null' when (l.mdHrLv::text = lu.mdHrLv::text) then '=' else coalesce(lu.mdHrLv::text, 'null') || ' [' || coalesce(l.mdHrLv::text, 'null') || ']' end as mdHrLv
, case when (l.RespParty_role = '' and lu.RespParty_role = '') then E'''''' when (l.RespParty_role is null and lu.RespParty_role is null) then 'null' when (l.RespParty_role::text = lu.RespParty_role::text) then '=' else coalesce(lu.RespParty_role::text, 'null') || ' [' || coalesce(l.RespParty_role::text, 'null') || ']' end as RespParty_role
, case when (l.licence_level is null and lu.licence_level is null) then 'null' when (l.licence_level::text = lu.licence_level::text) then '=' else coalesce(lu.licence_level::text, 'null') || ' [' || coalesce(l.licence_level::text, 'null') || ']' end as licence_level
, case when (l.licence_link = '' and lu.licence_link = '') then E'''''' when (l.licence_link is null and lu.licence_link is null) then 'null' when (l.licence_link::text = lu.licence_link::text) then '=' else coalesce(lu.licence_link::text, 'null') || ' [' || coalesce(l.licence_link::text, 'null') || ']' end as licence_link

, case when (l.licence_notes = '' and lu.licence_notes = '') then E'''''' when (l.licence_notes is null and lu.licence_notes is null) then 'null' when (l.licence_notes::text = lu.licence_notes::text) then '=' else coalesce( replace(lu.licence_notes::text,E'\n',E'\\n'), 'null' )  || ' [' || coalesce( replace(l.licence_notes::text,E'\n',E'\\n'), 'null' ) || ']' end as licence_notes
, case when (l.notes = '' and lu.notes = '') then E'''''' when (l.notes is null and lu.notes is null) then 'null' when (l.notes::text = lu.notes::text) then '=' else coalesce( replace(lu.notes::text,E'\n',E'\\n'), 'null' ) || ' [' || coalesce( replace(l.notes::text,E'\n',E'\\n'), 'null' ) || ']' end as notes


-- new fields
, case when (l.source_link = '' and lu.source_link = '') then E'''''' when (l.source_link is null and lu.source_link is null) then 'null' when (l.source_link::text = lu.source_link::text) then '=' else coalesce(lu.source_link::text, 'null') || ' [' || coalesce(l.source_link::text, 'null') || ']' end as source_link
--, case when (lu.source_link is null) then '' else coalesce(lu.source_link::text, 'null') || ' [null]' end as source_link

from layers as lu
full outer join layers_20110801_copy as l
on (l.id::int = lu.id::int)

order by l.id desc;
*/

--rollback transaction;
commit transaction;

