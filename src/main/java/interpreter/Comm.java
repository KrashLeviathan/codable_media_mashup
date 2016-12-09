package interpreter;

// import ANTLR's runtime libraries
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

import comm_grammar.*;
import utils.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.io.*;

public class Comm {
    public static final String USAGE = "USAGE:  java Comm <filename.comm>";
    public static final String C_YEL = "\033[01;33m";
    public static final String C_NRM = "\033[00m";

    public static void main(String[]args) throws Exception {
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
        comm_grammar_Code_Generator generator = new comm_grammar_Code_Generator();
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
            try { SoundUtils.tone(1000, 1000, 0.2); } catch (Exception e) { }
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
        boolean successful = dir.mkdirs();

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

    private static class CommLocation {
        private static final String cachesDirectory = "./comm_caches";
        private static final String scriptPrefix = "RUN_";
        public String filename;
        public String cacheName = "default";
        public String scriptName() { return scriptPrefix + filename + ".bash"; }
        public String cacheDir() { return cachesDirectory + "/" + cacheName; }
    }

    public static class comm_grammar_Code_Generator extends comm_grammarBaseListener {
        // The buffers are populated as the parse tree is walked, and then when it's complete
        // they're used to generate the bash script
        private StringBuffer resultsBuffer  = new StringBuffer();
        private StringBuffer downloadBuffer = new StringBuffer();
        private StringBuffer slicingBuffer  = new StringBuffer();
        private StringBuffer joiningBuffer  = new StringBuffer();
        private StringBuffer errorBuffer    = new StringBuffer();

        // This gets set to true when text is added to the errorBuffer. The bash script will
        // not run, and the errors will be shown to the user.
        private boolean errorStatus = false;

        // This gets set when config.noCache() is called.
        private boolean cachingDisabled = false;

        // Keeps track of the slice####.mkv files representing sections of video that will
        // be joined together to make the final video
        private int sliceIndex = 0;

        // Stores the value of variables for recall elsewhere in the comm
        private HashMap<String, String> variables = new HashMap<>();

        // Stores the hash codes for urls to be downloaded. Ensures we only download a url
        // once per cache (unless noCache() is set)
        // TODO: Convert this to a Set rather than checking for `.contains()` condition.
        private ArrayList<Integer> urlHashCodes = new ArrayList<>();

        // Directory information for the current CoMM
        private CommLocation location = new CommLocation();

        /**
         * Keeps track of the directory information for all CoMMs that get defined.
         */
        public ArrayList<CommLocation> previousLocations = new ArrayList<>();

        /**
         * Returns the contents of the bash script that does all the video magic.
         * This only gets called after the parse tree walker is finished walkin'.
         */
        public String getResults() {
            return "#!/usr/bin/env bash\n\n"
                    + "# Codable Media Mashup (CoMM) bash script\n\n"
                    + resultsBuffer.toString();
        }

        /**
         * @return `true` if errors were found during parsing; otherwise `false`.
         */
        public boolean containsErrors()   { return errorStatus;                   }

        /**
         * @return the String of all errors discovered during parsing.
         */
        public String getErrors()         { return errorBuffer.toString(); }

        /**
         * @return the file name of the bash script that creates the current video.
         */
        public String getScriptFilename() { return location.scriptName();         }

        /**
         * @return the relative directory of the cache containing the current video's files.
         */
        public String getCacheDirectory() { return location.cacheDir();           }

        // Returns the bash commands to echo back the command with a timestamp and then run it.
        private static String loggedCommand(String command, boolean timed) {
            String time = (timed) ? "time " : "";
            String extraEcho = (timed) ? "echo\n" : "";
            return "echo\necho \"" + command + "\" | ts '[%Y-%m-%d %H:%M:%.S]'\necho\n"
                    + time + command + "\n" + extraEcho;
        }

        // Returns all the file management commands for the current CoMM (cleaning the cache directory
        // and creating a new cache directory if needed).
        private String getFileManamentCommands() {
            if (cachingDisabled) {
                return loggedCommand("rm -rf " + getCacheDirectory(), false)
                        + loggedCommand("mkdir -p " + getCacheDirectory() + " 2>/dev/null", false);
            } else {
                return loggedCommand("rm -f " + getCacheDirectory() + "/slice*.mkv "
                        + getCacheDirectory() + "/*_slice_list.txt ", false)
                        + loggedCommand("mkdir -p " + getCacheDirectory() + " 2>/dev/null", false);
            }
        }

        // Writes to the downloadBuffer the bash commands for downloading a url.
        // Consider the following flags if CoMM is made public on a web server:
        //     --max-filesize
        //     --limit-rate
        //     --retries
        //     --buffer-size
        private void downloadIfNeeded(String url) {
            if (url == null) {
                return;
            }
            int hash = url.hashCode();
            if (errorStatus || urlHashCodes.contains(hash)) {
                return;
            }
            urlHashCodes.add(hash);
            String outputFormat = getCacheDirectory() + "/vid" + hash;
            String command = "youtube-dl --abort-on-error --no-color --recode-video mkv "
                    + "--no-playlist --no-overwrites --no-post-overwrites --no-cache-dir --newline "
                    + "--output '" + outputFormat + "' '" + url + "'";
            downloadBuffer.append(loggedCommand(command, true));
        }

        // Strips double quotes from around the given string, if it has them. Otherwise just returns the string.
        private String stripQuotes(String str) {
            if (str != null && str.charAt(0) == '"' && str.charAt(str.length()-1)=='"') {
                return str.substring(1, str.length() - 1);
            } else {
                return str;
            }
        }

        // Fetches the given variable value, writing to the errorBuffer if it's not there.
        private String fetchVariable(String vname, String statement, int line) throws IllegalArgumentException {
            if (vname != null) {
                if (variables.containsKey(vname)) {
                    return variables.get(vname);
                } else {
                    errorStatus = true;
                    errorBuffer.append("line " + line + " - " + statement + ";\n");
                    errorBuffer.append("  The variable '" + vname + "' does not exist!\n");
                }
            }
            throw new IllegalArgumentException("The variable '" + vname + "' does not exist!");
        }

        // Returns the number of seconds implied by the given string.
        // The string should be formatted as `minutes:seconds`
        private int getSecondsFromTime(String time) throws IllegalArgumentException {
            if (time == null) {
                throw new IllegalArgumentException("A time string was missing or invalid.\n  Time strings must"
                        + " be in the format 'minutes:seconds'.");
            }
            String[] parts = time.split(":");
            if (parts.length != 2) {
                errorStatus = true;
                throw new IllegalArgumentException("Time strings must be in the format 'minutes:seconds'.");
            }
            return (Integer.parseInt(parts[0]) * 60) + Integer.parseInt(parts[1]);
        }

        // Writes all the commands generated for the current CoMM definition to the resultsBuffer,
        // and then resets the instance variables in preparation for another CoMM definition.
        private void cleanupForNewComm() {
            // TODO: Add other metadata here, with credit back to our site / authors
            resultsBuffer.append("\n\n############################################\n"
                    + "#   Filename: " + location.filename + "\n"
                    + "#   Cache Folder: " + location.cacheName + "\n"
                    + "\n##########     File Management    ##########\n"
                    + getFileManamentCommands()
                    + "\n##########     Video Downloads    ##########\n"
                    + downloadBuffer.toString()
                    + "\n##########     Video Slicing      ##########\n"
                    + slicingBuffer.toString()
                    + "\n##########     Video Joining      ##########\n"
                    + joiningBuffer.toString());

            // Print out what videos will be created
            if (!errorStatus) {
                System.out.println("[*] Video Definition");
                System.out.println("        Filename: " + location.filename);
                System.out.println("        Cache:    " + location.cacheName);
                System.out.println("        Path:     " + CommLocation.cachesDirectory + "/" + location.cacheName);
            }

            // Clean things up for the next run
            downloadBuffer = new StringBuffer();
            slicingBuffer  = new StringBuffer();
            joiningBuffer  = new StringBuffer();
            previousLocations.add(location);
            location = new CommLocation();
            sliceIndex = 0;
            cachingDisabled = false;

            // This actually results in the youtube-dl command being run again on all
            // videos; HOWEVER, since we passed in the '--no-overwrites' flag, it won't
            // redownload them if they're already in the cache.
            urlHashCodes = new ArrayList<>();
        }




        // #######################  OVERWRITTEN ANTLR PARSER METHODS  ###########################
        //
        // There are exit and enter methods for every parser rule (plus some extras), and they
        // can all be found in the comm_grammarBaseListener.java file that gets generated after
        // running antlr4 on the comm_grammar.g4 file. Override these methods to implement the
        // different CoMM functionality. The `enter` methods are called as the parse tree walker
        // starts to enter a rule, and the `exit` methods are called when it exits the rule. Only
        // a fraction of the available methods have actually been overwritten, so you won't see
        // them all here.

        /**
         * When a CoMM definition is complete, all the "join" bash commands are added to the
         * joining buffer, and `cleanupForNewComm()` is called to get things ready for more CoMM
         * definitions.
         * @param ctx
         */
        public void exitComm(comm_grammarParser.CommContext ctx) {
            if (ctx.stmnt().size() == 0) {
                errorStatus = true;
                String line = "line " + ctx.start.getLine();
                errorBuffer.append(line + " - " + ctx.getText() + "\n");
                errorBuffer.append("  This CoMM definition is empty!\n");
            }

            joiningBuffer.append(loggedCommand("cd " + getCacheDirectory(), false));

            String sliceListFileName = location.filename + "_slice_list.txt";
            joiningBuffer.append(loggedCommand("touch " + sliceListFileName, false));

            joiningBuffer.append("echo \"for f in slice*.mkv; do echo \\\"file '\\$f'\\\" >> '"
                    + sliceListFileName + "'; done\" | ts '[%Y-%m-%d %H:%M:%.S]'\n");
            String createSliceList = "for f in slice*.mkv; do echo \"file '$f'\" >> '"
                    + sliceListFileName + "'; done\n";
            joiningBuffer.append(createSliceList);

            String concatFiles = String.format("ffmpeg -f concat -i '%s' -c copy -y '%s.mkv'",
                    sliceListFileName, location.filename);
            joiningBuffer.append(loggedCommand(concatFiles, true));

            joiningBuffer.append(loggedCommand("cd -", false));

            // Get ready for the next one
            cleanupForNewComm();
        }

        /**
         * Writes to the slicingBuffer the bash commands for adding an entire video.
         * @param ctx
         */
        public void exitAdd_all(comm_grammarParser.Add_allContext ctx) {
            String vname = (ctx.vname() != null) ? ctx.vname().getText() : null;
            String str_lit = (ctx.str_lit() != null) ? ctx.str_lit().getText() : null;

            try {
                str_lit = fetchVariable(vname, ctx.getText(), ctx.start.getLine());
            } catch (IllegalArgumentException e) {
                if (errorStatus) {
                    return;
                }
            }

            str_lit = stripQuotes(str_lit);

            downloadIfNeeded(str_lit);

            String targetFile = String.format("'%s/vid%d.mkv'", getCacheDirectory(), str_lit.hashCode());
            String sliceFile = String.format("'%s/slice%04d.mkv'", getCacheDirectory(), sliceIndex++);
            // When we add an entire video file, there's no need to slice, so we're
            // just going to add a link to the file as a placeholder for this "slice"
            // FIXME:
//            slicingBuffer.append(loggedCommand("ln -P " + targetFile + " " + sliceFile, false));
            // Until that works, let's just copy the file
            slicingBuffer.append(loggedCommand("cp " + targetFile + " " + sliceFile, false));
        }

        /**
         * Writes to the slicingBuffer the bash commands for adding part of a video.
         * @param ctx
         */
        public void exitAdd_rng(comm_grammarParser.Add_rngContext ctx) {
            String url_v = (ctx.v1 != null) ? ctx.v1.getText() : null;
            String url_s = (ctx.s1 != null) ? ctx.s1.getText() : null;
            String start_v = (ctx.v2 != null) ? ctx.v2.getText() : null;
            String start_s = (ctx.s2 != null) ? ctx.s2.getText() : null;
            String stop_v = (ctx.v3 != null) ? ctx.v3.getText() : null;
            String stop_s = (ctx.s3 != null) ? ctx.s3.getText() : null;

            try{
                if (url_v != null) {
                    url_s = fetchVariable(url_v, ctx.getText(), ctx.start.getLine());
                }
                if (start_v != null) {
                    start_s = fetchVariable(start_v, ctx.getText(), ctx.start.getLine());
                }
                if (stop_v != null) {
                    stop_s = fetchVariable(stop_v, ctx.getText(), ctx.start.getLine());
                }
            } catch (IllegalArgumentException e) {
                if (errorStatus) {
                    return;
                }
            }

            url_s = stripQuotes(url_s);
            start_s = stripQuotes(start_s);
            stop_s = stripQuotes(stop_s);

            int startSeconds;
            int stopSeconds;
            try {
                startSeconds = getSecondsFromTime(start_s);
                stopSeconds = getSecondsFromTime(stop_s);
            } catch (IllegalArgumentException exception) {
                errorStatus = true;
                String line = "line " + ctx.start.getLine();
                errorBuffer.append(line + " - " + ctx.getText() + "\n");
                errorBuffer.append("  " + exception.getMessage() + "\n");
                return;
            }
            int duration = stopSeconds - startSeconds;
            if (duration <= 0) {
                errorStatus = true;
                String line = "line " + ctx.start.getLine();
                errorBuffer.append(line + " - " + ctx.getText() + "\n");
                errorBuffer.append("  Stop time cannot occur before the start time!\n");
                return;
            }

            downloadIfNeeded(url_s);

            String targetFile = String.format("'%s/vid%d.mkv'", getCacheDirectory(), url_s.hashCode());
            String sliceFile = String.format("'%s/slice%04d.mkv'", getCacheDirectory(), sliceIndex++);
            // Use ffmpeg to extract the slice from the target file
            slicingBuffer.append(loggedCommand("ffmpeg -i " + targetFile + " -ss " + startSeconds
                    + " -t " + duration + " " + sliceFile, true));
        }

        /**
         * Assigns a value to a variable, which can be used later in the CoMM.
         * @param ctx
         */
        public void exitAssign(comm_grammarParser.AssignContext ctx) {
            String vname = ctx.VNAME().getText();
            String value = ctx.param().getText();
            if (ctx.param().vname() != null) {
                try{
                    value = fetchVariable(value, ctx.getText(), ctx.start.getLine());
                } catch (IllegalArgumentException e) {
                    return;
                }
            }
            variables.put(vname, stripQuotes(value));
        }

        /**
         * Caching won't be used for this video; it will download all videos in the current CoMM definition.
         * @param ctx
         */
        public void exitNo_cach(comm_grammarParser.No_cachContext ctx) {
            cachingDisabled = true;
        }

        /**
         * Finds the `CoMM <filename> [cache(cachename)];` statement, and sets the filename and cacheName variables.
         * @param ctx
         */
        public void exitComstmt(comm_grammarParser.ComstmtContext ctx) {
            if (ctx.VNAME() == null) {
                errorStatus = true;
                return;
            }
            location.filename = ctx.VNAME().getText();
            location.cacheName = (ctx.cache() != null && ctx.cache().VNAME() != null) ? ctx.cache().VNAME().getText() : location.filename;
            boolean previousFilenameFound = false;
            for (CommLocation cl : previousLocations) {
                if (cl.filename.equals(location.filename)) {
                    previousFilenameFound = true;
                    errorStatus = true;
                    String line = "line " + ctx.start.getLine();
                    errorBuffer.append(line + " -  " + ctx.getText() + "\n");
                    errorBuffer.append("  The filename '" + location.filename
                            + "' has already been used in this file!\n");
                    return;
                }
            }
        }

        /**
         * If the Lexer or Parser found any problems, they should set the errorStatus to `true` so the bash
         * script doesn't get run.
         * @param node
         */
        public void visitErrorNode(ErrorNode node) {
            errorStatus = true;
        }
    }
}
