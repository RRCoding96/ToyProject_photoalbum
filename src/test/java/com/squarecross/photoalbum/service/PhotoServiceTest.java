package com.squarecross.photoalbum.service;

import com.squarecross.photoalbum.domain.Photo;
import com.squarecross.photoalbum.dto.PhotoDto;
import com.squarecross.photoalbum.repository.PhotoRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;


@SpringBootTest
@Transactional
public class PhotoServiceTest {

    @Autowired
    private PhotoRepository photoRepository;

    @Autowired
    private PhotoService photoService;

    @Test
    void getPhoto() {
        Photo photo = new Photo();
        photo.setPhotoId(1L);
        Photo savedPhoto = photoRepository.save(photo);

        PhotoDto resPhoto = photoService.getPhoto(savedPhoto.getPhotoId());
        Assertions.assertEquals(1L, resPhoto.getPhotoId());
    }
}
