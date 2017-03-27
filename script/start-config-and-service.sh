#!/usr/bin/env bash

./gradlew configserver:bootRun &
./gradlew book:bootRun &
