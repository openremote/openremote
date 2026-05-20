#!/bin/sh
exec java ${OR_JAVA_OPTS} -cp "/opt/app/lib/*" org.openremote.manager.Main
