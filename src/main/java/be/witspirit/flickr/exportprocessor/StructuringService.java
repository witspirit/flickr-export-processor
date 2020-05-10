package be.witspirit.flickr.exportprocessor;

import be.witspirit.flickr.exportprocessor.json.Album;
import be.witspirit.flickr.exportprocessor.json.PhotoMeta;
import be.witspirit.flickr.exportprocessor.json.Tag;
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
import java.util.function.Function;
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

        DateTimeFormatter dateTakenFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd kk:mm:ss");
        List<PhotoMeta> photoMetas = album.getPhotoIds().stream().map(metadataService::getMetadata).filter(Objects::nonNull).collect(Collectors.toList());
        List<PhotoDescriptor> photoDescriptors = photoMetas.stream().map(photoMeta -> {
            Set<String> tags = photoMeta.getTags().stream().map(Tag::getTag).collect(Collectors.toSet());
            return PhotoDescriptor.builder()
                    .id(photoMeta.getId())
                    .name(photoMeta.getName())
                    .description(photoMeta.getDescription())
                    .dateTaken(LocalDateTime.parse(photoMeta.getDateTaken(), dateTakenFormat))
                    .tags(tags)
                    .contentDescriptor(contentIndex.get(photoMeta.getId()))
                    .build();
        }).collect(Collectors.toList());


        if (photoMetas.isEmpty()) {
            LOG.warn("Album {} has no photo metadata associated anymore. Ignoring...", album.getTitle());
            return Collections.emptySet();
        }

        Path albumPath = setupAlbumFolder(album, photoDescriptors);
        return photoDescriptors.stream().map(copyTo(albumPath)).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private Function<PhotoDescriptor, String> copyTo(Path destinationDir) {
        return photoDescriptor -> {
            ContentDescriptor content = photoDescriptor.getContentDescriptor();
            if (content == null) {
                return null;
            }


            String tagEncoding;
            if (photoDescriptor.getTags().isEmpty()) {
                tagEncoding = "";
            } else {
                tagEncoding = "_" + photoDescriptor.getTags().stream().map(this::sanitizeTagForFilename).collect(Collectors.joining("_"));
            }
            String destinationFileName = content.getName() + tagEncoding + "." + content.getExtension();

            Path sourcePath = content.getPath();
            Path destinationPath = destinationDir.resolve(destinationFileName);

            try {
                LOG.debug("Copying {} -> {}...", sourcePath, destinationPath);

                if (Files.exists(destinationPath)) {
                    long sourceFileSize = Files.size(sourcePath);
                    long destinationFileSize = Files.size(destinationPath);
                    if (destinationFileSize == sourceFileSize) {
                        LOG.debug("{} already exists and seems to be identical. Skipping...", destinationPath);
                        return photoDescriptor.getId();
                    } else {
                        LOG.debug("{} already exists, but does not seem to match the source...", destinationPath);
                        // Safe strategy first : Report !
                        LOG.error("{} already exists as target for {}, but sizes differ ! Manual correction required !", destinationPath, sourcePath);
                        return null;
                    }
                }

                Files.copy(sourcePath, destinationPath);
                LOG.debug("Copied  {} -> {}", sourcePath, destinationPath);
                return photoDescriptor.getId();

            } catch (IOException e) {
                throw new RuntimeException("Failed to copy " + sourcePath + " to " + destinationPath + " due to " + e.getMessage(), e);
            }
        };
    }

    private Path setupAlbumFolder(Album album, Collection<PhotoDescriptor> photoDescriptors) {
        String year = deriveYear(photoDescriptors);
        Path yearPath = createIfNotExists(destinationPath.resolve(year));

        String albumFolder = deriveAlbumFolder(album);
        long nrOfActualPhotos = photoDescriptors.stream().map(PhotoDescriptor::getContentDescriptor).filter(Objects::nonNull).count();
        if (nrOfActualPhotos == 0) {
            albumFolder+="-NOCONTENT";
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

    private String deriveYear(Collection<PhotoDescriptor> photoDescriptors) {
        // Let us try to obtain the 'earliest' date_taken
        return photoDescriptors.stream()
                .map(PhotoDescriptor::getDateTaken)
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

    private String sanitizeTagForFilename(String input) {
        return input
                .replaceAll(" ", "-")
                .replaceAll("\\\\", "-")
                .replaceAll("/", "-")
                ;
    }
}
