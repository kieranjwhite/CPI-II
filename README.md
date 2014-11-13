CPI-II
======

To index ../../data/CPI-poem.json:
=================================
java -ea -cp lib/jackson-annotations-2.4.2.jar:lib/jackson-core-2.4.2.jar:lib/jackson-databind-2.4.2.jar:lib/lucene-core-4.10.1.jar:lib/lucene-analyzers-common-4.10.1.jar:lib/lucene-analyzers-stempel-4.10.1.jar:lib/lucene-expressions-4.10.1.jar:lib/lucene-queries-4.10.1.jar:lib/lucene-facet-4.10.1.jar:lib/lucene-queryparser-4.10.1.jar:lib/commons-lang3-3.3.2.jar:bin:data com.hourglassapps.cpi_ii.MainIndexConductus ../../data/CPI-poem.json --serialise > misc/stem_maps/3grams.dat

switches:

--display-words
Lists stem groups of individual words

--serialise
Serialises stem groups to standard out

To give sorted list of TF-IDF scores of all tokens in poem with
eprintid of 756:

java -ea -cp lib/jackson-annotations-2.4.2.jar:lib/jackson-core-2.4.2.jar:lib/jackson-databind-2.4.2.jar:lib/lucene-core-4.10.1.jar:lib/lucene-analyzers-common-4.10.1.jar:lib/lucene-expressions-4.10.1.jar:lib/lucene-queries-4.10.1.jar:lib/lucene-facet-4.10.1.jar:lib/lucene-queryparser-4.10.1.jar:lib/commons-lang3-3.3.2.jar:bin:data com.hourglassapps.cpi_ii.MainListTerms 756

Please note that n-gram size is set by the constant
com.hourglassapps.cpi_ii.ConductusIndex.NGRAM_SIZE. 

Also it is important to appreciate that the stemmer creates different
stems for nouns and verbs and must be informed of a term's
part-of-speech. Currently this is set by the
com.hourglassapps.cpi_ii.latin.LatinStemmer.ASSUMED_POS constant and
is therefore fixed for all tokens.

-----------------------------------------------------------------

To stem Latin terms on standard input using the Schinke Snowball Latin stemmer, displaying the result or noun and verb stemming to standard output:
=================================================================================================================================================

java -ea -cp lib/jackson-annotations-2.4.2.jar:lib/jackson-core-2.4.2.jar:lib/jackson-databind-2.4.2.jar:lib/lucene-core-4.10.1.jar:lib/lucene-analyzers-common-4.10.1.jar:lib/lucene-expressions-4.10.1.jar:lib/lucene-queries-4.10.1.jar:lib/lucene-facet-4.10.1.jar:lib/lucene-queryparser-4.10.1.jar:lib/commons-lang3-3.3.2.jar:bin:data com.hourglassapps.cpi_ii.stem.snowball.tartarus.MainTestApp - -

The MainStemTest class will test either Stempel or Schinke stemmer (it will however only return one stem and might not therefore be suitable for testing Schinke):

java -ea -cp lib/jackson-annotations-2.4.2.jar:lib/jackson-core-2.4.2.jar:lib/jackson-databind-2.4.2.jar:lib/lucene-core-4.10.1.jar:lib/lucene-analyzers-common-4.10.1.jar:lib/lucene-expressions-4.10.1.jar:lib/lucene-queries-4.10.1.jar:lib/lucene-facet-4.10.1.jar:lib/lucene-queryparser-4.10.1.jar:lib/commons-lang3-3.3.2.jar:lib/lucene-analyzers-stempel-4.10.1.jar:bin:data com.hourglassapps.cpi_ii.stem.MainStemTest - -

-----------------------------------------------------------------

To generate a stemming model for Stempel stemmer:
================================================

grep ../../treebank/perseus_treebank/1.5/data/*.xml -e "lemma" -h|tr '[:upper:]' '[:lower:]'|sed -nr -e "s/^.* form=\"([^\"]*)\" lemma=\"([^\"]*)\".*$/\\2 \\1/p"| grep -v "[^a-zA-Z0-9 ]" |sort -u|sed -nr -e "s/^(.*) (.*)$/\\1\\n\\2/p" | java -ea -cp lib/jackson-annotations-2.4.2.jar:lib/jackson-core-2.4.2.jar:lib/jackson-databind-2.4.2.jar:lib/lucene-core-4.10.1.jar:lib/lucene-analyzers-common-4.10.1.jar:lib/lucene-expressions-4.10.1.jar:lib/lucene-queries-4.10.1.jar:lib/lucene-facet-4.10.1.jar:lib/lucene-queryparser-4.10.1.jar:lib/commons-lang3-3.3.2.jar:bin:data com.hourglassapps.cpi_ii.stem.MainGenStempelModel - data/com/hourglassapps/cpi_ii/latin/stem/stempel/model

-----------------------------------------------------------------

To list all terms and their frequencies in the collection
=========================================================

java -ea -cp lib/jackson-annotations-2.4.2.jar:lib/jackson-core-2.4.2.jar:lib/jackson-databind-2.4.2.jar:lib/lucene-core-4.10.1.jar:lib/lucene-analyzers-common-4.10.1.jar:lib/lucene-analyzers-stempel-4.10.1.jar:lib/lucene-expressions-4.10.1.jar:lib/lucene-queries-4.10.1.jar:lib/lucene-facet-4.10.1.jar:lib/lucene-queryparser-4.10.1.jar:lib/commons-lang3-3.3.2.jar:bin:data com.hourglassapps.cpi_ii.MainListIndexTerms > misc/tokens/<some_filename>

-----------------------------------------------------------------

To generate list of documents containing all n-grams (and a selection of their morphological variations) in conductus 
=====================================================================================================================

For a dummy run generating all queries: 

java -ea -cp lib/jackson-annotations-2.4.2.jar:lib/jackson-core-2.4.2.jar:lib/jackson-databind-2.4.2.jar:lib/lucene-core-4.10.1.jar:lib/lucene-analyzers-common-4.10.1.jar:lib/lucene-analyzers-stempel-4.10.1.jar:lib/lucene-expressions-4.10.1.jar:lib/lucene-queries-4.10.1.jar:lib/lucene-facet-4.10.1.jar:lib/lucene-queryparser-4.10.1.jar:lib/commons-lang3-3.3.2.jar:lib/commons-logging-1.1.3.jar:lib/httpclient-4.3.5.jar:lib/httpcore-4.3.2.jar:lib/httpasyncclient-4.0.2.jar:lib/httpcore-nio-4.3.2.jar:lib/commons-codec-1.9.jar:bin:data com.hourglassapps.cpi_ii.web_search.MainDownloader all misc/stem_maps/3grams.dat

To actually send queries to bing and save results to journal/:

java -ea -cp lib/jackson-annotations-2.4.2.jar:lib/jackson-core-2.4.2.jar:lib/jackson-databind-2.4.2.jar:lib/lucene-core-4.10.1.jar:lib/lucene-analyzers-common-4.10.1.jar:lib/lucene-analyzers-stempel-4.10.1.jar:lib/lucene-expressions-4.10.1.jar:lib/lucene-queries-4.10.1.jar:lib/lucene-facet-4.10.1.jar:lib/lucene-queryparser-4.10.1.jar:lib/commons-lang3-3.3.2.jar:lib/commons-logging-1.1.3.jar:lib/httpclient-4.3.5.jar:lib/httpcore-4.3.2.jar:lib/httpasyncclient-4.0.2.jar:lib/httpcore-nio-4.3.2.jar:lib/commons-codec-1.9.jar:bin:data com.hourglassapps.cpi_ii.web_search.MainDownloader all --real misc/stem_maps/3grams.dat

To download a link to a specified file:

java -ea -cp lib/jackson-annotations-2.4.2.jar:lib/jackson-core-2.4.2.jar:lib/jackson-databind-2.4.2.jar:lib/lucene-core-4.10.1.jar:lib/lucene-analyzers-common-4.10.1.jar:lib/lucene-analyzers-stempel-4.10.1.jar:lib/lucene-expressions-4.10.1.jar:lib/lucene-queries-4.10.1.jar:lib/lucene-facet-4.10.1.jar:lib/lucene-queryparser-4.10.1.jar:lib/commons-lang3-3.3.2.jar:lib/commons-logging-1.1.3.jar:lib/httpclient-4.3.5.jar:lib/httpcore-4.3.2.jar:lib/httpasyncclient-4.0.2.jar:lib/httpcore-nio-4.3.2.jar:lib/commons-codec-1.9.jar:bin:data com.hourglassapps.cpi_ii.web_search.MainDownloader download <URL> <FILE>

To download results for a single query to test_journal/:

echo https://api.datamarket.azure.com/Bing/SearchWeb/Web?Query=%27%28%22communis%20est%20utilitas%22%20OR%20%22commune%20est%20utilitas%22%20OR%20%22communem%20est%20utilitas%22%29%20AND%20NOT%20site%3Aarchive.org%2Fdetails%2Fanalectahymnica21drevuoft%20AND%20NOT%20site%3Adiamm.ac.uk%20AND%20NOT%20site%3Aarchive.org%2Fdetails%2Fanalectahymnicam21drev%20AND%20NOT%20site%3Aarchive.org%2Fdetails%2Fanalectahymnicam20drev%20AND%20NOT%20site%3Acatalogue.conductus.ac.uk%20AND%20NOT%20site%3Achmtl.indiana.edu%2Ftml%22%27 | java -ea -cp lib/jackson-annotations-2.4.2.jar:lib/jackson-core-2.4.2.jar:lib/jackson-databind-2.4.2.jar:lib/lucene-core-4.10.1.jar:lib/lucene-analyzers-common-4.10.1.jar:lib/lucene-analyzers-stempel-4.10.1.jar:lib/lucene-expressions-4.10.1.jar:lib/lucene-queries-4.10.1.jar:lib/lucene-facet-4.10.1.jar:lib/lucene-queryparser-4.10.1.jar:lib/commons-lang3-3.3.2.jar:lib/commons-logging-1.1.3.jar:lib/httpclient-4.3.5.jar:lib/httpcore-4.3.2.jar:lib/httpasyncclient-4.0.2.jar:lib/httpcore-nio-4.3.2.jar:lib/commons-codec-1.9.jar:lib/commons-io-2.4.jar:bin:data com.hourglassapps.cpi_ii.web_search.MainDownloader one "communis est utilitas"

--------------------------------------------------------

Generating the file misc/links.txt
==================================

find correct_journal/completed/ -iname "_*"| xargs cat | sed -r -e "s/https/http/"|sort > misc/correct_links.txt
