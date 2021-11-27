# Simple Search Engine

*Note:* this project is currently not working well and I will be fixing and tweaking it during the following months.

## Description
This project is part of the Information Retrieval course by Sara Cohen in The Hebrew University of Jerusalem.  
It is a relatively simple indexed dictionary and search engine for reviews supplied by the Stanford Large Network Dataset Collection (http://snap.stanford.edu/data/index.html).

### Initial Requirements:
The dictionary should be built in reasonable time.  
The dictionary should take as little space as possible.  
The search engine should be able to answer some specific typess of queries in reasonable time.

### Implementation:
Dictionary storage is done with a simple Concatenated String technique.  
Index list compression is done with VarInt technique.

## Extra notes:
This is currently a personal project, I am not accepting contributions at this time.  
In order to analyze disc space used during index construction, temporary construction files are not deleted automatically.
