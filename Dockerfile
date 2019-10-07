FROM docker.elastic.co/elasticsearch/elasticsearch:7.3.0

# version should match VERSION.txt plus hyphen plus ES version from gradle.properties
COPY build/distributions/full-lattice-search-2.0.0-rc2-7.3.0.zip /full-lattice-search.zip

RUN bin/elasticsearch-plugin install analysis-phonetic
RUN bin/elasticsearch-plugin install file:///full-lattice-search.zip && rm -f /full-lattice-search.zip
