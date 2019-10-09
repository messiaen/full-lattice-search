#!/usr/bin/env bash

curl -XGET -H 'Content-Type: application/json' 'http://localhost:9200/mytest?pretty' -d '
{
  "query": {
    "match_lattice": {
      "lattices": {
        "query": "quick brown fox",
        "slop": 0,
        "slop_seconds": 2.5,
        "include_span_score": "false",
        "payload_function": "default",
        "in_order": "true"
      }
    }
  }
}'
