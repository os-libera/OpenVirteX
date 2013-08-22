#!/bin/sh

OVXHOME=`dirname $0`/..
OVX_JAR="${OVXHOME}/target/OpenVirteX.jar"

JVM_OPTS=""
#JVM_OPTS="$JVM_OPTS -XX:+UseTieredCompilation"
JVM_OPTS="$JVM_OPTS -XX:+UseCompressedOops"
JVM_OPTS="$JVM_OPTS -XX:+UseConcMarkSweepGC -XX:+AggressiveOpts -XX:+UseFastAccessorMethods"
JVM_OPTS="$JVM_OPTS -XX:MaxInlineSize=8192 -XX:FreqInlineSize=8192" 
JVM_OPTS="$JVM_OPTS -XX:CompileThreshold=1500 -XX:PreBlockSpin=8" 



if [ ! -e ${OVX_JAR} ]; then
  cd ${OVXHOME}
  echo "Packaging OVX for you..."
  mvn package > /dev/null
  cd -
fi

echo "Starting OpenVirteX..."
java ${JVM_OPTS} -Dlog4j.configurationFile=log4j2.xml -jar ${OVX_JAR}
  
