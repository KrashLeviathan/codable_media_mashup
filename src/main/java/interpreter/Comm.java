package interpreter;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

import comm_grammar.*;
import utils.*;

import java.io.*;
import java.util.concurrent.TimeUnit;

public class Comm {
    private static final String USAGE = "USAGE:\n  java -jar Comm.jar [OPTIONS] <filename.comm>\nOPTIONS:\n" +
            "  -h | --help                 Shows this usage message.\n" +
            "  -t | --translation-only     Translates the CoMM to a bash script,\n" +
            "                              but doesn't execute the script.\n" +
            "EXAMPLE:\n" +
            "  java -jar Comm.jar -t path/to/my/file.comm";

    private static String commFilename;
    private static boolean translationOnly = false;

    public static void main(String[] args) throws Exception {
        long startTime = System.currentTimeMillis();

        parseArguments(args);

        // create a CharStream that reads from the input
        InputStream in = getInputStream(commFilename);
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
                if (!translationOnly) {
                    runScript(firstLoc.cacheDir() + "/" + firstLoc.scriptName());
                }
                long timeTaken = System.currentTimeMillis() - startTime;
                String elapsedTime = String.format("%d min, %d sec",
                        TimeUnit.MILLISECONDS.toMinutes(timeTaken),
                        TimeUnit.MILLISECONDS.toSeconds(timeTaken) -
                                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(timeTaken)));
                System.out.println("Elapsed Time:  " + elapsedTime);
            } catch (IOException exception) {
                System.err.println(exception.getMessage());
                System.exit(1);
            }
        }
    }

    private static void parseArguments(String[] args) {
        boolean filenameFound = false;

        for (String arg : args) {
            if (arg.charAt(0) == '-') {
                parseOption(arg);
            } else if (!filenameFound) {
                filenameFound = true;
                commFilename = arg;
            } else {
                System.err.println("UNKNOWN TOKEN: " + arg + "\n");
                System.err.println(USAGE);
                System.exit(1);
            }
        }
    }

    private static void parseOption(String arg) {
        if (arg.equals("-t") || arg.equals("--translation-only")) {
            translationOnly = true;
        } else if (arg.equals("-h") || arg.equals("--help")) {
            System.out.println(USAGE);
            System.exit(0);
        } else {
            System.err.println("UNKNOWN OPTION: " + arg + "\n");
            System.err.println(USAGE);
            System.exit(1);
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

    private static InputStream getInputStream(String arg) {
        if (arg == null) {
            System.out.println("No filename provided. Using stdin...");
            return System.in;
        }
        try {
            return new FileInputStream(arg);
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
