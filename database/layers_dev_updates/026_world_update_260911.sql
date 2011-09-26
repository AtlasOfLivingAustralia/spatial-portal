\set ON_ERROR_STOP 1
begin transaction;

update layers set
--name =                    'world'
description =             'Global Administrative Areas - World Boundary'
,source =                  'GADM'
,notes =                   E''
,displayname =             'World Country Boundaries'
,environmentalvalueunits = ''
,metadatapath =            ''
,mddatest =                '2011-09-26'
,citation_date =           '2011-09-26'
,datalang =                'eng'
,mdhrlv =                  ''
,respparty_role =          'distributor'
,licence_level =           2
,licence_link =            ''
,licence_notes =           E'This dataset is freely available for academic and other non-commercial use. Redistribution, or commercial use, is not allowed without prior permission.'
,source_link =             'http://www.gadm.org'

where id =                      932;

commit;
