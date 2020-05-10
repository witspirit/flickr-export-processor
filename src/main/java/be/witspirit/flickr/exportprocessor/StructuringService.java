package be.witspirit.flickr.exportprocessor;

import be.witspirit.flickr.exportprocessor.json.Album;
import be.witspirit.flickr.exportprocessor.json.PhotoMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
public class StructuringService {
    private static final Logger LOG = LoggerFactory.getLogger(StructuringService.class);

    private final Path destinationPath;
    private final MetadataService metadataService;

    public StructuringService(@Value("${folder.destination}") String destinationFolder, MetadataService metadataService) {
        this.destinationPath = Path.of(destinationFolder);
        this.metadataService = metadataService;
    }

    public Set<String> copyIntoAlbumStructure(Album album, Map<String, ContentDescriptor> contentIndex) {
        LOG.debug("Creating Album structure for {} (@{})", album.getTitle(), album.getId());

        List<PhotoMeta> photoMetas = album.getPhotoIds().stream().map(metadataService::getMetadata).filter(Objects::nonNull).collect(Collectors.toList());
        if (photoMetas.isEmpty()) {
            LOG.warn("Album {} has no photo metadata associated anymore. Ignoring...", album.getTitle());
            return Collections.emptySet();
        }

        List<ContentDescriptor> contentDescriptors = album.getPhotoIds().stream().map(contentIndex::get).filter(Objects::nonNull).collect(Collectors.toList());
        if (contentDescriptors.isEmpty()) {
            setupAlbumFolder(album, photoMetas, "-NOCONTENT");
            LOG.info("Album {} has no content associated anymore. Skipping content copy...", album.getTitle());
        } else {
            Path albumPath = setupAlbumFolder(album, photoMetas, null);
            contentDescriptors.forEach(copyTo(albumPath));
        }

        return contentDescriptors.stream().map(ContentDescriptor::getId).collect(Collectors.toSet());
    }

    private Consumer<ContentDescriptor> copyTo(Path destinationDir) {
        return contentDescriptor -> {
            String destinationFileName = contentDescriptor.getName() + "." + contentDescriptor.getExtension();

            Path sourcePath = contentDescriptor.getPath();
            Path destinationPath = destinationDir.resolve(destinationFileName);

            try {
                LOG.debug("Copying {} -> {}...", sourcePath, destinationPath);

                if (Files.exists(destinationPath)) {
                    long sourceFileSize = Files.size(sourcePath);
                    long destinationFileSize = Files.size(destinationPath);
                    if (destinationFileSize == sourceFileSize) {
                        LOG.debug("{} already exists and seems to be identical. Skipping...", destinationPath);
                    } else {
                        LOG.debug("{} already exists, but does not seem to match the source...", destinationPath);
                        // Safe strategy first : Report !
                        LOG.error("{} already exists as target for {}, but sizes differ ! Manual correction required !", destinationPath, sourcePath);
                        return;
                    }
                }

                Files.copy(sourcePath, destinationPath);
                LOG.debug("Copied  {} -> {}", sourcePath, destinationPath);

            } catch (IOException e) {
                throw new RuntimeException("Failed to copy " + sourcePath + " to " + destinationPath + " due to " + e.getMessage(), e);
            }
        };
    }

    private Path setupAlbumFolder(Album album, List<PhotoMeta> photoMetas, String modifier) {
        String year = deriveYear(photoMetas);
        Path yearPath = createIfNotExists(destinationPath.resolve(year));

        String albumFolder = deriveAlbumFolder(album);
        if (modifier != null) {
            albumFolder+=modifier;
        }
        Path albumPath = createIfNotExists(yearPath.resolve(albumFolder));

        return albumPath;
    }

    private Path createIfNotExists(Path path) {
        if (Files.notExists(path)) {
            try {
                Files.createDirectory(path);
                LOG.debug("Created {}", path);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create directory @ " + path + " : " + e.getMessage(), e);
            }
        } else {
            LOG.debug("{} already exists. Continuing in that folder...", path);
        }
        return path;
    }

    private String deriveYear(List<PhotoMeta> photoMetas) {
        // Let us try to obtain the 'earliest' date_taken
        DateTimeFormatter dateTakenFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd kk:mm:ss");
        return photoMetas.stream()
                .map(PhotoMeta::getDateTaken)
                .map(dateTaken -> LocalDateTime.parse(dateTaken, dateTakenFormat))
                .min(LocalDateTime::compareTo)
                .map(LocalDateTime::getYear)
                .map(Object::toString) // Using Object, instead of Integer as Integer has multiple toString methods, causing ambiguity
                .orElse("UNKNOWN");
    }

    private String deriveAlbumFolder(Album album) {
        return album.getTitle()
                .replaceAll(" ", "_")
                .replaceAll("\\.", "_")
                .replaceAll("<", "_")
                .replaceAll(">", "_")
                .replaceAll("/", "_")
                .replaceAll("\\\\", "_")
                .replaceAll(":", "_")
                ;
    }
}
