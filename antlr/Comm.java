// import ANTLR's runtime libraries
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.io.*;

public class Comm {
    public static final String USAGE = "USAGE:  java Comm <filename.comm>";

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

        // Here is where we save to a file or return errors
        if (generator.containsErrors()) {
            System.out.println(generator.getErrors());
        } else {
            saveToFile(generator.getFilename(), generator.getResults());
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

    private static void saveToFile(String filename, String contents) {
        // TODO
//        System.out.println("Saving to " + filename + ".sh ...");
        System.out.println("\n" + contents);
    }

    public static class comm_grammar_Code_Generator extends comm_grammarBaseListener {
        private StringBuffer downloadBuffer = new StringBuffer();
        private StringBuffer slicingBuffer = new StringBuffer();
        private int sliceIndex = 0;
        private StringBuffer joiningBuffer = new StringBuffer();
        private StringBuffer errorBuffer = new StringBuffer();
        private String filename = "";
        private boolean errorStatus = false;
        private boolean cachingDisabled = false;
        private static final String videoDirectory = "./downloaded_videos";
        private String cacheName = "default";

        private HashMap<String, String> variables = new HashMap<>();
        private ArrayList<Integer> urlHashCodes = new ArrayList<>();

        private ArrayList<String> previousFilenames = new ArrayList<>();

        public boolean containsErrors() {
            return errorStatus;
        }

        public String getResults() {
            // TODO: Add other metadata here, with credit back to our site / authors
            return "#!/usr/bin/env bash\n\n"
                    + "# Codable Media Mashup (CoMM) bash script\n"
                    + "# Filename: " + filename + "\n"
                    + "# Cache Folder: " + cacheName + "\n"
                    + "\n##########     File Management    ##########\n"
                    + getFileManamentCommands()
                    + "\n##########     Video Downloads    ##########\n"
                    + downloadBuffer.toString()
                    + "\n##########     Video Slicing      ##########\n"
                    + slicingBuffer.toString()
                    + "\n##########     Video Joining      ##########\n"
                    + joiningBuffer.toString();
        }

        public String getErrors() {
            return "\n" + errorBuffer.toString();
        }

        public String getFilename() {
            return filename;
        }

        private String getFileManamentCommands() {
            if (cachingDisabled) {
                return "rm -rf " + videoDirectory + "/" + cacheName + "\n"
                        + "mkdir -p " + videoDirectory + "/" + cacheName + " 2>/dev/null\n";
            } else {
                return "rm -f " + videoDirectory + "/" + cacheName + "/slice*.mkv\n"
                        + "rm -f " + videoDirectory + "/" + cacheName + "/*_slice_list.txt\n"
                        + "mkdir -p " + videoDirectory + "/" + cacheName + " 2>/dev/null\n";
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
            String outputFormat = videoDirectory + "/" + cacheName + "/vid" + hash;
            // TODO
            // --username
            // --password
            downloadBuffer.append("youtube-dl --abort-on-error --no-color --recode-video mkv "
                    + "--no-playlist --no-overwrites --no-post-overwrites --no-cache-dir --no-progress "
                    + "--output '" + outputFormat + "' '" + url + "'\n");
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
            sliceIndex = 0;
            filename = "";
            cachingDisabled = false;
            cacheName = "default";

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

            String cacheDir = videoDirectory + "/" + cacheName;
            joiningBuffer.append("cd " + cacheDir + "\n");

            String sliceListFileName = filename + "_slice_list.txt";
            joiningBuffer.append("touch " + sliceListFileName + "\n");

            String createSliceList = "for f in slice*.mkv; do echo \"file '$f'\" >> '"
                    + sliceListFileName + "'; done\n";
            joiningBuffer.append(createSliceList);

            String concatFiles = String.format("ffmpeg -f concat -i '%s' -c copy '%s.mkv'\n", sliceListFileName, filename);
            joiningBuffer.append(concatFiles);

            joiningBuffer.append("cd -\n");

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
            } catch (IllegalArgumentException e) {
                return;
            }

            str_lit = stripQuotes(str_lit);

            downloadIfNeeded(str_lit);

            String targetFile = String.format("'%s/%s/vid%d.mkv'", videoDirectory, cacheName, str_lit.hashCode());
            String sliceFile = String.format("'%s/%s/slice%04d.mkv'", videoDirectory, cacheName, sliceIndex++);
            // When we add an entire video file, there's no need to slice, so we're
            // just going to add a symlink to the file as a placeholder for this "slice"
            slicingBuffer.append("ln -s " + targetFile + " " + sliceFile + "\n");
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

            String targetFile = String.format("'%s/%s/vid%d.mkv'", videoDirectory, cacheName, url_s.hashCode());
            String sliceFile = String.format("'%s/%s/slice%04d.mkv'", videoDirectory, cacheName, sliceIndex++);
            // Use ffmpeg to extract the slice from the target file
            slicingBuffer.append("ffmpeg -i " + targetFile + " -ss " + startSeconds
                    + " -t " + duration + " " + sliceFile + "\n");
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
            filename = ctx.VNAME().getText();
            if (previousFilenames.contains(filename)) {
                errorStatus = true;
                // FIXME
                errorBuffer.append("ERROR: " + ctx.getText() + "\n");
                errorBuffer.append("  The filename '" + filename
                        + "' has already been used in this file!");
            } else {
                previousFilenames.add(filename);
            }
            cacheName = (ctx.cache() != null) ? ctx.cache().VNAME().getText() : filename;
        }

//        public void enterEveryRule(ParserRuleContext ctx) { }
//        public void exitEveryRule(ParserRuleContext ctx) { }
//        public void visitTerminal(TerminalNode node) { }
        public void visitErrorNode(ErrorNode node) {
            errorStatus = true;
        }
    }

}
