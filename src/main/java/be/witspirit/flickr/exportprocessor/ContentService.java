package be.witspirit.flickr.exportprocessor;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ContentService {

    public Map<String, ContentDescriptor> loadDescriptors(Path contentPath) {
        try {
            return Files.list(contentPath)
                    .map(ContentDescriptor::new)
                    .collect(Collectors.toMap(ContentDescriptor::getId, Function.identity()));
        } catch (IOException e) {
            throw new RuntimeException("Failed to list "+contentPath, e);
        }
    }

    public void log(Map<String, ContentDescriptor> contentById) {
        contentById.values().forEach(System.out::println);
    }
}
