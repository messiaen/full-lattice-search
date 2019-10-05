FROM docker.elastic.co/elasticsearch/elasticsearch:7.3.0

COPY build/distributions/full-lattice-search-2.0.0-rc1-7.3.0.zip /full-lattice-search.zip

RUN bin/elasticsearch-plugin install analysis-phonetic
RUN bin/elasticsearch-plugin install file:///full-lattice-search.zip && rm -f /full-lattice-search.zip
