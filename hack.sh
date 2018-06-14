#!/bin/bash

cDIR=`pwd`
mvn -DskipTests -Dlicense.skip=true compile install
rm -rf ~/qiniu/resources/alluxio/oss/com && cp -r target/classes/com ~/qiniu/resources/alluxio/oss

rm -f alluxio-underfs-oss-1.7.2-SNAPSHOT.jar && jar -cvf alluxio-underfs-oss-1.7.2-SNAPSHOT.jar . && mv ./alluxio-underfs-oss-1.7.2-SNAPSHOT.jar ../alluxio/lib/

cd /Users/bowenxie/qiniu/resources/alluxio/oss
rm -f alluxio-underfs-oss-1.7.2-SNAPSHOT.jar && jar -cvf alluxio-underfs-oss-1.7.2-SNAPSHOT.jar . && mv ./alluxio-underfs-oss-1.7.2-SNAPSHOT.jar ../lib/

cd $cDIR

