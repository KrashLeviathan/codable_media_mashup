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

mkdir class 2>/dev/null

echo -e "${C_YEL}antlr4 src/main/java/comm_grammar/comm_grammar.g4${C_NRM}     (abbreviated)"
java -Xmx500M -cp "src/main/resources/antlr-4.5.3-complete.jar" org.antlr.v4.Tool src/main/java/comm_grammar/comm_grammar.g4

echo -e "${C_YEL}javac src/main/java/*.java${C_NRM}                            (abbreviated)"
javac -cp "src/main/resources/antlr-4.5.3-complete.jar" -d class \
    src/main/java/interpreter/*.java          \
    src/main/java/comm_grammar/*.java         \
    src/main/java/utils/*.java

echo -e "${C_YEL}jar cmf0 src/main/resources/META-IF/MANIFEST.MF Comm.jar -C class .${C_NRM}"
jar cmf0 src/main/resources/META-IF/MANIFEST.MF Comm.jar -C class .

echo
echo "To run, use the following command:"
echo
echo "    java -jar Comm.jar input_file.comm"
echo
