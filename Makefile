ES_VERSION = $(shell grep elasticsearchVersion gradle.properties  | cut -d " " -f 3)
VERSION = $(shell cat VERSION.txt)
IMAGE_VERSION = $(VERSION)-$(ES_VERSION)
NAME = "messiaen/elasticsearch-plug-ph-lat"

clean:
	./gradlew clean

default: build_plugin

build_plugin:
	./gradlew clean assemble

build_image: build_plugin
	docker build --no-cache -t $(NAME):$(IMAGE_VERSION) .
	docker tag $(NAME):$(IMAGE_VERSION) $(NAME):latest

push:
	docker push $(NAME):$(IMAGE_VERSION)

test:
	./gradlew cleanTest test

build:
	./gradlew build

run: stop build_image
	docker-compose up -d

stop:
	docker-compose down --volumes
