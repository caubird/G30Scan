#!/bin/sh
APP_HOME=$(cd "$(dirname "$0")" && pwd -P) || exit
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
if [ -n "$JAVA_HOME" ]; then
    if [ -x "$JAVA_HOME/jre/sh/java" ]; then
        JAVACMD="$JAVA_HOME/jre/sh/java"
    else
        JAVACMD="$JAVA_HOME/bin/java"
    fi
    if [ ! -x "$JAVACMD" ]; then
        echo "ERROR: JAVA_HOME invalid: $JAVA_HOME" >&2
        exit 1
    fi
else
    JAVACMD=java
    if ! command -v java >/dev/null 2>&1; then
        echo "ERROR: JAVA_HOME not set and java not in PATH" >&2
        exit 1
    fi
fi
exec "$JAVACMD" -Dorg.gradle.appname=gradlew -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
