package com.intunes.repository;

import com.intunes.domain.Like;
import com.intunes.domain.Post;
import com.intunes.domain.Reply;
import com.intunes.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LikeRepository extends JpaRepository<Like, Long> {
    boolean existsByUserAndPost(User user, Post post);
    long countByPost(Post post);
    boolean existsByUserAndReply(User user, Reply reply);
    long countByReply(Reply reply);

    /** Atomic, idempotent insert. Caller owns the transaction. */
    @Modifying
    @Query(value = """
            insert into likes (user_id, post_id, created_at) values (:userId, :postId, current_timestamp)
            on conflict (post_id, user_id) where post_id is not null do nothing
            """, nativeQuery = true)
    int likePostIfAbsent(@Param("userId") Long userId, @Param("postId") Long postId);

    @Modifying
    @Query(value = """
            insert into likes (user_id, reply_id, created_at) values (:userId, :replyId, current_timestamp)
            on conflict (reply_id, user_id) where reply_id is not null do nothing
            """, nativeQuery = true)
    int likeReplyIfAbsent(@Param("userId") Long userId, @Param("replyId") Long replyId);
}
