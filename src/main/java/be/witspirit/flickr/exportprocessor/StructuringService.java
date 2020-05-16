package be.witspirit.flickr.exportprocessor;

import be.witspirit.flickr.exportprocessor.json.Album;
import be.witspirit.flickr.exportprocessor.json.PhotoMeta;
import be.witspirit.flickr.exportprocessor.json.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StructuringService {
    private static final Logger LOG = LoggerFactory.getLogger(StructuringService.class);

    private static final DateTimeFormatter DATE_TAKEN_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd kk:mm:ss");

    private final Path destinationPath;

    public StructuringService(@Value("${folder.destination}") String destinationFolder) {
        this.destinationPath = Path.of(destinationFolder);
    }

    public PhotoDescriptor toPhotoDescriptor(PhotoMeta photoMeta) {
        Set<String> tags = photoMeta.getTags().stream().map(Tag::getTag).collect(Collectors.toSet());
        return PhotoDescriptor.builder()
                .id(photoMeta.getId())
                .name(photoMeta.getName())
                .description(photoMeta.getDescription())
                .dateTaken(LocalDateTime.parse(photoMeta.getDateTaken(), DATE_TAKEN_FORMAT))
                .flickrFilename(photoMeta.getOriginal())
                .tags(tags)
                .build();
    }


    public Path deriveAlbumPath(Album album, Collection<PhotoDescriptor> albumPhotos) {
        String year = deriveYear(albumPhotos);
        String albumFolder = FileNameSanitizer.text(album.getTitle());

        return destinationPath.resolve(year).resolve(albumFolder);
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


    public Map<PhotoDescriptor, List<AlbumDescriptor>> computePhotoDescriptorToAlbumDescriptorIndex(List<AlbumDescriptor> albumDescriptors) {
        Map<PhotoDescriptor, List<AlbumDescriptor>> photoToAlbum = new HashMap<>();
        for (AlbumDescriptor album : albumDescriptors) {
            for (PhotoDescriptor photo : album.getPhotos()) {
                List<AlbumDescriptor> appearedInAlbums = photoToAlbum.computeIfAbsent(photo, id -> new ArrayList<>());
                appearedInAlbums.add(album);
            }
        }
        return photoToAlbum;
    }

    public List<AlbumDescriptor> deriveAlbumStructure(List<Album> albums, Map<String, PhotoMeta> photoMetaIndex) {
        Map<String, PhotoDescriptor> photos = photoMetaIndex.values().stream()
                .map(this::toPhotoDescriptor)
                .collect(Collectors.toMap(PhotoDescriptor::getId, Function.identity()));

        // First consider the Album folders and assign all photos to Albums...
        List<AlbumDescriptor> albumDescriptors = albums.stream()
                .map(album -> {
                    List<PhotoDescriptor> albumPhotos = album.getPhotoIds().stream()
                            .map(photoId -> {
                                PhotoDescriptor photoDesc = photos.get(photoId);
                                if (photoDesc == null) {
                                    LOG.warn("No PhotoDescriptor found for PhotoId {} in Album {}", photoId, album.getTitle());
                                }
                                return photoDesc;
                            })
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                    Path albumPath = deriveAlbumPath(album, albumPhotos);
                    return new AlbumDescriptor(album.getId(), album.getTitle(), albumPhotos, albumPath);
                })
                .collect(Collectors.toList());

        Set<String> albumAssignedPhotoIds = albumDescriptors.stream()
                .map(AlbumDescriptor::getPhotos)
                .flatMap(List::stream)
                .map(PhotoDescriptor::getId)
                .collect(Collectors.toSet());

        // Add an AlbumDescriptor with the photo's not in an album
        Set<String> photoIdsWithoutAlbum = photos.keySet();
        photoIdsWithoutAlbum.removeAll(albumAssignedPhotoIds);
        List<PhotoDescriptor> uncategorizedPhotos = photoIdsWithoutAlbum.stream().map(photos::get).collect(Collectors.toList());

        AlbumDescriptor uncategorizedAlbum = new AlbumDescriptor("UNCATEGORIZED", "Uncategorized Photos", uncategorizedPhotos, destinationPath.resolve("Uncategorized_Photos"));
        albumDescriptors.add(uncategorizedAlbum);
        return albumDescriptors;
    }
}
