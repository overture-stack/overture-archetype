###############################################################################
#
#  Copyright (c) 2019. Ontario Institute for Cancer Research
#  
#  This program is free software: you can redistribute it and/or modify
#  it under the terms of the GNU Affero General Public License as
#  published by the Free Software Foundation, either version 3 of the
#  License, or (at your option) any later version.
#  
#  This program is distributed in the hope that it will be useful,
#  but WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#  GNU Affero General Public License for more details.
#  
#  You should have received a copy of the GNU Affero General Public License
#  along with this program.  If not, see <https://www.gnu.org/licenses/>.
#
###############################################################################

#####################################################################
#          Project Configuration
#####################################################################
# Server Config
API_HOST_PORT := 8080

# Docker Config
DOCKERFILE_SERVER := Dockerfile
SOURCE_COMMIT := $$(git show | grep commit | head -1 | tr -s ' ' | cut -d ' ' -f 2 | cut -c 1-8)
DOCKER_REPO := mydockerrepo
DOCKER_PROJ := $$(sed -n -e 's/.*<name>\(.*\)<\/name>.*/\1/p' ./pom.xml)
DOCKER_CONTAINER_NAME := $(DOCKER_PROJ)-$(SOURCE_COMMIT)
DOCKER_IMAGE_NAME := $(DOCKER_REPO)/$(DOCKER_PROJ):$(SOURCE_COMMIT)

# Software Dependencies
DOCKER_EXE := /usr/bin/docker
DOCKER_COMPOSE_EXE := /usr/bin/docker-compose
MVNW_EXE := ./mvnw

DOCKER_EXE_VERSION := $$($(DOCKER_EXE) version --format '{{.Server.Version}}')
DOCKER_COMPOSE_EXE_VERSION := $$($(DOCKER_COMPOSE_EXE) version --short)
MVNW_EXE_VERSION := $$($(MVNW_EXE) -version | head -1 | tr -s ' ' | cut -d ' ' -f 3)
JAVA_VERSION := $$($(MVNW_EXE) -version | grep 'Java version' | sed 's/,//g' | tr -s ' ' | cut -d ' ' -f 3)

#####################################################################
#          Internal
#####################################################################

# STDOUT Formatting
RED := $$(echo  "\033[0;31m")
YELLOW := $$(echo "\033[0;33m")
END := $$(echo  "\033[0m")
ERROR_HEADER :=  [ERROR]:
INFO_HEADER := "**************** "
DONE_MESSAGE := $(YELLOW)$(INFO_HEADER) "- done\n" $(END)

.PHONY: 
.SILENT: docker-server-ps docker-server-logs

define exec_docker_compose_command
	DOCKER_REPO=$(DOCKER_REPO) DOCKER_PROJ=$(DOCKER_PROJ) \
				SOURCE_COMMIT=${SOURCE_COMMIT} API_HOST_PORT=${API_HOST_PORT} \
				docker-compose $(1)
endef

# Internal Targets
$(DOCKER_EXE):
	$(error $(ERROR_HEADER) The docker executable "$(DOCKER_EXE)" does not exist)

$(DOCKER_COMPOSE_EXE):
	$(error $(ERROR_HEADER) The docker-compose executable "$(DOCKER_COMPOSE_EXE)" does not exist)

$(MVNW_EXE):
	$(error $(ERROR_HEADER) The mvnw executable "$(MVNW_EXE)" does not exist)

_check_docker_software_exists: $(DOCKER_EXE) $(DOCKER_COMPOSE_EXE)

_check_mvn_software_exists: $(MVNW_EXE)
	@chmod +x $(MVNW_EXE)


#####################################################################
#          User Targets
#####################################################################

help:
	@echo
	@echo "**************************************************************"
	@echo "                  Help"
	@echo "**************************************************************"
	@echo "To dry-execute a target run: make -n <target> "
	@echo
	@echo "Available Targets: "
	@grep '^[A-Za-z][A-Za-z0-9_-]\+:.*' ./Makefile | sed 's/:.*//' | sed 's/^/\t/'
	@echo

info:
	@echo
	@echo "**************************************************************"
	@echo "                  Info"
	@echo "**************************************************************"
	@echo "              JAVA_HOME:  ${JAVA_HOME}"
	@echo "           JAVA_VERSION:  $(JAVA_VERSION)"
	@echo "               MVNW_EXE:  $(MVNW_EXE)"
	@echo "           MVNW_VERSION:  $(MVNW_EXE_VERSION)"
	@echo "             DOCKER_EXE:  $(DOCKER_EXE)"
	@echo "         DOCKER_VERSION:  $(DOCKER_EXE_VERSION)"
	@echo "     DOCKER_COMPOSE_EXE:  $(DOCKER_COMPOSE_EXE)"
	@echo " DOCKER_COMPOSE_VERSION:  $(DOCKER_COMPOSE_EXE_VERSION)"
	@echo "      DOCKERFILE_SERVER:  $(DOCKERFILE_SERVER)"
	@echo "          SOURCE_COMMIT:  $(SOURCE_COMMIT)"
	@echo "            DOCKER_REPO:  $(DOCKER_REPO)"
	@echo "            DOCKER_PROJ:  $(DOCKER_PROJ)"
	@echo "  DOCKER_CONTAINER_NAME:  $(DOCKER_CONTAINER_NAME)"
	@echo "      DOCKER_IMAGE_NAME:  $(DOCKER_IMAGE_NAME)"
	@echo "          API_HOST_PORT:  $(API_HOST_PORT)"
	@echo

# Local developement related targets
clean: _check_mvn_software_exists
	@echo $(YELLOW)$(INFO_HEADER) "Cleaning local project" $(END)
	@$(MVNW_EXE) clean
	@echo $(DONE_MESSAGE)

format: _check_mvn_software_exists
	@echo $(YELLOW)$(INFO_HEADER) "Formatting local project" $(END)
	@$(MVNW_EXE) fmt:format
	@echo $(DONE_MESSAGE)

proto: _check_mvn_software_exists
	@echo $(YELLOW)$(INFO_HEADER) "Compiling proto files only" $(END)
	@$(MVNW_EXE) protobuf:compile protobuf:compile-custom
	@echo $(DONE_MESSAGE)

compile: _check_mvn_software_exists
	@echo $(YELLOW)$(INFO_HEADER) "Cleaning and compiling everything" $(END)
	@$(MVNW_EXE) clean protobuf:compile protobuf:compile-custom test-compile
	@echo $(DONE_MESSAGE)

test: _check_mvn_software_exists
	@echo $(YELLOW)$(INFO_HEADER) "Running tests on the local project" $(END)
	@$(MVNW_EXE) test
	@echo $(DONE_MESSAGE)

package: _check_mvn_software_exists
	@echo $(YELLOW)$(INFO_HEADER) "Cleaning, compiling, testing, and packaging local project" $(END)
	@$(MVNW_EXE) clean package
	@echo $(DONE_MESSAGE)

package-no-test:_check_mvn_software_exists
	@echo $(YELLOW)$(INFO_HEADER) "Only cleaning, compiling and packaging local project" $(END)
	@$(MVNW_EXE) clean package -DskipTests
	@echo $(DONE_MESSAGE)

# Docker related targets
docker-server-logs: _check_docker_software_exists
	@echo $(YELLOW)$(INFO_HEADER) "Outputing docker-compose, logs to STDOUT" $(END)
	$(call exec_docker_compose_command, logs)
	@echo $(DONE_MESSAGE)
	
docker-server-ps: _check_docker_software_exists
	@echo $(YELLOW)$(INFO_HEADER) "Displaying running services from docker-compose"$(END)
	$(call exec_docker_compose_command, ps)
	@echo $(DONE_MESSAGE)

docker-server-kill: _check_docker_software_exists
	@echo $(YELLOW)$(INFO_HEADER) "Killing all docker-compose containers"$(END)
	$(call exec_docker_compose_command, down)
	@echo $(DONE_MESSAGE)

docker-server-clean: _check_docker_software_exists
	@echo $(YELLOW)$(INFO_HEADER) "Killing and removing all docker-compose containers, images and volumes"$(END)
	$(call exec_docker_compose_command, down --rmi all -v)
	@echo $(DONE_MESSAGE)

docker-server-build: _check_docker_software_exists $(DOCKERFILE_SERVER) init-submodules
	@echo $(YELLOW)$(INFO_HEADER) "Building docker-compose services"$(END)
	$(call exec_docker_compose_command, build)
	@echo $(DONE_MESSAGE)

docker-server-start: _check_docker_software_exists
	@echo $(YELLOW)$(INFO_HEADER) "Starting docker-compose services without building them"$(END)
	$(call exec_docker_compose_command, up --no-build -d)
	@echo $(DONE_MESSAGE)

docker-server-restart: docker-server-kill docker-server-start

docker-server-all: docker-server-kill
	@echo $(YELLOW)$(INFO_HEADER) "Building and Starting docker-compose services"$(END)
	$(call exec_docker_compose_command, up --build -d)
	@echo $(DONE_MESSAGE)
