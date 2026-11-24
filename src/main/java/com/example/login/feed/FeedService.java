package com.example.login.feed;

import com.example.login.feed.repository.FollowReader;
import com.example.login.post.entity.Post;
import com.example.login.post.repository.PostRepository;
import com.example.login.post.dto.PostResponseDto;
import com.example.login.postlike.PostLikeRepository;
import com.example.login.repost.RepostRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FeedService {
    //private final UserClient userClient;
    private final PostRepository postRepository;
    private final FeedCache feedCache;
    private final PostLikeRepository postLikeRepository;
    private final RepostRepository repostRepository;
    private final int cacheSize;
    private final FollowReader followReader;
    // 피드 캐시 사이즈 때문에 requireArgsconstructor 쓰지않음
    public FeedService(
            //UserClient userClient,
            PostRepository postRepository,
            FeedCache feedCache,
            PostLikeRepository postLikeRepository,
            RepostRepository repostRepository,
            @Value("${feed.cache-size:200}") int cacheSize,
            FollowReader followReader
    ){
        //this.userClient = userClient;
        this.postRepository = postRepository;
        this.feedCache = feedCache;
        this.postLikeRepository = postLikeRepository;
        this.repostRepository = repostRepository;
        this.cacheSize = cacheSize;
        this.followReader = followReader;
    }

    //팔로워 포스트, 좋아요 읽어옴
    @Transactional(readOnly = true)
    public FeedPageResponseDto getFeed(
            Long userId,
            Long cursor,
            int size) {
        int pageSize = Math.min(Math.max(size, 1), 50);
        // 캐시가 없다면, 채운다.
        if (!feedCache.isReady(userId)){
            List<Long> authorIds = new ArrayList<>(followReader.followingIds(userId));
            authorIds.add(userId);

            List<Long> postIds = postRepository.findRecentPostIdsByAuthors(authorIds, Limit.of(cacheSize));
            feedCache.rebuild(userId, postIds);

        }
    // 이번 페이지의 ID를 꺼낸다.
        List<Long> fetchedIds = feedCache.page(userId, cursor, pageSize + 1);
        // hasNext 판별 및 초과분 제거
        boolean hasNext = fetchedIds.size() > pageSize;
        List<Long> targetIds = hasNext ? fetchedIds.subList(0, pageSize) : fetchedIds;
        // 타겟 아이디에 게시글이 없다면 빈걸 리턴
        if(targetIds.isEmpty()){
            return new FeedPageResponseDto(List.of(),null);
        }

        // 글을 가져온 뒤 캐시에서 꺼낸 순서대로 재정렬
        Map<Long, Post> postMap = postRepository.findAllById(targetIds).stream()
                .collect(Collectors.toMap(Post::getId, Function.identity()));

        // 삭제된 글은 postMap에 없으니 자연스럽게 필터링
        List<Post> orderedPosts = targetIds.stream()
                .map(postMap::get)
                .filter(Objects::nonNull)
                .toList();

        // 좋아요 여부 일괄 조회 (N+1 방지)
        Set<Long> likedPostIds = postLikeRepository.findLikedPostIds(targetIds, userId);

        // 리포스트 여부 일괄 조회
        Set<Long> repostedPostIds = repostRepository.findRepostedPostIds(targetIds, userId);
        // dto 변환
        List<PostResponseDto> items = orderedPosts.stream()
                .map(post -> PostResponseDto.from(post, likedPostIds.contains(post.getId()), repostedPostIds.contains(post.getId())))
                .toList();
        // nextCursor 계산 및 반환
        Long nextCursor = hasNext ? targetIds.get(targetIds.size() - 1) : null;

        return new FeedPageResponseDto(items, nextCursor);
    }

}
