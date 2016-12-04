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
        private StringBuffer resultBuffer = new StringBuffer();
        private StringBuffer downloadBuffer = new StringBuffer();
        private StringBuffer errorBuffer = new StringBuffer();
        private String filename = "";
        private boolean errorStatus = false;

        private HashMap<String, String> variables = new HashMap<>();
        private ArrayList<String> urls = new ArrayList<>();

        public boolean containsErrors() {
            return errorStatus;
        }

        public String getResults() {
            return downloadBuffer.toString() + resultBuffer.toString();
        }

        public String getErrors() {
            return errorBuffer.toString();
        }

        public String getFilename() {
            return filename;
        }

        private void downloadIfNeeded(String url) {
            if (errorStatus) {
                return;
            }
            if (!urls.contains(url)) {
                urls.add(url);
                downloadBuffer.append("youtube-dl \"" + url + "\"\n");
            }
        }

        private String stripQuotes(String str) {
            if (str.charAt(0) == '"' && str.charAt(str.length()-1)=='"') {
                return str.substring(1, str.length() - 1);
            } else {
                return str;
            }
        }

        //        public void enterProgram(comm_grammarParser.ProgramContext ctx) { }

        public void exitProgram(comm_grammarParser.ProgramContext ctx) {
            // FIXME: Remove after complete; for reference only
//        String id = ctx.ID().getText(); // prop : ID '=' STRING '\n' ;
//        String value = ctx.STRING().getText();
        }

//        public void enterComm(comm_grammarParser.CommContext ctx) { }
//        public void exitComm(comm_grammarParser.CommContext ctx) { }
//        public void enterParam(comm_grammarParser.ParamContext ctx) { }
//        public void exitParam(comm_grammarParser.ParamContext ctx) { }
//        public void enterInt_lit(comm_grammarParser.Int_litContext ctx) { }
//        public void exitInt_lit(comm_grammarParser.Int_litContext ctx) { }
//        public void enterVname(comm_grammarParser.VnameContext ctx) { }
//        public void exitVname(comm_grammarParser.VnameContext ctx) { }
//        public void enterStr_lit(comm_grammarParser.Str_litContext ctx) { }
//        public void exitStr_lit(comm_grammarParser.Str_litContext ctx) { }
//        public void enterBool_lt(comm_grammarParser.Bool_ltContext ctx) { }
//        public void exitBool_lt(comm_grammarParser.Bool_ltContext ctx) { }
//        public void enterAdd_all(comm_grammarParser.Add_allContext ctx) { }
        public void exitAdd_all(comm_grammarParser.Add_allContext ctx) {
            String vname = (ctx.vname() != null) ? ctx.vname().getText() : null;
            String str_lit = (ctx.str_lit() != null) ? ctx.str_lit().getText() : null;

            variables.put(vname, "https://youtu.be/j5C6X9vO");
            if (vname != null) {
                if (variables.containsKey(vname)) {
                    str_lit = variables.get(vname);
                } else {
                    errorStatus = true;
                    errorBuffer.append("ERROR: add(" + vname + ");\n");
                    errorBuffer.append("  The variable '" + vname + "' does not exist!");
                }
            }

            str_lit = stripQuotes(str_lit);
            downloadIfNeeded(str_lit);
            resultBuffer.append("ffmpeg " + str_lit + "\n");
        }
//        public void enterAdd_rng(comm_grammarParser.Add_rngContext ctx) { }
//        public void exitAdd_rng(comm_grammarParser.Add_rngContext ctx) { }
//        public void enterAssign(comm_grammarParser.AssignContext ctx) { }
//        public void exitAssign(comm_grammarParser.AssignContext ctx) { }
//        public void enterReq_vc(comm_grammarParser.Req_vcContext ctx) { }
//        public void exitReq_vc(comm_grammarParser.Req_vcContext ctx) { }
//        public void enterConfig(comm_grammarParser.ConfigContext ctx) { }
//        public void exitConfig(comm_grammarParser.ConfigContext ctx) { }
//        public void enterScale(comm_grammarParser.ScaleContext ctx) { }
//        public void exitScale(comm_grammarParser.ScaleContext ctx) { }
//        public void enterScl_bh(comm_grammarParser.Scl_bhContext ctx) { }
//        public void exitScl_bh(comm_grammarParser.Scl_bhContext ctx) { }
//        public void enterScl_bw(comm_grammarParser.Scl_bwContext ctx) { }
//        public void exitScl_bw(comm_grammarParser.Scl_bwContext ctx) { }
//        public void enterPvt_ups(comm_grammarParser.Pvt_upsContext ctx) { }
//        public void exitPvt_ups(comm_grammarParser.Pvt_upsContext ctx) { }
//        public void enterCache(comm_grammarParser.CacheContext ctx) { }
//        public void exitCache(comm_grammarParser.CacheContext ctx) { }
//        public void enterNo_cach(comm_grammarParser.No_cachContext ctx) { }
//        public void exitNo_cach(comm_grammarParser.No_cachContext ctx) { }
//        public void enterComstmt(comm_grammarParser.ComstmtContext ctx) { }
//        public void exitComstmt(comm_grammarParser.ComstmtContext ctx) { }
//        public void enterStmnt(comm_grammarParser.StmntContext ctx) { }
//        public void exitStmnt(comm_grammarParser.StmntContext ctx) { }

//        public void enterEveryRule(ParserRuleContext ctx) { }
//        public void exitEveryRule(ParserRuleContext ctx) { }
//        public void visitTerminal(TerminalNode node) { }
//        public void visitErrorNode(ErrorNode node) { }
    }

}
