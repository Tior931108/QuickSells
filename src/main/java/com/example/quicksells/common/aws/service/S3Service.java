package com.example.quicksells.common.aws.service;

import com.example.quicksells.common.enums.ExceptionCode;
import com.example.quicksells.common.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    public String uploadImage(MultipartFile file) {

        try {
            // 원본 파일명 추출
            String originalFilename = file.getOriginalFilename();

            if (!StringUtils.hasText(originalFilename)) {
                throw new CustomException(ExceptionCode.NOT_FOUND_FILE);
            }

            // 파일 확장자 추출
            String fileExtension = getFileExtension(originalFilename);

            if (!isImageFile(fileExtension)) {
                throw new CustomException(ExceptionCode.INVALID_FILE_TYPE);
            }

            // 파일 용량 체크
            validateImageFile(file);


            // S3에 저장할 고유한 파일명 생성 (UUID + 확장자)
            String key = "products/" + UUID.randomUUID() + fileExtension;

            // S3 파일 업로드 요청 생성
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .acl("public-read")
                    .build();

            // 파일 데이터를 RequestBody로 변환 (AWS SDK가 이해하도록)
            RequestBody requestBody = RequestBody.fromBytes(file.getBytes());

            // S3에 파일 업로드 실행
            s3Client.putObject(putObjectRequest, requestBody);

            // 업로드 성공 시 파일 URL 반환
            return "https://" + bucket + ".s3.amazonaws.com/" + key;

        } catch (IOException | S3Exception e) {
            log.error("S3 Upload Error: ", e);
            throw new CustomException(ExceptionCode.FILE_UPLOAD_FAIL);
        }
    }

    // S3에 저장된 이미지 삭제
    public void deleteImage(String imageUrl) {

        if (!StringUtils.hasText(imageUrl)) return;

        try {
            String key = extractKeyFromUrl(imageUrl);
            s3Client.deleteObject(b -> b.bucket(bucket).key(key));
        } catch (S3Exception e) {
            log.error("S3 Delete Error", e);
            throw new CustomException(ExceptionCode.FILE_DELETE_FAIL);
        }
    }

    // S3 key 추출
    private String extractKeyFromUrl(String imageUrl) {
        String prefix = "https://" + bucket + ".s3.amazonaws.com/";
        return imageUrl.replace(prefix, "");
    }

    // 파일 확장자 추출 메서드
    private String getFileExtension(String filename) {

        int lastDotIndex = filename.lastIndexOf('.');

        if (lastDotIndex == -1) {
            return "";
        }

        return filename.substring(lastDotIndex);
    }

    // 이미지 파일 확인 메서드
    private boolean isImageFile(String fileExtension) {

        String lowerExtension = fileExtension.toLowerCase();

        return lowerExtension.equals(".jpg") ||
                lowerExtension.equals(".jpeg") ||
                lowerExtension.equals(".png") ||
                lowerExtension.equals(".gif");
    }

    // 이미지 파일 용량 제한
    private MultipartFile validateImageFile(MultipartFile file) {
        String contentType = file.getContentType();

        // 파일 타입 검증
        if (contentType == null || !contentType.startsWith("image/")){
            throw new CustomException(ExceptionCode.ONLY_IMAGE_FILE);
        }

        // 파일 용량 제한 체크
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new CustomException(ExceptionCode.PAYLOAD_TOO_LARGE);
        }
        // 검증 후 통과 시 원본 파일 반환
        return file;
    }
}
