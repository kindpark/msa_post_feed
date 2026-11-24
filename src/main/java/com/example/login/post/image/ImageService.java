package com.example.login.post.image;

import com.example.login.common.BusinessException;
import com.example.login.common.ErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
// 여기서는 Lombok 생성자 관련을 뗀 이유
// Lombok은 매개변수에 @Value를 붙여주지 않는다.
// 생성자 안에서는 로직을 넣을수 없다.
@Service
public class ImageService {
    private final Path uploadPath;

    public ImageService(@Value("${file.upload-dir}") Path uploadPath){
        this.uploadPath = uploadPath;
    }
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("png", "jpg", "jpeg", "gif", "webp");
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/png", "image/jpeg", "image/gif", "image/webp"
    );
    @PostConstruct
    public void init(){
        try{
            Files.createDirectories(this.uploadPath);
        }catch(IOException e){
            throw new RuntimeException("업로드 디렉토리를 생성할수 없습니다.", e);
        }
    }
    // 저장된 이미지 로드
    // @param fileName : 저장된 고유 파일명
    // 이미지 리소스를 리턴
    public Resource loadImageAsResource(String fileName) {
        try{
            Path filePath = this.uploadPath.resolve(fileName).normalize();
            // 저장할때보다 로드할때 이미지 루트 해킹이 더 쉬움
            // 따라서 로드할때 또한 검증 코드(startsWith를) 넣는게 좋음
            if(!filePath.startsWith(this.uploadPath)){
                throw new BusinessException(ErrorCode.IMAGE_NOT_FOUND);
            }

            Resource resource = new UrlResource(filePath.toUri());

            if(resource.exists() && resource.isReadable()){
                return resource;
            }
            else{
                throw new BusinessException(ErrorCode.IMAGE_NOT_FOUND);
            }
        }catch(MalformedURLException e){
            throw new BusinessException(ErrorCode.IMAGE_NOT_FOUND);
        }
    }
    // 이미지를 저장한다.
    public String storeImage(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_IMAGE);
        }

        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));

        // 1. 확장자 추출 및 허용 여부 검증 (png, jpg, jpeg, gif, webp)
        String extension = extractAndValidateExtension(originalFilename);

        // 2. MIME 타입(파일 헤더 기반 Content-Type) 추가 검증 (확장자 위조 방지)
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessException(ErrorCode.INVALID_IMAGE);
        }

        // 3. 파일명 암호화 (UUID + 원본파일명 + 현재시간을 SHA-256으로 해싱)
        String encryptedName = generatedEncryptedFileName(originalFilename);
        String storedFilename = encryptedName + "." + extension;

        try {
            // 경로 조작 방지
            Path targetLocation = this.uploadPath.resolve(storedFilename).normalize();
            if (!targetLocation.startsWith(this.uploadPath)) {
                throw new SecurityException("잘못된 파일 경로입니다.");
            }

            // 파일 저장
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            return storedFilename;

        } catch (IOException e) {
            throw new RuntimeException("파일 저장 중 오류가 발생하였습니다: " + originalFilename, e);
        }
    }
    /**
     * 확장자 추출 및 화이트리스트 검증
     */
    private String extractAndValidateExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == filename.length() - 1) {
            throw new BusinessException(ErrorCode.INVALID_IMAGE);
        }

        String ext = filename.substring(dotIndex + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BusinessException(ErrorCode.INVALID_IMAGE);
        }
        return ext;
    }

    // 파일명 단방향 암호화
    private String generatedEncryptedFileName(String originalFilename){
        try{
            String rawText = originalFilename + "_" + UUID.randomUUID() + "_" + System.nanoTime();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawText.getBytes(StandardCharsets.UTF_8));

            // 바이트 배열을 16진수(Hex) 문자열로 변환 (Java 17 이상)
            return HexFormat.of().formatHex(hash);
        }catch(NoSuchAlgorithmException e){
            return UUID.randomUUID().toString().replace("-","");
        }
    }
}
