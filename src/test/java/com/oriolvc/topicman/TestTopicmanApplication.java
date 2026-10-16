package com.oriolvc.topicman;

import org.springframework.boot.SpringApplication;

public class TestTopicmanApplication {

    public static void main(String[] args) {
        SpringApplication.from(TopicManApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
