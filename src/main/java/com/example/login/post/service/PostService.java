package com.example.login.post.service;

import com.example.login.common.BusinessException;
import com.example.login.common.ErrorCode;
import com.example.login.event.EventMessage;
import com.example.login.feed.FeedCache;
import com.example.login.post.dto.PostWithLiked;
import com.example.login.post.dto.PostCountDto;
import com.example.login.post.dto.PostPageResponseDto;
import com.example.login.post.dto.PostRequestDto;
import com.example.login.post.dto.PostResponseDto;
import com.example.login.post.entity.Post;
import com.example.login.post.repository.PostRepository;
import com.example.login.repost.RepostService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;
    private final RepostService repostService;
    private final FeedCache feedCache;
    private final ApplicationEventPublisher eventPublisher;

    // 내꺼 조회
    @Transactional(readOnly = true)
    public List<PostResponseDto> getMyPosts(Long userId){
        //내 글목록을 조회 (글쓴이도 나, 보는 사람도 나)
        List<PostWithLiked> rows = postRepository.findByUserIdWithLiked(userId, userId);
        return rows.stream()
                .map(row -> PostResponseDto.from(row.getPost(), row.getLiked(), row.getReposted()))
                .toList();
    }
    // 전체 조회
    @Transactional(readOnly = true)
    public List<PostResponseDto> getAllPosts(Long viewerId){
        return postRepository.findAllWithLiked(viewerId)
                .stream()
                .map(row -> PostResponseDto.from(row.getPost(), row.getLiked(), row.getReposted()))
                .toList();
    }
    // 상세 조회
    @Transactional(readOnly = true)
    public PostResponseDto getPost(Long postId, Long viewerId){
        PostWithLiked row = postRepository.findByIdWithLiked(postId, viewerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
        return PostResponseDto.from(row.getPost(), row.getLiked(), row.getReposted());
    }
    //좋아요 토글(postlike로 주요 기능 이관)

    //댓글작성(단순 게시판의 답글 개념이 아니라, X마냥 피드의 답글을 남기는 개념)
    //long id가 글의 id
    //JPA의 @ManyToOne 같은 연관관계를 따로 만들 필요가 없습니다.
    // PostLike에서 postId를 숫자로 저장한 것과 같은 방식입니다.
    // 숫자를 넣어 저장하면 그게 관계입니다.
    @Transactional
    public PostResponseDto createReply(
            PostRequestDto dto,
            Long parentId,
            Long userId){
        // 부모글 있는지 확인
        Post parent = postRepository.findById(parentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
        // 있다면 답글 작성
        Post post = Post.builder()
                .rootId((parent.getRootId() != null) ? parent.getRootId() : parent.getId())
                //parentId의 sonId 관계 설정
                .parentId(parentId)
                .userId(userId)
                .content(dto.getContent())
                .build();
        post.changeImages(dto.getImageUrls());
        //답글 증가
        postRepository.increaseComment(parentId);
        //세이브(답글로)
        postRepository.save(post);

        eventPublisher.publishEvent(
                EventMessage.ofReply(userId,
                        parent.getUserId(),
                        post.getId(),
                        parentId));

        return PostResponseDto.from(post, false, false);
    }
    // 댓글과 글의 관계

    // 쓰레드 목록(마찬가지로 게시판의 댓글을 보는 기능이 아니라, X마냥 피드의 답글을 보는 개념)
    // 루트 id
    @Transactional(readOnly = true)
    public PostPageResponseDto getReplies(
            Long viewerId,
            Long postId,
            Long cursor,
            int size){
        int pageSize = Math.min(Math.max(size, 1), 50);
        List<PostWithLiked> rows = postRepository.findThreadWithLiked(postId,
                 cursor, viewerId, Limit.of(pageSize+1));
        boolean hasNext = rows.size() > pageSize;
        // 넘치면 버리고 이번분만 남김
        List<PostWithLiked> page = hasNext ? rows.subList(0, pageSize) : rows;

        // 얻기로 한 페이지 수 만큼
        Long nextCursor = hasNext ? page.get(page.size() - 1).getPost().getId() : null;

        // 포스트 수 만큼 페이지 가져옴
        List<PostResponseDto> posts = page.stream()
                .map(row -> PostResponseDto.from(row.getPost(), row.getLiked(), row.getReposted()))
                .toList();
        //이것을 반환
        return new PostPageResponseDto(posts, nextCursor);
    }
    //C
    @Transactional
    public PostResponseDto createPost(
            PostRequestDto dto,
            Long userId){
        Post post = Post.builder()
                .userId(userId)
                .content(dto.getContent())
                .build();
        post.changeImages(dto.getImageUrls());
        Post saved = postRepository.save(post);
        feedCache.evict(userId);
        return PostResponseDto.from(saved, false, false);
    }
    //U
    @Transactional
    public PostResponseDto updatePost(
            Long postId,
            Long userId,
            PostRequestDto dto){
        PostWithLiked row = postRepository.findByIdWithLiked(postId, userId)
                .orElseThrow(()-> new BusinessException(ErrorCode.POST_NOT_FOUND));
        Post post = row.getPost();
        post.update(userId, dto.getContent(), dto.getImageUrls());

        return PostResponseDto.from(post, row.getLiked(), row.getReposted());
    }
    //D
    //soft delete로 방향 수정
    @Transactional
    public void deletePost(Long postId, Long userId){
        // 삭제할 게시글이 존재하는지 확인 및 조회
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        // 권한 검증: 게시글의 작성자와 현재 로그인한 유저의 ID가 일치하는지 확인
        if (!post.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 검증을 통과하면 데이터베이스에서 삭제 진행
        // (entity에서 조건을 바꿔놔서 update가 실제로 진행)
        postRepository.delete(post);
        //원글이 아닐때
        if(post.getParentId() != null){
            postRepository.decreaseComment(post.getParentId());
        }
        if(post.getQuoteId() != null){
            postRepository.decreaseQuoteCount(post.getQuoteId());
        }
        // 원글·답글·인용 모두 내 피드 캐시를 비운다
        feedCache.evict(userId);
    }

    @Transactional(readOnly = true)
    public Boolean existsPost(Long postId){

        return postRepository.existsById(postId);
    }

    // 탭 목록 반환을 위한것
    @Transactional(readOnly = true)
    public PostPageResponseDto getUserPosts(
            Long viewerId,
            Long targetUserId,
            String tab,
            Long cursor,
            int size) {
        if(targetUserId == null){
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        int pageSize = Math.min(Math.max(size, 1), 50);
        if(tab.equals("reposts")){
            return repostService.getUserReposts(viewerId, targetUserId, cursor, size);
        }
        List<PostWithLiked> rows = switch(tab){
            case "posts" -> postRepository.findUserRootPostsWithCursor(viewerId, targetUserId, cursor, Limit.of(pageSize+1));
            case "replies" -> postRepository.findUserRepliesPostsWithCursor(viewerId, targetUserId, cursor, Limit.of(pageSize+1));
            case "media" -> postRepository.findUserMediaPostsWithCursor(viewerId, targetUserId, cursor, Limit.of(pageSize+1));
            default -> postRepository.findUserRootPostsWithCursor(viewerId, targetUserId, cursor, Limit.of(pageSize+1));
        };
        boolean hasNext = rows.size() > pageSize;
        // 넘치면 버리고 이번분만 남김
        List<PostWithLiked> page = hasNext ? rows.subList(0, pageSize) : rows;

        // 얻기로 한 페이지 수 만큼(포스트, 이미지, 답글)
        Long nextCursor = hasNext ? page.get(page.size() - 1).getPost().getId() : null;
        // 포스트 수 만큼 페이지 가져옴
        List<PostResponseDto> posts = page.stream()
                .map(row -> PostResponseDto.from(row.getPost(), row.getLiked(), row.getReposted()))
                .toList();
        // 3. Entity 리스트를 PostPageResponseDto로 변환하여 반환
        return new PostPageResponseDto(posts, nextCursor);
    }
    @Transactional
    public PostCountDto getPostCount(Long userId) {

        Long count =  postRepository.countByUserId(userId);

        return PostCountDto.builder()
                .count(count)
                .build();
    }
}
