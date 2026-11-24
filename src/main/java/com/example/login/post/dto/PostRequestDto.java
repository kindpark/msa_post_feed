package com.example.login.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostRequestDto {
    @NotBlank
    @Size(max=1000, message="최대 1000자까지 입력 가능합니다.")
    private String content;
    // 주소 하나하나는 리스트 안쪽에서 검사한다. @Pattern 은 null 을 통과시키므로 @NotNull 도 필요하다
    @Size(max=4, message="이미지는 최대 4장까지 가능합니다.")
    private List<
            @NotNull(message="이미지 주소가 비어 있습니다.")
            @Size(max=255, message="이미지 주소는 255자 이하여야 합니다.")
            @Pattern(regexp="^(https?://|/api/v1/images/).+", message="올바른 이미지 URL 형식이 아닙니다.")
            String> imageUrls;
}
