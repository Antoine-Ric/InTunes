package com.intunes.repository;

import com.intunes.domain.Reply;
import com.intunes.domain.Post;
import com.intunes.domain.User;
import com.intunes.dto.PinnedReplyDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ReplyRepository extends JpaRepository<Reply, Long> {
    @Query("select r from Reply r where r.post = :post order by r.createdAt, r.id")
    List<Reply> findByPost(@Param("post") Post post);

    Optional<Reply> findByAuthorAndPinnedTrue(User author);

    /** Zero or one entries, ready for the profile template's pinnedReplies model attribute. */
    @Query("""
            select new com.intunes.dto.PinnedReplyDto(r.id, r.body, concat('Reply · ', c.name))
            from Reply r join r.post p join p.community c where r.author = :author and r.pinned = true
            """)
    List<PinnedReplyDto> findPinnedReplies(@Param("author") User author);
}
