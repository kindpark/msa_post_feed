package com.example.login.post.controller;

import com.example.login.post.dto.PostCountDto;
import com.example.login.post.dto.PostPageResponseDto;
import com.example.login.post.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserPostController {

    private final PostService postService;
    // 탭 목록들
    @GetMapping("/{targetUserId}/posts")
    public ResponseEntity<PostPageResponseDto> getUserPosts(
            @AuthenticationPrincipal Long viewerId,
            @PathVariable Long targetUserId,
            @RequestParam(defaultValue = "posts") String tab,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int size){
        return ResponseEntity.ok(postService.getUserPosts(
                viewerId,
                targetUserId,
                tab,
                cursor,
                size
                ));
    }

    @GetMapping("/{id}/posts/count")
    public ResponseEntity<PostCountDto> getUserPostCount(
            @PathVariable Long id
    ){
        return ResponseEntity.ok(postService.getPostCount(id));
    }
}
