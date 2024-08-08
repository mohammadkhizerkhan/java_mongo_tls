package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.mongodb.core.MongoTemplate;

@RestController
@RequestMapping("/api")
public class HelloController {

    @Autowired
    private MongoTemplate mongoTemplate;

    @GetMapping
    public String helloWorld() {
        // Create a new Message object
        Message message = new Message("helloworld");

        // Save the Message object into the MongoDB collection
        mongoTemplate.save(message);

        // Return a response
        return "Hello, World! Document inserted with msg: " + message.getMsg();
    }
}
