# Full Lattice Search

## Overview

This [Elasticsearch](https://github.com/elastic/elasticsearch) plugin enables search across probabilistic
lattice structures.  These lattices are in the form output by
Automated Speech Recognition
(ASR) or Speech-to-text (STT), Optical Character recognition (OCR), Machine Translation (MT), Automated Image Captioning,
etc.  The lattices, regardless of the analytic, can be viewed as the Finite State Machine (FST) structure below, 
where each set of arcs (transitioning from one state to another) represents a set of possible outputs at some location
in the source document (e.g. at the first location below, the possible outputs are 'the' and 'a'). 
In the case of STT the locations would be time ranges, in the case of OCR the locations could be x y
coordinates, or perhaps a reading order location.  Each possible output has an associated probability of occurrence at
that location allowing relevance scoring to be affected by the quality of the lattice output.

![Location aligned lattice example](/doc/open_fst_lattice_example.svg)

## Plugin

### LatticeTokenFilter
A token filter of type `lattice` that processes a lattice token stream.

Accepts tokens in one of two forms (see `lattice_format` parameter)

#### `lattice_format=lattice`
Tokens should be in the form 

`<token:string>|<position:int>|<rank:int>|<score:float>`

Example stream: `the|0|0|0.9`,  `quick|1|0|0.6`, `brick|1|1|0.2`, `fox|2|0|0.5`, `box|2|1|0.09`, `jumped|3|0|1.0`

In the example above the tokens `quick` and `brick` will be index at the same location, because they both have position
set to 1.

- `token` the actual string token to be searched against and to be processed be follow-on filters
- `position` is the global position of the token in the document 
  (used to determine if the token should be places at same location as the previous token)
- `rank` is the tokens rank relative to the other possible tokens at this position (0 is the most probable rank)
- `score` a float between 0.0 and 1.0 (inclusive), which is the probability of a this token at this position
  (higher is better). Note if you actually have a score of zero the token will not return is a search, and should
  probably be omitted from the stream.

#### `lattice_format=audio`
Tokens have all the fields from the `lattice` format with the addition of times

Tokens should be in the form 

`<token:string>|<position:int>|<rank:int>|<score:float>|<start_time:float>|<stop_time:float>`

Example stream: `the|0|0|0.9|0.15|0.25`,  `quick|1|0|0.6|0.25|0.5`, `brick|1|1|0.2|0.25|0.5`, `fox|2|0|0.5|1.0|1.3`, `box|2|1|0.09|1.0|1.3`, `jumped|3|0|1.0|2.0|2.5`

In the example above the tokens `quick` and `brick` will be index at the same location, because they both have position
set to 1.  The actual position of the tokens is determined by the times and `audio_position_increment_seconds`.

If `audio_position_increment_seconds=0.01` in the example above `the` would be indexed with a position of 15; 
`quick` and `brick` would be indexed at a position of 25; etc.

- `start_time` the start time in seconds of this token relative to the beginning of the source audio
- `stop_time` the start time in seconds of this token relative to the beginning of the source audio

Parameters include:
- `lattice_format` (default is lattice)
  - defines the fields in a lattice token either `audio` or `lattice`
  - allows positionIncrement to be affected by the distance between the tokens in the source document.  
    See `audio_position_increment_seconds`
- `score_buckets` (default is no duplication)
  - put duplicate tokens at the same position as the original token based on a score threshold.  This hacks the term
    frequency so that matches on higher scoring tokens will appear more relevant than lower scoring tokens
    (see limitations below).
  - for a value of `[0.9, 10, 0.8, 8, 0.7, 7, 0.2, 1]`, tokens with a score >= 0.9 will be duplicated 10 times; tokens
    with a score >= 0.8 will be duplicated 8 times, etc.
- `audio_position_increment_seconds` (default is 0.01)
  - for `lattice=format=audio` this is the precision at which the audio times are encoded into position in the index
  - position of a token will be `floor(token_start_time / audio_position_increment_seconds)`
 
 ### LatticeField
 
A field of type `lattice` holds parameters of LatticeTokenFilter for reference at search time. Functions like a `text`
field.

**If you use `lattice_format=audio` you need to use this fields type for query to work correctly with times.**

**Note:** this only exists because currently there does not seem to be a way to get the necessary (or any) information
from the analyzer at query time.  I think there could be a `getChainAware()` or similar method added to `AnalysisProvider`
functioning similar to `SynonymGraphTokenFilterFactory.getChainAwareTokenFilterFactory()` within `AnalysisRegistry`.

Parameters include:
- `lattice_format` must match the configuration of the `LatticeTokenFilter` set on this field.
- `audio_position_increment_seconds` must match the configuration of the `LatticeTokenFilter` set on this field.

### MatchLatticeQuery

A query of type `match_lattice` queries a `lattice` field configured with a `lattice` token filter.

Performs a `SpanNearQuery` wrapped in a `LatticePayloadScoreQuery` (extension of `PayloadScoreQuery`), which uses the
scores encoded in each token payload to score matching spans. The score from each span is summed to give the document
score.  If `include_span_score` is set, the score above is multiplied by the configured similarity score.

Parameters include:
- `slop` number of skipped tokens allowed in match
- `slop_seconds` used when `lattice_format=audio`. Maximum seconds the match is allowed to span.
- `in_order` whether the token must appear in order (should be `true` for `lattice_format=audio`)
- `include_span_score` if `true` the configured similarity score will be multiplied with the payload score (described above)
- `payload_function` one of `sum`, `max`, or `min`
  - `sum` sums the scores of matching spans
  - `max` selects the max score from all the matching spans
  - `min` selects the min score from all the matching spans
- `payload_length_norm_factor` a float defining how much the length of the matching span should normalize the span score.
  A value of one means that score are divided by the length of the span (Note this in not the width of the span in lucene terms).
  A value of 0 means there is no length normalization.
  
## Installation

### Development

#### Docker

`pull messiaen/full-text-search:latest`

The image is the official Elasticsearch with the full-text-search plugin installed

`docker-compose.yaml` example:

```yaml
version: "2"
services:
  kibana:
    image: docker.elastic.co/kibana/kibana:7.3.0
    ports:
      - 5601:5601
    environment:
      ELASTICSEARCH_HOSTS: http://es01:9200
  es01:
    image: registry.gitlab.com/hedgehogai/full-lattice-search/full-lattice-search:2.0.0-rc2-7.3.0
    environment:
      - node.name=es01
      - discovery.type=single-node
      - "ES_JAVA_OPTS=-Xms1024m -Xmx1024m"
    ulimits:
      memlock:
        soft: -1
        hard: -1
    volumes:
      - esdata01:/usr/share/elasticsearch/data
    ports:
      - 9200:9200

volumes:
  esdata01:
    driver: local
```

### Production

Requires [Elasticsearch 7.3.0](https://www.elastic.co/guide/en/elasticsearch/reference/7.3/install-elasticsearch.html)
(Support for other versions (>=6.0.0) coming soon / on request)

1. Download the appropriate release from [releases tab](https://github.com/messiaen/full-lattice-search/releases)
2. Install the plugin using the Elasticsearch docs [here](https://www.elastic.co/guide/en/elasticsearch/plugins/7.4/plugin-management-custom-url.html)
  or [here](https://www.elastic.co/guide/en/elasticsearch/plugins/7.4/_plugins_directory.html)
 
## Example Usage with [Kibana](https://www.elastic.co/guide/en/kibana/current/index.html)

### Usage with audio transcripts with times

```
PUT audio_lattices
{
  "settings": {
    "index": {
      "number_of_shards": 1,
      "number_of_replicas": 0
    },
    "analysis": {
      "analyzer": {
        "lattice_analyzer": {
          "type": "custom",
          "tokenizer": "whitespace",
          "filter": ["lattice_filter", "lowercase"]
        }
      },
      "filter": {
        "lattice_filter": {
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
      "lattices": {
        "type": "lattice",
        "lattice_format": "audio",
        "audio_position_increment_seconds": 0.1,
        "analyzer": "lattice_analyzer"
      }
    }
  }
}

POST audio_lattices/_doc/1
{
  "lattices": """the|0|0|0.9|0.15|0.25
  quick|1|0|0.6|0.25|0.5 brick|1|1|0.2|0.25|0.5
  fox|2|0|0.5|1.0|1.3 box|2|1|0.09|1.0|1.3
  jumped|3|0|1.0|2.0|2.5"""
}

GET audio_lattices/_search
{
  "query": {
    "match_lattice": {
      "lattices": {
      
        "query": "quick box jumped",
        "slop_seconds": 2,
        "include_span_score": "true",
        "payload_function": "sum",
        "in_order": "true"
      }
    }
  }
}
```

Search Response

```
{
  "took" : 1,
  "timed_out" : false,
  "_shards" : {
    "total" : 1,
    "successful" : 1,
    "skipped" : 0,
    "failed" : 0
  },
  "hits" : {
    "total" : {
      "value" : 1,
      "relation" : "eq"
    },
    "max_score" : 36.987705,
    "hits" : [
      {
        "_index" : "audio_lattices",
        "_type" : "_doc",
        "_id" : "1",
        "_score" : 36.987705,
        "_source" : {
          "lattices" : """
the|0|0|0.9|0.15|0.25
  quick|1|0|0.6|0.25|0.5 brick|1|1|0.2|0.25|0.5
  fox|2|0|0.5|1.0|1.3 box|2|1|0.09|1.0|1.3
  jumped|3|0|1.0|2.0|2.5
"""
        }
      }
    ]
  }
}
```

### Usage with text transcripts with position, rank, and score only

```
{
  "settings": {
    "index": {
      "number_of_shards": 1,
      "number_of_replicas": 0
    },
    "analysis": {
      "analyzer": {
        "lattice_analyzer": {
          "type": "custom",
          "tokenizer": "whitespace",
          "filter": ["lattice_filter", "lowercase"]
        }
      },
      "filter": {
        "lattice_filter": {
          "type": "lattice",
          "lattice_format": "lattice"
        }
      }
    }
  },
  "mappings": {
    "dynamic": "strict",
    "properties": {
      "lattices": {
        "type": "lattice",
        "lattice_format": "lattice",
        "analyzer": "lattice_analyzer"
      }
    }
  }
}

POST text_lattices/_doc/1
{
  "lattices": """the|0|0|0.9
  quick|1|0|0.6 brick|1|1|0.2
  fox|2|0|0.5 box|2|1|0.09
  jumped|3|0|1.0"""
}

GET text_lattices/_search
{
  "query": {
    "match_lattice": {
      "lattices": {
      
        "query": "quick jumped",
        "slop": 1,
        "include_span_score": "true",
        "payload_function": "sum",
        "in_order": "true"
      }
    }
  }
}
```

Search Response

```
{
  "took" : 0,
  "timed_out" : false,
  "_shards" : {
    "total" : 1,
    "successful" : 1,
    "skipped" : 0,
    "failed" : 0
  },
  "hits" : {
    "total" : {
      "value" : 1,
      "relation" : "eq"
    },
    "max_score" : 9041.438,
    "hits" : [
      {
        "_index" : "text_lattices",
        "_type" : "_doc",
        "_id" : "1",
        "_score" : 9041.438,
        "_source" : {
          "lattices" : """
the|0|0|0.9
  quick|1|0|0.6 brick|1|1|0.2
  fox|2|0|0.5 box|2|1|0.09
  jumped|3|0|1.0
"""
        }
      }
    ]
  }
}
```
