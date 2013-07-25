-- Table: tabulation

-- DROP TABLE tabulation;

CREATE TABLE tabulation
(
  fid1 character varying,
  pid1 character varying,
  fid2 character varying,
  pid2 character varying,
  the_geom geometry,
  area double precision,
  occurrences integer DEFAULT 0,
  species integer DEFAULT 0,
  CONSTRAINT tabulation_unqiue_constraint UNIQUE (fid1 , pid1 , fid2 , pid2 )
)
WITH (
  OIDS=FALSE
);
ALTER TABLE tabulation
  OWNER TO postgres;

-- Index: tabulation_fid1_idx

-- DROP INDEX tabulation_fid1_idx;

CREATE INDEX tabulation_fid1_idx
  ON tabulation
  USING btree
  (fid1 );

-- Index: tabulation_fid2_idx

-- DROP INDEX tabulation_fid2_idx;

CREATE INDEX tabulation_fid2_idx
  ON tabulation
  USING btree
  (fid2 );

-- Index: tabulation_pid1_idx

-- DROP INDEX tabulation_pid1_idx;

CREATE INDEX tabulation_pid1_idx
  ON tabulation
  USING btree
  (pid1 );

-- Index: tabulation_pid2_idx

-- DROP INDEX tabulation_pid2_idx;

CREATE INDEX tabulation_pid2_idx
  ON tabulation
  USING btree
  (pid2 );

