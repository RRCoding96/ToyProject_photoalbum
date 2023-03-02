package com.squarecross.photoalbum.repository;

import com.squarecross.photoalbum.domain.Photo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PhotoRepository extends JpaRepository<Photo, Long> {
    int countByAlbum_AlbumId(Long AlbumId);

    List<Photo> findTop4ByAlbum_AlbumIdOrderByUploadedAtDesc(Long AlbumId);

    Optional<Photo> findByFileNameAndAlbum_AlbumId(String photoName, Long albumId);

    List<Photo> findByFileNameContainingOrderByUploadedAtDesc(String keyword);
    List<Photo> findByFileNameContainingOrderByUploadedAtAsc(String keyword);
    List<Photo> findByFileNameContainingOrderByFileNameDesc(String keyword);
    List<Photo> findByFileNameContainingOrderByFileNameAsc(String keyword);
}
