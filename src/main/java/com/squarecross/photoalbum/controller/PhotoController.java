package com.squarecross.photoalbum.controller;

import com.squarecross.photoalbum.dto.PhotoDto;
import com.squarecross.photoalbum.service.PhotoService;
import org.apache.tomcat.util.http.fileupload.ByteArrayOutputStream;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/albums/{albumId}/photos")
public class PhotoController {

    @Autowired
    private PhotoService photoService;

    @RequestMapping(value = "/{photoId}", method = RequestMethod.GET)
    public ResponseEntity<PhotoDto> getPhotoInfo(@PathVariable("photoId") final Long photoId) {
        PhotoDto photoDto = photoService.getPhoto(photoId);
        return new ResponseEntity<>(photoDto, HttpStatus.OK);
    }

    @RequestMapping(value = "", method = RequestMethod.POST)
    public ResponseEntity<List<PhotoDto>> uploadPhotos(@PathVariable("albumId") final Long albumId,
                                                       @RequestParam("photos") MultipartFile[] files) throws IOException {
        List<PhotoDto> photoDtos = new ArrayList<>();
        for (MultipartFile file : files) {
            if (!file.getContentType().equals("image/jpeg")
                    && !file.getContentType().equals("image/png")
                    && !file.getContentType().equals("image/gif")
                    && !file.getContentType().equals("image/jpg")) {
                throw new RuntimeException("이미지가 아닌 파일이 입력되었습니다.");
            }

            PhotoDto photoDto = photoService.savePhoto(file, albumId);
            photoDtos.add(photoDto);
        }
        return new ResponseEntity<>(photoDtos, HttpStatus.OK);
    }

    @RequestMapping(value = "/download", method = RequestMethod.GET)
    public void downloadPhotos(@RequestParam("photoIds") Long[] photoIds, HttpServletResponse response) {
        try {
            if (photoIds.length == 1) {
                File file = photoService.getImageFile(photoIds[0]);
                OutputStream outputStream = response.getOutputStream();
                IOUtils.copy(new FileInputStream(file), outputStream);
                outputStream.close();
            } else {
//                List<File> fileList = new ArrayList<>();
//                for (Long id : photoIds) {
//                    File imageFile = photoService.getImageFile(id);
//                    fileList.add(imageFile);
//                }
//
//                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//                ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream);
//
//                for (File imageFile : fileList) {
//                    FileInputStream fileInputStream = new FileInputStream(imageFile);
//                    ZipEntry zipEntry = new ZipEntry(imageFile.getName());
//                    zipOutputStream.putNextEntry(zipEntry);
//
//                    byte[] buffer = new byte[1024];
//                    int len;
//                    while ((len = fileInputStream.read(buffer)) > 0) {
//                        zipOutputStream.write(buffer, 0, len);
//                    }
//
//                    fileInputStream.close();
//                    zipOutputStream.closeEntry();
//                }
//
//                zipOutputStream.close();
//
//                // Zip 파일을 byte[] 형태로 변환합니다.
//                byte[] zipBytes = byteArrayOutputStream.toByteArray();
//
//                // HTTP 응답으로 Zip 파일을 반환합니다.
//                HttpHeaders headers = new HttpHeaders();
//                headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
//                headers.setContentDisposition(ContentDisposition.builder("attachment")
//                        .filename("images.zip")
//                        .build());

                // 새 임시 zip 파일 생성
                File zipFile = File.createTempFile("photos", ".zip");

                // zip 파일에 쓸 ZipOutputStream을 생성합니다.
                ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFile));

                // 각 사진 ID를 반복하여 zip 파일에 추가합니다.
                for (Long photoId : photoIds) {
                    File photoFile = photoService.getImageFile(photoId);

                    // zip 파일에 새 항목 만들기
                    zipOut.putNextEntry(new ZipEntry(photoFile.getName()));

                    // 사진 파일을 zip 출력 스트림에 쓰기
                    IOUtils.copy(new FileInputStream(photoFile), zipOut);

                    // 현재 항목 닫기
                    zipOut.closeEntry();
                }

                // zip output stream 닫기
                zipOut.close();

                // zip 파일이 반환되고 있음을 나타내도록 응답 헤더를 설정합니다.
                response.setContentType("application/zip");
                response.setHeader("Content-Disposition", "attachment; filename=\"photos.zip\"");

                // 응답 출력 스트림에 zip 파일 쓰기
                OutputStream outputStream = response.getOutputStream();
                IOUtils.copy(new FileInputStream(zipFile), outputStream);
                outputStream.close();

                // 임시 zip file을 삭제
                zipFile.delete();
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Error");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @RequestMapping(value = "", method = RequestMethod.GET)
    public ResponseEntity<List<PhotoDto>> getPhotoList(@RequestParam(value = "sort", required = false, defaultValue = "byDate") String sort,
                                                 @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
                                                 @RequestParam(value = "orderBy", required = false, defaultValue = "desc") String orderBy) {
        List<PhotoDto> photoList = photoService.getPhotoList(keyword, sort, orderBy);
        return new ResponseEntity<>(photoList, HttpStatus.OK);
    }
}
