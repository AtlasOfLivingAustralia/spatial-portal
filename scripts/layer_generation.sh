#!/bin/bash

# This is the parent build script which kicks off the scripts that generate the occurrence density, species richness and endemism layers.
# After they have been generated, this script does some post processing to generate analysis layers and layer distance matrix values for
# the newly created layers.

# Run density_layers.sh and record exit code
sh density_layers.sh
DENSITY_LAYERS_RET_CODE=$?


# Run endemism.sh and record exit code
sh endemism.sh
ENDEMISM_RET_CODE=$?

# If both successful
if [[DENSITY_LAYERS_RET_CODE -eq 0 -a ENDEMISM_RET_CODE -eq 0]]
then

# If only density layers successful
elif [[DENSITY_LAYERS_RET_CODE -eq 0]]
then

# If only endemism successful
elif [[ENDEMISM_RET_CODE  -eq 0]]
then


fi
