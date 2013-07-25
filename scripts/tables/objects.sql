-- Table: objects

-- DROP TABLE objects;

CREATE TABLE objects
(
  pid character varying(256) NOT NULL,
  id character varying(256) DEFAULT nextval('objects_id_seq'::regclass),
  "desc" character varying(256),
  name character varying,
  fid character varying(8),
  the_geom geometry,
  name_id integer,
  namesearch boolean DEFAULT false,
  bbox character varying(200),
  area_km double precision,
  CONSTRAINT objects_pid_pk PRIMARY KEY (pid )
)
WITH (
  OIDS=FALSE
);
ALTER TABLE objects
  OWNER TO postgres;

-- Index: idx_objects_name_id

-- DROP INDEX idx_objects_name_id;

CREATE INDEX idx_objects_name_id
  ON objects
  USING btree
  (name_id );

-- Index: objects_fid_idx

-- DROP INDEX objects_fid_idx;

CREATE INDEX objects_fid_idx
  ON objects
  USING btree
  (fid );

-- Index: objects_geom_idx

-- DROP INDEX objects_geom_idx;

CREATE INDEX objects_geom_idx
  ON objects
  USING gist
  (the_geom );

-- Index: objects_namesearch_idx

-- DROP INDEX objects_namesearch_idx;

CREATE INDEX objects_namesearch_idx
  ON objects
  USING btree
  (namesearch );

