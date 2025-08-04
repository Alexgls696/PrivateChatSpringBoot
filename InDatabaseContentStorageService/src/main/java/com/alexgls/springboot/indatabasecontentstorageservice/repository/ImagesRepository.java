package com.alexgls.springboot.indatabasecontentstorageservice.repository;

import com.alexgls.springboot.indatabasecontentstorageservice.entity.ChatImage;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImagesRepository extends CrudRepository<ChatImage,Integer> {
}
