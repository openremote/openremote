#!/bin/bash

# Initialize variables
DRY_RUN=false
SKIP_BUILD=false
BALENA_FLEET_SLUG=""

# Parse arguments
for arg in "$@"
do
    case $arg in
        --dry-run)
        DRY_RUN=true
        ;;
        --skip-build)
        SKIP_BUILD=true
        ;;
        *)
        BALENA_FLEET_SLUG="$arg"
        ;;
    esac
done

# Ensure BALENA_FLEET_SLUG is set
if [ -z "$BALENA_FLEET_SLUG" ]; then
    echo "Usage: $0 [--dry-run] [--skip-build] <balena_fleet_slug>"
    exit 1
fi

# Login to balena
balena login

# Remove existing manager-build directory if skip-build is not enabled
if [ "$SKIP_BUILD" = false ]; then
    rm -rf ./manager-build

    # Go to the parent directory, run clean installDist
    cd ..
    ./gradlew clean installDist

    # Return to the balena directory
    cd balena
fi

# Copy new builds if manager-build directory does not exist
if [ ! -d "./manager-build" ]; then
    cp -r ../manager/build/install/manager ./manager-build
fi

# Push to balena with no cache or run docker-compose up if dry-run
if [ "$DRY_RUN" = true ]; then
    docker compose build --no-cache && docker-compose -f ./docker-compose.yml -p openremote up --force-recreate -d
else
    balena push "$BALENA_FLEET_SLUG" --nocache
fi
