package be.witspirit.flickr.exportprocessor;

import be.witspirit.flickr.exportprocessor.json.Album;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Consumer;

@Service
public class StructuringService {
    private static final Logger LOG = LoggerFactory.getLogger(StructuringService.class);

    private Path destinationPath;

    public StructuringService(@Value("${folder.destination}") String destinationFolder) {
        this.destinationPath = Path.of(destinationFolder);
    }

    public void copyIntoAlbumStructure(Album album, Map<String, ContentDescriptor> contentIndex) {
        LOG.debug("Creating Album structure for {} (@{})", album.getTitle(), album.getId());

        Path albumPath = setupAlbumFolder(album);

        album.getPhotoIds().forEach(copyTo(albumPath, contentIndex));

    }

    private Consumer<String> copyTo(Path destinationDir, Map<String, ContentDescriptor> contentIndex) {
        return photoId -> {
            ContentDescriptor contentDescriptor = contentIndex.get(photoId);
            if (contentDescriptor == null) {
                LOG.error("Failed to find content for PhotoId {} !", photoId);
                return;
            }

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

    private Path setupAlbumFolder(Album album) {
        String albumFolder = deriveAlbumFolder(album);
        Path albumPath = destinationPath.resolve(albumFolder);
        if (Files.notExists(albumPath)) {
            try {
                Files.createDirectory(albumPath);
                LOG.debug("Created {}", albumPath);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create Album directory @ " + albumPath + " : " + e.getMessage(), e);
            }
        } else {
            LOG.debug("{} already exists. Continuing in that folder...", albumPath);
        }
        return albumPath;
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
