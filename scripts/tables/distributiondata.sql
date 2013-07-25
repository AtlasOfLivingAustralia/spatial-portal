-- Table: distributiondata

-- DROP TABLE distributiondata;

CREATE TABLE distributiondata
(
  gid integer NOT NULL DEFAULT nextval('distributiondata_id_seq'::regclass),
  spcode numeric,
  scientific character varying(254),
  authority_ character varying(254),
  common_nam character varying(254),
  family character varying(254),
  genus_name character varying(254),
  specific_n character varying(254),
  min_depth numeric,
  max_depth numeric,
  pelagic_fl numeric,
  metadata_u character varying(254),
  the_geom geometry,
  wmsurl character varying,
  lsid character varying,
  geom_idx integer,
  type character(1),
  checklist_name character varying,
  notes text,
  estuarine_fl numeric,
  coastal_fl numeric,
  desmersal_fl numeric,
  group_name text,
  genus_exemplar boolean,
  family_exemplar boolean,
  caab_species_number text,
  caab_species_url text,
  caab_family_number text,
  caab_family_url text,
  metadata_uuid text,
  family_lsid text,
  genus_lsid text,
  bounding_box geometry,
  data_resource_uid character varying,
  original_scientific_name character varying,
  image_quality character(1),
  CONSTRAINT copy_distributiondata_pkey PRIMARY KEY (gid ),
  CONSTRAINT copy_distributiondata_spcode_key UNIQUE (spcode ),
  CONSTRAINT copy_distributiondata_the_geom_check CHECK (st_ndims(the_geom) = 2),
  CONSTRAINT copy_distributiondata_the_geom_check1 CHECK (geometrytype(the_geom) = 'MULTIPOLYGON'::text OR the_geom IS NULL),
  CONSTRAINT copy_distributiondata_the_geom_check3 CHECK (st_srid(the_geom) = 4326)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE distributiondata
  OWNER TO postgres;

-- Index: distributiondata_family_idx

-- DROP INDEX distributiondata_family_idx;

CREATE INDEX distributiondata_family_idx
  ON distributiondata
  USING btree
  (family );

-- Index: distributiondata_idx

-- DROP INDEX distributiondata_idx;

CREATE INDEX distributiondata_idx
  ON distributiondata
  USING btree
  (geom_idx );

-- Index: distributiondata_scientific_idx

-- DROP INDEX distributiondata_scientific_idx;

CREATE INDEX distributiondata_scientific_idx
  ON distributiondata
  USING btree
  (scientific );

-- Index: distributiondata_spcode_idx

-- DROP INDEX distributiondata_spcode_idx;

CREATE INDEX distributiondata_spcode_idx
  ON distributiondata
  USING btree
  (spcode );

-- Index: distributiondata_the_geom_idx

-- DROP INDEX distributiondata_the_geom_idx;

CREATE INDEX distributiondata_the_geom_idx
  ON distributiondata
  USING gist
  (the_geom );

-- Index: distributiondata_type_idx

-- DROP INDEX distributiondata_type_idx;

CREATE INDEX distributiondata_type_idx
  ON distributiondata
  USING btree
  (type );

