package hairwhere.hairwhere.service;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class GCPStorageService {
    private final Storage gcpStorage;

    @Value("${cloud.gcp.bucket-name}")
    private String bucketName;

    public String upload(MultipartFile image) {
        if (image.isEmpty()) {
            throw new IllegalArgumentException("사진이 없습니다.");
        }
        return this.uploadImage(image);
    }

    private String uploadImage(MultipartFile image) {
        validateImageFileExtention(image.getOriginalFilename());
        try {
            return this.uploadImageToGCP(image);
        } catch (IOException e) {
            throw new IllegalArgumentException("이미지 저장 실패");
        }
    }

    private void validateImageFileExtention(String filename) {
        int lastDotIndex = filename.lastIndexOf(".");
        if (lastDotIndex == -1) {
            throw new IllegalArgumentException("잘못된 형식입니다.");
        }
        String extension = filename.substring(lastDotIndex + 1).toLowerCase();
    }

    private boolean isValidImageContentType(String contentType) {
        return Arrays.asList(
            "image/jpeg", "image/jpg", "image/png",
            "image/gif", "image/bmp", "image/webp"
        ).contains(contentType.toLowerCase());
    }

    private String uploadImageToGCP(MultipartFile image) throws IOException {
        if (!isValidImageContentType(image.getContentType())) {
            throw new IllegalArgumentException("Invalid image content type: " + image.getContentType());
        }

        String originalFilename = image.getOriginalFilename();
        String gcpFileName = UUID.randomUUID().toString().substring(0, 10) + originalFilename;

        BlobId blobId = BlobId.of(bucketName, gcpFileName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
            .setContentType(image.getContentType())
            .build();

        try {
            gcpStorage.create(blobInfo, image.getBytes());

            // URL 인코딩 적용
            String encodedFileName = URLEncoder.encode(gcpFileName, StandardCharsets.UTF_8.toString())
                .replace("+", "%20"); // 공백을 %20으로 변환

            // 공개 URL 생성 (Storage 객체에 설정된 ACL에 따라 달라질 수 있음)
            return String.format("https://storage.googleapis.com/%s/%s", bucketName, encodedFileName);
        } catch (Exception e) {
            log.error("GCP 업로드 중 오류 발생: {}", e.getMessage());
            throw new RuntimeException("GCP 업로드 실패", e);
        }
    }

    public void deleteImageFromGCP(String imageUrl) {
        String key = extractKeyFromUrl(imageUrl);
        try {
            BlobId blobId = BlobId.of(bucketName, key);
            gcpStorage.delete(blobId);
        } catch (Exception e) {
            throw new IllegalArgumentException("삭제 중 문제 발생");
        }
    }

    private String extractKeyFromUrl(String imageUrl) {
        return imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
    }
}
