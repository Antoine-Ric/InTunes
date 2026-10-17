package com.situnes.service;

import com.situnes.domain.*;
import org.springframework.stereotype.Service;

/**
 * Communities, board posts, replies, likes, pins.
 * TODO: implement. Board reads should be paginated from the start
 * (Pageable) — boards get long fast.
 */
@Service
public class CommunityService {

    /** Find-or-create the community for an archetype key; the profile join is implicit. */
    public Community resolveCommunity(String archetypeKey) {
        throw new UnsupportedOperationException("TODO: find by archetypeKey or create it");
    }

    public Post createPost(User author, Community community, String body) {
        throw new UnsupportedOperationException("TODO: persist post (validate body not blank, <= 2000 chars)");
    }

    public Reply createReply(User author, Post post, String body) {
        throw new UnsupportedOperationException("TODO: persist reply");
    }

    /** Pin a reply so it renders on the author's public profile. One pin per user (unpin the old). */
    public void pinReply(User author, Long replyId) {
        throw new UnsupportedOperationException("TODO: unpin existing, pin this one; verify ownership");
    }

    public void likePost(User user, Long postId) {
        throw new UnsupportedOperationException("TODO: idempotent like (ignore duplicates)");
    }
}
