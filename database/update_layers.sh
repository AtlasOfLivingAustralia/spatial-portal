#!/bin/bash
PSQL_CMD=/usr/local/pgsql/bin/psql

# run layers create/update script
$PSQL_CMD -U postgres spatialdb -f ./layers.sql
