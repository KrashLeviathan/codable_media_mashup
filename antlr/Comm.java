// import ANTLR's runtime libraries
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

import java.util.ArrayList;
import java.util.HashMap;

public class Comm {
    public static void main(String[]args) throws Exception {
        // create a CharStream that reads from standard input
        ANTLRInputStream input = new ANTLRInputStream(System.in);

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
            // TODO: Handle errors
            System.out.println(generator.getErrors());
        } else {
            saveToFile(generator.getFilename(), generator.getResults());
        }
    }

    private static void saveToFile(String filename, String contents) {
        // TODO
        System.out.println("Saving to " + filename + ".sh ...");
        System.out.println(contents);
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
                    + ((cachingDisabled) ? ("rm -rf " + videoDirectory + "/" + cacheName + "\n") : "")
                    + "mkdir -p " + videoDirectory + "/" + cacheName + " 2>/dev/null\n"
                    + "\n##########     Video Downloads    ##########\n"
                    + downloadBuffer.toString()
                    + "\n##########     Video Slicing      ##########\n"
                    + slicingBuffer.toString()
                    + "\n##########     Video Joining      ##########\n"
                    + joiningBuffer.toString();
        }

        public String getErrors() {
            return errorBuffer.toString();
        }

        public String getFilename() {
            return filename;
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
            String outputFormat = videoDirectory + "/" + cacheName + "/vid"
                    + hash + ".mkv";
            // TODO
            // --username
            // --password
            downloadBuffer.append("youtube-dl --abort-on-error --no-color "
                    + "--no-playlist --no-overwrites --no-cache-dir --no-progress "
                    + "--output '" + outputFormat + "' '" + url + "'\n");
        }

        private String stripQuotes(String str) {
            if (str != null && str.charAt(0) == '"' && str.charAt(str.length()-1)=='"') {
                return str.substring(1, str.length() - 1);
            } else {
                return str;
            }
        }

        private void fetchVariable(String mutatedTarget, String stmtLHS, String vname, String stmtRHS) {
            if (vname != null) {
                if (variables.containsKey(vname)) {
                    mutatedTarget = variables.get(vname);
                } else {
                    errorStatus = true;
                    errorBuffer.append("ERROR: " + stmtLHS + vname + stmtRHS + ";\n");
                    errorBuffer.append("  The variable '" + vname + "' does not exist!");
                }
            }
        }

        private static int getSecondsFromTime(String time) throws Exception {
            String[] parts = time.split(":");
            if (parts.length != 2) {
                throw new Exception();
            }
            return (Integer.parseInt(parts[0]) * 60) + Integer.parseInt(parts[1]);
        }

        // #######################  OVERWRITTEN METHODS  ###########################

        public void exitProgram(comm_grammarParser.ProgramContext ctx) {
            // FIXME: Remove after complete; for reference only
//        String id = ctx.ID().getText(); // prop : ID '=' STRING '\n' ;
//        String value = ctx.STRING().getText();
        }

        public void enterComm(comm_grammarParser.CommContext ctx) {
            filename = ctx.comstmt().VNAME().getText();
            cacheName = (ctx.comstmt().cache() != null) ? ctx.comstmt().cache().VNAME().getText() : filename;
        }
//        public void exitParam(comm_grammarParser.ParamContext ctx) { }
//        public void exitInt_lit(comm_grammarParser.Int_litContext ctx) { }
//        public void exitVname(comm_grammarParser.VnameContext ctx) { }
//        public void exitStr_lit(comm_grammarParser.Str_litContext ctx) { }
//        public void exitBool_lt(comm_grammarParser.Bool_ltContext ctx) { }
        public void exitAdd_all(comm_grammarParser.Add_allContext ctx) {
            String vname = (ctx.vname() != null) ? ctx.vname().getText() : null;
            String str_lit = (ctx.str_lit() != null) ? ctx.str_lit().getText() : null;

            fetchVariable(str_lit, "add(", vname, ")");
            if (str_lit == null) {
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

            fetchVariable(url_s, "add(", url_v, ", _, _)");
            fetchVariable(start_s, "add(_, ", start_v, ", _)");
            fetchVariable(stop_s, "add(_, _, ", stop_v, ")");

            url_s = stripQuotes(url_s);
            start_s = stripQuotes(start_s);
            stop_s = stripQuotes(stop_s);

            int startSeconds;
            try {
                startSeconds = getSecondsFromTime(start_s);
            } catch (Exception exception) {
                // TODO error message
                startSeconds = 0;
            }
            int stopSeconds;
            try {
                stopSeconds = getSecondsFromTime(stop_s);
            } catch (Exception exception) {
                // TODO error message
                stopSeconds = 0;
            }
            int duration = stopSeconds - startSeconds;
            if (duration <= 0) {
                // TODO error message
            }

            downloadIfNeeded(url_s);

            String targetFile = String.format("'%s/%s/vid%d.mkv'", videoDirectory, cacheName, url_s.hashCode());
            String sliceFile = String.format("'%s/%s/slice%04d.mkv'", videoDirectory, cacheName, sliceIndex++);
            // Use ffmpeg to extract the slice from the target file
            slicingBuffer.append("ffmpeg -i " + targetFile + " -ss " + startSeconds
                    + " -t " + duration + " " + sliceFile + "\n");
        }
//        public void exitAssign(comm_grammarParser.AssignContext ctx) { }
//        public void exitReq_vc(comm_grammarParser.Req_vcContext ctx) { }
//        public void exitConfig(comm_grammarParser.ConfigContext ctx) { }
//        public void exitScale(comm_grammarParser.ScaleContext ctx) { }
//        public void exitScl_bh(comm_grammarParser.Scl_bhContext ctx) { }
//        public void exitScl_bw(comm_grammarParser.Scl_bwContext ctx) { }
//        public void exitPvt_ups(comm_grammarParser.Pvt_upsContext ctx) { }
//        public void exitCache(comm_grammarParser.CacheContext ctx) { }
//        public void exitNo_cach(comm_grammarParser.No_cachContext ctx) { }
//        public void exitComstmt(comm_grammarParser.ComstmtContext ctx) { }
//        public void exitStmnt(comm_grammarParser.StmntContext ctx) { }

//        public void enterEveryRule(ParserRuleContext ctx) { }
//        public void exitEveryRule(ParserRuleContext ctx) { }
//        public void visitTerminal(TerminalNode node) { }
//        public void visitErrorNode(ErrorNode node) { }
    }

}
