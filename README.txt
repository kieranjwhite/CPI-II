To index ../../data/CPI-poem.json:

java -ea -cp lib/jackson-annotations-2.4.2.jar:lib/jackson-core-2.4.2.jar:lib/jackson-databind-2.4.2.jar:lib/lucene-core-4.10.1.jar:lib/lucene-analyzers-common-4.10.1.jar:lib/lucene-core-4.10.1.jar:lib/lucene-analyzers-stempel-4.10.1.jar:lib/lucene-expressions-4.10.1.jar:lib/lucene-queries-4.10.1.jar:lib/lucene-facet-4.10.1.jar:lib/lucene-queryparser-4.10.1.jar:lib/commons-lang3-3.3.2.jar:bin com.hourglassapps.cpi_ii.MainIndexConductus ../../data/CPI-poem.json


To give sorted list of TF-IDF scores of all tokens in poem with
eprintid of 756:

java -ea -cp lib/jackson-annotations-2.4.2.jar:lib/jackson-core-2.4.2.jar:lib/jackson-databind-2.4.2.jar:lib/lucene-core-4.10.1.jar:lib/lucene-analyzers-common-4.10.1.jar:lib/lucene-expressions-4.10.1.jar:lib/lucene-queries-4.10.1.jar:lib/lucene-facet-4.10.1.jar:lib/lucene-queryparser-4.10.1.jar:lib/commons-lang3-3.3.2.jar:bin com.hourglassapps.cpi_ii.MainListTerms 756

Please note that n-gram size is set by the constant
com.hourglassapps.cpi_ii.ConductusIndex.NGRAM_SIZE. Stemming can be
controlled by the boolean constant
com.hourglassapps.cpi_ii.ConductusIndex.STEM.

Also it is important to appreciate that the stemmer creates different
stems for nouns and verbs and must be informed of a term's
part-of-speech. Currently this is set by the
com.hourglassapps.cpi_ii.latin.LatinStemmer.ASSUMED_POS constant and
is therefore fixed for all tokens.

-----------------------------------------------------------------

To stem Latin terms on standard input, displaying the result to
standard output:

java -ea -cp lib/jackson-annotations-2.4.2.jar:lib/jackson-core-2.4.2.jar:lib/jackson-databind-2.4.2.jar:lib/lucene-core-4.10.1.jar:lib/lucene-analyzers-common-4.10.1.jar:lib/lucene-expressions-4.10.1.jar:lib/lucene-queries-4.10.1.jar:lib/lucene-facet-4.10.1.jar:lib/lucene-queryparser-4.10.1.jar:bin:lib/commons-lang3-3.3.2.jar com.hourglassapps.cpi_ii.snowball.tartarus.MainTestApp - -
