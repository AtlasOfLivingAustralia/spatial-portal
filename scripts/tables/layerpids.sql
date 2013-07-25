-- Table: layerpids

-- DROP TABLE layerpids;

CREATE TABLE layerpids
(
  id character varying(256),
  type integer,
  "unique" character varying(255),
  path character varying(500),
  pid character varying(256),
  metadata character varying(500),
  the_geom geometry
)
WITH (
  OIDS=FALSE
);
ALTER TABLE layerpids
  OWNER TO postgres;
COMMENT ON TABLE layerpids
  IS 'Values for "type" column are: 
1 = Contextual
2 = Environmental
3 = Checklist
4 = Shapefile
5 = Distribution';
