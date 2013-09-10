CREATE TABLE distributions AS
 SELECT d.gid, d.spcode, d.scientific, d.authority_, d.common_nam, d.family, d.genus_name,
 d.specific_n, d.min_depth, d.max_depth, d.pelagic_fl, d.estuarine_fl, d.coastal_fl, d.desmersal_fl,
 d.metadata_u, d.wmsurl, d.lsid, d.family_lsid, d.genus_lsid, d.caab_species_number, d.caab_family_number,
 o.the_geom, o.name AS area_name, o.pid, d.type, d.checklist_name, o.area_km, d.notes, d.geom_idx, d.group_name,
 d.genus_exemplar, d.family_exemplar, d.data_resource_uid, d.image_quality, d.bounding_box
   FROM distributionshapes o
   JOIN distributiondata d ON d.geom_idx = o.id;

CREATE INDEX distributions_spcode_idx ON distributions(spcode);
CREATE INDEX distributions_scientific_idx ON distributions(scientific);
CREATE INDEX distributions_family_idx ON distributions(family);
CREATE INDEX distributions_genus_name_idx ON distributions(genus_name);
CREATE INDEX distributions_min_depth_idx ON distributions(min_depth);
CREATE INDEX distributions_max_depth_idx ON distributions(max_depth);
CREATE INDEX distributions_pelagic_fl_idx ON distributions(pelagic_fl);
CREATE INDEX distributions_estuarine_fl_idx ON distributions(estuarine_fl);
CREATE INDEX distributions_coastal_fl_idx ON distributions(coastal_fl);
CREATE INDEX distributions_desmersal_fl_idx ON distributions(desmersal_fl);
CREATE INDEX distributions_metadata_u_idx ON distributions(metadata_u);
CREATE INDEX distributions_lsid_idx ON distributions(lsid);
CREATE INDEX distributions_family_lsid_idx ON distributions(family_lsid);
CREATE INDEX distributions_genus_lsid_idx ON distributions(genus_lsid);
CREATE INDEX distributions_caab_species_number_idx ON distributions(caab_species_number);
CREATE INDEX distributions_caab_family_number_idx ON distributions(caab_family_number);
CREATE INDEX distributions_type_idx ON distributions(type);
CREATE INDEX distributions_checklist_name_idx ON distributions(checklist_name);
CREATE INDEX distributions_group_name_idx ON distributions(group_name);
CREATE INDEX distributions_genus_exemplar_idx ON distributions(genus_exemplar);
CREATE INDEX distributions_family_exemplar_idx ON distributions(family_exemplar);
CREATE INDEX distributions_data_resource_uid_idx ON distributions(data_resource_uid);
CREATE INDEX distributions_image_quality ON distributions(image_quality);
CREATE INDEX distributions_geom ON distributions USING GIST (the_geom);

ALTER TABLE distributions OWNER TO postgres;