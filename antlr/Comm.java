// import ANTLR's runtime libraries
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

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
//        System.out.println(tree.toStringTree(parser)); // print LISP-style tree
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
        System.out.println("Running the script... Please be patient!\n"
                + "If necessary, you can 'cat' the log to the terminal to see \n"
                + "what's happening. The logs are located at:\n"
                + "    " + pathToScript + ".log");
        ProcessBuilder pb = new ProcessBuilder("bash", pathToScript);
        File log = new File(pathToScript + ".log");
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
        System.out.println("Saving run script to " + directory + "/" + filename);

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
        private StringBuffer resultsBuffer = new StringBuffer();
        private StringBuffer downloadBuffer = new StringBuffer();
        private StringBuffer slicingBuffer = new StringBuffer();
        private int sliceIndex = 0;
        private StringBuffer joiningBuffer = new StringBuffer();
        private StringBuffer errorBuffer = new StringBuffer();
        private CommLocation location = new CommLocation();
        private boolean errorStatus = false;
        private boolean cachingDisabled = false;

        private HashMap<String, String> variables = new HashMap<>();
        private ArrayList<Integer> urlHashCodes = new ArrayList<>();

        public ArrayList<CommLocation> previousLocations = new ArrayList<>();


        public String getResults() {
            return "#!/usr/bin/env bash\n\n"
                    + "# Codable Media Mashup (CoMM) bash script\n\n"
                    + resultsBuffer.toString();
        }

        public boolean containsErrors()   { return errorStatus;                   }
        public String getErrors()         { return "\n" + errorBuffer.toString(); }
        public String getScriptFilename() { return location.scriptName();         }
        public String getCacheDirectory() { return location.cacheDir();           }

        private static String loggedCommand(String command, boolean timed) {
            String time = (timed) ? "time " : "";
            String extraEcho = (timed) ? "echo\n" : "";
            return "echo\necho \"" + command + "\" | ts '[%Y-%m-%d %H:%M:%.S]'\necho\n"
                    + time + command + "\n" + extraEcho;
        }

        private String getFileManamentCommands() {
            if (cachingDisabled) {
                return loggedCommand("rm -rf " + getCacheDirectory(), false)
                        + loggedCommand("mkdir -p " + getCacheDirectory() + " 2>/dev/null", false);
            } else {
                return loggedCommand("rm -f " + getCacheDirectory() + "/slice*.mkv", false)
                        + loggedCommand("rm -f " + getCacheDirectory() + "/*_slice_list.txt", false)
                        + loggedCommand("mkdir -p " + getCacheDirectory() + " 2>/dev/null", false);
            }
        }

        // Consider the following flags if CoMM is made public:
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
            // TODO
            // --username
            // --password
            String command = "youtube-dl --abort-on-error --no-color --recode-video mkv "
                    + "--no-playlist --no-overwrites --no-post-overwrites --no-cache-dir --newline "
                    + "--output '" + outputFormat + "' '" + url + "'";
            downloadBuffer.append(loggedCommand(command, true));
        }

        private String stripQuotes(String str) {
            if (str != null && str.charAt(0) == '"' && str.charAt(str.length()-1)=='"') {
                return str.substring(1, str.length() - 1);
            } else {
                return str;
            }
        }

        private String fetchVariable(String vname, String statement) throws IllegalArgumentException {
            if (vname != null) {
                if (variables.containsKey(vname)) {
                    return variables.get(vname);
                } else {
                    errorStatus = true;
                    errorBuffer.append("ERROR: " + statement + ";\n");
                    errorBuffer.append("  The variable '" + vname + "' does not exist!\n");
                }
            }
            throw new IllegalArgumentException("The variable '" + vname + "' does not exist!");
        }

        private int getSecondsFromTime(String time) throws IllegalArgumentException {
            String[] parts = time.split(":");
            if (parts.length != 2) {
                errorStatus = true;
                throw new IllegalArgumentException("Time strings must be in the format 'minutes:seconds'.");
            }
            return (Integer.parseInt(parts[0]) * 60) + Integer.parseInt(parts[1]);
        }

        private void cleanupForNewComm() {
            // Put everything for the current CoMM definition into the resultsBuffer
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

            // Clean things up for the next run
            downloadBuffer = new StringBuffer();
            slicingBuffer  = new StringBuffer();
            joiningBuffer  = new StringBuffer();
            previousLocations.add(location);
            location = new CommLocation();
            sliceIndex = 0;
            cachingDisabled = false;

            // Keep variables around for additiona CoMM's?
//            private HashMap<String, String> variables = new HashMap<>();

            // This will result in the youtube-dl command being run again on all
            // videos, but since we passed in the '--no-overwrites' flag, it won't
            // redownload them if they're already in the cache.
            urlHashCodes = new ArrayList<>();
        }

        // #######################  OVERWRITTEN METHODS  ###########################

//        public void exitProgram(comm_grammarParser.ProgramContext ctx) { }

        public void exitComm(comm_grammarParser.CommContext ctx) {
            if (ctx.stmnt().size() == 0) {
                errorStatus = true;
                errorBuffer.append("ERROR: " + ctx.getText() + "\n");
                errorBuffer.append("  This CoMM definition is empty!");
            }

            joiningBuffer.append(loggedCommand("cd " + getCacheDirectory(), false));

            String sliceListFileName = location.filename + "_slice_list.txt";
            joiningBuffer.append(loggedCommand("touch " + sliceListFileName, false));

            joiningBuffer.append("echo \"for f in slice*.mkv; do echo \\\"file '\\$f'\\\" >> '"
                    + sliceListFileName + "'; done\" | ts '[%Y-%m-%d %H:%M:%.S]'\n");
            String createSliceList = "for f in slice*.mkv; do echo \"file '$f'\" >> '"
                    + sliceListFileName + "'; done\n";
            joiningBuffer.append(createSliceList);

            String concatFiles = String.format("ffmpeg -f concat -i '%s' -c copy -y '%s.mkv'", sliceListFileName, location.filename);
            joiningBuffer.append(loggedCommand(concatFiles, true));

            joiningBuffer.append(loggedCommand("cd -", false));

            // Get ready for the next one
            cleanupForNewComm();
        }

//        public void exitParam(comm_grammarParser.ParamContext ctx) { }
//        public void exitInt_lit(comm_grammarParser.Int_litContext ctx) { }
//        public void exitVname(comm_grammarParser.VnameContext ctx) { }
//        public void exitStr_lit(comm_grammarParser.Str_litContext ctx) { }
//        public void exitBool_lt(comm_grammarParser.Bool_ltContext ctx) { }

        public void exitAdd_all(comm_grammarParser.Add_allContext ctx) {
            String vname = (ctx.vname() != null) ? ctx.vname().getText() : null;
            String str_lit = (ctx.str_lit() != null) ? ctx.str_lit().getText() : null;

            try {
                str_lit = fetchVariable(vname, ctx.getText());
            } catch (IllegalArgumentException e) { }

            str_lit = stripQuotes(str_lit);

            downloadIfNeeded(str_lit);

            String targetFile = String.format("'%s/vid%d.mkv'", getCacheDirectory(), str_lit.hashCode());
            String sliceFile = String.format("'%s/slice%04d.mkv'", getCacheDirectory(), sliceIndex++);
            // When we add an entire video file, there's no need to slice, so we're
            // just going to add a link to the file as a placeholder for this "slice"
            // FIXME:
//            slicingBuffer.append("eal \"ln -P " + targetFile + " " + sliceFile + "\"\n");
            // Until that works, let's just copy the file
            slicingBuffer.append(loggedCommand("cp " + targetFile + " " + sliceFile, false));
        }

        public void exitAdd_rng(comm_grammarParser.Add_rngContext ctx) {
            String url_v = (ctx.v1 != null) ? ctx.v1.getText() : null;
            String url_s = (ctx.s1 != null) ? ctx.s1.getText() : null;
            String start_v = (ctx.v2 != null) ? ctx.v2.getText() : null;
            String start_s = (ctx.s2 != null) ? ctx.s2.getText() : null;
            String stop_v = (ctx.v3 != null) ? ctx.v3.getText() : null;
            String stop_s = (ctx.s3 != null) ? ctx.s3.getText() : null;

            try{
                if (url_v != null) {
                    url_s = fetchVariable(url_v, ctx.getText());
                }
                if (start_v != null) {
                    start_s = fetchVariable(start_v, ctx.getText());
                }
                if (stop_v != null) {
                    stop_s = fetchVariable(stop_v, ctx.getText());
                }
            } catch (IllegalArgumentException e) {
                return;
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
                errorBuffer.append("ERROR: " + ctx.getText() + "\n");
                errorBuffer.append("  " + exception.getMessage() + "\n");
                return;
            }
            int duration = stopSeconds - startSeconds;
            if (duration <= 0) {
                errorStatus = true;
                errorBuffer.append("ERROR: " + ctx.getText() + "\n");
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

        public void exitAssign(comm_grammarParser.AssignContext ctx) {
            String vname = ctx.VNAME().getText();
            String value = ctx.param().getText();
            if (ctx.param().vname() != null) {
                try{
                    value = fetchVariable(value, ctx.getText());
                } catch (IllegalArgumentException e) {
                    return;
                }
            }
            variables.put(vname, stripQuotes(value));
        }

//        public void exitReq_vc(comm_grammarParser.Req_vcContext ctx) { }
//        public void exitConfig(comm_grammarParser.ConfigContext ctx) { }
//        public void exitScale(comm_grammarParser.ScaleContext ctx) { }
//        public void exitScl_bh(comm_grammarParser.Scl_bhContext ctx) { }
//        public void exitScl_bw(comm_grammarParser.Scl_bwContext ctx) { }
//        public void exitPvt_ups(comm_grammarParser.Pvt_upsContext ctx) { }

        public void exitNo_cach(comm_grammarParser.No_cachContext ctx) {
            cachingDisabled = true;
        }

        public void exitComstmt(comm_grammarParser.ComstmtContext ctx) {
            if (ctx.VNAME() == null) {
                errorStatus = true;
                return;
            }
            location.filename = ctx.VNAME().getText();
            location.cacheName = (ctx.cache() != null) ? ctx.cache().VNAME().getText() : location.filename;
            boolean previousFilenameFound = false;
            for (CommLocation cl : previousLocations) {
                if (cl.filename.equals(location.filename)) {
                    previousFilenameFound = true;
                    errorStatus = true;
                    // FIXME
                    errorBuffer.append("ERROR: " + ctx.getText() + "\n");
                    errorBuffer.append("  The filename '" + location.filename
                            + "' has already been used in this file!");
                    return;
                }
            }
        }

//        public void enterEveryRule(ParserRuleContext ctx) { }
//        public void exitEveryRule(ParserRuleContext ctx) { }
//        public void visitTerminal(TerminalNode node) { }
        public void visitErrorNode(ErrorNode node) {
            errorStatus = true;
        }
    }

}
