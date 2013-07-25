-- Table: distributionshapes

-- DROP TABLE distributionshapes;

CREATE TABLE distributionshapes
(
  id serial NOT NULL,
  the_geom geometry,
  name character varying(256),
  pid character varying,
  area_km double precision,
  CONSTRAINT distributionshapes_pkey PRIMARY KEY (id )
)
WITH (
  OIDS=FALSE
);
ALTER TABLE distributionshapes
  OWNER TO postgres;

-- Index: distributiondata_the_geom

-- DROP INDEX distributiondata_the_geom;

CREATE INDEX distributiondata_the_geom
  ON distributionshapes
  USING gist
  (the_geom );

