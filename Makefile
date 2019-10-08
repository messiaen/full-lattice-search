# Disable built-in rules and variables
MAKEFLAGS += --no-builtin-rules
MAKEFLAGS += --no-builtin-variables

ES_VERSION = $(shell grep ES_VERSION .env | cut -d '=' -f 2)
VERSION = $(shell grep PLUGIN_VERSION .env | cut -d '=' -f 2)
NAME = "registry.gitlab.com/hedgehogai/full-lattice-search/full-lattice-search"
IMAGE_VERSION = $(VERSION)-$(ES_VERSION)

default: build_plugin

clean:
	./gradlew clean

build_plugin:
	./gradlew clean assemble

build_image: build_plugin
	docker build --no-cache --build-arg plugin_version=$(VERSION) --build-arg es_version=$(ES_VERSION) -t $(NAME):$(IMAGE_VERSION) .
	docker tag $(NAME):$(IMAGE_VERSION) $(NAME):latest

push:
	docker push $(NAME):$(IMAGE_VERSION)
	docker push $(NAME):latest

test:
	./gradlew cleanTest test

build:
	./gradlew build

run: stop build_image
	docker-compose up -d

stop:
	docker-compose down --volumes
