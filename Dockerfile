FROM docker.elastic.co/elasticsearch/elasticsearch:7.3.0

COPY build/distributions/es-lattice-index-plugin-2.0.0-rc1-7.3.0.zip /es-lattice-index-plugin.zip

RUN bin/elasticsearch-plugin install analysis-phonetic
RUN bin/elasticsearch-plugin install file:///es-lattice-index-plugin.zip && rm -f /es-lattice-index-plugin.zip
