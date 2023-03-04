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
import java.nio.file.Path;
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

        return PhotoMapper.convertToDtoList(photos);
    }

    public List<PhotoDto> movePhotos(Long fromAlbumId, Long toAlbumId, List<Long> photoIds) throws IOException {
        // 1. photo의 fileName 을 가져오고 같은 이름을 toAlbumId에서 체킹하여 fileName 수정
        // 2. photo의 original, thumb 주소를 toAlbumId에 맞춰서 저장
        // 3. photo의 원본을 File들로 저장하고 toAlbumId로 옮기기
        // 4. fromAlbumId에 있던 원본 파일들 삭제

        List<Photo> photos = new ArrayList<>();
        for (Long photoId : photoIds) {
            Optional<Photo> res = photoRepository.findById(photoId);
            if (res.isPresent()) {
                // photo 얻고 초기 주소에 대한 파일 얻음
                Photo photo = res.get();
                File originalFile = new File(Constants.PATH_PREFIX + photo.getOriginalUrl());
                File thumbFile = new File(Constants.PATH_PREFIX + photo.getThumbUrl());

                // 옮기려는 폴더에 겹치는 이름 있는지 확인하고 있으면 새 이름 만든 후 photo의 url 변경
                String fileName = photo.getFileName();
                fileName = getNextFileName(fileName, toAlbumId);
                photo.setOriginalUrl("/photos/original/" + toAlbumId + "/" + fileName);
                photo.setThumbUrl("/photos/thumb/" + toAlbumId + "/" + fileName);

                // 변경된 url에 대한 파일 받음
                File originalChangeFile = new File(Constants.PATH_PREFIX + photo.getOriginalUrl());
                File thumbChangeFile = new File(Constants.PATH_PREFIX + photo.getThumbUrl());

                // 기존에 있던 사진 내용을 변경 주소로 옮김
                Files.copy(originalFile.toPath(), originalChangeFile.toPath());
                Files.copy(thumbFile.toPath(), thumbChangeFile.toPath());

                // 원본 주소에 있는 파일 삭제
                Files.delete(Path.of(String.valueOf(originalFile.toPath())));
                Files.delete(Path.of(String.valueOf(thumbFile.toPath())));

                // 변경된 사진 추가
                photos.add(photo);
            } else {
                throw new EntityNotFoundException("PhotoId에 해당하는 Photo 없음");
            }
        }
        return PhotoMapper.convertToDtoList(photos);
    }

    public List<PhotoDto> deletePhotos(List<Long> photoIds) throws IOException {
        List<Photo> photos = new ArrayList<>();
        for (Long photoId : photoIds) {
            Optional<Photo> res = photoRepository.findById(photoId);
            if (res.isPresent()) {
                Photo photo = res.get();
                photos.add(photo);
                Files.delete(Path.of(Constants.PATH_PREFIX + photo.getOriginalUrl()));
                Files.delete(Path.of(Constants.PATH_PREFIX + photo.getThumbUrl()));
                photoRepository.deleteById(photoId);
            } else {
                throw new EntityNotFoundException("사진을 삭제 에러");
            }
        }
        return PhotoMapper.convertToDtoList(photos);
    }
}
