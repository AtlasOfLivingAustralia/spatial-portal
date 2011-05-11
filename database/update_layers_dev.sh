#!/bin/bash
#
# This script restores the layers table and associated table sequence
# Gavin Jackson 11/2/2011

PSQL_CMD=/usr/local/pgsql/bin/psql

#allows script to work on local (OSX) postgres environment
if [ -e /Library/PostgreSQL/9.0/bin/psql ]
then
  PSQL_CMD=/Library/PostgreSQL/9.0/bin/psql
fi

# run layers create/update script
$PSQL_CMD -U postgres spatialdb -f ./layers_dev.sql
$PSQL_CMD -U postgres spatialdb -f ./layers_dev_updates/001_ger_updates_12052011.sql

