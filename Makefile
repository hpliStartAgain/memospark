# MemoSpark — convenience Makefile
#
# All targets delegate to the bundled Maven Wrapper so contributors do not
# need a system-wide Maven installation. Environment variables (DB_URL,
# DB_USERNAME, DB_PASSWORD, AI_API_KEY, JUDGE0_URL, ...) are picked up by
# Spring Boot at runtime — see README.md for the full list.

# Use bash on Unix; on Windows, GNU Make defaults to cmd which is also fine.
SHELL := /bin/sh

# Pick the right Maven Wrapper for the platform.
ifeq ($(OS),Windows_NT)
    MVNW := mvnw.cmd
else
    MVNW := ./mvnw
endif

ARTIFACT := target/memospark-0.0.1-SNAPSHOT.jar

.PHONY: help run package run-jar test clean verify deps tree fmt

help:                  ## Show this help.
	@echo "MemoSpark Makefile targets:"
	@echo "  make run         Start the app via spring-boot:run (dev mode)"
	@echo "  make package     Build the executable jar (skips tests)"
	@echo "  make run-jar     Run the previously built jar"
	@echo "  make test        Run unit tests"
	@echo "  make verify      Run full Maven verify lifecycle (tests + checks)"
	@echo "  make clean       Remove target/"
	@echo "  make deps        Download dependencies into the local repo"
	@echo "  make tree        Print the dependency tree"

run:                   ## Start the application in development mode.
	$(MVNW) spring-boot:run

package:               ## Build the executable jar (skip tests).
	$(MVNW) -DskipTests package

run-jar: package       ## Build (if needed) and run the executable jar.
	java -jar $(ARTIFACT)

test:                  ## Run unit tests.
	$(MVNW) test

verify:                ## Full Maven verify lifecycle.
	$(MVNW) verify

clean:                 ## Remove build artifacts.
	$(MVNW) clean

deps:                  ## Resolve and download dependencies.
	$(MVNW) -q dependency:go-offline

tree:                  ## Print Maven dependency tree.
	$(MVNW) dependency:tree
