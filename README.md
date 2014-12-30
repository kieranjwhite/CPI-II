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

Please ensure that document collection is indexed appropriately with MainIndexCondcutus beforehand. For example you might want to specify a different type of ngram.

This command lists frequencies of stemmed trigrams in indexed document collection.

java -ea -cp lib/jackson-annotations-2.4.2.jar:lib/jackson-core-2.4.2.jar:lib/jackson-databind-2.4.2.jar:lib/lucene-core-4.10.1.jar:lib/lucene-analyzers-common-4.10.1.jar:lib/lucene-analyzers-stempel-4.10.1.jar:lib/lucene-expressions-4.10.1.jar:lib/lucene-queries-4.10.1.jar:lib/lucene-facet-4.10.1.jar:lib/lucene-queryparser-4.10.1.jar:lib/commons-lang3-3.3.2.jar:bin:data com.hourglassapps.cpi_ii.MainListIndexTerms --display-freqs > misc/tokens/trigrams_stemmed_freqs.txt

The following lists all ngram (in this case 3-gram) stem groups. All morphological variations in the collection for each term are listed.

java -ea -cp lib/jackson-annotations-2.4.2.jar:lib/jackson-core-2.4.2.jar:lib/jackson-databind-2.4.2.jar:lib/lucene-core-4.10.1.jar:libucene-analyzers-common-4.10.1.jar:lib/lucene-analyzers-stempel-4.10.1.jar:lib/lucene-expressions-4.10.1.jar:lib/lucene-queries-4.10.1.jar:lib/lucene-facet-4.10.1.jar:lib/lucene-queryparser-4.10.1.jar:lib/commons-lang3-3.3.2.jar:bin:data com.hourglassapps.cpi_ii.MainListIndexTerms misc/stem_maps/3grams.dat

-----------------------------------------------------------------

To generate list of documents containing all n-grams (and a selection of their morphological variations) in conductus 
=====================================================================================================================

For a dummy run generating all queries: 

java -ea -cp lib/jackson-annotations-2.4.2.jar:lib/jackson-core-2.4.2.jar:lib/jackson-databind-2.4.2.jar:lib/lucene-core-4.10.1.jar:lib/lucene-analyzers-common-4.10.1.jar:lib/lucene-analyzers-stempel-4.10.1.jar:lib/lucene-expressions-4.10.1.jar:lib/lucene-queries-4.10.1.jar:lib/lucene-facet-4.10.1.jar:lib/lucene-queryparser-4.10.1.jar:lib/commons-lang3-3.3.2.jar:lib/commons-logging-1.1.3.jar:lib/httpclient-4.3.5.jar:lib/httpcore-4.3.2.jar:lib/httpasyncclient-4.0.2.jar:lib/httpcore-nio-4.3.2.jar:lib/commons-codec-1.9.jar:lib/commons-io-2.4.jar:bin:data com.hourglassapps.cpi_ii.web_search.MainDownloader all misc/stem_maps/3grams.dat

To actually send queries to Bing and save results to journal/:

java -ea -cp lib/jackson-annotations-2.4.2.jar:lib/jackson-core-2.4.2.jar:lib/jackson-databind-2.4.2.jar:lib/lucene-core-4.10.1.jar:lib/lucene-analyzers-common-4.10.1.jar:lib/lucene-analyzers-stempel-4.10.1.jar:lib/lucene-expressions-4.10.1.jar:lib/lucene-queries-4.10.1.jar:lib/lucene-facet-4.10.1.jar:lib/lucene-queryparser-4.10.1.jar:lib/commons-lang3-3.3.2.jar:lib/commons-logging-1.1.3.jar:lib/httpclient-4.3.5.jar:lib/httpcore-4.3.2.jar:lib/httpasyncclient-4.0.2.jar:lib/httpcore-nio-4.3.2.jar:lib/commons-codec-1.9.jar:lib/commons-io-2.4.jar:bin:data com.hourglassapps.cpi_ii.web_search.MainDownloader all --real misc/stem_maps/3grams.dat

To partition queries between a number of different processes and download results: 

java -ea -cp lib/jackson-annotations-2.4.2.jar:lib/jackson-core-2.4.2.jar:lib/jackson-databind-2.4.2.jar:lib/lucene-core-4.10.1.jar:lib/lucene-analyzers-common-4.10.1.jar:lib/lucene-analyzers-stempel-4.10.1.jar:lib/lucene-expressions-4.10.1.jar:lib/lucene-queries-4.10.1.jar:lib/lucene-facet-4.10.1.jar:lib/lucene-queryparser-4.10.1.jar:lib/commons-lang3-3.3.2.jar:lib/commons-logging-1.1.3.jar:lib/httpclient-4.3.5.jar:lib/httpcore-4.3.2.jar:lib/httpasyncclient-4.0.2.jar:lib/httpcore-nio-4.3.2.jar:lib/commons-codec-1.9.jar:lib/commons-io-2.4.jar:bin:data com.hourglassapps.cpi_ii.web_search.MainDownloader partition misc/stem_maps/3grams.dat 3 0|1|2

Multithreaded Bing search and download:

java -ea -cp lib/jackson-annotations-2.4.2.jar:lib/jackson-core-2.4.2.jar:lib/jackson-databind-2.4.2.jar:lib/lucene-core-4.10.1.jar:lib/lucene-analyzers-common-4.10.1.jar:lib/lucene-analyzers-stempel-4.10.1.jar:lib/lucene-expressions-4.10.1.jar:lib/lucene-queries-4.10.1.jar:lib/lucene-facet-4.10.1.jar:lib/lucene-queryparser-4.10.1.jar:lib/commons-lang3-3.3.2.jar:lib/commons-logging-1.1.3.jar:lib/httpclient-4.3.5.jar:lib/httpcore-4.3.2.jar:lib/httpasyncclient-4.0.2.jar:lib/httpcore-nio-4.3.2.jar:lib/commons-codec-1.9.jar:lib/commons-io-2.4.jar:lib/tika-app-1.6.jar:bin:data com.hourglassapps.cpi_ii.web_search.MainDownloader threads misc/stem_maps/3grams.dat 2

To download a random selection of query results: 

java -ea -cp lib/jackson-annotations-2.4.2.jar:lib/jackson-core-2.4.2.jar:lib/jackson-databind-2.4.2.jar:lib/lucene-core-4.10.1.jar:lib/lucene-analyzers-common-4.10.1.jar:lib/lucene-analyzers-stempel-4.10.1.jar:lib/lucene-expressions-4.10.1.jar:lib/lucene-queries-4.10.1.jar:lib/lucene-facet-4.10.1.jar:lib/lucene-queryparser-4.10.1.jar:lib/commons-lang3-3.3.2.jar:lib/commons-logging-1.1.3.jar:lib/httpclient-4.3.5.jar:lib/httpcore-4.3.2.jar:lib/httpasyncclient-4.0.2.jar:lib/httpcore-nio-4.3.2.jar:lib/commons-codec-1.9.jar:lib/commons-io-2.4.jar:bin:data com.hourglassapps.cpi_ii.web_search.MainDownloader random misc/stem_maps/3grams.dat 123456

To download a link to a specified file:

java -ea -cp lib/jackson-annotations-2.4.2.jar:lib/jackson-core-2.4.2.jar:lib/jackson-databind-2.4.2.jar:lib/lucene-core-4.10.1.jar:lib/lucene-analyzers-common-4.10.1.jar:lib/lucene-analyzers-stempel-4.10.1.jar:lib/lucene-expressions-4.10.1.jar:lib/lucene-queries-4.10.1.jar:lib/lucene-facet-4.10.1.jar:lib/lucene-queryparser-4.10.1.jar:lib/commons-lang3-3.3.2.jar:lib/commons-logging-1.1.3.jar:lib/httpclient-4.3.5.jar:lib/httpcore-4.3.2.jar:lib/httpasyncclient-4.0.2.jar:lib/httpcore-nio-4.3.2.jar:lib/commons-codec-1.9.jar:bin:lib/commons-io-2.4.jar:data com.hourglassapps.cpi_ii.web_search.MainDownloader download <URL> <FILE>

To download results for a single query to test_journal/:

echo https://api.datamarket.azure.com/Bing/SearchWeb/Web?Query=%27%28%22communis%20est%20utilitas%22%20OR%20%22commune%20est%20utilitas%22%20OR%20%22communem%20est%20utilitas%22%29%20AND%20NOT%20site%3Aarchive.org%2Fdetails%2Fanalectahymnica21drevuoft%20AND%20NOT%20site%3Adiamm.ac.uk%20AND%20NOT%20site%3Aarchive.org%2Fdetails%2Fanalectahymnicam21drev%20AND%20NOT%20site%3Aarchive.org%2Fdetails%2Fanalectahymnicam20drev%20AND%20NOT%20site%3Acatalogue.conductus.ac.uk%20AND%20NOT%20site%3Achmtl.indiana.edu%2Ftml%22%27 | java -ea -cp lib/jackson-annotations-2.4.2.jar:lib/jackson-core-2.4.2.jar:lib/jackson-databind-2.4.2.jar:lib/lucene-core-4.10.1.jar:lib/lucene-analyzers-common-4.10.1.jar:lib/lucene-analyzers-stempel-4.10.1.jar:lib/lucene-expressions-4.10.1.jar:lib/lucene-queries-4.10.1.jar:lib/lucene-facet-4.10.1.jar:lib/lucene-queryparser-4.10.1.jar:lib/commons-lang3-3.3.2.jar:lib/commons-logging-1.1.3.jar:lib/httpclient-4.3.5.jar:lib/httpcore-4.3.2.jar:lib/httpasyncclient-4.0.2.jar:lib/httpcore-nio-4.3.2.jar:lib/commons-codec-1.9.jar:lib/commons-io-2.4.jar:bin:data com.hourglassapps.cpi_ii.web_search.MainDownloader one "communis est utilitas"

Ping google regularly to check for local network issues.
=======================================================
This relies on the jpingy library, which only works on unix-like systems. Running this might not be required on Windows as I believe if a network interface goes down on Windows any TCP connections are closed, quitting any MainDownloader process. The arguments below correspond to PIDs of processes you wish to kill if a ping fails.

java -ea -cp lib/jackson-annotations-2.4.2.jar:lib/jackson-core-2.4.2.jar:lib/jackson-databind-2.4.2.jar:lib/lucene-core-4.10.1.jar:lib/lucene-analyzers-common-4.10.1.jar:lib/lucene-analyzers-stempel-4.10.1.jar:lib/lucene-expressions-4.10.1.jar:lib/lucene-queries-4.10.1.jar:lib/lucene-facet-4.10.1.jar:lib/lucene-queryparser-4.10.1.jar:lib/commons-lang3-3.3.2.jar:lib/commons-logging-1.1.3.jar:lib/httpclient-4.3.5.jar:lib/httpcore-4.3.2.jar:lib/httpasyncclient-4.0.2.jar:lib/httpcore-nio-4.3.2.jar:lib/commons-codec-1.9.jar:lib/commons-io-2.4.jar:bin:data com.hourglassapps.util.MainHeartBeat <PID_1> <PID_2> <PID_3>

--------------------------------------------------------

Generating the file misc/links.txt
==================================

First create journal by invoking "MainDownloader all" as described above with a RNG seed of 234567. Then:

find correct_journal/completed/ -iname "_*"| xargs cat | sed -r -e "s/https/http/"|sort > misc/correct_links.txt

Listing queries containing downloaded links
=========================================

find journal/completed -iname "_[a-z]*"|xargs ls -l |egrep -e "kieran +[1-9]"|sed -nr -e "s/^.* ([^ ]+)\$/\1/p"

Display random query in a journal directory
===========================================

We are assuming 60 queries containing downloaded links...
find ./ -iname "_[a-z]*"|xargs ls -l |egrep -e "kieran +[1-9]"|sed -nr -e "s/^.* ([^ ]+)\$/\1/p"|rnd_line 60

Decode a percent-encoded string
===============================

java -ea -cp bin:data com.hourglassapps.util.URLUtils decode %27%28%22vita%20gaudium%20nos%22%20OR%20%22vita%20gaudia%20nos%22%20OR%20%22vita%20gaudio%20nos%22%20OR%20%22vitam%20gaudia%20nos%22%20OR%20%22vitam%20gaudio%20nos%22%20OR%20%22vitam%20gaudium%20nos%22%20OR%20%22vita%20gaudiis%20nos%22%20OR%20%22vitam%20gaudiis%20nos%22%29%27

-------------------------------------------------------

Indexing downloaded files
=========================

The following command will create an index in the 'downloaded_index' subdirectory of the working directory:

java -Xmx1g -ea -cp lib/jackson-annotations-2.4.2.jar:lib/jackson-core-2.4.2.jar:lib/jackson-databind-2.4.2.jar:lib/lucene-core-4.10.1.jar:lib/lucene-analyzers-common-4.10.1.jar:lib/lucene-analyzers-stempel-4.10.1.jar:lib/lucene-expressions-4.10.1.jar:lib/lucene-queries-4.10.1.jar:lib/lucene-facet-4.10.1.jar:lib/lucene-queryparser-4.10.1.jar:lib/commons-lang3-3.3.2.jar:lib/commons-logging-1.1.3.jar:lib/httpclient-4.3.5.jar:lib/httpcore-4.3.2.jar:lib/httpasyncclient-4.0.2.jar:lib/httpcore-nio-4.3.2.jar:lib/commons-codec-1.9.jar:lib/commons-io-2.4.jar:lib/tika-app-1.6.jar:bin:data com.hourglassapps.cpi_ii.MainIndexDownloaded ../../journals/random_no_blacklist_300_234567_journal/

More than one directory can listed as arguments, however they must all be *_journal directories (containing a completed subdir).

-------------------------------------------------------

Generate report
===============

java -ea -cp lib/jackson-annotations-2.4.2.jar:lib/jackson-core-2.4.2.jar:lib/jackson-databind-2.4.2.jar:lib/lucene-core-4.10.1.jar:lib/lucene-analyzers-common-4.10.1.jar:lib/lucene-analyzers-stempel-4.10.1.jar:lib/lucene-expressions-4.10.1.jar:lib/lucene-queries-4.10.1.jar:lib/lucene-facet-4.10.1.jar:lib/lucene-queryparser-4.10.1.jar:lib/commons-lang3-3.3.2.jar:lib/commons-logging-1.1.3.jar:lib/httpclient-4.3.5.jar:lib/httpcore-4.3.2.jar:lib/httpasyncclient-4.0.2.jar:lib/httpcore-nio-4.3.2.jar:lib/commons-codec-1.9.jar:lib/commons-io-2.4.jar:lib/tika-app-1.6.jar:bin:data com.hourglassapps.cpi_ii.web_search.MainDownloader threads misc/stem_maps/3grams.dat 3

To download a random selection of query results: 

java -ea -cp lib/jackson-annotations-2.4.2.jar:lib/jackson-core-2.4.2.jar:lib/jackson-databind-2.4.2.jar:lib/lucene-core-4.10.1.jar:lib/lucene-analyzers-common-4.10.1.jar:lib/lucene-analyzers-stempel-4.10.1.jar:lib/lucene-expressions-4.10.1.jar:lib/lucene-queries-4.10.1.jar:lib/lucene-facet-4.10.1.jar:lib/lucene-queryparser-4.10.1.jar:lib/commons-lang3-3.3.2.jar:lib/commons-logging-1.1.3.jar:lib/httpclient-4.3.5.jar:lib/httpcore-4.3.2.jar:lib/httpasyncclient-4.0.2.jar:lib/httpcore-nio-4.3.2.jar:lib/commons-codec-1.9.jar:lib/commons-io-2.4.jar:bin:data com.hourglassapps.cpi_ii.web_search.MainDownloader random misc/stem_maps/3grams.dat 123456

To download a link to a specified file:

java -ea -cp lib/jackson-annotations-2.4.2.jar:lib/jackson-core-2.4.2.jar:lib/jackson-databind-2.4.2.jar:lib/lucene-core-4.10.1.jar:lib/lucene-analyzers-common-4.10.1.jar:lib/lucene-analyzers-stempel-4.10.1.jar:lib/lucene-expressions-4.10.1.jar:lib/lucene-queries-4.10.1.jar:lib/lucene-facet-4.10.1.jar:lib/lucene-queryparser-4.10.1.jar:lib/commons-lang3-3.3.2.jar:lib/commons-logging-1.1.3.jar:lib/httpclient-4.3.5.jar:lib/httpcore-4.3.2.jar:lib/httpasyncclient-4.0.2.jar:lib/httpcore-nio-4.3.2.jar:lib/commons-codec-1.9.jar:bin:lib/commons-io-2.4.jar:data com.hourglassapps.cpi_ii.web_search.MainDownloader download <URL> <FILE>

To download results for a single query to test_journal/:

echo https://api.datamarket.azure.com/Bing/SearchWeb/Web?Query=%27%28%22communis%20est%20utilitas%22%20OR%20%22commune%20est%20utilitas%22%20OR%20%22communem%20est%20utilitas%22%29%20AND%20NOT%20site%3Aarchive.org%2Fdetails%2Fanalectahymnica21drevuoft%20AND%20NOT%20site%3Adiamm.ac.uk%20AND%20NOT%20site%3Aarchive.org%2Fdetails%2Fanalectahymnicam21drev%20AND%20NOT%20site%3Aarchive.org%2Fdetails%2Fanalectahymnicam20drev%20AND%20NOT%20site%3Acatalogue.conductus.ac.uk%20AND%20NOT%20site%3Achmtl.indiana.edu%2Ftml%22%27 | java -ea -cp lib/jackson-annotations-2.4.2.jar:lib/jackson-core-2.4.2.jar:lib/jackson-databind-2.4.2.jar:lib/lucene-core-4.10.1.jar:lib/lucene-analyzers-common-4.10.1.jar:lib/lucene-analyzers-stempel-4.10.1.jar:lib/lucene-expressions-4.10.1.jar:lib/lucene-queries-4.10.1.jar:lib/lucene-facet-4.10.1.jar:lib/lucene-queryparser-4.10.1.jar:lib/commons-lang3-3.3.2.jar:lib/commons-logging-1.1.3.jar:lib/httpclient-4.3.5.jar:lib/httpcore-4.3.2.jar:lib/httpasyncclient-4.0.2.jar:lib/httpcore-nio-4.3.2.jar:lib/commons-codec-1.9.jar:lib/commons-io-2.4.jar:bin:data com.hourglassapps.cpi_ii.web_search.MainDownloader one "communis est utilitas"

Ping google regularly to check for local network issues.
=======================================================
This relies on the jpingy library, which only works on unix-like systems. Running this might not be required on Windows as I believe if a network interface goes down on Windows any TCP connections are closed, quitting any MainDownloader process. The arguments below correspond to PIDs of processes you wish to kill if a ping fails.

java -ea -cp lib/jackson-annotations-2.4.2.jar:lib/jackson-core-2.4.2.jar:lib/jackson-databind-2.4.2.jar:lib/lucene-core-4.10.1.jar:lib/lucene-analyzers-common-4.10.1.jar:lib/lucene-analyzers-stempel-4.10.1.jar:lib/lucene-expressions-4.10.1.jar:lib/lucene-queries-4.10.1.jar:lib/lucene-facet-4.10.1.jar:lib/lucene-queryparser-4.10.1.jar:lib/commons-lang3-3.3.2.jar:lib/commons-logging-1.1.3.jar:lib/httpclient-4.3.5.jar:lib/httpcore-4.3.2.jar:lib/httpasyncclient-4.0.2.jar:lib/httpcore-nio-4.3.2.jar:lib/commons-codec-1.9.jar:lib/commons-io-2.4.jar:bin:data com.hourglassapps.util.MainHeartBeat <PID_1> <PID_2> <PID_3>

--------------------------------------------------------

Generating the file misc/links.txt
==================================

First create journal by invoking "MainDownloader all" as described above with a RNG seed of 234567. Then:

find correct_journal/completed/ -iname "_*"| xargs cat | sed -r -e "s/https/http/"|sort > misc/correct_links.txt

Listing queries containing downloaded links
=========================================

find journal/completed -iname "_[a-z]*"|xargs ls -l |egrep -e "kieran +[1-9]"|sed -nr -e "s/^.* ([^ ]+)\$/\1/p"

Display random query in a journal directory
===========================================

We are assuming 60 queries containing downloaded links...
find ./ -iname "_[a-z]*"|xargs ls -l |egrep -e "kieran +[1-9]"|sed -nr -e "s/^.* ([^ ]+)\$/\1/p"|rnd_line 60

Decode a percent-encoded string
===============================

java -ea -cp bin:data com.hourglassapps.util.URLUtils decode %27%28%22vita%20gaudium%20nos%22%20OR%20%22vita%20gaudia%20nos%22%20OR%20%22vita%20gaudio%20nos%22%20OR%20%22vitam%20gaudia%20nos%22%20OR%20%22vitam%20gaudio%20nos%22%20OR%20%22vitam%20gaudium%20nos%22%20OR%20%22vita%20gaudiis%20nos%22%20OR%20%22vitam%20gaudiis%20nos%22%29%27

-------------------------------------------------------

Generate report
===============

java -ea -cp lib/jackson-annotations-2.4.2.jar:lib/jackson-core-2.4.2.jar:lib/jackson-databind-2.4.2.jar:lib/lucene-core-4.10.1.jar:lib/lucene-analyzers-common-4.10.1.jar:lib/lucene-analyzers-stempel-4.10.1.jar:lib/lucene-expressions-4.10.1.jar:lib/lucene-queries-4.10.1.jar:lib/lucene-facet-4.10.1.jar:lib/lucene-queryparser-4.10.1.jar:lib/commons-lang3-3.3.2.jar:lib/commons-logging-1.1.3.jar:lib/httpclient-4.3.5.jar:lib/httpcore-4.3.2.jar:lib/httpasyncclient-4.0.2.jar:lib/httpcore-nio-4.3.2.jar:lib/commons-codec-1.9.jar:lib/commons-io-2.4.jar:lib/tika-app-1.6.jar:bin:data com.hourglassapps.cpi_ii.report.MainReporter <CONDUCTUS_XML_EXPORT>

-------------------------------------------------------

Decode Base64 HTML HashIds from Report
=================================

Links to results / documents in the report are JSON objects encoded as Base64 strings. In this way arguments are passed to the appropriate script. We rely on hash ids to pass arguments since currently the report is viewed with a browser running on the same computer as the report (as opposed to retrieving pages from a server). Viewing the report locally means we can't pass arguments with the usual URL encoding method.

java -ea -cp lib/jackson-annotations-2.4.2.jar:lib/jackson-core-2.4.2.jar:lib/jackson-databind-2.4.2.jar:lib/lucene-core-4.10.1.jar:lib/lucene-analyzers-common-4.10.1.jar:lib/lucene-analyzers-stempel-4.10.1.jar:lib/lucene-expressions-4.10.1.jar:lib/lucene-queries-4.10.1.jar:lib/lucene-facet-4.10.1.jar:lib/lucene-queryparser-4.10.1.jar:lib/commons-lang3-3.3.2.jar:lib/commons-logging-1.1.3.jar:lib/httpclient-4.3.5.jar:lib/httpcore-4.3.2.jar:lib/httpasyncclient-4.0.2.jar:lib/httpcore-nio-4.3.2.jar:lib/commons-codec-1.9.jar:lib/commons-io-2.4.jar:lib/tika-app-1.6.jar:bin:data com.hourglassapps.persist.MainHashTagDict J2eyJ0IjoiQWRqYWNlbnQ6IExlY3RpbyIsImYiOiJlcHJpbnRpZF8yNDYwX3NpbmdsZV83NDAifQ