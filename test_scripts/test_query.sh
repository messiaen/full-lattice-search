#!/usr/bin/env bash

curl -XGET "http://localhost:9200/mytest/_search?pretty" -H 'Content-Type: application/json' -d'{  "query": {    "match_lattice": {      "thetext": {        "query": "quick fox",        "slop": 1,        "include_span_score": "true",        "payload_function": "default"      }    }  }}'
