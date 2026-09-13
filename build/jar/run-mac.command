#!/bin/bash
cd "$(dirname "$0")"
if ! command -v java >/dev/null 2>&1; then
    osascript -e 'display alert "Java가 필요합니다" message "https://adoptium.net 에서 Java 11 이상을 설치한 뒤 다시 실행해주세요."'
    exit 1
fi
java -jar "NotebookRPG.jar"
