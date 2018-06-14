#!/bin/bash

cDIR=`pwd`
mvn -DskipTests -Dlicense.skip=true compile install
rm -rf ~/qiniu/resources/alluxio/oss/com && cp -r target/classes/com ~/qiniu/resources/alluxio/oss

rm -f alluxio-underfs-oss-1.7.1.jar && jar -cvf alluxio-underfs-oss-1.7.1.jar . && mv ./alluxio-underfs-oss-1.7.1.jar ../alluxio/lib/

cd /Users/bowenxie/qiniu/resources/alluxio/oss
rm -f alluxio-underfs-oss-1.7.1.jar && jar -cvf alluxio-underfs-oss-1.7.1.jar . && mv ./alluxio-underfs-oss-1.7.1.jar ../alluxio/lib/

cd $cDIR

