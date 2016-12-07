#!/usr/bin/env bash

# This script will build the Comm.jar file that is used to process comm programs
#
# It should be run from the project's root directory like so:
#     tools/build.sh


if [[ `basename $(pwd)` != "codable_media_mashup" ]]; then
    echo "ERROR: Current directory isn't 'codable_media_mashup'."
    echo "  Please run this tool from the project's root directory."
    exit
fi

C_YEL="\033[01;33m"
C_NRM="\033[00m"

echo -e "${C_YEL}cd antlr${C_NRM}"
cd antlr

echo -e "${C_YEL}antlr4 comm_grammar.g4$1${C_NRM}"
java -Xmx500M -cp "../antlr-4.5.3-complete.jar" org.antlr.v4.Tool comm_grammar.g4

echo -e "${C_YEL}javac Comm.java comm_*.java${C_NRM}"
javac -cp "../antlr-4.5.3-complete.jar" Comm.java comm_*.java

echo -e "${C_YEL}jar cmf0 META-IF/MANIFEST.MF ../Comm.jar *${C_NRM}"
jar cmf0 META-IF/MANIFEST.MF ../Comm.jar *

# Not really necessary, but might be useful later
# echo "Cleaning up files..."
# echo -e "${C_YEL}rm -f *.class *.java *.tokens *~${C_NRM}"
# rm -f *.class *.java *.tokens *~

echo -e "${C_YEL}cd -${C_NRM}"
cd -

echo
echo "To run, use the following command:"
echo
echo "    java -jar Comm input_file.comm"
echo
