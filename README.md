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
that location.

![Location aligned lattice example](/doc/open_fst_lattice_example.svg)
