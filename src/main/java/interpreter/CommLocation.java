package interpreter;

/**
 * Stores the filename and cache directory for a single CoMM definition.
 * 
 * Ex:
 * CoMM MyFile;                       // filename = cacheName = "MyFile"
 * CoMM AnotherFile cache(MyCache);   // filename = "AnotherFile", cacheName = "MyCache"
 */
class CommLocation {
    // The location for all CoMM caches.
    private static final String cachesDirectory = "./comm_caches";

    // Prefixes the generated bash script
    private static final String scriptPrefix = "RUN_";

    /**
     * The filename, without the file extension.
     */
    String filename;

    /**
     * The output file extension. It's not static, because maybe we'll make a config option later
     * to specify the file output encoding format.
     */
    String extension = "mp4";

    /**
     * The cache name for a CoMM. It will never be 'default', but if anything crazy happens,
     * at least there's a fallback. The cacheName is defined by the 'cache()' option, if provided.
     * Otherwise it defaults to the filename.
     */
    String cacheName = ".default";

    /**
     * The name of the script file for this CoMM (if it's the first CoMM defined in the file).
     */
    String scriptName() {
        return scriptPrefix + filename + ".bash";
    }

    /**
     * The full relative path to this CoMM's cache.
     */
    String cacheDir() {
        return cachesDirectory + "/" + cacheName;
    }
}
