package interpreter;

class CommLocation {
    static final String cachesDirectory = "./comm_caches";
    private static final String scriptPrefix = "RUN_";
    String filename;
    String cacheName = "default";

    String scriptName() {
        return scriptPrefix + filename + ".bash";
    }

    String cacheDir() {
        return cachesDirectory + "/" + cacheName;
    }
}
