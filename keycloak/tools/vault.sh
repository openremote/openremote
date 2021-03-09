#!/bin/bash

if [ -d "$JBOSS_HOME/secrets" ]; then
    echo "set plaintext_vault_provider_dir=${JBOSS_HOME}/secrets" >> "$JBOSS_HOME/bin/.jbossclirc"

    echo "set configuration_file=standalone.xml" >> "$JBOSS_HOME/bin/.jbossclirc"
    $JBOSS_HOME/bin/jboss-cli.sh --file=/opt/jboss/tools/cli/files-plaintext-vault.cli
    sed -i '$ d' "$JBOSS_HOME/bin/.jbossclirc"

    echo "set configuration_file=standalone-ha.xml" >> "$JBOSS_HOME/bin/.jbossclirc"
    $JBOSS_HOME/bin/jboss-cli.sh --file=/opt/jboss/tools/cli/files-plaintext-vault.cli
    sed -i '$ d' "$JBOSS_HOME/bin/.jbossclirc"
fi
