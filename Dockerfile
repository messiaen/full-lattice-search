ARG es_version=9.0.0.0
FROM docker.elastic.co/elasticsearch/elasticsearch:${es_version}

# got to declare again here
ARG es_version=0.1
ARG plugin_version=0.2
COPY build/distributions/es-lattice-index-plugin-${plugin_version}-${es_version}.zip /es-lattice-index-plugin.zip

RUN bin/elasticsearch-plugin install analysis-phonetic
RUN bin/elasticsearch-plugin install file:///es-lattice-index-plugin.zip && rm -f /es-lattice-index-plugin.zip
