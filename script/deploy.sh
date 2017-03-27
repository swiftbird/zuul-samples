#!/usr/bin/env bash

curl -X POST http://houdini-config-server.apps-np.homedepot.com/admin/refresh

curl -X POST http://gateway-service.apps-np.homedepot.com/refresh