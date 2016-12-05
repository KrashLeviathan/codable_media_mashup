#!/usr/bin/env bash

if [[ $1 != *".g4" ]]; then
    echo "USAGE: compile.sh <filename.g4>"
    echo
    echo "For troubleshooting, check out README.md"
    exit
fi

FILENAME=`echo $1 | sed -r s/\.g4//`
C_YEL="\033[01;33m"
C_NRM="\033[00m"

echo -e "${C_YEL}antlr4 $1${C_NRM}"
java -Xmx500M -cp "/usr/local/lib/antlr-4.5.3-complete.jar:$CLASSPATH" org.antlr.v4.Tool $1

echo -e "${C_YEL}javac ${FILENAME}*.java${C_NRM}"
javac ${FILENAME}*.java

echo -e "${C_YEL}grun $FILENAME tokens < $FILENAME.in${C_NRM}"
echo
java org.antlr.v4.gui.TestRig $FILENAME tokens < $FILENAME.in

echo
echo "Cleaning up files..."
echo -e "${C_YEL}rm -f *.class *.java *.tokens *~${C_NRM}"
rm -f *.class *.java *.tokens *~
