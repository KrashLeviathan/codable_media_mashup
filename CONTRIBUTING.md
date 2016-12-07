## Contributing

If you like using CoMM (or if you don't like it) and want to make it better,
feel free to contribute! Fork the repository to your own account, create a
new branch to work on, and have at it! When you're ready to merge changes,
create a pull request to the main repository and I'll review and merge those
changes if they fit the overall vision of the project. For ideas on ways to
contribute, see the
[Future Development Plans](https://github.com/KrashLeviathan/codable_media_mashup/blob/master/README.md#future-development-plans)
section at the bottom of the README.

Feedback can be sent to [nate@krashdev.com](mailto:nate@krashdev.com).

## ANTLR Setup for Development

*NOTE: These instructions were written assuming a bash environment
on a Mac/Linux system.*

You don't *really* need to do all these things, but it makes the ANTLR
development easier IMHO. Copy the ANTLR jar file to your local library
folder (adjust path if necessary):

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
grun comm_grammar program -gui ../examples/syntax_tests.comm
# To run the bash script generator
java Comm comm_file.comm
```

## Cleanup

Included as a convenience tool.

```
./cleanup.sh
```
