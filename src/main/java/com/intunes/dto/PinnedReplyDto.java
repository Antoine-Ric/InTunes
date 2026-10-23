package com.intunes.dto;

/** The existing profile template expects body and from, not a persistence entity. */
public record PinnedReplyDto(Long id, String body, String from) { }
