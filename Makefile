VERSION = $(shell cat VERSION.txt)
NAME = "messiaen/elasticsearch-plug-ph-lat"

default: build_plugin

build_plugin:
	gradle clean assemble

build_image: build_plugin
	docker build --no-cache -t $(NAME):$(VERSION) .

push:
	docker push $(NAME):$(VERSION)

test:
	gradle test

build:
	gradle build
