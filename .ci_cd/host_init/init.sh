#!/bin/bash
sudo rm /etc/cron.d/openremote
sudo echo "0 5 * * * docker restart or_manager_1" >> /etc/cron.d/openremote
