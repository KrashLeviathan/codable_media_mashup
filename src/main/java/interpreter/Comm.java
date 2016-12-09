package interpreter;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

import comm_grammar.*;
import utils.*;

import java.io.*;

public class Comm {
    private static final String USAGE = "USAGE:  java -jar Comm.jar <filename.comm>";
//    public static final String C_YEL = "\033[01;33m";
//    public static final String C_NRM = "\033[00m";

    public static void main(String[] args) throws Exception {
        // create a CharStream that reads from the input
        InputStream in = getInputStream(args);
        ANTLRInputStream input = new ANTLRInputStream(in);

        // create a lexer that feeds off of input CharStream
        comm_grammarLexer lexer = new comm_grammarLexer(input);

        // create a buffer of tokens pulled from the lexer
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        // create a parser that feeds off the tokens buffer
        comm_grammarParser parser = new comm_grammarParser(tokens);

        // create a standard ANTLR parse tree and parse tree walker
        ParseTree tree = parser.program(); // begin parsing at program rule
        ParseTreeWalker walker = new ParseTreeWalker();

        // create our custom listener, then feed to the walker
        CodeGenerator generator = new CodeGenerator();
        walker.walk(generator, tree);

        // Here is where we save and run the script or return errors
        if (generator.containsErrors()) {
            System.err.println(generator.getErrors());
            System.exit(1);
        } else {
            CommLocation firstLoc = generator.previousLocations.get(0);
            saveToFile(firstLoc.scriptName(), firstLoc.cacheDir(), generator.getResults());
            try {
                runScript(firstLoc.cacheDir() + "/" + firstLoc.scriptName());
            } catch (IOException exception) {
                System.err.println(exception.getMessage());
                System.exit(1);
            }
        }
    }

    private static void runScript(String pathToScript) throws IOException {
        System.out.println("[*] Running the script... Please be patient! If necessary, you can 'cat'\n"
                + "    the log to the terminal to see what's happening. The logs are located at:\n"
                + "        " + pathToScript + ".log");
        ProcessBuilder pb = new ProcessBuilder("bash", pathToScript);
        File log = new File(pathToScript + ".log");
        // We delete and create it again to make sure we're not appending to an existing log file.
        //noinspection ResultOfMethodCallIgnored
        log.delete();
        log = new File(pathToScript + ".log");
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.appendTo(log));
        Process p = pb.start();
        try {
            int exitCode = p.waitFor();
            if (exitCode != 0) {
                System.err.println("Completed, but with errors. See log for details.");
            }
        } catch (InterruptedException exception) {
            System.err.println(exception.getMessage());
            System.exit(1);
        } finally {
            try {
                // Play a sound when the script is finished running
                SoundUtils.tone(1000, 1000, 0.2);
            } catch (Exception ignored) {
            }
        }
    }

    private static InputStream getInputStream(String[] args) {
        if (args.length == 0) {
            System.out.println("No filename provided. Using stdin...");
            return System.in;
        }
        if (args.length > 1) {
            System.err.println("Too many parameters provided!");
            System.err.println(USAGE);
            System.exit(1);
        }
        try {
            return new FileInputStream(args[0]);
        } catch (IOException exception) {
            System.err.println(exception.getMessage());
            System.exit(1);
        }
        // If all else fails...
        return System.in;
    }

    private static void saveToFile(String filename, String directory, String contents) {
        System.out.println("[*] Saving run script to\n        " + directory + "/" + filename);

        // create multiple directories at one time
        File dir = new File(directory);
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();

        try {
            PrintWriter out = new PrintWriter(directory + "/" + filename, "UTF-8");
            out.print(contents);
            out.close();
        } catch (Exception exception) {
            System.err.println("ERROR SAVING RUN SCRIPT");
            System.err.println("  " + exception.getMessage());
            System.err.println("\nPrinting script to terminal...\n");
            System.out.println(contents);
            System.exit(1);
        }
    }
}
