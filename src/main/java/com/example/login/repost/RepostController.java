package com.example.login.repost;

import com.example.login.post.dto.PostRequestDto;
import com.example.login.post.dto.PostResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/posts/{postId}")
@RequiredArgsConstructor
public class RepostController {
    private final RepostService repostService;

    // 재게시를 하는 컨트롤러
    @PostMapping("/reposts")
    public ResponseEntity<RepostResponseDto> repost(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long postId
    ){
        return ResponseEntity.ok(repostService.repost(userId, postId));
    }
    // postresponse?
    @PostMapping("/quotes")
    public ResponseEntity<PostResponseDto> quote(
            @RequestBody @Valid PostRequestDto dto,
            @AuthenticationPrincipal Long userId,
            @PathVariable Long postId
    ){
        return ResponseEntity.ok(repostService.quote(dto, userId, postId));
    }
}
