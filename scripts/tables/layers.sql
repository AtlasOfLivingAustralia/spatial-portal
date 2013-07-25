-- Table: layers

-- DROP TABLE layers;

CREATE TABLE layers
(
  id integer NOT NULL,
  name character varying(150),
  description text,
  type character varying(20),
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
  licence_level character varying,
  licence_link character varying(300),
  licence_notes character varying(1024),
  source_link character varying(300),
  path_orig character varying,
  path_1km character varying(256),
  path_250m character varying(256),
  pid character varying,
  keywords character varying,
  domain character varying(100),
  CONSTRAINT pk_layers_id PRIMARY KEY (id )
)
WITH (
  OIDS=FALSE
);
ALTER TABLE layers
  OWNER TO postgres;
