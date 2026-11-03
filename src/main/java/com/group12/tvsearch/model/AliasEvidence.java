package com.group12.tvsearch.model;

/** Stored verification evidence, not an assertion that a caller's source was independently audited. */
public record AliasEvidence(SeriesAlias alias, String evidence) {}
