package com.group12.tvsearch.model;

/** Planned canonical identity, with optional externally verified ID. */
public record Series(long id, String originalTitle, String originCountry, String externalId) {}
