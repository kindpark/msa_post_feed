package com.example.login.post.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Post.changeImages: imageUrls(전체)와 imageUrl(첫 장)이 항상 같이 바뀌는지. DB 없이 도는 단위 테스트 */
class PostTest {

    private static final List<String> THREE = List.of(
            "/api/v1/images/1.png", "/api/v1/images/2.png", "/api/v1/images/3.png");

    private Post newPost() {
        return Post.builder().userId(1L).content("본문").build();
    }

    @Test
    @DisplayName("null 이면 이미지 없는 글이 된다")
    void nullImages() {
        Post post = newPost();

        post.changeImages(null);

        assertThat(post.getImageUrls()).isEmpty();
        assertThat(post.getImageUrl()).isNull();
    }

    @Test
    @DisplayName("빈 목록이면 이미지 없는 글이 된다")
    void emptyImages() {
        Post post = newPost();

        post.changeImages(List.of());

        assertThat(post.getImageUrls()).isEmpty();
        assertThat(post.getImageUrl()).isNull();
    }

    @Test
    @DisplayName("1장이면 목록과 대표 이미지가 같다")
    void oneImage() {
        Post post = newPost();

        post.changeImages(List.of("/api/v1/images/1.png"));

        assertThat(post.getImageUrls()).containsExactly("/api/v1/images/1.png");
        assertThat(post.getImageUrl()).isEqualTo("/api/v1/images/1.png");
    }

    @Test
    @DisplayName("여러 장이면 순서대로 모두 저장하고 첫 장이 대표 이미지다")
    void manyImages() {
        Post post = newPost();

        post.changeImages(THREE);

        assertThat(post.getImageUrls()).containsExactlyElementsOf(THREE); // 개수와 순서까지 비교
        assertThat(post.getImageUrl()).isEqualTo(THREE.getFirst());
    }

    @Test
    @DisplayName("이미지가 있던 글에서 모두 빼면 대표 이미지도 사라진다")
    void removeAllImages() {
        Post post = newPost();
        post.changeImages(THREE);

        post.changeImages(List.of());

        assertThat(post.getImageUrls()).isEmpty();
        assertThat(post.getImageUrl()).isNull();
    }

    @Test
    @DisplayName("다른 이미지로 바꾸면 이전 이미지는 남지 않는다")
    void replaceImages() {
        Post post = newPost();
        post.changeImages(THREE);

        post.changeImages(List.of("https://example.com/new.png"));

        assertThat(post.getImageUrls()).containsExactly("https://example.com/new.png");
        assertThat(post.getImageUrl()).isEqualTo("https://example.com/new.png");
    }
}
