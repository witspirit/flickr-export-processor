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

    public Map<String, PhotoDescriptor> toPhotoDescriptors(Map<String, ContentDescriptor> contentIndex) {
        DateTimeFormatter dateTakenFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd kk:mm:ss");
        Set<String> photoIds = contentIndex.keySet();

        return photoIds.stream()
                .map(metadataService::getMetadata)
                .map(photoMeta -> {
                    Set<String> tags = photoMeta.getTags().stream().map(Tag::getTag).collect(Collectors.toSet());
                    return PhotoDescriptor.builder()
                            .id(photoMeta.getId())
                            .name(photoMeta.getName())
                            .description(photoMeta.getDescription())
                            .dateTaken(LocalDateTime.parse(photoMeta.getDateTaken(), dateTakenFormat))
                            .tags(tags)
                            .contentDescriptor(contentIndex.get(photoMeta.getId()))
                            .build();
                })
                .collect(Collectors.toMap(PhotoDescriptor::getId, Function.identity()));
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
            ContentDescriptor content = photoDescriptor.getSourceDescriptor();
            if (content == null) {
                return null;
            }

            Path sourcePath = content.getPath();
            Path destinationPath = destinationDir.resolve(photoDescriptor.getDestinationFileName());

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

        String albumFolder = deriveAlbumFolderName(album);
        long nrOfActualPhotos = photoDescriptors.stream().map(PhotoDescriptor::getSourceDescriptor).filter(Objects::nonNull).count();
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

    public Path deriveAlbumPath(Album album, Collection<PhotoDescriptor> albumPhotos) {
        String year = deriveYear(albumPhotos);
        String albumFolder = deriveAlbumFolderName(album);
        albumFolder = markMissingContent(albumFolder, albumPhotos);

        return destinationPath.resolve(year).resolve(albumFolder);
    }

    // In my initial download, not all files were present. Hence, devised a marking strategy to easily identify these folders
    // with missing content.
    // With the latest version, I don't expect this to trigger, as our initial validation showed all content present
    private String markMissingContent(String albumFolder, Collection<PhotoDescriptor> albumPhotos) {
        long nrOfActualPhotos = albumPhotos.stream().map(PhotoDescriptor::getSourceDescriptor).filter(Objects::nonNull).count();
        if (nrOfActualPhotos == 0) {
            albumFolder+="-NOCONTENT";
        }
        return albumFolder;
    }


    private String deriveYear(Collection<PhotoDescriptor> photoDescriptors) {
        // Let us try to obtain the 'earliest' date_taken
        return photoDescriptors.stream()
                .map(PhotoDescriptor::getDateTaken)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .map(LocalDateTime::getYear)
                .map(Object::toString) // Using Object, instead of Integer as Integer has multiple toString methods, causing ambiguity
                .orElse("UNKNOWN");
    }

    private String deriveAlbumFolderName(Album album) {
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

    public Map<String, List<Album>> computePhotoToAlbumIndex(List<Album> albums) {
        Map<String, List<Album>> photoIdToAlbum = new HashMap<>();
        for (Album album : albums) {
            for (String photoId : album.getPhotoIds()) {
                List<Album> appearedInAlbums = photoIdToAlbum.computeIfAbsent(photoId, id -> new ArrayList<>());
                appearedInAlbums.add(album);
            }
        }
        return photoIdToAlbum;
    }

    public List<AlbumDescriptor> deriveAlbumStructure(List<Album> albums, Map<String, ContentDescriptor> photoIdToContentDescriptor) {
        Map<String, PhotoDescriptor> photoIdToPhotoDescriptor = toPhotoDescriptors(photoIdToContentDescriptor);
        Map<String, List<Album>> photoIdToAlbums = computePhotoToAlbumIndex(albums);


        // First consider the Album folders and assign all photos to Albums...
        List<AlbumDescriptor> albumDescriptors = albums.stream()
                .map(album -> {
                    List<PhotoDescriptor> albumPhotos = album.getPhotoIds().stream().map(photoId -> {
                        PhotoDescriptor photoDesc = photoIdToPhotoDescriptor.get(photoId);
                        if (photoDesc == null) {
                            LOG.warn("No PhotoDescriptor found for PhotoId {} in Album {}", photoId, album.getTitle());
                        }
                        return photoDesc;
                    }).filter(Objects::nonNull).collect(Collectors.toList());
                    Path albumPath = deriveAlbumPath(album, albumPhotos);
                    return new AlbumDescriptor(album.getId(), album.getTitle(), albumPhotos, albumPath);
                })
                .collect(Collectors.toList());

        // Add an AlbumDescriptor with the photo's not in an album
        Set<String> photoIdsWithoutAlbum = photoIdToPhotoDescriptor.keySet();
        photoIdsWithoutAlbum.removeAll(photoIdToAlbums.keySet());
        List<PhotoDescriptor> uncategorizedPhotos = photoIdsWithoutAlbum.stream().map(photoIdToPhotoDescriptor::get).collect(Collectors.toList());

        AlbumDescriptor uncategorizedAlbum = new AlbumDescriptor("UNCATEGORIZED", "Uncategorized Photos", uncategorizedPhotos, destinationPath.resolve("Uncategorized"));
        albumDescriptors.add(uncategorizedAlbum);
        return albumDescriptors;
    }
}
