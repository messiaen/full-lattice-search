#!/bin/bash

curl -XGET "http://localhost:9200/_cluster/health?wait_for_status=green&timeout=120s&pretty"

curl -XPUT "http://localhost:9200/mytest?pretty" -H 'Content-Type: application/json' -d'{  "settings": {    "index": {      "number_of_shards": 1,      "number_of_replicas": 0    },    "analysis": {      "analyzer": {        "ana1": {          "type": "custom",          "tokenizer": "whitespace",          "filter": ["payfilt", "lowercase"]        }      },      "filter": {        "payfilt": {          "type": "delimited_payload",          "delimiter": "|",          "encoding": "float"        }      }    }  },  "mappings": {    "dynamic": "strict",    "properties": {      "thetext": {        "type": "text",        "analyzer": "ana1",        "term_vector": "with_positions_offsets_payloads"      }    }  }}'

curl -XPOST "http://localhost:9200/mytest/_doc/12345?pretty" -H 'Content-Type: application/json' -d'{  "thetext": "the|1.0 quick|1.0 fox|1.0 foo|1.0 quick|0.5 foo|1.0 fox|0.5"}'

curl -XPOST "http://localhost:9200/mytest/_doc/9876?pretty" -H 'Content-Type: application/json' -d'{  "thetext": "the|0.001 quick|0.001 fox|0.001 quick|0.01 fox|0.001"}'

curl -XGET "http://localhost:9200/mytest/_termvectors/12345?pretty"

