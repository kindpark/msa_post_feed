package com.example.login.feed.controller;

import com.example.login.feed.FeedPageResponseDto;
import com.example.login.feed.FeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/feeds")
@RequiredArgsConstructor
public class FeedController {
    private final FeedService feedService;


    @GetMapping
    public ResponseEntity<FeedPageResponseDto> getFeed(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Long cursor, //seq,
            /*@RequestParam(required = false) String type,*/
            @RequestParam(defaultValue="10") int size){
        return ResponseEntity.ok(feedService.getFeed(userId, cursor, size));
    }
}
