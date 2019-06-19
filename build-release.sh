#!/bin/bash


version="$1"

if [ -z "$version" ]; then
    echo "Please specify version"
    exit 1
fi

set -ex
docker build -t jaemk/badge:$version .
docker build -t jaemk/badge:latest .

docker push jaemk/badge:$version
docker push jaemk/badge:latest
