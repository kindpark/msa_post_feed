package com.example.login.post.controller;

import com.example.login.post.dto.PostPageResponseDto;
import com.example.login.post.dto.PostRequestDto;
import com.example.login.post.dto.PostResponseDto;
import com.example.login.post.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    // 내꺼조회
    @GetMapping("/me")
    public ResponseEntity<List<PostResponseDto>> getMyPosts(
            @AuthenticationPrincipal Long userId
    ){
        return ResponseEntity.ok(postService.getMyPosts(userId));
    }
    // 전체조회
    @GetMapping("/all")
    public ResponseEntity<List<PostResponseDto>> getAllPosts(@AuthenticationPrincipal Long userId){
        return ResponseEntity.ok(postService.getAllPosts(userId));
    }
    // 게시글 상세
    @GetMapping("/{postId}")
    public ResponseEntity<PostResponseDto> getPost(
            @PathVariable Long postId,
            @AuthenticationPrincipal Long userId){
        return ResponseEntity.ok(postService.getPost(postId, userId));
    }

    //C
    @PostMapping
    public ResponseEntity<PostResponseDto> createPost(
            @RequestBody @Valid PostRequestDto dto,
            @AuthenticationPrincipal Long userId
    ){
        return ResponseEntity.ok(postService.createPost(dto, userId));
    }

    //댓글 작성(댓글이 아니라 X마냥 피드에 답 남기는)
    @PostMapping("/{postId}/replies")
    public ResponseEntity<PostResponseDto> createReply(
            @RequestBody @Valid PostRequestDto dto,
            @PathVariable Long postId,
            @AuthenticationPrincipal Long userId
    ){
        return ResponseEntity.ok(
                postService.createReply(
                        dto,
                        postId,
                        userId
                ));
    }
    // 답글 목록 (커서 페이지네이션, 오래된 순)
// 응답: { posts, nextCursor } — nextCursor 를 다음 요청의 cursor 로 넘기면 이어서 조회, null 이면 끝
// size: 기본 10, 서비스에서 최대 50 으로 제한
    @GetMapping("/{postId}/replies")
    public ResponseEntity<PostPageResponseDto> getReplies(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long postId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int size
    ){
        return ResponseEntity.ok(postService.getReplies(userId, postId, cursor, size));
    }
    //수정
    @PutMapping("/{postId}")
    public ResponseEntity<PostResponseDto> updatePost(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long postId,
            @RequestBody @Valid PostRequestDto dto) {
            return ResponseEntity.ok(postService.updatePost(postId, userId, dto));
    }
    //삭제
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal Long userId
    ){
        postService.deletePost(postId, userId);
        return ResponseEntity.noContent().build();
    }
}
