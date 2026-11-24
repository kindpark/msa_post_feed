package com.example.login.post.repository;


import com.example.login.post.dto.ParentIdAndCount;
import com.example.login.post.dto.PostWithLiked;
import com.example.login.post.entity.Post;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {
    //회원 탈퇴할때 글을 지우는 처리
    List<Post> findByUserId(Long userId);

    Long countByUserId(Long userId);

    List<Post> findAllById(Iterable<Long> ids);
    //최근 포스트 한 사람들중 포스트아이디,

    @Query("select p.id from Post p" +
            " where p.userId in :authorIds" +
            " and p.parentId is null order by p.id desc")
    List<Long> findRecentPostIdsByAuthors(
            @Param("authorIds") List<Long> authorIds,
            Limit size);
    // 글마다 나의 좋아요가 있는가?
    // DB의 조건문에 대해선 내가 안배웠으므로..
    String TEXT_ON_MY_LIKE_AND_REPOST = "select p as post," +
            " case when l.id is not null then true else false end as liked," +
            " case when r.id is not null then true else false end as reposted"+
            " from Post p" +
            " left join PostLike l" +
            " on l.postId = p.id" +
            " and l.userId =:viewerId"+
            " left join Repost r" +
            " on r.postId = p.id" +
            " and r.userId =:viewerId";
    // 내가 좋아요를 했는지에 대한 여부
    // 나에 대한 식별
    @Query(TEXT_ON_MY_LIKE_AND_REPOST + " where p.id =:id")
    Optional<PostWithLiked> findByIdWithLiked(
            @Param("id") Long id,
            @Param("viewerId") Long viewerId
    );
    // 답글 목록 및 답글에 좋아요를 눌렀는가
    @Query(TEXT_ON_MY_LIKE_AND_REPOST +
            " where p.parentId =:parentId" +
            " and (:cursor is null or p.id > :cursor)" +
            " order by p.id asc")
    List<PostWithLiked> findThreadWithLiked(
        @Param("parentId") Long parentId,
        @Param("cursor") Long cursor,
        @Param("viewerId") Long viewerId,
        Limit limit
    );
    // userId 기반 Liked를 찾음
    @Query(TEXT_ON_MY_LIKE_AND_REPOST +
        " where p.userId =:userId" +
            " order by p.id desc")
    List<PostWithLiked> findByUserIdWithLiked(
            @Param("userId") Long userId,
            @Param("viewerId") Long viewerId
    );
    // 모든 글과 내가 좋아요를 눌렀는가
    @Query(TEXT_ON_MY_LIKE_AND_REPOST +
    " order by p.id desc")
    List<PostWithLiked> findAllWithLiked(
            @Param("viewerId") Long viewerId);

    // 커서 기반 쓰레드 페이지 조회, 쓰레드 기반 배치 작업
    @Query("select p from Post p" +
            " where p.parentId = :parentId" +
            " and ( :cursor is null or p.id > :cursor)" +
            " order by p.id asc")
    List<Post> findThread(
            @Param("parentId") Long parentId,
            @Param("cursor") Long cursor,
            Limit limit);
    // 댓글 계산
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Post p set p.commentCount = p.commentCount+1 where p.id= :id")
    void increaseComment(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Post p set p.commentCount = p.commentCount-1 "
    + "where p.id= :id and p.commentCount > 0")
    void decreaseComment(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Post p set p.likeCount = p.likeCount+1" +
            " where p.id= :id")
    void increaseLike(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Post p set p.likeCount = p.likeCount-1 "
            + "where p.id= :id and p.likeCount > 0")
    void decreaseLike(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Post p" +
            " set p.repostCount = p.repostCount+1" +
            " where p.id=:id")
    void increaseRepostCount(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Post p" +
            " set p.repostCount = p.repostCount-1" +
            " where p.id=:id" +
            " and p.repostCount > 0")
    void decreaseRepostCount(@Param("id") Long id);
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Post p" +
            " set p.quoteCount = p.quoteCount+1" +
            " where p.id=:id")
    void increaseQuoteCount(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Post p" +
            " set p.quoteCount = p.quoteCount-1" +
            " where p.id=:id" +
            " and p.quoteCount > 0")
    void decreaseQuoteCount(@Param("id") Long id);
//커서별로 유저 구분
    // 답글이 아닌 첫글
    @Query(TEXT_ON_MY_LIKE_AND_REPOST+
            " where p.userId = :userId" +
            " and p.parentId is null" +
            " and (:cursor is null or p.id < :cursor)" +
            " order by p.id desc")
    List<PostWithLiked> findUserRootPostsWithCursor(
            @Param("viewerId") Long viewerId,
            @Param("userId") Long userId,
            @Param("cursor") Long cursor,
            Limit pageable
    );

    @Query(TEXT_ON_MY_LIKE_AND_REPOST+
            " where p.userId = :userId" +
            " and p.parentId is not null" +
            " and (:cursor is null or p.id < :cursor)" +
            " order by p.id desc")
    List<PostWithLiked> findUserRepliesPostsWithCursor(
            @Param("viewerId") Long viewerId,
            @Param("userId") Long userId,
            @Param("cursor") Long cursor,
            Limit pageable
    );
    @Query(TEXT_ON_MY_LIKE_AND_REPOST+
            " where p.userId = :userId" +
            " and p.imageUrl is not null" +
            " and (:cursor is null or p.id < :cursor)" +
            " order by p.id desc")
    List<PostWithLiked> findUserMediaPostsWithCursor(
            @Param("viewerId") Long viewerId,
            @Param("userId") Long userId,
            @Param("cursor") Long cursor,
            Limit pageable
    );

    // 리포스트 카운트 리턴
    @Query("select p.repostCount" +
            " from Post p" +
            " where p.id =:id")
    Integer findRepostCountById(@Param("id") Long postId);

    // 락 잡는 메서드
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p" +
            " from Post p" +
            " where p.id =:id")
    Optional<Post> findByIdForUpdate(@Param("id") Long id);
    // 글 좋아요 1행 감소
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Post p set p.likeCount = p.likeCount-1" +
            " where p.id in (select l.postId from PostLike l" +
            " where l.userId =:userId)")
    void decreaseLikeCountOfPostsLikedBy(@Param("userId")Long userId);
    // repost 카운트 감소
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Post p set p.repostCount = p.repostCount-1" +
            " where p.id in (select r.postId from Repost r" +
            " where r.userId =:userId)")
    void decreaseRepostCountOfPostsRepostedBy(@Param("userId")Long userId);
    // 탈퇴할 사람이 부모 글마다 단 답글 수 (부모 commentCount 에서 뺄 양)
    @Query("select r.parentId as parentId, count(r) as count from Post r" +
            " where r.userId= :userId and r.parentId is not null" +
            " group by r.parentId")
    List<ParentIdAndCount> countRepliesPerParentByUser(@Param("userId") Long userId);

    // 탈퇴할 사람이 원글마다 쓴 인용 글 수 (원글 quoteCount 에서 뺄 양)
    @Query("select r.quoteId as parentId, count(r) as count from Post r" +
            " where r.userId= :userId and r.quoteId is not null" +
            " group by r.quoteId")
    List<ParentIdAndCount> countQuotesPerOriginalByUser(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    UPDATE Post p
    SET p.commentCount = GREATEST(p.commentCount - :n, 0)
    WHERE p.id = :id
""")
    void decreaseCommentCountBy(@Param("id") Long id, @Param("n") long n);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    UPDATE Post p
    SET p.quoteCount = GREATEST(p.quoteCount - :n, 0)
    WHERE p.id = :id
""")
    void decreaseQuoteCountBy(@Param("id") Long id, @Param("n") long n);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Post p set p.isDeleted=true" +
            " where p.userId =:userId")
    void softDeleteAllByUserId(@Param("userId")Long userId);

}

