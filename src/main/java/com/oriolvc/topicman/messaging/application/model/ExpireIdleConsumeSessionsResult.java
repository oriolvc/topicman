package com.oriolvc.topicman.messaging.application.model;

/** @param expired how many sessions were closed */
public record ExpireIdleConsumeSessionsResult(int expired) {}
