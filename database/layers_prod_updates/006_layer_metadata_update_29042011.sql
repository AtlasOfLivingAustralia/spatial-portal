begin transaction;

update layers set
--name =                    'ramsar'
description =             'RAMSAR wetland regions'
,source =                  'ERIN'
,notes =                   E'Australia\'s Ramsar Wetlands - Downloadable (Subset Only).\n\nThe Ramsar Wetlands data contained within this dataset is from QLD, NSW, WA and Commonwealth agencies only. Ramsar Wetland boundaries for all other jurisdictions are to be sourced by contacting the relevant State/Territory agency.'
,displayname =             'RAMSAR wetland regions'
,environmentalvalueunits = 'class'
,metadatapath =            'http://www.environment.gov.au/metadataexplorer/full_metadata.jsp?docId={D06B412B-A1F5-42F8-BE39-1E4C17312FBD}'
,mddatest =                '2011-01-18'
,citation_date =           '2010-11-26'
,datalang =                'eng'
,mdhrlv =                  ''
,respparty_role =          'distributor'
,licence_level =           2
,licence_link =            ''
,licence_notes =           E'Copyright Commonwealth of Australia, Department of Sustainability, Environment, Water, Population and Communities with data compiled through cooperative efforts of the States/Territories Government wetland agencies. October 2010. All rights reserved\n\nParticipating Agencies:\n - ACT Department of Territory and Municipal Services\n - NSW Department of Environment, Climate Change and Water\n - NT Department of Natural Resources, Environment, The Arts and Sport\n - Qld Department of Environment and Resource Management\n - SA Department for Environment and Natural Resources\n - Tas Department of Primary Industries, Parks, Water and Environment\n - Vic Department of Sustainability and Environment\n - WA Department of Environment and Conservation'
,source_link =             'http://www.environment.gov.au/metadataexplorer/full_metadata.jsp?docId={D06B412B-A1F5-42F8-BE39-1E4C17312FBD}'

where id =                      915;


update layers set
--name =                    'nrm_regions_2010'
description =             'National Resource Management (NRM) Regions'
,source =                  'ERIN'
,notes =                   E'The Natural Resource Management (NRM) Regions dataset has been prepared for the purpose of reporting on the Australian Government\'s Caring for our Country investments. The dataset is designed to cover all Australian territory where Caring for our Country projects might take place and includes major islands; external territories; state and coastal waters; in addition to the 56 NRM regions. This version of the data is an update and formalisation of the \'interim 2010\' dataset (which was an interim update of the NRM Regions 2009 dataset- publicly released in Feb 09).\n\nWhilst the boundaries of NRM Regions are defined by legislation in some states and territories this dataset should not be used to represent legal boundaries in any way. It is an administrative dataset developed for the purpose of reporting and public information. It should be noted that from time to time the states and/or territories may revise their regional boundaries in accordance with local needs and therefore alterations to either the attribution or boundaries of the data may occur in the future.\n\nCaring for our Country commenced on 1 July 2008. It integrates delivery of the Australian Government\'s previous natural resource management programs, including the Natural Heritage Trust, the National Landcare Program, the Environmental Stewardship Program and the Working on Country Indigenous land and sea ranger program.'
,displayname =             'NRM Regions'
,displaypath =             'http://spatial.ala.org.au/geoserver/gwc/service/wms?service=WMS&version=1.1.0&request=GetMap&layers=ALA:nrm_regions_2010&format=image/png&styles='
,environmentalvalueunits = 'class'
,metadatapath =            'http://www.environment.gov.au/metadataexplorer/full_metadata.jsp?docId={FA68F769-550B-4605-A0D5-50B10ECD0EB9}'
,mddatest =                '2011-01-27'
,citation_date =           '2010-11-11'
,datalang =                'eng'
,mdhrlv =                  ''
,respparty_role =          'custodian'
,licence_level =           2
,licence_link =            ''
,licence_notes =           E'Access constraints: This data will be made available for download from the Department\'s website subject to Commonwealth of Australia Copyright and a licence agreement.\nLimitations of use: This is an administrative dataset developed for the purpose of reporting and public information. The dataset is not a legal boundary dataset and does not represent legal boundaries in any way.'
,source_link =             'http://www.environment.gov.au/metadataexplorer/full_metadata.jsp?docId={FA68F769-550B-4605-A0D5-50B10ECD0EB9}'

where id =                      916;

update layers set
--name =                    'australian_coral_ecoregions'
description =             'Australian Coral Ecoregions'
,source =                  ''
,path =                    '/data/ala/shapefiles/coral_ecoregions'
,notes =                   E'Australian Coral Ecoregions:\n - Arnhem Land\n - Central & North GBR\n - Keppel Islands & Capricorn Bunker Reefs, Souther GBR\n - Darwin\n - Rowley Shoals\n - Ningaloo Reef & NW Western Australia\n - SW West Australia\n - Recherche Archipelago\n - Houtman Abrolhos Islands\n - Direction Bank\n - Ashmore Reef\n - Scott Reef\n - Christmas Island\n - Cocos Keeling\n - Arafura Sea Gulf of Carpenteria\n - Shark Bay\n - Kimberley Coast\n - Elizabeth & Middleton Reefs\n - Lord Howe Island\n - Solitary Islands\n - Moreton Bay\n - Pompey & Swain Reefs, South-east GBR\n - Torres Strait & far Northern GBR\n - South-east Australia\n - Coral Sea'
--,enabled =                 true
,displayname =             'Australian Coral Ecoregions'
,displaypath =             'http://spatial.ala.org.au/geoserver/gwc/service/wms?service=WMS&version=1.1.0&request=GetMap&layers=ALA:australian_coral_ecoregions&format=image/png&styles='
,environmentalvalueunits = 'class'
,metadatapath =            ''
,mddatest =                '2011'
,citation_date =           '2011-01-20'
,datalang =                'eng'
,mdhrlv =                  ''
,respparty_role =          'distributor'
,licence_level =           2
,licence_link =            'http://creativecommons.org.au/learn-more/licences'
,licence_notes =           E'Attribution: J. Veron Coral Reef Research\nLicence: Creative Commons Attribution-Non-Commercial 3.0 Australia'
,source_link =             ''

where id =                      917;

--SELECT id, "name", description, "type", source, path, extents, minlatitude, 
--       minlongitude, maxlatitude, maxlongitude, notes, enabled, displayname, 
--       displaypath, scale, environmentalvaluemin, environmentalvaluemax, 
--       environmentalvalueunits, lookuptablepath, metadatapath, classification1, 
--       classification2, uid, mddatest, citation_date, datalang, mdhrlv, 
--       respparty_role, licence_level, licence_link, licence_notes, source_link
--  FROM layers

--where id in (917,916,915)
----name like 'ger_%'
----description like '%land%' or displayname like 'Mining%'

--order by classification2 asc, displayname asc, classification1 asc;

commit transaction;

