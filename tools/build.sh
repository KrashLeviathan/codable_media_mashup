#!/usr/bin/env bash

if [[ `basename $(pwd)` != "codable_media_mashup" ]]; then
    echo "Please run this tool from the project's root directory."
    exit
fi

C_YEL="\033[01;33m"
C_NRM="\033[00m"

echo -e "${C_YEL}cd antlr${C_NRM}"
cd antlr

echo -e "${C_YEL}antlr4 comm_grammar.g4$1${C_NRM}"
java -Xmx500M -cp "../antlr-4.5.3-complete.jar" org.antlr.v4.Tool comm_grammar.g4   # :$CLASSPATH    ???   or absolute?

echo -e "${C_YEL}javac Comm.java comm_*.java${C_NRM}"
javac Comm.java comm_*.java

echo -e "${C_YEL}jar cmvf0 META-IF/MANIFEST.MF ../Comm.jar *${C_NRM}"
jar cmvf0 META-IF/MANIFEST.MF ../Comm.jar *

# echo "Cleaning up files..."
# echo -e "${C_YEL}rm -f *.class *.java *.tokens *~${C_NRM}"
# rm -f *.class *.java *.tokens *~

echo -e "${C_YEL}cd -${C_NRM}"
cd -
