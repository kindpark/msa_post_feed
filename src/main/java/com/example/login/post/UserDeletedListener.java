package com.example.login.post;

import com.example.login.config.RabbitMQConfig;
import com.example.login.event.EventMessage;
import com.example.login.post.dto.ParentIdAndCount;
import com.example.login.post.repository.PostRepository;
import com.example.login.postlike.PostLikeRepository;
import com.example.login.repost.RepostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;

@Component
@RequiredArgsConstructor
public class UserDeletedListener {
    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final RepostRepository repostRepository;


    @Transactional
    @RabbitListener(
            queues = RabbitMQConfig.USER_DELETED_QUEUE

    )
    public void handleUserDeleted(EventMessage eventMessage){
        postRepository.decreaseLikeCountOfPostsLikedBy(eventMessage.getActorId());
        postLikeRepository.deleteAllByUserId(eventMessage.getActorId());
        // 탈퇴 유저의 리포스트 카운트 및 리포스트 게시글 삭제 감소
        postRepository.decreaseRepostCountOfPostsRepostedBy(eventMessage.getActorId());
        repostRepository.deleteAllByUserId(eventMessage.getActorId());
        // 얼마나 답글을 남겼는지 보고 해당 수 만큼 답글 전체 삭제
        List<ParentIdAndCount> replies = postRepository.countRepliesPerParentByUser(eventMessage.getActorId());
        for (ParentIdAndCount reply : replies) {
            postRepository.decreaseCommentCountBy(reply.getParentId(), reply.getCount());
        }
        // 인용도 마찬가지로
        List<ParentIdAndCount> quotes = postRepository.countQuotesPerOriginalByUser(eventMessage.getActorId());
        for (ParentIdAndCount quote : quotes) {
            postRepository.decreaseQuoteCountBy(quote.getParentId(), quote.getCount());
        }
        postRepository.softDeleteAllByUserId(eventMessage.getActorId());
    }
}
