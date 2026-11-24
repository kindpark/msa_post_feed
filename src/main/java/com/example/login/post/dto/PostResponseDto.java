package com.example.login.post.dto;

import com.example.login.post.entity.Post;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostResponseDto {
    private Long id;
    private Long userId;
    private Long parentId;
    private Long quoteId;
    private String content;
    private boolean liked;
    private boolean reposted;
    private String imageUrl;
    private List<String> imageUrls;
    private Integer likeCount;
    private Integer commentCount;
    private Integer repostCount;
    private Integer quoteCount;
    private LocalDateTime createdAt;

    public static PostResponseDto from(
            Post entity,
            boolean liked,
            boolean reposted) {
        return PostResponseDto.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .parentId(entity.getParentId())
                .quoteId(entity.getQuoteId())
                .content(entity.getContent())
                .liked(liked)
                .reposted(reposted)
                .imageUrl(entity.getImageUrl())
                .imageUrls(List.copyOf(entity.getImageUrls()))
                .likeCount(entity.getLikeCount())
                .commentCount(entity.getCommentCount())
                .repostCount(entity.getRepostCount())
                .quoteCount(entity.getQuoteCount())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
