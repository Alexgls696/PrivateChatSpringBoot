package com.alexgls.springboot.indatabasecontentstorageservice.service;

import com.alexgls.springboot.indatabasecontentstorageservice.entity.ChatImage;
import com.alexgls.springboot.indatabasecontentstorageservice.exception.NoSuchImageException;
import com.alexgls.springboot.indatabasecontentstorageservice.repository.ImagesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ImagesServiceImpl implements ImagesService {

    private final ImagesRepository imagesRepository;

    @Override
    public ChatImage findById(int id) {
        return imagesRepository.findById(id)
                .orElseThrow(() -> new NoSuchImageException("Image with id %d not found".formatted(id)));
    }

    @Override
    public void deleteById(int id) {
        imagesRepository.deleteById(id);
    }

    @Override
    public ChatImage save(String path) {
        ChatImage chatImage = new ChatImage();
        chatImage.setPath(path);
        return imagesRepository.save(chatImage);
    }
}
