#!/usr/bin/env bash

rm out.txt
export MAVEN_OPTS="-Xmx6144m"
mvn package -Dmaven.test.skip
#mvnDebug -T 1C exec:java -Dexec.QueryEngine=edu.arizona.cs.QueryEngine -Dexec.args="0 /mnt/e/wiki_data/indexes /mnt/e/wiki_data/wiki-subset-20140602"
#mvnDebug exec:java -Dexec.QueryEngine=edu.arizona.cs.QueryEngine -Dexec.args="1 /mnt/e/wiki_data/indexes /mnt/e/wiki_data/questions.txt"
mvnDebug exec:java -Dexec.QueryEngine=edu.arizona.cs.QueryEngine -Dexec.args="2 /mnt/e/wiki_data/indexes/S_NL_BM25 /mnt/e/wiki_data/questions.txt"
#mvnDebug exec:java -Dexec.QueryEngine=edu.arizona.cs.QueryEngine -Dexec.args="3 /mnt/e/wiki_data/indexes/S_NL_BM25 /mnt/e/wiki_data/questions.txt"
