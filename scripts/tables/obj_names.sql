-- Table: obj_names

-- DROP TABLE obj_names;

CREATE TABLE obj_names
(
  id serial NOT NULL,
  name character varying,
  CONSTRAINT obj_names_id_pk PRIMARY KEY (id ),
  CONSTRAINT obj_names_name_unique UNIQUE (name )
)
WITH (
  OIDS=FALSE
);
ALTER TABLE obj_names
  OWNER TO postgres;
