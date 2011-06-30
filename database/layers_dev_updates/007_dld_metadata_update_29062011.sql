--rollback transaction;

begin transaction;

update layers set
description =             'The National Dynamic Land Cover Dataset'
,source =                  'GA'
,notes =                   E''
,displayname =             'National Dynamic Land Cover'
,environmentalvalueunits = 'class'
,metadatapath =            'http://www.ga.gov.au'
,mddatest =                '2011-06-29'
,citation_date =           '2010-06-29'
,classification1 =         'Vegetation'
,datalang =                'eng'
,mdhrlv =                  ''
,respparty_role =          'distributor'
,licence_level =           2
,licence_link =            ''
,licence_notes =           E''
,source_link =             ''
,scale =                   '250m'
,path =                    '/data/ala/data/envlayers/dynamic_land_data_set'
where name = 'dld_DLCMv1_Class';

commit transaction;
