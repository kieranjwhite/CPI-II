To index ../../data/CPI-poem.json:

java -ea -cp lib/jackson-annotations-2.4.2.jar:lib/jackson-core-2.4.2.jar:lib/jackson-databind-2.4.2.jar:lib/lucene-core-4.10.1.jar:lib/lucene-analyzers-common-4.10.1.jar:lib/lucene-core-4.10.1.jar:lib/lucene-analyzers-stempel-4.10.1.jar:lib/lucene-expressions-4.10.1.jar:lib/lucene-queries-4.10.1.jar:lib/lucene-facet-4.10.1.jar:lib/lucene-queryparser-4.10.1.jar:lib/commons-lang3-3.3.2.jar:bin:data com.hourglassapps.cpi_ii.MainIndexConductus ../../data/CPI-poem.json

switches:

--display-words
Lists stem groups of individual words

--display-ngrams
List stem groups of n-grams

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

To stem Latin terms on standard input using the Snowball Latin
stemmer, displaying the result to standard output:

java -ea -cp lib/jackson-annotations-2.4.2.jar:lib/jackson-core-2.4.2.jar:lib/jackson-databind-2.4.2.jar:lib/lucene-core-4.10.1.jar:lib/lucene-analyzers-common-4.10.1.jar:lib/lucene-expressions-4.10.1.jar:lib/lucene-queries-4.10.1.jar:lib/lucene-facet-4.10.1.jar:lib/lucene-queryparser-4.10.1.jar:lib/commons-lang3-3.3.2.jar:bin:data com.hourglassapps.cpi_ii.stem.snowball.tartarus.MainTestApp - -


-----------------------------------------------------------------

To generate a stemming model for Stempel stemmer:

grep ../../treebank/perseus_treebank/1.5/data/*.xml -e "lemma" -h|tr '[:upper:]' '[:lower:]'|sed -nr -e "s/^.* form=\"([^\"]*)\" lemma=\"([^\"]*)\".*$/\\2 \\1/p"| grep -v "[^a-zA-Z0-9 ]" |sort -u|sed -nr -e "s/^(.*) (.*)$/\\1\\n\\2/p" | java -ea -cp lib/jackson-annotations-2.4.2.jar:lib/jackson-core-2.4.2.jar:lib/jackson-databind-2.4.2.jar:lib/lucene-core-4.10.1.jar:lib/lucene-analyzers-common-4.10.1.jar:lib/lucene-expressions-4.10.1.jar:lib/lucene-queries-4.10.1.jar:lib/lucene-facet-4.10.1.jar:lib/lucene-queryparser-4.10.1.jar:lib/commons-lang3-3.3.2.jar:bin:data com.hourglassapps.cpi_ii.stem.MainGenStempelModel - data/com/hourglassapps/cpi_ii/latin/stem/stempel/model

-----------------------------------------------------------------

To list all terms and their frequencies in the collection

java -ea -cp lib/jackson-annotations-2.4.2.jar:lib/jackson-core-2.4.2.jar:lib/jackson-databind-2.4.2.jar:lib/lucene-core-4.10.1.jar:lib/lucene-analyzers-common-4.10.1.jar:lib/lucene-core-4.10.1.jar:lib/lucene-analyzers-stempel-4.10.1.jar:lib/lucene-expressions-4.10.1.jar:lib/lucene-queries-4.10.1.jar:lib/lucene-facet-4.10.1.jar:lib/lucene-queryparser-4.10.1.jar:lib/commons-lang3-3.3.2.jar:bin:data com.hourglassapps.cpi_ii.MainListIndexTerms > misc/tokens/<some_filename>
