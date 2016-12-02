#!/usr/bin/env bash

C_YEL="\033[01;33m"
C_NRM="\033[00m"

TESTNAME="TESTS_json_long.in"
#TESTNAME="TESTS_json_short.in"
#TESTNAME="TESTS_json_lab8.in"

echo -e "${C_YEL}antlr4 json_lexer.g4${C_NRM}"
java -Xmx500M -cp "/usr/local/lib/antlr-4.5.3-complete.jar:$CLASSPATH" org.antlr.v4.Tool json_lexer.g4

echo -e "${C_YEL}javac json_lexer*.java${C_NRM}"
javac json_lexer*.java

echo -e "${C_YEL}grun json_lexer tokens < ${TESTNAME}${C_NRM}"
java org.antlr.v4.gui.TestRig json_lexer tokens < ${TESTNAME} > log.txt
echo "Output dumped to log.txt"

echo
echo "Cleaning up files..."
echo -e "${C_YEL}rm -f *.class *.java *.tokens *~${C_NRM}"
rm -f *.class *.java *.tokens *~
