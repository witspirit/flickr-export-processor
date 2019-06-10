package be.witspirit.flickr.exportprocessor;

import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ContentDescriptor {
    private static final Pattern FILENAME_STRUCTURE = Pattern.compile("^(.+)_(\\d+)(_o)?\\.(.+)$");

    private Path path;

    private String id;
    private String name;
    private String extension;

    public ContentDescriptor(Path path) {
        this.path = path;

        String fileName = path.getFileName().toString();

        Matcher fileNameMatcher = FILENAME_STRUCTURE.matcher(fileName);
        if (fileNameMatcher.matches()) {
            this.name = fileNameMatcher.group(1);
            this.id = fileNameMatcher.group(2);
            this.extension = fileNameMatcher.group(4);
        } else {
            throw new IllegalArgumentException(path+" does not represent a Content filename");
        }
    }

    public Path getPath() {
        return path;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getExtension() {
        return extension;
    }

    @Override
    public String toString() {
        return String.format("%-11s : %-60s (%s) @ %s", id, name, extension, path);
    }
}
