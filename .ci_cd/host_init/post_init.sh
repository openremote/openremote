#!/bin/bash
# -----------------------------------------------------------------------------------------------------
# Script that is called after a successful stack up; by default it prunes volumes and images
# -----------------------------------------------------------------------------------------------------
echo "Pruning volumes"
docker volume prune -f
echo "Pruning images"
docker image prune -f -a

