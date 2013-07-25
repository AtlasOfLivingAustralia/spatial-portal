-- Type: searchobjectstype

-- DROP TYPE searchobjectstype;

CREATE TYPE searchobjectstype AS
   (pid character varying,
    id character varying,
    name character varying,
    "desc" character varying,
    fid character varying,
    fieldname character varying);
ALTER TYPE searchobjectstype
  OWNER TO postgres;
