#!/bin/sh

# A little shell script for making battleship jars
# to be run by the developer from the command line only

PROJECT="battleship"
rm $PROJECT.jar
lein test, uberjar
FILE=`ls $PROJECT*standalone.jar`
cp $FILE $PROJECT.jar
echo Created $PROJECT.jar
