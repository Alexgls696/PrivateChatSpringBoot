package com.alexgls.springboot.indatabasecontentstorageservice.service;

import com.alexgls.springboot.indatabasecontentstorageservice.entity.ChatImage;

public interface ImagesService {
    ChatImage findById(int id);

    void deleteById(int id);

    ChatImage save(String path);
}
