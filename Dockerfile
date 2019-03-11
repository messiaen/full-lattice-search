FROM docker.elastic.co/elasticsearch/elasticsearch:6.5.1

COPY build/distributions/es-lattice-index-plugin-6.5.1.zip /es-lattice-index-plugin-6.5.1.zip

RUN bin/elasticsearch-plugin install analysis-phonetic
RUN bin/elasticsearch-plugin install file:///es-lattice-index-plugin-6.5.1.zip && rm -f /es-lattice-index-plugin-6.5.1.zip
