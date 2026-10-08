#!/usr/bin/env sh
# Minimal Gradle Wrapper bootstrap. Wrapper implementation is the official Apache-2.0 Gradle wrapper JAR.
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P) || exit 1
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
if [ ! -f "$CLASSPATH" ]; then
  echo "Gradle wrapper JAR is missing: $CLASSPATH" >&2
  exit 1
fi
exec java ${JAVA_OPTS:-} ${GRADLE_OPTS:-} -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
