package be.witspirit.flickr.exportprocessor;

public class FileNameSanitizer {

    public static String text(String input) {
        return input
                .replaceAll("\\.", "_")
                .replaceAll("<", "_")
                .replaceAll(">", "_")
                .replaceAll("/", "_")
                .replaceAll("\\\\", "_")
                .replaceAll(":", "_")
                ;
    }
}
