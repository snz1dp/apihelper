
.PHONY: test build clean debug install deploy

export SHELL:=/bin/bash

OS := $(shell uname | awk '{print tolower($$0)}')
ROOT_DIR:=$(shell dirname $(realpath $(lastword $(MAKEFILE_LIST))))

DOCKER_COMMAND?=docker build
DOCKER_RUN?=docker run

BUILD_SKIP_TESTS?=true
MAVEN_TESTS_DEFINES?=-DskipTests

ifneq ($(BUILD_SKIP_TESTS), true)
	MAVEN_TESTS_DEFINES=-Dtest=gateway.api.*
endif

debug:
	echo OS=${OS}
	echo DOCKER_COMMAND=$(DOCKER_COMMAND)
	echo DOCKER_RUN=$(DOCKER_RUN)

clean:
	rm -rf target

ifneq ($(BUILD_SKIP_TESTS), true)
build: test
else
build:
endif
	$(ROOT_DIR)/mvnw package -DskipTests

install: build
	$(ROOT_DIR)/mvnw install -DskipTests

deploy: install
	$(ROOT_DIR)/mvnw deploy -DskipTests

deploy-ossrh: install
	$(ROOT_DIR)/mvnw deploy -DskipTests -P ossrh

release-ossrh:
	$(ROOT_DIR)/mvnw nexus-staging:release -P ossrh
