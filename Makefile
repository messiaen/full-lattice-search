# Disable built-in rules and variables
MAKEFLAGS += --no-builtin-rules
MAKEFLAGS += --no-builtin-variables

ES_VERSION = $(shell grep ES_VERSION .env | cut -d '=' -f 2)
VERSION = $(shell grep PLUGIN_VERSION .env | cut -d '=' -f 2)
COMMIT_HASH = $(shell git describe --match=NeVeRmAtCh --always --abbrev=8 --dirty)
NAME = "messiaen/full-lattice-search"
IMAGE_VERSION = $(VERSION)-$(ES_VERSION)

default: build

clean:
	./gradlew clean

build_image: build
	docker build --no-cache --build-arg commit_hash=$(COMMIT_HASH) --build-arg plugin_version=$(VERSION) --build-arg es_version=$(ES_VERSION) -t $(NAME):$(IMAGE_VERSION) .
	docker tag $(NAME):$(IMAGE_VERSION) $(NAME):latest

push:
	docker push $(NAME):$(IMAGE_VERSION)
	docker push $(NAME):latest

test:
	./gradlew cleanTest test integTest

.PHONY: build
build:
	./gradlew build

run: stop build_image
	docker-compose up -d

stop:
	docker-compose down --volumes
