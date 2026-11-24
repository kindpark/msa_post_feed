package com.example.login.repost;

import com.example.login.common.BusinessException;
import com.example.login.common.ErrorCode;
import com.example.login.event.EventMessage;
import com.example.login.feed.FeedCache;
import com.example.login.post.dto.PostPageResponseDto;
import com.example.login.post.dto.PostRequestDto;
import com.example.login.post.dto.PostResponseDto;
import com.example.login.post.entity.Post;
import com.example.login.post.repository.PostRepository;
import com.example.login.postlike.PostLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.example.login.event.EventMessage.ofQuote;

// 리포스트 수정의 개념은, 게시글 수정으로 넘겨도 될듯 하다.
@Service
@RequiredArgsConstructor
public class RepostService {
    private final RepostRepository repostRepository;
    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final FeedCache feedCache;
    //리포스트 글 조회?(이건 게시글로 되지 않을까?)


    // 리포스트
    @Transactional(timeout = 2)
    public RepostResponseDto repost(
            Long userId,
            Long postId
    ){
        Post post = postRepository.findByIdForUpdate(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
        Optional<Repost> existing = repostRepository.findByUserIdAndPostId(userId, postId);
        boolean reposted;
        if(existing.isPresent()){
            repostRepository.delete(existing.get());
            postRepository.decreaseRepostCount(postId);
            reposted = false;
        }
        else{
            repostRepository.save(new Repost(userId, postId));
            postRepository.increaseRepostCount(postId);
            eventPublisher.publishEvent(
                    EventMessage.ofRepost(
                            userId,
                            postId,
                            post.getUserId()
                    )
                );
            reposted = true;
        }
        int currentRepostCount = postRepository.findRepostCountById(postId);
        return RepostResponseDto.from(reposted, currentRepostCount);
    }
    @Transactional
    public PostResponseDto quote(
            PostRequestDto dto,
            Long userId,
            Long postId
    ){
        Post post = postRepository.findById(postId)
                .orElseThrow(()-> new BusinessException(ErrorCode.POST_NOT_FOUND));

        // 인용 가능하고, 부모글 있으면 작성
        Post quotePost = createQuote(dto.getContent(), dto.getImageUrls(), userId, postId);

        Post savedPost = postRepository.save(quotePost);

        // 인용수 증가
        postRepository.increaseQuoteCount(postId);

        feedCache.evict(userId);
        eventPublisher.publishEvent(ofQuote(userId, post.getUserId(), savedPost.getId()));

        return PostResponseDto.from(savedPost, false, false);
    }

    private Post createQuote(
            String content,
            List<String> imageUrls,
            Long userId,
            Long postId
    ){
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        Post save = Post.builder()
                .userId(userId)
                .content(content)
                .quoteId(postId)
                .build();
        save.changeImages(imageUrls);
        return save;
    }

    @Transactional(readOnly = true)
    public PostPageResponseDto getUserReposts(
            Long viewerId,
            Long targetUserId,
            Long cursor,
            int size
    ){
        int pageSize = Math.min(Math.max(size, 1), 50);
        // 첫 스크롤인가? 첫 스크롤이 아니라 이후 스크롤인가?
        List<Repost> repost = (cursor == null)
               ? repostRepository.findByUserIdOrderByIdDesc(targetUserId, Limit.of(pageSize+1))
                : repostRepository.findByUserIdAndIdLessThanOrderByIdDesc(targetUserId, cursor, Limit.of(pageSize+1));
        boolean hasNext = repost.size() > pageSize;
        // 포스트의 재게시자, 재게시 횟수 조회
        List<Repost> targetPosts = hasNext ? repost.subList(0, pageSize) : repost;
        if(targetPosts.isEmpty()){
            return new PostPageResponseDto(List.of(), null);
        }

        List<Long> usersReposts = targetPosts.stream()
                .map(Repost::getPostId)
                .toList();
        // 유저 레포지토리를 통해 유저 정보 추출
        Map<Long, Post> postMap = postRepository.findAllById(usersReposts)
                .stream()
                .collect(Collectors.toMap(Post::getId, Function.identity()));

        List<Post> orderUsers = usersReposts.stream()
                .map(postMap::get)
                .filter(Objects::nonNull)
                .toList();
        Long nextCursor = hasNext ? targetPosts.get(targetPosts.size() - 1).getId() : null;

        // 로그인 유저가 이 사람의 글을 리포스트를 하였는지 Set으로 추출
        Set<Long> likedSet = (viewerId == null)
                ? Set.of()
                : postLikeRepository.findLikedPostIds(usersReposts, viewerId);
        Set<Long> repostedSet = (viewerId == null)
                ? Set.of()
                : repostRepository.findRepostedPostIds(usersReposts, viewerId);
        List<PostResponseDto> items = orderUsers.stream()
                .map(post -> PostResponseDto.from(post,
                        likedSet.contains(post.getId()),
                        repostedSet.contains(post.getId())
                        ))
                .toList();
        return new PostPageResponseDto(items, nextCursor);
    }
}
