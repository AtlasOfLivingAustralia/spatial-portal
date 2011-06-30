begin;

update layers set displayname = description where name in ('ger_slopes_to_summit','ger_kosciuszko_to_cost','ger_k2c_management_regions_oct2009','ger_s2s_priority_area_billabong_creek_v01','ger_s2s_priority_areas_v05');

update layers set displayname = 'Great Eastern Ranges Initiative Boundary', description = 'Great Eastern Ranges Initiative Boundary' where name = 'ger_geri_boundary_v102_australia';

commit;
