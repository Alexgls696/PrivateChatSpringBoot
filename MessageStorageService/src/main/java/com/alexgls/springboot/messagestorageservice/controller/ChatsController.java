package com.alexgls.springboot.messagestorageservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chats/{id:\\d+}")
@RequiredArgsConstructor
public class ChatsController {
}
