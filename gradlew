#!/bin/sh

# Gradle startup script for POSIX shells.

set -e

app_path=$0
while [ -h "$app_path" ]; do
    ls=$(ls -ld "$app_path")
    link=$(expr "$ls" : '.*-> \(.*\)$')
    case $link in
      /*) app_path=$link ;;
      *) app_path=$(dirname "$app_path")/$link ;;
    esac
done

APP_HOME=$(cd "${app_path%/*}" && pwd -P)

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

if [ -n "$JAVA_HOME" ]; then
    JAVACMD=$JAVA_HOME/bin/java
else
    JAVACMD=java
fi

if ! command -v "$JAVACMD" >/dev/null 2>&1; then
    echo "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH." >&2
    exit 1
fi

set -- "-Xmx64m" "-Xms64m" "-Dorg.gradle.appname=gradlew" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"

exec "$JAVACMD" "$@"
