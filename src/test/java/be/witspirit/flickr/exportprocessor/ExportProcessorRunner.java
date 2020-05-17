package be.witspirit.flickr.exportprocessor;

import be.witspirit.flickr.exportprocessor.json.Album;
import be.witspirit.flickr.exportprocessor.json.PhotoMeta;
import be.witspirit.flickr.exportprocessor.json.Tag;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ExportProcessorRunner {
    private static final Logger LOG = LoggerFactory.getLogger(ExportProcessorRunner.class);

    @Autowired
    private MetadataService metadataService;

    @Autowired
    private ContentService contentService;

    @Autowired
    private StructuringService structuringService;

    @Test
    public void checkForPhotosInMultipleAlbums() {
        List<Album> albums = metadataService.loadAlbums();

        Map<String, List<Album>> photoIdToAlbum = new HashMap<>();
        for (Album album : albums) {
                for (String photoId : album.getPhotoIds()) {
                    List<Album> appearedInAlbums = photoIdToAlbum.computeIfAbsent(photoId, id -> new ArrayList<>());
                    appearedInAlbums.add(album);
                }
        }

        Map<String, List<Album>> photosAppearingInMultipleAlbums = photoIdToAlbum.entrySet().stream()
                .filter(e -> e.getValue().size() > 1)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (photosAppearingInMultipleAlbums.isEmpty()) {
            System.out.println("There are no photo's appearing in multiple albums. Analyzed "+albums.size()+" albums.");
        } else {
            System.out.println(photosAppearingInMultipleAlbums.size()+" photo's appear in multiple Albums: ");
            for (String photoId : photosAppearingInMultipleAlbums.keySet()) {
                System.out.printf("%-10s : %s\n", photoId, photosAppearingInMultipleAlbums.get(photoId).stream().map(Album::getTitle).collect(Collectors.joining(", ")));
            }
        }
    }

    @Test
    public void doWeHaveMetadataForEachContentItemAndViceVersa() {
        Map<String, ContentDescriptor> photoIdToContentItem = contentService.loadDescriptors();
        Set<String> photoMetaIds = metadataService.getPhotoMetaIds();

        Set<String> photosInContentWithoutMetadata = new HashSet<>(photoIdToContentItem.keySet());
        photosInContentWithoutMetadata.removeAll(photoMetaIds);

        Set<String> photosWithMetadataButMissingContent = new HashSet<>(photoMetaIds);
        photosWithMetadataButMissingContent.removeAll(photoIdToContentItem.keySet());

        System.out.printf("Found %d content items and %d metadata items\n", photoIdToContentItem.size(), photoMetaIds.size());
        System.out.printf("%d/%d content items without metadata\n", photosInContentWithoutMetadata.size(), photoIdToContentItem.size());
        System.out.printf("%d/%d metadata items without content\n", photosWithMetadataButMissingContent.size(), photoMetaIds.size());

        System.out.println("Content items without metadata:");
        for (String photoId : photosInContentWithoutMetadata) {
            System.out.println(photoId + " : " + photoIdToContentItem.get(photoId));
        }

        System.out.println("Metadata items without content:");
        for (String photoId : photosWithMetadataButMissingContent) {
            System.out.println(photoId + " : " + metadataService.getMetadata(photoId).getName());
        }

    }

    @Test
    public void describeTargetStructure() {
        List<Album> albums = metadataService.loadAlbums();
        Map<String, PhotoMeta> photoMetaIndex = metadataService.loadPhotoMetadata();

        List<AlbumDescriptor> albumDescriptors = structuringService.deriveAlbumStructure(albums, photoMetaIndex);

        System.out.println("Target Structure:");
        for (AlbumDescriptor album : albumDescriptors) {
            System.out.println(album.getAlbumPath());
            for (PhotoDescriptor photo : album.getPhotos()) {
                System.out.println("  " + photo.getDestinationFileName());
            }
        }
    }

    @Test
    public void listDistinctTags() {
        SortedSet<String> distinctTags = metadataService.loadPhotoMetadata().values().stream()
                .map(PhotoMeta::getTags)
                .flatMap(List::stream)
                .map(Tag::getTag)
                .collect(Collectors.toCollection(TreeSet::new));

        System.out.println("Distinct Tags in all Photo Metadata:");
        distinctTags.forEach(System.out::println);
    }

    @Test
    public void moveFilesIntoTargetStructure() {
        Map<String, ContentDescriptor> photoIdToContentDescriptor = contentService.loadDescriptors();

        List<Album> albums = metadataService.loadAlbums();
        Map<String, PhotoMeta> photoMetaIndex = metadataService.loadPhotoMetadata();

        List<AlbumDescriptor> albumDescriptors = structuringService.deriveAlbumStructure(albums, photoMetaIndex);


        // First, create all folders
        for (AlbumDescriptor albumDescriptor : albumDescriptors) {
            Path albumPath = albumDescriptor.getAlbumPath();

            try {
                Files.createDirectories(albumPath);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create directory structure for "+albumPath, e);
            }
        }

        // Then, move or copy all files to their respective folders
        Map<PhotoDescriptor, List<AlbumDescriptor>> photos = structuringService.computePhotoDescriptorToAlbumDescriptorIndex(albumDescriptors);
        for (PhotoDescriptor photo : photos.keySet()) {
            List<AlbumDescriptor> albumAppearances = photos.get(photo);

            ContentDescriptor contentDescriptor = photoIdToContentDescriptor.get(photo.getId());
            if (contentDescriptor == null) {
                // Probably a photo we already moved...
                LOG.debug("No source for {}-{}", photo.getId(), photo.getName());
            } else {
                Path source = contentDescriptor.getPath();

                AlbumDescriptor moveToAlbum = albumAppearances.get(0);
                if (albumAppearances.size() > 1) {
                    // We copy to extra albums first
                    albumAppearances.stream()
                            .skip(1) // The first one is our 'moveTo' Album
                            .map(copyToAlbum -> copyToAlbum.getAlbumPath().resolve(photo.getDestinationFileName()))
                            .forEach(destination -> Transfer.COPY.transfer(source, destination))
                            ;
                }

                // Finally, we MOVE the remaining file
                Path destination = moveToAlbum.getAlbumPath().resolve(photo.getDestinationFileName());
                Transfer.MOVE.transfer(source, destination);
            }

        }

    }

    @Test
    public void renameAllToReintroduceSpaces() {
        // After the structure was setup, I decided to change strategy on the handling of spaces. In comparison with
        // my earlier Album approach, it seems more logical to retain spaces.
        // Hence, instead of reprocessing everything, I will generate the structure again, but now with the retention
        // of spaces and then perform a rename/move operation.

        List<Album> albums = metadataService.loadAlbums();
        Map<String, PhotoMeta> photoMetaIndex = metadataService.loadPhotoMetadata();

        List<AlbumDescriptor> albumDescriptors = structuringService.deriveAlbumStructure(albums, photoMetaIndex);

        for (AlbumDescriptor albumDescriptor : albumDescriptors) {
            Path albumDestination = albumDescriptor.getAlbumPath();
            Path albumSource = albumDestination.resolveSibling(albumDestination.getFileName().toString().replaceAll(" ", "_"));

            if (!forExistingSource(albumSource, albumDestination, Transfer.MOVE)) {
                // We had a special case with trailing spaces, which in the original approach yielded a trailing _
                Path altAlbumSource = albumSource.resolveSibling(albumSource.getFileName().toString()+"_");
                forExistingSource(altAlbumSource, albumDestination, Transfer.MOVE);
            }

            for (PhotoDescriptor photo : albumDescriptor.getPhotos()) {
                Path photoDestination = albumDestination.resolve(photo.getDestinationFileName());
                Path photoSource = albumDestination.resolve(photo.getDestinationFileName().replaceAll(" ", "_"));

                forExistingSource(photoSource, photoDestination, Transfer.MOVE);
            }

        }



    }

    private boolean forExistingSource(Path source, Path destination, Transfer transfer) {
        if (Files.exists(source)) {
            transfer.transfer(source, destination);
            return true;
        } else {
            LOG.debug(source+" does not exist. Probably already processed. Skipping...");
            return false;
        }
    }

}
