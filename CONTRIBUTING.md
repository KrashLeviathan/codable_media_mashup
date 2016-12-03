## Contributing

## ANTLR Setup

*NOTE: These instructions were written assuming a bash environment
on a Mac/Linux system. Use instructions from the ANTLR website if
you plan to run it on Windows.*

Copy the ANTLR jar file to your local library folder (adjust path
if necessary):

```
sudo cp antlr-4.5.3-complete.jar /usr/local/lib/antlr-4.5.3-complete.jar
```

Add the following to your `.bashrc` or other startup script (again,
adjusting the path of the jar if necessary):

```
# ANTLR commands
alias antlr4='java -Xmx500M -cp "/usr/local/lib/antlr-4.5.3-complete.jar:$CLASSPATH" org.antlr.v4.Tool'
alias grun='java org.antlr.v4.gui.TestRig'
export CLASSPATH=".:/usr/local/lib/antlr-4.5.3-complete.jar:$CLASSPATH"
```

Then run `source ~/.bashrc` or just restart the terminal to apply the changes.

## How to use it

```
antlr4 comm_grammar.g4
javac comm_grammar*.java Comm.java
# To view the parse tree:
grun comm_grammar program -gui tests/TEST_simple.comm
# To run the bash script generator
java Comm < comm_file.comm
```

## Cleanup

Included as a convenience tool.

```
./cleanup.sh
```
