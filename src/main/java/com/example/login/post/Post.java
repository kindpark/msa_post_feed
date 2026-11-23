package com.example.login.post;

import com.example.login.common.BusinessException;
import com.example.login.common.ErrorCode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="posts")
@Getter
@NoArgsConstructor(access= AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@SQLDelete(sql = "update posts set is_deleted=true where id = ?")
@SQLRestriction("is_deleted = false")
@EntityListeners(AuditingEntityListener.class)
public class Post {
    //글 그자체의 ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //글 작성자
    private Long userId;
    // 답글 만들기 위한것(없으면 null)
    private Long parentId;
    // 이 게시글 형 대화가 시작된 원본 글의 id
    private Long rootId;

    private Long quoteId;

    private String content;

    private String imageUrl;
    @ElementCollection
    @CollectionTable(name="post_images")
    @OrderColumn(name="sort_order")
    @BatchSize(size=100)
    @Builder.Default
    private List<String> imageUrls=new ArrayList<>();

    @Builder.Default
    @Column(nullable=false)
    private Integer likeCount=0;
    @Builder.Default
    @Column(nullable = false)
    private Integer commentCount=0;
    @Builder.Default
    @Column(nullable = false)
    private Integer repostCount=0;
    @Builder.Default
    @Column(nullable = false)
    private Integer quoteCount=0;
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // 삭제 상태 필드 기본값은 false
    @Builder.Default
    @Column(nullable = false)
    private boolean isDeleted = false;

    public void update(Long userId, String content, List<String> imageUrls){
        if(!userId.equals(this.userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        this.content = content;
        changeImages(imageUrls);
    }

    public void changeImages(List<String> urls){
        if(urls ==null){
            this.imageUrl = null;
        }
        if(urls.size() == 1){
            this.imageUrl = urls.getFirst();
            this.imageUrls.clear();
            this.imageUrls.addAll(urls);
        }
        else{
            this.imageUrl = urls.getFirst();
            this.imageUrls.clear();
            this.imageUrls.addAll(urls);
        }
    }
}
