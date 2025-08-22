package com.alexgls.springboot.client;

import com.alexgls.springboot.dto.ChatImage;
import com.alexgls.springboot.dto.CreateFileMetadataRequest;

public interface InDatabaseStorageServiceRestClient {
    ChatImage findChatImageById(int id);

    ChatImage saveChatImage(CreateFileMetadataRequest createFileMetadataRequest);
}
