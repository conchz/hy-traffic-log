#!/usr/bin/env bash
mode=prod
topic=huiyan_relog
port=20001

BASE_DIR=`dirname $0`
JAVA_HOME=`${JAVA_HOME}`
if [${JAVA_HOME} == ""]
    then
        JAVA_HOME=/usr/local/jdk/jdk1.8.0_45
fi

while getopts "m:p:t:" arg
    do
        case $arg in
            "m")
                mode=$OPTARG
                ;;
            "p")
                port=$OPTARG
                ;;
            "t")
                topic=$OPTARG
                ;;
            "?")
                echo "unknow argument"
                ;;
        esac
    done

${JAVA_HOME}/bin/java -Dfile.encoding=UTF-8 -Xms2g -Xmx2g -cp ${BASE_DIR}:${BASE_DIR}/lib/*:${BASE_DIR}/conf com.ss.main.RelogProducerMain ${mode} ${topic} ${port}