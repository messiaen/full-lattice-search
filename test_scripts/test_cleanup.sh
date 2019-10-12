#!/usr/bin/env bash

curl -XDELETE "http://localhost:9200/mytest?pretty"
curl -XDELETE "http://localhost:9200/mytest2?pretty"
