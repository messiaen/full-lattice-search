VERSION = $(shell cat VERSION.txt)
NAME = "registry.gitlab.com/hedgehogai/full-lattice-search/full-lattice-search"

clean:
	./gradlew clean

default: build_plugin

build_plugin:
	./gradlew clean assemble

build_image: build_plugin
	docker build --no-cache -t $(NAME):$(VERSION) .
	docker tag $(NAME):$(VERSION) $(NAME):latest

push:
	docker push $(NAME):$(VERSION)

test:
	./gradlew cleanTest test

build:
	./gradlew build

run: stop build_image
	docker-compose up -d

stop:
	docker-compose down --volumes
