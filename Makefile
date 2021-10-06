# Makefile

MAKEFLAGS += -j2
-include .env
export

CURRENT_BRANCH := $(shell git rev-parse --abbrev-ref HEAD)
CURRENT_PATH := $(shell pwd)
AMM := ${HOME}/amm

.PHONY: gitRebase
gitRebase:
	git checkout develop && \
		git pull upstream develop && \
		git push origin develop && \
		git checkout $(CURRENT_BRANCH) && \
		git rebase develop

.PHONY: gitAmmend
gitAmmend:
	git add . && git commit --amend --no-edit && git push --force origin $(CURRENT_BRANCH)

.PHONY: killJava
killJava:
	ps ax | grep java | grep -v 'grep' | cut -d '?' -f1 | xargs kill -9

.PHONY: dependencyTree
dependencyTree:
	sbt dependencyBrowseTree

.PHONY: amm
amm:
ifeq ("$(wildcard $(AMM))", "")
	@echo "Installing ammonite $(AMM)"
	sudo sh -c '(echo "#!/usr/bin/env sh" && \
		curl -L https://github.com/lihaoyi/Ammonite/releases/download/2.3.8/2.13-2.3.8) \
		> $(AMM) && \
		chmod +x $(AMM)'
endif
	$(AMM)

.PHONY: prepare
prepare:
	sbt prepare

.PHONY: startDocker
startDocker:
	docker-compose -f docker-compose.yml up -d

.PHONY: header
header:
	sbt headerCreate

.PHONY: testUserCreate
testUserCreate:
	curl --header "Content-Type: application/json" \
		--request POST \
		--data '{ "publicAddress": "0x31b26E43651e9371C88aF3D36c14CfD938BaF4Fd" }' \
		http://localhost:8080/users
