package com.situnes.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * JSON endpoints for the community board.
 * TODO: implement with CommunityService.
 */
@RestController
@RequestMapping("/api/communities/{archetypeKey}")
public class CommunityController {

    public record PostRequest(String body) {}
    public record ReplyRequest(String body) {}

    @PostMapping("/posts")
    public ResponseEntity<Void> createPost(@PathVariable String archetypeKey,
                                           @RequestBody PostRequest request) {
        throw new UnsupportedOperationException("TODO: communityService.createPost(...)");
    }

    @PostMapping("/posts/{postId}/replies")
    public ResponseEntity<Void> reply(@PathVariable String archetypeKey,
                                      @PathVariable Long postId,
                                      @RequestBody ReplyRequest request) {
        throw new UnsupportedOperationException("TODO: communityService.createReply(...)");
    }

    @PostMapping("/posts/{postId}/like")
    public ResponseEntity<Void> likePost(@PathVariable String archetypeKey,
                                         @PathVariable Long postId) {
        throw new UnsupportedOperationException("TODO: communityService.likePost(...)");
    }

    @PostMapping("/replies/{replyId}/pin")
    public ResponseEntity<Void> pinReply(@PathVariable String archetypeKey,
                                         @PathVariable Long replyId) {
        throw new UnsupportedOperationException("TODO: communityService.pinReply(...)");
    }
}
