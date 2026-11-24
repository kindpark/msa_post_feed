package com.example.login.feed.repository;

import java.util.List;

/**
 * 피드가 팔로우 관계를 얻는 유일한 통로 (API_SPEC §7.4).
 * feed 패키지는 이 인터페이스만 알고, follow 패키지의 클래스는 import 하지 않는다.
 *
 * 지금: follow.JpaFollowReader 가 FollowRepository 로 구현
 * MSA 분리 후: user-service 를 HTTP 로 호출하는 구현으로 교체 (FeedService 는 그대로)
 */
public interface FollowReader {

    // userId 가 팔로우하는 사람들의 id. pull(캐시 재구성) 에서 사용
    List<Long> followingIds(Long userId);

    // TODO push(fan-out) 단계에서 추가: userId 를 팔로우하는 사람들의 id
    // List<Long> followerIds(Long userId);
}
