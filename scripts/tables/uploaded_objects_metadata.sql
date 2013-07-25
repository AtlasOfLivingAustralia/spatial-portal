-- Table: uploaded_objects_metadata

-- DROP TABLE uploaded_objects_metadata;

CREATE TABLE uploaded_objects_metadata
(
  pid character varying(256) NOT NULL,
  user_id text,
  time_last_updated timestamp with time zone,
  id serial NOT NULL,
  CONSTRAINT pk_uploaded_objects_metadata PRIMARY KEY (id ),
  CONSTRAINT fk_uploaded_objects_metadata FOREIGN KEY (pid)
      REFERENCES objects (pid) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);
ALTER TABLE uploaded_objects_metadata
  OWNER TO postgres;
