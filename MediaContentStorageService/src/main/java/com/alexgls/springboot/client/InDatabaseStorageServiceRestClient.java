package com.alexgls.springboot.client;

import com.alexgls.springboot.dto.ChatImage;

public interface InDatabaseStorageServiceRestClient {
    ChatImage findChatImageById(int id);

    ChatImage saveChatImage(String path);
}
