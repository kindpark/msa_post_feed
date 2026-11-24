package com.example.login.postlike;

import com.example.login.common.BusinessException;
import com.example.login.common.ErrorCode;
import com.example.login.event.EventMessage;
import com.example.login.post.entity.Post;
import com.example.login.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class PostLikeService {

    private final PostLikeRepository postLikeRepository;
    private final PostRepository postRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(timeout=2)
    public boolean toggle(Long postId, Long userId){
        // 포스트 없으면 에러 반환
        if(postId == null){
            throw new BusinessException((ErrorCode.POST_NOT_FOUND));
        }
        //Post 엔티티에서 글 작성자 id를 불러오기 위함.
        // 여기서 userId는 글을 보는 사람을 의미
        Post post = postRepository.findByIdForUpdate(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
        // 이미 좋아요를 한 상태에서 누른다면 좋아요를 취소함(모달같은거 안띄움)
        // 아니면 좋아요 눌려짐
        // 저장 성공시
        return postLikeRepository.findByPostIdAndUserId(postId, userId)
                .map(postLike -> {
                    postLikeRepository.delete(postLike);
                    postRepository.decreaseLike(postId);
                    return false;
                })
                .orElseGet(()->{
                    postLikeRepository.save(new PostLike(postId, userId));
                    postRepository.increaseLike(postId);
                    eventPublisher.publishEvent(
                            EventMessage.ofPostLike(
                                    userId,
                                    postId,
                                    post.getUserId()
                                    ));
                    return true;
                });
    }


    @Transactional(readOnly = true)
    public Set<Long> getLikePosts(Set<Long> posts, Long userId){
        return postLikeRepository.findLikedPostIds(posts, userId);
    }
}
