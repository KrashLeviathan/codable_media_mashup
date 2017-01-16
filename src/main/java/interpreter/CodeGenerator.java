package interpreter;

import org.antlr.v4.runtime.tree.*;

import comm_grammar.*;

import java.util.ArrayList;
import java.util.HashMap;

public class CodeGenerator extends comm_grammarBaseListener {
    // The buffers are populated as the parse tree is walked, and then when it's complete
    // they're used to generate the bash script
    private StringBuffer resultsBuffer = new StringBuffer();
    private StringBuffer downloadBuffer = new StringBuffer();
    private StringBuffer slicingBuffer = new StringBuffer();
    private StringBuffer joiningBuffer = new StringBuffer();
    private StringBuffer errorBuffer = new StringBuffer();

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

    // Stores information about credentials for certain video downloads
    private ArrayList<Credential> credentials = new ArrayList<>();
    private class Video {
        public String urlHashCode;
        public String username;
        public String password;

        public Video(String url) {

        }

        public Credential(String uhc, String un, String p) {
            urlHashCode = uhc;
            username = un;
            password = p;
        }
    }

    /**
     * Keeps track of the directory information for all CoMMs that get defined.
     */
    ArrayList<CommLocation> previousLocations = new ArrayList<>();

    /**
     * Returns the contents of the bash script that does all the video magic.
     * This only gets called after the parse tree walker is finished walkin'.
     */
    String getResults() {
        return "#!/usr/bin/env bash\n\n"
                + "# Codable Media Mashup (CoMM) bash script\n\n"
                + resultsBuffer.toString();
    }

    /**
     * @return `true` if errors were found during parsing; otherwise `false`.
     */
    boolean containsErrors() {
        return errorStatus;
    }

    /**
     * @return the String of all errors discovered during parsing.
     */
    String getErrors() {
        return errorBuffer.toString();
    }

    // Returns the bash commands to echo back the command with a timestamp and then run it.
    private static String loggedCommand(String command, boolean timed) {
        String time = (timed) ? "time " : "";
        String extraEcho = (timed) ? "echo\n" : "";
        // The "ts" command is from the "moreutils" package and puts a timestamp on the output.
        return "echo\necho \"" + command + "\" | ts '[%Y-%m-%d %H:%M:%.S]'\necho\n"
                + time + command + "\n" + extraEcho;
    }

    // Returns all the file management commands for the current CoMM (cleaning the cache directory
    // and creating a new cache directory if needed).
    private String getFileManamentCommands() {
        if (cachingDisabled) {
            // If caching is disabled, we remove the directory and then create it again
            return loggedCommand("rm -rf " + location.cacheDir(), false)
                    + loggedCommand("mkdir -p " + location.cacheDir() + " 2>/dev/null", false);
        } else {
            // If caching isn't disabled, we just remove the slice and slice list files. The downloaded source
            // video files will still remain in the cache. We use "2>/dev/null" to redirect errors to the abyss,
            // because if the directory already exists, mkdir will give an error.
            return loggedCommand("rm -f " + location.cacheDir() + "/slice* "
                    + location.cacheDir() + "/*_slice_list.txt ", false)
                    + loggedCommand("mkdir -p " + location.cacheDir() + " 2>/dev/null", false);
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
        String outputFormat = location.cacheDir() + "/vid" + hash;
        // We can change this extension later if we want, but I think youtube-dl defaults to mkv,
        // so it's faster not to recode it as something else
        String ext = "mkv";        // (currently supported in youtube-dl: mp4|flv|ogg|webm|mkv|avi)
        // TODO: Explain youtube-dl flags
        String command = "youtube-dl --abort-on-error --no-color --recode-video " + ext
                + " --no-playlist --no-overwrites --no-post-overwrites --no-cache-dir --newline"
                + " --output '" + outputFormat + "' '" + url + "'";
        downloadBuffer.append(loggedCommand(command, true));
    }

    // Strips double quotes from around the given string, if it has them. Otherwise just returns the string.
    private String stripQuotes(String str) {
        if (str != null && str.length() >= 2 && str.charAt(0) == '"' && str.charAt(str.length() - 1) == '"') {
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
                String errMsg = "line " + line + " - " + statement + ";\n"
                        + "  The variable '" + vname + "' does not exist!\n";
                errorBuffer.append(errMsg);
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
        // TODO: Add other metadata here as necessary
        String resultAddition = "\n\n############################################\n"
                + "#   Filename: " + location.filename + "\n"
                + "#   Cache Folder: " + location.cacheName + "\n"
                + "\n##########     File Management    ##########\n"
                + getFileManamentCommands()
                + "\n##########     Video Downloads    ##########\n"
                + downloadBuffer.toString()
                + "\n##########     Video Slicing      ##########\n"
                + slicingBuffer.toString()
                + "\n##########     Video Joining      ##########\n"
                + joiningBuffer.toString();
        resultsBuffer.append(resultAddition);

        // Print out what videos will be created
        if (!errorStatus) {
            System.out.println("[*] Video Definition");
            System.out.println("        Filename: " + location.filename + "." + location.extension);
            System.out.println("        Cache:    " + location.cacheName);
            System.out.println("        Path:     " + location.cacheDir());
        }

        // Clean things up for the next run
        downloadBuffer = new StringBuffer();
        slicingBuffer = new StringBuffer();
        joiningBuffer = new StringBuffer();
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
     */
    public void exitComm(comm_grammarParser.CommContext ctx) {
        if (ctx.stmnt().size() == 0) {
            errorStatus = true;
            String errMsg = "line " + ctx.start.getLine() + " - " + ctx.getText() + "\n"
                    + "  This CoMM definition is empty!\n";
            errorBuffer.append(errMsg);
        }

        // Change directories into the CoMM's cache directory
        joiningBuffer.append(loggedCommand("cd " + location.cacheDir(), false));

        // Create the text file that will hold a list of all the slice filenames
        String sliceListFileName = location.filename + "_slice_list.txt";
        joiningBuffer.append(loggedCommand("touch " + sliceListFileName, false));

        // Iterate through all the slice filenames and add them to the newly-created slice list file
        joiningBuffer.append("echo \"for f in slice*; do echo \\\"file '\\$f'\\\" >> '")
                .append(sliceListFileName)
                .append("'; done\" | ts '[%Y-%m-%d %H:%M:%.S]'\n");
        joiningBuffer.append("for f in slice*; do echo \"file '$f'\" >> '")
                .append(sliceListFileName)
                .append("'; done\n");

        // Use ffmpeg to concatenate all the slices from the slice file list
        // "-f concat" says we're concatenating the files
        // "-i '%s'" is the input file, which lists all the slice filenames
        // "-y" forces overwrite of the output file if it already exists
        // "%s.%s" is the file with extension
        String concatFiles = String.format("ffmpeg -f concat -i '%s' -y '%s.%s'",
                sliceListFileName, location.filename, location.extension);
        joiningBuffer.append(loggedCommand(concatFiles, true));

        // Return to the root directory
        joiningBuffer.append(loggedCommand("cd -", false));

        // Get ready for the next one
        cleanupForNewComm();
    }

    /**
     * Writes to the slicingBuffer the bash commands for adding an entire video.
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

        String targetFile = String.format("'%s/vid%d.mkv'", location.cacheDir(), str_lit.hashCode());
        String sliceFile = String.format("'%s/slice%04d.mkv'", location.cacheDir(), sliceIndex++);
        // When we add an entire video file, there's no need to slice, so we're
        // just going to add a link to the file as a placeholder for this "slice"
        // FIXME: Maybe try to make this a link instead of a copy in the future
//            slicingBuffer.append(loggedCommand("ln -P " + targetFile + " " + sliceFile, false));
        // Until that works, let's just copy the file
        slicingBuffer.append(loggedCommand("cp " + targetFile + " " + sliceFile, false));
    }

    /**
     * Writes to the slicingBuffer the bash commands for adding part of a video.
     */
    public void exitAdd_rng(comm_grammarParser.Add_rngContext ctx) {
        String url_v = (ctx.v1 != null) ? ctx.v1.getText() : null;
        String url_s = (ctx.s1 != null) ? ctx.s1.getText() : null;
        String start_v = (ctx.v2 != null) ? ctx.v2.getText() : null;
        String start_s = (ctx.s2 != null) ? ctx.s2.getText() : null;
        String stop_v = (ctx.v3 != null) ? ctx.v3.getText() : null;
        String stop_s = (ctx.s3 != null) ? ctx.s3.getText() : null;

        try {
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
            String errMsg = "line " + ctx.start.getLine() + " - " + ctx.getText() + "\n"
                    + "  " + exception.getMessage() + "\n";
            errorBuffer.append(errMsg);
            return;
        }
        int duration = stopSeconds - startSeconds;
        if (duration <= 0) {
            errorStatus = true;
            String errMsg = "line " + ctx.start.getLine() + " - " + ctx.getText() + "\n"
                    + "  Stop time cannot occur before the start time!\n";
            errorBuffer.append(errMsg);
            return;
        }

        downloadIfNeeded(url_s);

        String targetFile = String.format("'%s/vid%d.mkv'", location.cacheDir(), url_s.hashCode());
        String sliceFile = String.format("'%s/slice%04d.mkv'", location.cacheDir(), sliceIndex++);
        // Use ffmpeg to extract the slice from the target file
        slicingBuffer.append(loggedCommand("ffmpeg -i " + targetFile + " -ss " + startSeconds
                + " -t " + duration + " " + sliceFile, true));
    }

    /**
     * Assigns a value to a variable, which can be used later in the CoMM.
     */
    public void exitAssign(comm_grammarParser.AssignContext ctx) {
        String vname = ctx.VNAME().getText();
        String value = ctx.param().getText();
        if (ctx.param().vname() != null) {
            try {
                value = fetchVariable(value, ctx.getText(), ctx.start.getLine());
            } catch (IllegalArgumentException e) {
                return;
            }
        }
        variables.put(vname, stripQuotes(value));
    }

    /**
     * Requests username and password for the given set of videos.
     */
    public void exitReq_vc(comm_grammarParser.Req_vcContext ctx) {
        String vname;
        String str_lit;
        ArrayList<String> str_lits = new ArrayList<>();

        // Fetch the string literal for any variable parameters, and add them to str_lits
        try {
            for (int i = 0; i < ctx.vname().size(); i++) {
                vname = ctx.vname().get(i).getText();
                str_lit = fetchVariable(vname, ctx.getText(), ctx.start.getLine());
                str_lits.add(str_lit);
            }
        } catch (IllegalArgumentException e) {
            if (errorStatus) {
                return;
            }
        }

        // Add any string literal parameters to str_lits
        for (i = 0; i < ctx.str_lit().size(); i++) {
            str_lit = ctx.str_lit().get(i).getText();
            str_lits.add(str_lit);
        }

        // Strip quotes from each one and download if needed
        for (i = 0; i < str_lits.size(); i++) {
            str_lit = stripQuotes(str_lits.get(i));
            // TODO: The download part needs to be reworked!
            downloadIfNeeded(str_lit);
        }
    }

    /**
     * Caching won't be used for this video; it will download all videos in the current CoMM definition.
     */
    public void exitNo_cach(comm_grammarParser.No_cachContext ctx) {
        cachingDisabled = true;
    }

    /**
     * Finds the `CoMM <filename> [cache(cachename)];` statement, and sets the filename and cacheName variables.
     */
    public void exitComstmt(comm_grammarParser.ComstmtContext ctx) {
        if (ctx.VNAME() == null) {
            errorStatus = true;
            return;
        }
        location.filename = ctx.VNAME().getText();
        location.cacheName = (ctx.cache() != null && ctx.cache().VNAME() != null) ? ctx.cache().VNAME().getText() : location.filename;
        for (CommLocation cl : previousLocations) {
            if (cl.filename.equals(location.filename)) {
                errorStatus = true;
                String errMsg = "line " + ctx.start.getLine() + " -  " + ctx.getText() + "\n"
                        + "  The filename '" + location.filename + "' has already been used in this file!\n";
                errorBuffer.append(errMsg);
                return;
            }
        }
    }

    /**
     * If the Lexer or Parser found any problems, they should set the errorStatus to `true` so the bash
     * script doesn't get run.
     */
    public void visitErrorNode(ErrorNode node) {
        errorStatus = true;
    }
}
