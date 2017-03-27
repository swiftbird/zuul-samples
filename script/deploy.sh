#!/usr/bin/env bash

curl -X POST http://<config-server>.<MYRUL>/admin/refresh

curl -X POST http://gateway-service.<MYURL>/refresh
