package com.example.login.post.dto;


import com.example.login.post.entity.Post;

public interface PostWithLiked {
    Post getPost();
    Boolean getLiked();
    Boolean getReposted();
}
