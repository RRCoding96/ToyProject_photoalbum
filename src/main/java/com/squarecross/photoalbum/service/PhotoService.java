package com.squarecross.photoalbum.service;

import com.squarecross.photoalbum.Constants;
import com.squarecross.photoalbum.domain.Album;
import com.squarecross.photoalbum.domain.Photo;
import com.squarecross.photoalbum.dto.PhotoDto;
import com.squarecross.photoalbum.mapper.PhotoMapper;
import com.squarecross.photoalbum.repository.AlbumRepository;
import com.squarecross.photoalbum.repository.PhotoRepository;
import org.imgscalr.Scalr;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.persistence.EntityNotFoundException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class PhotoService {

    @Autowired
    private PhotoRepository photoRepository;

    @Autowired
    private AlbumRepository albumRepository;

    private final String original_path = Constants.PATH_PREFIX + "/photos/original";
    private final String thumb_path = Constants.PATH_PREFIX + "/photos/thumb";

    public PhotoDto getPhoto(Long photoId) {
        Optional<Photo> res = photoRepository.findById(photoId);
        if(res.isPresent()) {
            PhotoDto photoDto = PhotoMapper.convertToDto(res.get());
            return photoDto;
        } else {
            throw new EntityNotFoundException(String.format("사진 아이디 %d로 조회되지 않았습니다.", photoId));
        }
    }

    public PhotoDto savePhoto(MultipartFile file, Long albumId) throws IOException {
        Optional<Album> res = albumRepository.findById(albumId);
        if(res.isEmpty()) {
            throw new EntityNotFoundException("앨범이 존재하지 않습니다");
        }
        String fileName = file.getOriginalFilename();
        int fileSize = (int)file.getSize(); //long은 64바이트 int는 32바이트다. int로 나타낼 수 있는 최대 는 대략 2GB인데 그렇게 커질 일 없으니 int 로 변환
        fileName = getNextFileName(fileName, albumId);
        saveFile(file, albumId, fileName);

        Photo photo = new Photo();
        photo.setOriginalUrl("/photos/original/" + albumId + "/" + fileName);
        photo.setThumbUrl("/photos/thumb/" + albumId + "/" + fileName);
        photo.setFileName(fileName);
        photo.setFileSize(fileSize);
        photo.setAlbum(res.get());
        Photo createdPhoto = photoRepository.save(photo);
        return PhotoMapper.convertToDto(createdPhoto);
    }

    private String getNextFileName(String fileName, Long albumId) {
        String fileNameNoExt = StringUtils.stripFilenameExtension(fileName);
        String ext = StringUtils.getFilenameExtension(fileName);

        Optional<Photo> res = photoRepository.findByFileNameAndAlbum_AlbumId(fileName, albumId);

        int count = 2;
        while (res.isPresent()) {
            fileName = String.format("%s (%d).%s", fileNameNoExt, count, ext);
            res = photoRepository.findByFileNameAndAlbum_AlbumId(fileName, albumId);
            count++;
        }

        return fileName;
    }

    public void saveFile(MultipartFile file, Long albumId, String fileName) throws IOException {
        try {
            String filePath = albumId + "/" + fileName;
            Files.copy(file.getInputStream(), Paths.get(original_path + "/" + filePath));

            BufferedImage thumbImg = Scalr.resize(ImageIO.read(file.getInputStream()), Constants.THUMB_SIZE, Constants.THUMB_SIZE);
            File thumbFile = new File(thumb_path + "/" + filePath);
            String ext = StringUtils.getFilenameExtension(fileName);
            if (ext == null) {
                throw new IllegalArgumentException("No Extension");
            }
            ImageIO.write(thumbImg, ext, thumbFile);
        } catch (Exception e) {
            throw new RuntimeException("Could not store the file. Error: " + e.getMessage());
        }

    }

    public File getImageFile(Long photoId) {
        Optional<Photo> res = photoRepository.findById(photoId);
        if(res.isEmpty()) {
            throw new EntityNotFoundException(String.format("사진을 ID %d으로 찾을 수 없습니다.", photoId));
        }
        return new File(Constants.PATH_PREFIX + res.get().getOriginalUrl());
    }

    public List<PhotoDto> getPhotoList(String keyword, String sort, String orderBy) {
        List<Photo> photos;
        if (sort.equals("byDate")) {
            if (orderBy.equals("asc")) {
                photos = photoRepository.findByFileNameContainingOrderByUploadedAtAsc(keyword);
            } else {
                photos = photoRepository.findByFileNameContainingOrderByUploadedAtDesc(keyword);
            }
        } else if (sort.equals("byName")) {
            if (orderBy.equals("asc")) {
                photos = photoRepository.findByFileNameContainingOrderByFileNameAsc(keyword);
            } else {
                photos = photoRepository.findByFileNameContainingOrderByFileNameDesc(keyword);
            }
        } else {
            throw new EntityNotFoundException("알 수 없는 정렬 기준입니다.");
        }

        List<PhotoDto> photoDtos = new ArrayList<>();
        for (Photo photo : photos) {
            photoDtos.add(PhotoMapper.convertToDto(photo));
        }

        return photoDtos;
    }
}
