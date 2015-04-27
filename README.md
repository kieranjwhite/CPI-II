Discovering online references to the poems of the Conductus
===============================================

Introduction
============

The objective of the mini-project Medieval Music, Big Data and the Research Blend (http://www.southampton.ac.uk/music/news/2014/04/11_medieval_music_big_data_and_research_blend.page) is to identify the purpose of the Conductus, a corpus of almost 900 thirteenth-century Latin poems variably set to monophonic and polyphonic music. The known manuscript sources of the Conductus (i.e. organised collections of music and poetry) do not provide much information about the significance and scope of the genre.

Manual text searches of web pages led to the discovery by members of the Cantum pulcriorem invenire (http://www.southampton.ac.uk/music/research/projects/cantum_pulcriorem_invenire.page) research project of text from the poem "Naturas Deus regulis" on the Web. This in turn inspired the development of a more systematic and automated approach to searching for online references to more Conductus material: this project. The goal was to automatically generate a report listing likely reference to poems in the Conductus from the World Wide Web. The project can be broken into a number of subtasks:

(1) Developing and identifying the tools necessary to index and search Latin documents.<br>
(2a) Generating a list of search engine queries for the purpose of obtaining a list of as many potentially relevant online documents as possible.<br>
(2b) Submitting these queries to a search engine and then downloading and indexing the relevant documents.<br>
(3) Interrogating these downloaded documents and generating a final report from the results.<br>

Descriptions of how we completed each of these as well as how another person can avail of this codebase to accomplish these tasks are provided below. Each sub-task above depends on the successful completion of the tasks preceding it. There are a number of other possible uses for this codebase that we will also explain how to perform these in more detail:

(4) Regenerating a report at a later date.<br>
(5) Modifying the codebase to generate reports for alternative repertories.<br>

Most programming was done in Java, however the final generated HTML report contains some Javascript. All programming source code and resources are available on github at https://github.com/kieranjwhite/CPI-II. The extant version of the codebase is not on the master branch, but on the branch titled orginal_report_generation. You access this branch by first cloning the repository and then checking out the branch as follows:

<pre>
git clone https://github.com/kieranjwhite/CPI-II.git
git checkout original_report_generation
</pre>

Your java classpath should include the jar files in the lib/ directory and when compiling please ensure that the sourcepath includes both the src/ and data/ directories.

Before reading the remainder of this document it is suggested that you first read the Lucene analysis package summary document at https://lucene.apache.org/core/4_10_1/core/org/apache/lucene/analysis/package-summary.html. Ensure you understand what the Lucene Analyzer, Tokenizer and TokenFilter classes do and the part they play in the overall Lucene library.

Prior to running any of the commands listed below, set your working directory to the parent of the lib directory of your local git repository. This directory should also contain the bin directory, for all the compiled .class files. The use of the bash shell is assumed in any instructions below, but should not be required.

(1) Developing the tools necessary to index and search Latin documents
======================================================================

Primarily we've depended on Lucene for this sub-task but we also needed to identify a Latin stemmer. Initially we experimented with the Schinke stemmer (Schinke, Greengrass, Robertson & Willet, 1996) (http://snowball.tartarus.org/otherapps/schinke/intro.html) but found that it generates two stemming tables: one for nouns and the other for verbs. Therefore to use it you need to apply part-of-speech tagging to terms. Consequently we investigated another stemmer: Stempel (Galambos, 2001, 2004) (http://getopt.org/stempel/). Stempel is distributed with Lucene but we needed to train a Latin stemming model for it and for that we needed a Latin treebank such as that of the Perseus project (Bamman and Crane, 2006) (http://nlp.perseus.tufts.edu/syntax/treebank/).

In our code the stemmer (either Stempel, Schinke or no stemming) and can be specified during the creation of a StandardLatinAnalyzer (a subclass of Lucene's Analyzer class) object. For most indexing and searching we invoke the static StandardLatinAnalyzer.searchAnalyer() method to instantiate the StandardLatinAnalyzer. The searchAnalyzer() method itself calls a setStemmer() method passing in an argument to a Factory that generates a stemmer instance when required to do so. This Factory can return instances of either StempelRecorderFilter (for Stempel), SnowballRecorderFilter (for Schinke) or IdentityRecorderFilter (to disable stemming). As their names suggest these three classes not only stem terms but can also record stem groups if that is required.

This slightly convoluted approach of having a Factory instantiate the stemmer is needed because the stemmers' constructors each require a TokenStream argument (providing access to the input tokens) and this is provided by the Analyzer.createComponents method. However we wish to be able to specify the stemmer from the outset and before the Analyzer.createComponents method is even invoked.

There are also instances of StempelRecorderFilter, SnowballRecorderFilter and IdentityRecorderFilter available as static fields in the LatinAnalyzer class. These instances have already been configured to record stem groups and are intended for use by the MainIndexConductus class -- our trigram generator.

Stempel must be trained and the result of this is a stemming model. The file at path data/com/hourglassapps/cpi_ii/latin/stem/stempel/model is the model we created. It works quite well, but it may be important to know how to train a new model. Invoke the following to replace the existing model with a newly trained version:

grep <path to unzipped Perseus treebank file>/1.5/data/*.xml -e "lemma" -h|tr '[:upper:]' '[:lower:]'|sed -nr -e "s/^.* form=\"([^\"]*)\" lemma=\"([^\"]*)\".*$/\\2 \\1/p"| grep -v "[^a-zA-Z0-9 ]" |sort -u|sed -nr -e "s/^(.*) (.*)$/\\1\\n\\2/p" | java -ea -cp lib/jackson-annotations-2.4.2.jar:lib/jackson-core-2.4.2.jar:lib/jackson-databind-2.4.2.jar:lib/lucene-core-4.10.1.jar:lib/lucene-analyzers-common-4.10.1.jar:lib/lucene-expressions-4.10.1.jar:lib/lucene-queries-4.10.1.jar:lib/lucene-facet-4.10.1.jar:lib/lucene-queryparser-4.10.1.jar:lib/commons-lang3-3.3.2.jar:bin:data com.hourglassapps.cpi_ii.stem.MainGenStempelModel - data/com/hourglassapps/cpi_ii/latin/stem/stempel/model

The StandardLatinAnalyzer.searchAnalyer() method mentioned above assumes that the model should be saved to the path data/com/hourglassapps/cpi_ii/latin/stem/stempel/model.

Finally, our StandardLatinAnalyzer instance doesn't filter stopwords. This can be changed easily by instantiating a StandardLatinAnalyzer with a single argument:
<pre>
Analyzer analyser=new StandardLatinAnalyzer(LatinAnalyzer.PERSEUS_STOPWORD_FILE);
</pre>
That will configure the StandardLatinAnalyzer to filter out 92 commonly occurring Latin words selected by the Perseus project. However omitting a stoplist does simplify the highlighting of matching phrases during the report generation task. 

(2a) Generating a list of search engine queries for the purpose of obtaining a list of as many potentially relevant online documents as possible
================================================================================================================================================

A JSON export of the Conductus collection was parsed and any text in Latin poems (i.e. titles, refrains and the actual stanzas) was tokenised and stemmed with the aid of Lucene and Stempel. The JSON export had to be preprocessed before a complete parse was successful due to certain special characters not being escaped. There is also an XML export of the same collection which does not seem to suffer from the same issue. 

No stopword filtering was performed at this point. We consider any terms with a common stem to comprise a stem group.

Parse the Conductus with the following:

java -ea -cp lib/jackson-annotations-2.4.2.jar:lib/jackson-core-2.4.2.jar:lib/jackson-databind-2.4.2.jar:lib/lucene-core-4.10.1.jar:lib/lucene-analyzers-common-4.10.1.jar:lib/lucene-analyzers-stempel-4.10.1.jar:lib/lucene-expressions-4.10.1.jar:lib/lucene-queries-4.10.1.jar:lib/lucene-facet-4.10.1.jar:lib/lucene-queryparser-4.10.1.jar:lib/commons-lang3-3.3.2.jar:bin:data com.hourglassapps.cpi_ii.MainIndexConductus PATH_TO_JSON_EXPORT/CPI-poem.json --serialise > PATH_TO_STEM_GROUPS/3grams.dat

A number of resources are generated by this command:
* A file containing stemmed (term -> unstemmed) term mappings: PATH_TO_STEM_GROUPS/3grams.dat
* Index from which we can obtain the total frequency of each unstemmed term in the Conductus, allowing us to order morphological variations of the same ngram by the frequency of its constituent terms:  ./unstemmed_term_index
* Index to allow iterating through all unstemmed ngrams which in combination with our (stemmed -> unstemmed) term mapping above allows us to group morphological variations of the same ngram together in the one Boolean Bing query: ./unstemmed_to_stemmed_index
* Index recording the eprintid of the poem associated with each unstemmed ngram --- used primarily during the creation of the other indexes: ./unstemmed_index

The length of any ngrams saved by the MainIndexConductus program is specified by the MainIndexConductus.NGRAM_LENGTH field. It is currently configured to generate and save trigrams.

(2b) Submitting these queries to a search engine and then downloading and indexing the relevant documents
=========================================================================================================

To create search engine queries we considered each distinct trigram of stemmed terms in turn --- there were 65490 in total. A Boolean query of disjunctions was created from a trigram's three respective associated stem groups, with each disjunction comprising a quoted phrase of three terms, one term drawn from each stem group.

For example consider the trigram "mundi pro salute" from "Ad cultum tue laudis". The following are the relevant stem groups:
<pre>
mundi
mundo

pro

salute
salutis
salutem
salus
</pre>
The resulting Boolean query which was submitted to Bing was therefore:

"mundi pro salute" OR "mundi pro salutis" OR "mundi pro salutem" OR "mundi pro salus" OR "mundo pro salute" OR "mundo pro salutis" OR "mundo pro salutem" OR "mundo pro salus".

Our chosen search engine, Bing, has a query length limit of approximately 2000 characters and this meant that sometimes, when this limit was exceeded, disjunctions containing the rarest terms had to be omitted.

The queries were presented in turn to Bing as there were generated and up to 100 URLs were returned for each. The URLs were saved to allow us to open these links later. The documents at these URLs were downloaded. We also recorded the "Content-Type" HTTP header field value of each document as this information can be helpful when indexing and also again when displaying local copies of the documents. We do not currently make use of this information however. Text was extracted from the documents with the aid of the Apache Tika library. This text was tokenised, terms were stemmed by Stempel and then finally indexed per document by Lucene.

We found Bing's behaviour to be inconsistent at times. Mostly it returned a set of URLs of documents that satisfied our Boolean query. However on occasion the documents were completely unrelated to our query. Sometimes this seems to be a result of the document having changed since Bing indexed it but some queries returned links to phone reviews and other content from unrelated topics; possibly these were advertisements. On rare occasions adding a disjunction to the submitted Boolean query reduced the number of URLs returned --- something which should never occur.

It should be noted that this part of our task can be quite time consuming; querying Bing, downloading URLs and indexing those document may take 1-2 weeks. The downloaded documents also require storage space, approximately 500-600GB. The index itself requires an additional 100-200GB. These figures are all estimates as we have only tested the process on a subset of queries.

Due to the time it takes to complete this task it is necessary to ensure that in the event the program crashes, upon a restart it continues its downloading from a point close to where it stopped, rather than returning to the beginning. Additionally care may need to be taken that in the event of an unreliable network connection the program does not simply run to completion, failing to download any results. To achieve this we pinged a site such as http://google.com regularly and killed the downloading process in the case of a failed ping. The jpingy library (https://code.google.com/p/jpingy/downloads/detail?name=jpingy0_1-alpha.jar&can=2&q=) with some modifications was helpful: it provides a Java wrapper around the ping command.

The following command will generate Boolean queries (one corresponding to each stemmed ngram), submit them to Bing and download and then index the top 100 matching documents (when available) for each:

echo | java -ea -cp lib/jackson-annotations-2.4.2.jar:lib/jackson-core-2.4.2.jar:lib/jackson-databind-2.4.2.jar:lib/lucene-core-4.10.1.jar:lib/lucene-analyzers-common-4.10.1.jar:lib/lucene-analyzers-stempel-4.10.1.jar:lib/lucene-expressions-4.10.1.jar:lib/lucene-queries-4.10.1.jar:lib/lucene-facet-4.10.1.jar:lib/lucene-queryparser-4.10.1.jar:lib/commons-lang3-3.3.2.jar:lib/commons-logging-1.1.3.jar:lib/httpclient-4.3.5.jar:lib/httpcore-4.3.2.jar:lib/httpasyncclient-4.0.2.jar:lib/httpcore-nio-4.3.2.jar:lib/commons-codec-1.9.jar:lib/commons-io-2.4.jar:lib/tika-app-1.6.jar:bin:data com.hourglassapps.cpi_ii.web_search.MainDownloader threads <path to ngrams>/3grams.dat NUM_THREADS

Please note that for testing purposes this command currently only sends a subset of Boolean queries to Bing. Instructions on submitting a full run of queries to Bing can be found in a comment in the MainDownloader.main() method.

The above command takes as input the (term -> unstemmed term) mappings of 3grams.dat (mentioned above) and the Lucene index of unstemmed trigrams (./unstemmed_to_stemmed_index). The name of the index is currently hard-coded.

The results from each query are all downloaded asynchronously. However a downloading thread does not proceed to the next query until all results from the current query have either been downloaded or timed out during a download. To ensure that the downloader is not stalled by a small number of slow sites for a given query we typically have more than one downloading thread (specified by the NUM_THREADS argument). After the relevant documents for a query have all been downloaded, their paths are passed to an indexing thread. There is only one of these so ideally we wish to increase the value of NUM_THREADS as much as possible while ensuring the the downloading threads do not outpace the indexing thread. If they do, downloaded documents will have been removed from the operating system's file cache before needing to be read again for indexing, thus slowing the whole process down considerably. For most runs we have set NUM_THREADS to 2 while running MainDownloader on a 2.2GHz dual core mid-range laptop from 2010 with 4GB of RAM. On a faster computer with more memory a larger value can be used.

The downloaded documents and the index of those document will be saved to the ./documents directory. There will be a subdirectory matching the glob ./documents/*_journal for each downloading thread: documents downloaded by the first downloading thread are saved to ./documents/0_journal/completed, by the second download thread to ./document/1_journal/completed etc. The *_journal directories themselves contain a number of directories one corresponding to each Bing query and are named based on one of the trigrams in the query. In addition the ./documents/downloaded_index directory contains the Lucene index generated from these documents.

There are other files in these directory that may also be of interest. Files named __types.txt contain the list of mime types for each downloaded document as indicated by the "Content-Type" HTTP header in a server's response. Each line in __types.txt comprises a number field followed by the mime type. The mime type is that of the file with a name starting with the value of the number field (followed by an extension where one is provided). For example the line:
<pre>
5 text/html; charset=iso-8859-1
</pre>
means that the file in that directory with a filename, excluding its extension, of "5" was reported by the server as having a mime type of "text/html; charset=iso-8859-1".

Any files with names beginning with a single underscore ('_') character contain the URLs we downloaded (below we will refer to these files as URL files) The file with a filename of 1 corresponds the URL on the first line of this file and so on. Sometimes a file corresponding to a particular line will be absent; this corresponds to a failed download. If a URL has an extension then that extension will be appended to the downloaded file's filename. So if the URL on the first line is http://www.thelatinlibrary.com/ambrose/mysteriis.html then the corresponding downloaded file will be the file in the same directory named 1.html

(3) Interrogating these downloaded documents and generating a final report from the results
===========================================================================================

An individual query was generated from most lines of text in the Conductus. The lines were tokenised and the remaining terms were stemmed as above. A phrasal search was performed in Lucene for each query where a match between a query and document was only recognised where all stemmed terms in a line were found adjacent to each other and in the same order in an indexed document. A ranked list of up to 100 results were generated in this manner for each line in the collection except for poem titles and those lines containing a single word.

Lines containing a single word were not considered sufficiently discriminatory. Therefore for these lines two phrases were generated, one where the the single word line was appended to its preceding line and the other where it prefixed its succeeding line. These two phrases then comprised a single Boolean query of two disjunctions from which up to 100 matching documents were generated.

Poem titles were also treated differently. As these were typically identical to the first line of a poem, generating a query as above would usually result in a redundant list of results. Therefore this time phrases were generated for all other lines in the poem in the manner described above and the query presented to Lucene comprised all these phrases. The returned results could then be considered to be those documents most similar to the poem as a whole.

Finally a HTML report was generated for the user linking all lines in each poem to a matching list of results. For the full complement of 65490 queries this should take approximately 1-2 days. As we were linking lines from poems to lists of relevant documents we needed to format each poem on the screen. Since the Conductus JSON export did not retain any newline information for the poems' content we had to rely of the Conductus XML export instead. The report contained links to relevant documents as well as links to local versions of those documents so that if the original documents were ever taken down, the user would still have a local copy available.

We wished to filter out certain known URLs such as diamm.ac.uk and chmtl.indiana.edu/tml and this task was performed by a simple Javascript function within the report itself, allowing the set of blacklisted sites to be changed easily at a later date. Filtering blacklisted sites could also have been attempted when submitting queries to Bing and doing so would have saved us downloading and indexing blacklisted documents. However attempts to apply filtering when querying the search engine were not consistently successful: for some queries Bing seemingly interpreted the additional "AND NOT site:..." Boolean clauses to be additional desired keywords.

There was another type of result filtering performed on the result list: due to the multi-threaded implementation of our downloading program in task (2b) above, two independent stores of documents were downloaded in response to queries. It's likely that certain documents will have been downloaded twice with one copy in each store. Anytime such duplicates were encountered in the results list one of them is removed and not displayed to the user. In a worst case scenario then, of the 100 results retrieved, 50 might be duplicates and in that case the user will only ever see 50 results for a given line in a poem.

Some of the links in our report are to pertinent documents with numerous OCR errors. It's possible that there are relevant documents that are not listed due to such errors impeding the search. A fuzzy matching algorithm could alleviate this problem. However that doesn't seem to be necessary as even if some lines fail to match due to an error in an indexed document, more than likely other lines will not contain an error and will therefore match the corresponding line in the Conductus.

The report can be generated with the command:

java -Xmx1700m -ea -cp lib/guava-18.0.jar:lib/jackson-annotations-2.4.2.jar:lib/jackson-core-2.4.2.jar:lib/jackson-databind-2.4.2.jar:lib/lucene-core-4.10.1.jar:lib/lucene-analyzers-common-4.10.1.jar:lib/lucene-analyzers-stempel-4.10.1.jar:lib/lucene-expressions-4.10.1.jar:lib/lucene-queries-4.10.1.jar:lib/lucene-facet-4.10.1.jar:lib/lucene-queryparser-4.10.1.jar:lib/commons-lang3-3.3.2.jar:lib/commons-logging-1.1.3.jar:lib/httpclient-4.3.5.jar:lib/httpcore-4.3.2.jar:lib/httpasyncclient-4.0.2.jar:lib/httpcore-nio-4.3.2.jar:lib/commons-codec-1.9.jar:lib/commons-io-2.4.jar:lib/tika-app-1.6.jar:bin:data com.hourglassapps.cpi_ii.report.MainReporter <CONDUCTUS_XML_EXPORT_PATH>

This command takes as input the XML export of the Conductus as well as the directory of downloaded documents and Lucene index produced by the previous step. The result is a HTML report relying on some Javascript saved to the ./poems directory. Any static content (files or parts of files) will have been copied from the data/com/hourglassapps/cpi_ii/report/ directory of the git repository. The blacklist.js file in that directory may be of particular interest as it includes a list of regular expressions matching all blacklisted sites that have been determined not be of interest to the user. Changing this list will change which URLs are deemed to have been blacklisted.

When viewing the report you will need to ensure that the "poems" directory shares a directory with the "documents" directory of the previous step, as the report retrieves the original URLs from the "documents" directory's URL files. If disk space is limited, it is possible to view the report even if the downloaded files themselves (ie those with names starting with a number within the "documents" directory) and the "download_index" have been deleted. Those particular files and directories are not needed to view the report.

Please note that MainReporter requires an unusually large heap size (as specified by the -Xmx1700m switch). This is due to its caching of Lucene document vectors during report generation. If required the size of this cache (and consequently the maximum heap size) can be reduced by changing the value of the static field Query.NUM_CACHE_ENTRIES. Obviously this may affect the speed with which the report is generated. However it might be worthwhile evaluating the effectiveness of this cache and optimising the report generation algorithm as there was not sufficient time to adequately optimise the report generator prior the the conclusion of my contribution to this project.

The report can be viewed by opening the poems/poems.html file in a web browser. Due to restrictions imposed by browsers on the viewing of locally stored files you may need to configure your browser to relax these security precautions. If viewing the report in Chrome (or Chromium) you will need to launch the browser with the --allow-file-access-from-files switch. In Firefox a configuration option needs to be changed as follows:

In the Firefox address bar type:
<pre>
about:config
</pre>
and hit return. A warning will be displayed. Proceed past the warning. A list of configurable settings will appear. In the search field below the address bar type security.fileuri.strict_origin_policy
Double-click on the 'true' value of the security.fileuri.strict_origin_policy entry, changing it to false. Now you should be able to open poems.html and view the links in the report. 

Alternatively hosting the report on a webserver will avoid these problems entirely. The easiest way to do this on a Linux computer is to change to the parent directory of the "poems" and "documents" directories and invoke the command:
<pre>
python -mSimpleHTTPServer
</pre>
Opening your browser at http://localhost:8000/poems/poems.html will now display the report.

(4) Regenerating a report at a later date

Sometimes a user will create a report as described above, but at a later date wish to create another with any previously downloaded URLs filtered out. This will reduce the effort involved in manually inspecting all snippets in the new report. The first step in doing so is to invoke the following:

mv poems old_poems
mv documents old_documents
java -ea -cp lib/jackson-annotations-2.4.2.jar:lib/jackson-core-2.4.2.jar:lib/jackson-databind-2.4.2.jar:lib/lucene-core-4.10.1.jar:lib/lucene-analyzers-common-4.10.1.jar:lib/lucene-analyzers-stempel-4.10.1.jar:lib/lucene-expressions-4.10.1.jar:lib/lucene-queries-4.10.1.jar:lib/lucene-facet-4.10.1.jar:lib/lucene-queryparser-4.10.1.jar:lib/commons-lang3-3.3.2.jar:lib/commons-logging-1.1.3.jar:lib/httpclient-4.3.5.jar:lib/httpcore-4.3.2.jar:lib/httpasyncclient-4.0.2.jar:lib/httpcore-nio-4.3.2.jar:lib/commons-codec-1.9.jar:lib/commons-io-2.4.jar:lib/tika-app-1.6.jar:bin:data com.hourglassapps.cpi_ii.report.blacklist.MainBlacklistReported old_poems/ old_documents/ > poems_urls.txt

This will first move the report (in the poems directory) and its constituent documents (in the documents directory) out of the way and then save a list of all URLs in the report to the poems_urls.txt file. The command needs to be able to access the directory of downloaded documents too so this must be provided as one of the arguments. The file poems_urls.txt is merely a text file of all URLs that comprise the reports source documents. Files like this can be concatenated into one larger file if if the user wishes to filter out URLs from multiple earlier reports.

Now when finally downloading documents for the latest report you use a command similar to this:

cat poems_urls.txt | java -ea -cp lib/jackson-annotations-2.4.2.jar:lib/jackson-core-2.4.2.jar:lib/jackson-databind-2.4.2.jar:lib/lucene-core-4.10.1.jar:lib/lucene-analyzers-common-4.10.1.jar:lib/lucene-analyzers-stempel-4.10.1.jar:lib/lucene-expressions-4.10.1.jar:lib/lucene-queries-4.10.1.jar:lib/lucene-facet-4.10.1.jar:lib/lucene-queryparser-4.10.1.jar:lib/commons-lang3-3.3.2.jar:lib/commons-logging-1.1.3.jar:lib/httpclient-4.3.5.jar:lib/httpcore-4.3.2.jar:lib/httpasyncclient-4.0.2.jar:lib/httpcore-nio-4.3.2.jar:lib/commons-codec-1.9.jar:lib/commons-io-2.4.jar:lib/tika-app-1.6.jar:bin:data com.hourglassapps.cpi_ii.web_search.MainDownloader threads misc/stem_maps/3grams.dat 2

When reading the results returned by Bing this command will not download any URLs included in the poems_urls.txt file. Once the command has run to completion any new documents downloaded will be in the documents directory. Now simply generate a report in the poems directory as before:

java -Xmx1700m -ea -cp lib/guava-18.0.jar:lib/jackson-annotations-2.4.2.jar:lib/jackson-core-2.4.2.jar:lib/jackson-databind-2.4.2.jar:lib/lucene-core-4.10.1.jar:lib/lucene-analyzers-common-4.10.1.jar:lib/lucene-analyzers-stempel-4.10.1.jar:lib/lucene-expressions-4.10.1.jar:lib/lucene-queries-4.10.1.jar:lib/lucene-facet-4.10.1.jar:lib/lucene-queryparser-4.10.1.jar:lib/commons-lang3-3.3.2.jar:lib/commons-logging-1.1.3.jar:lib/httpclient-4.3.5.jar:lib/httpcore-4.3.2.jar:lib/httpasyncclient-4.0.2.jar:lib/httpcore-nio-4.3.2.jar:lib/commons-codec-1.9.jar:lib/commons-io-2.4.jar:lib/tika-app-1.6.jar:bin:data com.hourglassapps.cpi_ii.report.MainReporter <CONDUCTUS_XML_EXPORT_PATH>

(5) Modifying the codebase to generate reports for alternative repertories

Steps 2 and 3 above require changes to be made to the codebase in order to operate on a different collection. Firstly, the MainIndexConductus class which indexes the collection must be altered to facilitate the generation of n-grams and consequently the downloading and indexing of the documents listed in Bing results. Secondly, minor modifications must be made to the MainReporter class to allow the final report to be generated.

Altering MainIndexConductus

MainIndexConductus currently employs a parser, JSONParser to read the collection and generate a series of Id-Content couples. The Id in each couple is a Long instance and a unique identifier for an individual poem as defined by the collection. The Content is a String and as the name suggests in each case is the words of poem itself. You will need to modify MainIndexConductus to allow it to parse your collection (a file specified by the mInput field) by modifying the indexById method. In this method instantiate a parser that implements a ThrowableIterator that iterates through a series of Id-Content couples of the type Record<Long, String> by replacing the
<pre>
JSONParser<Long,String,PoemRecord> parser=...
</pre>
line with something similar to
<pre>
CustomParser<Long,String,CustomRecord> parser=new CustomParser<>(
				       new BufferedReader(
				       	   new FileReader(mInput)), CustomRecord.class);
</pre>

After making these changes you should be able to perform Step 2 above on your collection.

Changes to MainReporter

MainReporter also relies on a parser, PoemRecordXMLParser. Ideally this would have been the same parser as we used earlier (JSONParser) but during development we discovered that the Conductus JSON export lacked the some of the information we needed to create a presentable report so instead we turned to the Conductus XML export in order to complete our task. Assuming that you have a single export of your collection with all the information you require, then a single parser should suffice.

You need to alter the create method. Similarly to before, change the line
<pre>
PoemRecordXMLParser parser=new PoemRecordXMLParser(new BufferedReader(new FileReader(mInput.toFile())));
</pre>
to
<pre>
CustomParser<Long,String,CustomRecord> parser=new CustomParser<>(
				       new BufferedReader(
				       	   new FileReader(mInput.toFile())));
</pre>

MainReporter's create method requires that your parser can return a ThrowableIterator of ReportRecords, instances of a subinterface of Record.

Now Step 3 can also be run on your collection.

Description of selected classes
===============================

* src/com/hourglassapps/threading/FilterTemplate.java.
Converts a task representation (eg a list of trigrams for submission to Bing) to a ThreadFunction that selects a thread to process the task.
* src/com/hourglassapps/threading/ThreadFunction.java.
Interface for any object that determines which thread is responsible for a given task. An instance of FilterTemplate will generate a ThreadFunction instance from a representation of a task. In the case of our MainDownloader program, this is how Boolean queries are distributed between QueryThread instances. Tasks are represented as a list of ngrams.
* src/com/hourglassapps/threading/HashTemplate.java.
Instantiates a ThreadFunction that accepts a different thread depending on the hashCode() of the argument passed to the HashTemplate.convert() method.
* src/com/hourglassapps/threading/JobDelegator.java.
This is instantiated with a FilterTemplate which it converts to one or more Filter instances (one per thread typically). These filters accept a task (eg a list of ngrams for submission to Bing) and return a boolean value to indicate whether a given thread should accept responsibility for the task.

-------------------------------------------------------

* src/com/hourglassapps/util/Promiser.java.
Promises are a concept more commonly associated with Javascript (see http://blog.parse.com/2013/01/29/whats-so-great-about-javascript-promises/) and are an attempt to simplify the handling of results of asynchronous methods. Usually asynchronous methods deliver their results via a myriad of different callbacks and make it difficult to structure code in a manner which is readable. Promises provide a consistent API allowing the developer to structure asynchronous method calls in a manner that is reminiscent of synchronous code.

This codebase relies on a Java implementation of Promises known as the jdeferred library. Asynchronous results are generated by invoking methods such as notify, resolve or reject on the DeferredObject and the Promise is the interface through which these results can be passed to other objects. It desirable then that the DeferredObject remains private while the promise is publicly available. The Promiser interface provides a public method for an object that manages a DeferredObject to return a Promise. 

-------------------------------------------------------

* src/com/hourglassapps/util/Ii.java.
Immutable couple implementation.

-------------------------------------------------------

* src/com/hourglassapps/util/ExclusiveTimeKeeper.java.
A class for measuring the length of time it takes to execute blocks of code. Instantiate the ExclusiveTimeKeeper object at the beginning of a try block in a try-with-resources statement. When the block closes, the timer is paused until the block is entered again. Sub-blocks, also surrounded by try statements, allow for further, more fine-grained, timing information to be recorded. Times recorded in sub-blocks are subtracted from the times associated with the outer-block -- hence the name ExclusiveTimeKeeper.
* src/com/hourglassapps/util/Clock.java.
An interface allowing 'child' clocks to be instantiated from the ExclusiveTimeKeeper instance as well any other Clock.

-------------------------------------------------------

* src/com/hourglassapps/util/ExpansionDistributor.java.
<pre>
For all queries:
    Receives an unsorted list of ngrams corresponding to a single Boolean query for Bing and
    Sorts them according to a provided Comparator.

Later For all the sorted queries:
    The query is distributed to a single QueryThread which generates and submits a Bing query.
    Once the query's top results are all downloaded the ExpansionDistributer instance notifies
     any waiting clients (in our code this will be a single IndexingThread instance) of the
     newly saved documents that require indexing.
</pre>
* src/com/hourglassapps/util/AsyncExpansionReceiver.java.
Receives a list of elements (3 long for trigrams) corresponding to a single permutation of unstemmed terms for the current stemmed trigram via the onExpansion method.
An AsyncExpansionReceiver instance is notified when the permutations of the current stemmed trigram are exhausted by the onGroupDone() method being invoked.

-------------------------------------------------------

* src/com/hourglassapps/util/MainHeartBeat.java.
Regularly pings a hardcoded website, typically one with near 100% availability. If the ping fails then the all processes corresponding to a list of provided PIDs are killed.
This was written to kill the MainDownloader program in the case of network problems. It's a class that will only work on Unix-like systems, but might not be required at all on Window. On Linux if the network interface drops any sockets depending on it can remain alive whereas on Windows this may not to be the case.

-------------------------------------------------------

* src/com/hourglassapps/util/Filter.java.
A single method interface returning a Boolean value in response to a single argument.

-------------------------------------------------------

* src/com/hourglassapps/util/FileWrapper.java.
A FileWrapper instance creates a file with a static beginning and end (saved as files themselves within the data/ directory) but with a middle section that must be created at runtime.

-------------------------------------------------------

* src/com/hourglassapps/util/ConcreteThrower.java.
An attempt to solve the problem of interface or subclass implementations that throw exceptions within an overridden method that are not permitted by the interface / superclass. Intended usage is as follows.

Within a method that does not permit SomeException to be thrown:
<pre>
//mThrower is a ConcreteThrower instance
...
try {
    //do something
} catch(SomeException e) {
  mThrower.ctch(e)
}
...
</pre>

Then later in a different part of the implementation that you know will be invoked regularly and can throw a SomeException instance:
<pre>
try {
    // If mThrower has caught an exception, it will be thrown now if
    // it's an instance of the class passed as an argument to throwCaught()
    mThrower.throwCaught(SomeException.class) 
} catch(Throwable t) {
    SomeException e=(SomeException)t;
    throw e;
}
</pre>

The ConcreteThrower instance's client can invoke the fallThrough() method anytime to check whether a SomeException instance has been caught. When the ConcreteThrower instance is closed any pending SomeException instance will be thrown

-------------------------------------------------------

* src/com/hourglassapps/util/Combinator.java.
See inline Javadoc comment

-------------------------------------------------------

* src/com/hourglassapps/util/Rtu.java.
Class comprising static utility methods.

-------------------------------------------------------

* src/com/hourglassapps/util/InputStreamFactory.java.
Implementations can instantiate a new InputStream wrapping another.

-------------------------------------------------------

* src/com/hourglassapps/util/Closer.java.
Class to facilitate the closing of a number of AutoCloseable resources in a given order

-------------------------------------------------------

* src/com/hourglassapps/util/Throttle.java.
Class to slow down a thread. An instance only allows a limited number of calls to its choke() method over a period of time and waits as necessary to fulfill this condition. Was intended to prevent network link saturation if running MainDownloader with many threads, but not really required currently.

-------------------------------------------------------

* src/com/hourglassapps/cpi_ii/stem/StemRecorderFilter.java.
A subclass of Lucene's TokenFilter, instances of StemRecorderFilter maintain mappings between any token read by this TokenFilter and its output token (if any). These mappings can be saved to an OutputStream or restored from one.
* src/com/hourglassapps/cpi_ii/stem/StempelRecorderFilter.java.
Subclass of StemRecorderFilter that applies Stempel stemming to input tokens
* src/com/hourglassapps/cpi_ii/stem/SnowballRecorderFilter.java.
Subclass of StemRecorderFilter that applies Schinke stemming to input tokens

-------------------------------------------------------

* src/com/hourglassapps/cpi_ii/web_search/QueryThread.java.
When invoking MainDownloader to generate and submit queries for Bing, one or more of these Threads are started. Each one submits queries to Bing and downloads the results (although the downloading itself is delegated to a CloseableHttpAsyncClient instance -- an Apache class).

-------------------------------------------------------

* src/com/hourglassapps/cpi_ii/web_search/AbstractSearchEngine.java.
Base class to simplify implementing RestrictedSearchEngine
* src/com/hourglassapps/cpi_ii/web_search/bing/BingSearchEngine.java.
RestrictedSearchEngine implementation for interacting with Bing
* src/com/hourglassapps/cpi_ii/web_search/RestrictedSearchEngine.java.
Implementations can query a search engine, but also formulate a query to filter out certain results (e.g. URLs from sites we are not interested in)
* src/com/hourglassapps/cpi_ii/web_search/SearchEngine.java.
Interface for objects that query a search engine

-------------------------------------------------------

* src/com/hourglassapps/cpi_ii/web_search/ExpansionComparator.java.
A Comparator that allows us to sort unstemmed ngrams according to the frequency of their constituent terms in the Conductus.

-------------------------------------------------------

* src/com/hourglassapps/cpi_ii/latin/LatinStemmer.java.
The Schinke Latin stemmer

-------------------------------------------------------

* src/com/hourglassapps/cpi_ii/synonyms
This package was intended for any classes required for synonym recognition using WordNet. This aspect of the project is incomplete.

-------------------------------------------------------

* src/com/hourglassapps/cpi_ii/report/Queryer.java.
An instance of this class populates the poems/results directory during report generation.
The search() method of this class is invoked for each line in the Conductus. It is passed two arguments: the line from the poem and name of the directory in which to save a Javascript file (called links.js) of results data.

A Lucene query is instantiated from the Line instance and the Lucene index that was created while downloading URLs from Bing's result is searched. The following results data is saved to the links.js file for each document in the order of document's rank in the results list: document title, path to local copy the document and a list of absolute start-end character offsets corresponding to query phrase matches within the document.

-------------------------------------------------------

* src/com/hourglassapps/cpi_ii/report/PoemsReport.java.
Creates the report, except for the contents of the poems/results subdirectory.

-------------------------------------------------------

* src/com/hourglassapps/cpi_ii/lucene/Phrase.java.
The findIn() method of Phrase instances returns an Iterator over DocSpan instances listing all matches to the phrase in a given document.
* src/com/hourglassapps/cpi_ii/lucene/DocSpan.java.
A DocSpan instance contains the start and end character offsets to a matching phrase within a relevant document.

-------------------------------------------------------

* src/com/hourglassapps/persist/Journal.java.
Implementations provides Atomic / Durable transactions. The usual idiom clients of a Journal (j) implement is:
<pre>
foreach allTransactions transaction:
   if !j.addedAlready(transaction.key): 
      foreach transaction.subjobs sub:
         j.addNew(sub)
      j.commit(transaction.key)
</pre>

If the process dies before all transactions have been committed then after a restart the process will iterate through the same list of transactions (allTransactions) again, checking in turn whether each transaction has been committed. Any uncommitted transactions are processed anew. Any work done on a transaction before a commit is lost if the process crashes before transaction.commit() takes effect. Whether each transaction in allTransactions is independent of the others, or whether transactions must be committed in a certain order is implementation dependent.
* src/com/hourglassapps/persist/FileCopyJournal.java.
Journal implementation where a commit results in the creation of a single file. The addNew() method does nothing and the transaction key passed to the addedAlready() and commit() methods is the Path of an existing file.
* src/com/hourglassapps/persist/FileJournal.java.
Journal implementation that saves a new file on commit() with each respective line in the file corresponding to an argument passed to the addNew() method.
* src/com/hourglassapps/persist/NullJournal.java.
Journal implementation that does nothing.

-------------------------------------------------------

* src/com/hourglassapps/persist/DoneStore.java.
Store implementation that maintains (URL -> Path) mappings, augmenting them with each invocation of addNew(). The addNew() and addExisting() methods both accept an Ii argument instance corresponding to a source URL and a destination file path. Calling the addExisting() method has the side-effect if the source URL is found in the instance's mapping of symlinking the destination path provided to the path corresponding to the provided source URL in the existing mappings. Our code relies on this behaviour to ensure that each URL returned by Bing is only downloaded once per journal, even if the URL is returned in response to multiple queries.

-------------------------------------------------------

* src/com/hourglassapps/persist/MainHashTagDict.java.
The HTML report produced by MainReporter contains href links between lines in poems and their corresponding results lists as well as between each individual result and a locally stored text version of the document. Both of these types of href links encode arguments in their hash id as modified Base64 encoded JSON objects. MainHashTagDict implements the encoder that generates these hash ids. It can also decode these hash ids for debugging purposes. The following fields may be included in these JSON objects:
<pre>
t: document title,
f: directory containing results (this is a subdirectory of poems/results/completed),
n: document rank in results list (the results list can be found in the links.js file within the directory).
</pre>

-------------------------------------------------------

* src/com/hourglassapps/persist/Shortener.java.
A Converter instance the takes as input a String representing a possibly invalid path (due to filename length or invalid characters) and returns a String representing a valid path. A mapping of input -> output Strings is maintained so that a given input always returns the same output as long as the same series of inputs are provided across different runs. Therefore when utilising a Shortener instance you must ensure that calls to Shortener.convert() are not ever skipped due to, for example, a Journal instance recognising that a particular transaction was previously committed.

References
==========

Bamman, David and Gregory Crane (2006), "The Design and Use of a Latin Dependency Treebank," Proceedings of the Fifth International Workshop on Treebanks and Linguistic Theories (TLT 2006) (Prague), pp. 67-78.<br>
Galambos, L. (2001), Lemmatizer for Document Information Retrieval Systems in JAVA. http://www.informatik.uni-trier.de/%7Eley/db/conf/sofsem/sofsem2001.html#Galambos01 SOFSEM 2001, Piestany, Slovakia. <br>
Galambos, L. (2004), Semi-automatic Stemmer Evaluation. International Intelligent Information Processing and Web Mining Conference, 2004, Zakopane, Poland.<br>
Schinke R, Greengrass M, Robertson AM and Willett P (1996), A stemming algorithm for Latin text databases. Journal of Documentation, 52: 172-187. <br>

Third party libraries and API dependencies
==========================================

Apache HTTPAsyncClient (http://www.whoishostingthis.com/mirrors/apache//httpcomponents/httpasyncclient/binary/httpcomponents-asyncclient-4.0.2-bin.zip): Asynchronous HTTP request library,<br>
Apache Tika (https://tika.apache.org/download.html): Library to extract text from different types of files,<br>
Bing Web Search (https://datamarket.azure.com/dataset/bing/searchweb): HTTP REST API for searching Bing,<br>
Jackson (https://github.com/FasterXML/jackson): JSON parsing library,<br>
jdeferred (https://github.com/jdeferred/jdeferred): Promises API for Java that simplifies the ordering of asynchronous methods, <br>
jpingy (https://code.google.com/p/jpingy/downloads/detail?name=jpingy0_1-alpha.jar&can=2&q=): Ping command invocation from Java,<br>
jQuery (http://jquery.com): General purpose Javascript library that is particularly helpful for accessing the DOM,<br>
jQuery Mobile (http://jquerymobile.com): Cross platform user interface library for Javascript,<br>
jx (http://www.openjs.com/scripts/jx/): Simple AJAX library,<br>
Lucene (http://lucene.apache.org): tokenising, indexing and searching library,<br>
when.js (https://github.com/cujojs/when): Javascript Promises implementation to facilitate asynchronous function calls, required by our version of jx,<br>

Linguistic resources
====================

Perseus treebank (http://nlp.perseus.tufts.edu/syntax/treebank/ldt/1.5/ldt-1.5.tar.gz): training Latin stemming model for Stempel.<br>
Latin stoplist from the Perseus Hopper project (http://sourceforge.net/projects/perseus-hopper/files/perseus-hopper/hopper-20110527/hopper-source-20110527.tar.gz/download): 92 common Latin words.<br>

//  LocalWords:  tokenised treebank disjunction unstemmed prioritised
LocalWords:  Conductus
