#!/bin/bash

curl -XGET "http://localhost:9200/_cluster/health?wait_for_status=green&timeout=120s&pretty"

curl -H 'Content-Type: application/json' -XPUT "http://localhost:9200/mytest?pretty" -d '{
  "settings": {
    "index": {
      "number_of_shards": 1,
      "number_of_replicas": 0
    },
    "analysis": {
      "analyzer": {
        "ana1": {
          "type": "custom",
          "tokenizer": "whitespace",
          "filter": ["payfilt", "lowercase"]
        },
        "lat_ana": {
          "type": "custom",
          "tokenizer": "whitespace",
          "filter": ["lat_filter", "lowercase"]
        }
      },
      "filter": {
        "payfilt": {
          "type": "delimited_payload",
          "delimiter": "|",
          "encoding": "float"
        },
        "lat_filter": {
          "type": "lattice",
          "lattice_format": "audio",
          "audio_position_increment_seconds": 0.5
        }
      }
    }
  },
  "mappings": {
    "dynamic": "strict",
    "properties": {
      "thetext": {
        "type": "text",
        "analyzer": "ana1",
        "term_vector": "with_positions_offsets_payloads"
      },
      "lattices": {
        "type": "lattice",
        "lattice_format": "audio",
        "audio_position_increment_seconds": 0.5,
        "analyzer": "lat_ana",
        "term_vector": "with_positions_offsets_payloads"
      }
    }
  }
}'

curl -H 'Content-Type: application/json' -XPUT "http://localhost:9200/mytest2?pretty" -d '{
  "settings": {
    "index": {
      "number_of_shards": 1,
      "number_of_replicas": 0
    },
    "analysis": {
      "analyzer": {
        "ana1": {
          "type": "custom",
          "tokenizer": "whitespace",
          "filter": ["payfilt", "lowercase"]
        },
        "lat_ana": {
          "type": "custom",
          "tokenizer": "whitespace",
          "filter": ["lat_filter", "lowercase"]
        }
      },
      "filter": {
        "payfilt": {
          "type": "delimited_payload",
          "delimiter": "|",
          "encoding": "float"
        },
        "lat_filter": {
          "type": "lattice",
          "lattice_format": "audio",
          "audio_position_increment_seconds": 0.1
        }
      }
    }
  },
  "mappings": {
    "dynamic": "strict",
    "properties": {
      "thetext": {
        "type": "text",
        "analyzer": "ana1",
        "term_vector": "with_positions_offsets_payloads"
      },
      "lattices": {
        "type": "lattice",
        "lattice_format": "audio",
        "audio_position_increment_seconds": 0.1,
        "analyzer": "lat_ana",
        "term_vector": "with_positions_offsets_payloads"
      }
    }
  }
}'

curl -XGET "http://localhost:9200/_cluster/health?wait_for_status=green&timeout=120s&pretty"

curl -XPOST -H 'Content-Type: application/json' 'http://localhost:9200/mytest/_doc/12345?pretty' -d '{
  "lattices": "quick|0|0|1.0|0.0|0.5 brown|1|0|1.0|1.5|1.7 fox|2|0|1.0|2.5|3.0 box|2|0|1.0|2.5|3.0"
}'

curl -XPOST -H 'Content-Type: application/json' 'http://localhost:9200/mytest2/_doc/12345?pretty' -d '{
  "lattices": "quick|0|0|1.0|0.0|0.5 brown|1|0|1.0|1.5|1.7 fox|2|0|1.0|2.5|3.0 box|2|0|1.0|2.5|3.0"
}'
