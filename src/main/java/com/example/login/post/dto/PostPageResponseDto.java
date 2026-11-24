package com.example.login.post.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PostPageResponseDto {
    // 이대로면 프론트는 리스트만 받아, 답글이 안보일수 있다.
    // 따라서 리스트도 반환하고 커서도 반환하는 dto를 생성
    // PostResponseDto에 nextCursor를 넣으면 안 되는 이유는,
    // 글 하나를 응답할 때도 의미 없는 nextCursor:
    // null이 붙어 나가기 때문입니다. 역할이 다르면 클래스를 나누는 게 맞습니다.
    private List<PostResponseDto> posts;
    private Long nextCursor;

}
