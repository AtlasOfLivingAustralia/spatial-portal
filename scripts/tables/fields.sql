-- Table: fields

-- DROP TABLE fields;

CREATE TABLE fields
(
  name character varying(256),
  id character varying(8) NOT NULL,
  "desc" character varying(256),
  type character(1),
  spid character varying(256),
  sid character varying(256),
  sname character varying(256),
  sdesc character varying(256),
  indb boolean DEFAULT false,
  enabled boolean DEFAULT false,
  last_update timestamp without time zone,
  namesearch boolean DEFAULT false,
  defaultlayer boolean,
  "intersect" boolean DEFAULT false,
  layerbranch boolean DEFAULT false,
  analysis boolean DEFAULT true,
  addtomap boolean DEFAULT true,
  CONSTRAINT pk_id PRIMARY KEY (id )
)
WITH (
  OIDS=FALSE
);
ALTER TABLE fields
  OWNER TO postgres;

-- Index: idx_fields_id

-- DROP INDEX idx_fields_id;

CREATE INDEX idx_fields_id
  ON fields
  USING btree
  (id );

