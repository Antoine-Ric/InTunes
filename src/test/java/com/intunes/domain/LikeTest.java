package com.intunes.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class LikeTest {

    @Test
    void constructorsRequireAnOwnerAndExactlyOneNonNullTarget() {
        User user = new User("listener", "encoded-password", null);
        Community community = new Community("ROCK_DEEP_90S", "Test community", null);
        Post post = new Post(community, user, "Post");
        Reply reply = new Reply(post, user, "Reply");

        assertThatNullPointerException().isThrownBy(() -> new Like(user, (Post) null));
        assertThatNullPointerException().isThrownBy(() -> new Like(user, (Reply) null));
        assertThatNullPointerException().isThrownBy(() -> new Like(null, post));
        assertThatNullPointerException().isThrownBy(() -> new Like(null, reply));
    }
}
