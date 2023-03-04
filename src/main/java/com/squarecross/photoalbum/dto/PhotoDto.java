package com.squarecross.photoalbum.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Setter
@Getter
public class PhotoDto {

    private Long photoId;
    private String fileName;
    private long fileSize;
    private String originalUrl;
    private String thumbUrl;
    private Date uploadedAt;
    private Long albumId;
    private List<Long> photoIds;

//    public Long getPhotoId() {
//        return photoId;
//    }
//
//    public void setPhotoId(Long photoId) {
//        this.photoId = photoId;
//    }
//
//    public String getFileName() {
//        return fileName;
//    }
//
//    public void setFileName(String fileName) {
//        this.fileName = fileName;
//    }
//
//    public long getFileSize() {
//        return fileSize;
//    }
//
//    public void setFileSize(long fileSize) {
//        this.fileSize = fileSize;
//    }
//
//    public String getOriginalUrl() {
//        return originalUrl;
//    }
//
//    public void setOriginalUrl(String originalUrl) {
//        this.originalUrl = originalUrl;
//    }
//
//    public String getThumbUrl() {
//        return thumbUrl;
//    }
//
//    public void setThumbUrl(String thumbUrl) {
//        this.thumbUrl = thumbUrl;
//    }
//
//    public Date getUploadedAt() {
//        return uploadedAt;
//    }
//
//    public void setUploadedAt(Date uploadedAt) {
//        this.uploadedAt = uploadedAt;
//    }
//
//    public Long getAlbumId() {
//        return albumId;
//    }
//
//    public void setAlbumId(Long albumId) {
//        this.albumId = albumId;
//    }
}
