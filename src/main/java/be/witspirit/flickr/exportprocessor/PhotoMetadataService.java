package be.witspirit.flickr.exportprocessor;

import be.witspirit.flickr.exportprocessor.json.PhotoMeta;
import be.witspirit.flickr.exportprocessor.json.Tag;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PhotoMetadataService {
    private static final Logger LOG = LoggerFactory.getLogger(PhotoMetadataService.class);

    private ObjectMapper objectMapper;

    public PhotoMetadataService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Map<String, PhotoMeta> loadPhotoMetadata(Path metadataPath) {
        try {
            return Files.list(metadataPath)
                    .filter(this::isPhotoMetadataFile)
                    .map(this::parsePhotoMetadata)
                    .collect(Collectors.toMap(PhotoMeta::getId, Function.identity()));
        } catch (IOException e) {
            throw new RuntimeException("Failed to list "+metadataPath, e);
        }
    }

    public void log(Map<String, PhotoMeta> photoMetadata) {
        photoMetadata.values().forEach(meta -> {
            System.out.printf("%s : %s @ %s : %s\n",
                    meta.getId(),
                    meta.getName(),
                    meta.getDateTaken(),
                    meta.getTags().stream().map(Tag::getTag).collect(Collectors.toList())
            );
            meta.getExif().forEach((key, value) -> {
                System.out.printf("  %s -> %s\n", key, value);
            });
        });
    }

    private boolean isPhotoMetadataFile(Path path) {
        String fileName = path.getFileName().toString();
        return fileName.startsWith("photo_") && fileName.endsWith(".json");
    }

    private PhotoMeta parsePhotoMetadata(Path photoMetadataPath) {
        try {
            PhotoMeta photoMeta = objectMapper.readValue(photoMetadataPath.toFile(), PhotoMeta.class);
            LOG.debug(photoMetadataPath+" : OK");
            return photoMeta;
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse Photo metadata from "+photoMetadataPath, e);
        }
    }
}
