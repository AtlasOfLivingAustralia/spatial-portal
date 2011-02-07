#!/bin/bash

# run layers create/update script
psql -U postgres spatialdb -f ./layers.sql
