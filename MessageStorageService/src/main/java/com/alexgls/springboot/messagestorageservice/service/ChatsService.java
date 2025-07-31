package com.alexgls.springboot.messagestorageservice.service;

import com.alexgls.springboot.messagestorageservice.dto.ChatListElementDto;
import com.alexgls.springboot.messagestorageservice.repository.ChatsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class ChatsService {

    private final ChatsRepository chatsRepository;

   /* public Flux<ChatListElementDto> findAllChatsByUserId(int userId){
        return chatsRepository.findChatsByUserId(userId)
                .flatMap(chat->{
                    ChatListElementDto chatListElementDto = new ChatListElementDto();
                    chatListElementDto.setChatId(chat.getChatId());
                    chatListElementDto.set
                })

    }*/

}
