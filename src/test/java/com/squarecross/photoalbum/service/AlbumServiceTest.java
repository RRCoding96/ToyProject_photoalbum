package com.squarecross.photoalbum.service;

import com.squarecross.photoalbum.controller.AlbumController;
import com.squarecross.photoalbum.domain.Album;
import com.squarecross.photoalbum.domain.Photo;
import com.squarecross.photoalbum.dto.AlbumDto;
import com.squarecross.photoalbum.repository.AlbumRepository;
import com.squarecross.photoalbum.repository.PhotoRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AlbumServiceTest {

    @Autowired
    AlbumRepository albumRepository;
    @Autowired
    PhotoRepository photoRepository;
    @Autowired
    AlbumService albumService;
    @Autowired
    AlbumController albumController;

    @Test
    void getAlbum() {
        Album album = new Album();
        album.setAlbumName("테스트");
        Album savedAlbum = albumRepository.save(album);

        AlbumDto resAlbum = albumService.getAlbum(savedAlbum.getAlbumId());
        assertEquals("테스트", resAlbum.getAlbumName());
    }

    @Test
    @DisplayName("이름으로 앨범 찾기 실패시 예외 발생")
    void getAlbumByNameException() {
        Album album = new Album();
        album.setAlbumName("테스트");
        Album savedAlbum = albumRepository.save(album);

        assertThrows(EntityNotFoundException.class, () -> {
            Album unSavedName = albumService.getAlbumByName("없는 이름");
        });
    }

    @Test
    void testPhotoCount() {
        Album album = new Album();
        album.setAlbumName("테스트 앨범");
        Album savedAlbum = albumRepository.save(album);

        // 사진을 생성하고, setAlbum을통해 앨범을 지정해준 이후, repository에 사진을 저장한다
        Photo photo1 = new Photo();
        photo1.setFileName("사진1");
        photo1.setAlbum(savedAlbum);
        photoRepository.save(photo1);

        Photo photo2 = new Photo();
        photo2.setFileName("사진2");
        photo2.setAlbum(savedAlbum);
        photoRepository.save(photo2);

        Photo photo3 = new Photo();
        photo3.setFileName("사진3");
        photo3.setAlbum(savedAlbum);
        photoRepository.save(photo3);

        int expectedCount = 3;
        int actualCount = photoRepository.countByAlbum_AlbumId(savedAlbum.getAlbumId());
        assertThat(actualCount).isEqualTo(expectedCount);
    }

    @Test
    void testAlbumCreate() throws IOException {
        AlbumDto albumDto = new AlbumDto();
        albumDto.setAlbumName("test 1");

        AlbumDto createdAlbum = albumService.createAlbum(albumDto);

        assertThat(albumDto.getAlbumName()).isEqualTo(createdAlbum.getAlbumName());
    }

    @Test
    void deleteMadeFolder() throws IOException {
        Path testFolderPath = Paths.get("테스트 폴더");
        Files.createDirectory(testFolderPath);

        Files.delete(testFolderPath);

        assertFalse(Files.exists(testFolderPath));
    }
}