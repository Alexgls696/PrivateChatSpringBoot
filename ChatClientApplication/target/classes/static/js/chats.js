document.addEventListener('DOMContentLoaded', () => {
    // --- DOM Элементы ---
    const statusEl = document.getElementById('status');
    const chatListEl = document.getElementById('chatList');
    const chatWindowEl = document.getElementById('chatWindow');
    const chatTitleEl = document.getElementById('chatTitle');
    const messagesEl = document.getElementById('messages');
    const closeChatBtn = document.getElementById('closeChatBtn');
    const messageForm = document.getElementById('messageForm');
    const messageInput = document.getElementById('messageInput');
    const userSearchModal = document.getElementById('userSearchModal');
    const logoutBtn = document.getElementById('logoutBtn');

    const findUserBtn = document.getElementById('findUserBtn');
    const closeModalBtn = document.getElementById('closeModalBtn');
    const userListContainer = document.getElementById('userListContainer');

    const attachFileBtn = document.getElementById('attachFileBtn');
    const fileInput = document.getElementById('fileInput');
    const attachmentPreviewContainer = document.getElementById('attachmentPreviewContainer');

    const attachmentsBtn = document.getElementById("attachmentsBtn");
    const attachmentsModal = document.getElementById("attachmentsModal");
    const closeAttachmentsBtn = document.getElementById("closeAttachmentsBtn");
    const attachmentsTabs = document.querySelectorAll(".attachments-tabs .tab-btn");
    const attachmentsContent = document.getElementById("attachmentsContent");
    let pendingAttachments = [];


    // --- Состояние приложения ---
    let activeChatId = null;
    let chatListPage = 0;
    let messagePage = 0;
    const pageSize = 15; // Сообщений на странице
    let isLoading = false;
    let hasMoreMessages = true;
    let participantCache = {};
    let currentUserId = null;
    let isChatsLoading = false;
    let hasMoreChats = true;

    const API_URL = 'http://localhost:8086'; // URL для WebSocket
    const API_FETCH_URL = 'http://localhost:8087'; // URL для REST API
    const API_USERS_URL = 'http://localhost:8085/api'; // REST API пользователей
    const API_STORAGE_URL = 'http://localhost:8088/api/storage'; //URL хранилища


    const chatManager = {
        stompClient: null,
        isConnected: false,
        isConnecting: false,
        retryCount: 0,
        maxRetries: 5,

        start: function () {
            if (this.isConnected || this.isConnecting) return;
            this.connect();
        },

        connect: function () {
            this.isConnecting = true;
            const accessToken = localStorage.getItem('accessToken');
            if (!accessToken) {
                statusEl.textContent = "Ошибка: токен доступа не найден. Пожалуйста, войдите снова.";
                return;
            }

            const socket = new SockJS(`${API_URL}/ws-chat?token=${accessToken}`);
            this.stompClient = Stomp.over(socket);
            this.stompClient.debug = null;

            this.stompClient.connect({},
                (frame) => this.onConnectSuccess(frame),
                (error) => this.onConnectError(error)
            );
        },

        onConnectSuccess: function (frame) {
            console.log('WebSocket Connected: ' + frame);
            this.isConnecting = false;
            this.isConnected = true;
            this.retryCount = 0;

            this.stompClient.subscribe(`/user/queue/messages`, async (message) => {
                try {
                    const newMsg = JSON.parse(message.body);
                    await updateOrFetchChatInList(newMsg);

                    if (newMsg.chatId === activeChatId) {
                        const isSentByMe = newMsg.senderId === currentUserId;

                        // Если сообщение имеет tempId, ищем и обновляем его
                        if (newMsg.tempId) {
                            const pendingEl = document.querySelector(`[data-temp-id='${newMsg.tempId}']`);
                            if (pendingEl) {
                                const finalEl = await createMessageElement(newMsg, isSentByMe);
                                pendingEl.replaceWith(finalEl);  // Заменяем элемент
                                return;
                            }
                        }

                        // В противном случае добавляем обычное сообщение
                        addMessageToUI(newMsg, isSentByMe);

                        if (!isSentByMe) {
                            markMessagesAsRead([newMsg]);
                        }
                    }
                } catch (error) {
                    console.error('Ошибка обработки нового сообщения:', error);
                }
            });


            this.stompClient.subscribe(`/user/queue/read-status`, (notification) => {
                try {
                    const readInfo = JSON.parse(notification.body);
                    if (readInfo.chatId === activeChatId) {
                        readInfo.messageIds.forEach(id => {
                            const msgEl = document.querySelector(`[data-message-id='${id}']`);
                            if (msgEl) {
                                const statusEl = msgEl.querySelector('.message-status');
                                if (statusEl) {
                                    statusEl.textContent = 'Прочитано';
                                    statusEl.classList.add('read');
                                }
                            }
                        });
                    }
                } catch (error) {
                    console.error('Ошибка обработки уведомления о прочтении:', error);
                }
            });
        },

        onConnectError: function (error) {
            this.isConnecting = false;
            this.isConnected = false;
            if (this.retryCount >= this.maxRetries) {
                alert("Не удалось подключиться к серверу чатов. Попробуйте обновить страницу.");
                return;
            }
            this.retryCount++;
            const delay = 1000 * this.retryCount;
            console.error(`Соединение потеряно. Повторная попытка через ${delay}ms...`, error);
            setTimeout(() => this.connect(), delay);
        },

        sendMessageWithAttachments: function (content, attachments) {
            if (this.stompClient && this.isConnected && activeChatId) {
                const tempId = generateTempId();

                // 1. Рисуем сообщение сразу в UI с временным статусом
                const pendingMsgHtml = renderPendingMessage(content, attachments, tempId);
                messagesEl.insertAdjacentHTML("beforeend", pendingMsgHtml);
                messagesEl.scrollTop = messagesEl.scrollHeight;

                // 2. Отправляем на сервер
                const chatMessage = {
                    chatId: activeChatId,
                    content: content,
                    attachments: attachments,
                    tempId: tempId
                };
                this.stompClient.send("/app/chat.send", {}, JSON.stringify(chatMessage));

                // 3. Обновляем статус сразу, что сообщение отправляется
                const pendingEl = document.querySelector(`[data-temp-id='${tempId}']`);
                if (pendingEl) {
                    const statusEl = pendingEl.querySelector('.message-status');
                    statusEl.textContent = "Отправка...";
                    statusEl.classList.add('sending');
                }
            } else {
                alert("Нет подключения для отправки сообщения.");
            }
        },

    };

    async function updateOrFetchChatInList(newMsg) {
        const chatId = newMsg.chatId;
        const existingChatItemEl = chatListEl.querySelector(`[data-chat-id='${chatId}']`);

        if (existingChatItemEl) {
            const lastMsgEl = existingChatItemEl.querySelector('.last-message');
            const timeEl = existingChatItemEl.querySelector('.message-time');

            if (lastMsgEl) {
                lastMsgEl.textContent = newMsg.content || 'Вложение';
            }
            if (timeEl) {
                timeEl.textContent = `Отправлено: ${formatDate(newMsg.createdAt)}`;
            }
            chatListEl.prepend(existingChatItemEl);
        } else {
            try {
                const newChatDto = await apiFetch(`${API_FETCH_URL}/api/chats/${chatId}`);
                const newChatItemEl = await createChatItem(newChatDto);
                chatListEl.prepend(newChatItemEl);

            } catch (error) {
                console.error(`Не удалось загрузить информацию о новом чате #${chatId}:`, error);
            }
        }
    }

    function renderUsers(users) {
        userListContainer.innerHTML = '';
        if (!users || users.length === 0) {
            userListContainer.innerHTML = '<p class="placeholder">Пользователи не найдены.</p>';
            return;
        }
        users.forEach(user => {
            if (user.id === currentUserId) return;

            const userDiv = document.createElement('div');
            userDiv.className = 'user-item';
            userDiv.innerHTML = `
                <div class="user-name">${user.name} ${user.surname}</div>
                <div class="user-username">@${user.username}</div>
            `;
            // Вот здесь используется ваша функция startChatWithUser
            userDiv.addEventListener('click', () => startChatWithUser(user));
            userListContainer.appendChild(userDiv);
        });
    }

    async function loadAndShowUsers() {
        userListContainer.innerHTML = '<p class="placeholder">Загрузка пользователей...</p>';
        userSearchModal.classList.remove('hidden');
        try {
            const users = await apiFetch(`${API_USERS_URL}/users`);
            renderUsers(users);
        } catch (error) {
            userListContainer.innerHTML = `<p class="placeholder">Ошибка загрузки пользователей: ${error.message}</p>`;
        }
    }

    function formatDate(isoString) {
        if (!isoString) return '';
        const date = new Date(isoString);
        return date.toLocaleString('ru-RU', {
            hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit', year: 'numeric'
        });
    }

    async function loadChats() {
        if (isChatsLoading || !hasMoreChats) return;

        isChatsLoading = true;

        try {
            const data = await apiFetch(`${API_FETCH_URL}/api/chats/find-by-id/${chatListPage}`);
            statusEl.textContent = '';
            if (Array.isArray(data) && data.length > 0) {
                const chatItemsPromises = data.map(chat => createChatItem(chat));
                const chatItems = await Promise.all(chatItemsPromises);
                chatItems.forEach(li => chatListEl.appendChild(li));

                chatListPage++;
            } else {
                hasMoreChats = false;
                if (chatListPage === 0) {
                    statusEl.textContent = 'Чаты не найдены';
                }
            }
        } catch (error) {
            statusEl.textContent = `Ошибка загрузки чатов: ${error.message}`;
        } finally {
            isChatsLoading = false;
        }
    }

    async function createChatItem(chat) {
        const li = document.createElement('li');
        li.dataset.chatId = chat.chatId;

        // Определяем, какое имя показать сразу
        const initialTitle = chat.group ? chat.name : 'Загрузка имени...';

        li.innerHTML = `
            <div class="chat-title">${initialTitle}</div>
            <div class="chat-type">Тип: ${chat.type}</div>
            <div class="last-message">${chat.lastMessage ? chat.lastMessage.content : 'Нет сообщений'}</div>
            <div class="message-time">${chat.lastMessage ? `Отправлено: ${formatDate(chat.lastMessage.createdAt)}` : ''}</div>
        `;

        if (!chat.group) {
            try {
                const recipient = await apiFetch(`${API_FETCH_URL}/api/chats/find-recipient-by-private-chat-id/${chat.chatId}`);
                const titleDiv = li.querySelector('.chat-title');
                if (titleDiv) {
                    titleDiv.textContent = `${recipient.name} ${recipient.surname}`;
                }
            } catch (error) {
                console.error(`Не удалось загрузить собеседника для чата ${chat.chatId}:`, error);
                const titleDiv = li.querySelector('.chat-title');
                if (titleDiv) {
                    titleDiv.textContent = 'Ошибка загрузки чата'; // Имя по умолчанию в случае ошибки
                }
            }
        }

        li.addEventListener('click', () => openChat(chat));
        return li;
    }

    // В файле chats.js

    async function markMessagesAsRead(messagesToRead) {
        // Проверяем, есть ли вообще что отправлять
        if (!messagesToRead || messagesToRead.length === 0) {
            return;
        }

        // Формируем payload в том формате, который ожидает бэкенд
        const payload = messagesToRead.map(msg => ({
            messageId: msg.id,
            senderId: msg.senderId,
            chatId: activeChatId // Используем ID активного чата
        }));

        console.log('Отправка на прочтение:', payload);
        try {
            await apiFetch(`${API_FETCH_URL}/api/messages/read-messages`, {
                method: 'POST',
                body: JSON.stringify(payload)
            });
        } catch (error) {
            console.error("Не удалось отправить статус прочтения:", error);
        }
    }

    async function openChat(chat) {
        if (activeChatId === chat.chatId && !chatWindowEl.classList.contains('hidden')) {
            return;
        }

        activeChatId = chat.chatId;
        messagePage = 0;
        hasMoreMessages = true;
        participantCache = {};
        // -------------------------------------------------------------------

        [...chatListEl.children].forEach(li => {
            li.classList.toggle('active', li.dataset.chatId == activeChatId);
        });

        chatTitleEl.textContent = 'Загрузка...';
        messagesEl.innerHTML = '<p class="placeholder">Загрузка данных...</p>';
        chatWindowEl.classList.remove('hidden');

        try {
            const [chatDetailsResult, messages] = await Promise.all([
                (async () => {
                    if (chat.group) {
                        chatTitleEl.textContent = chat.name;
                    } else {
                        const recipient = await apiFetch(`${API_FETCH_URL}/api/chats/find-recipient-by-private-chat-id/${chat.chatId}`);
                        chatTitleEl.textContent = `Чат с ${recipient.name} ${recipient.surname}`;
                    }
                    const participants = await apiFetch(`${API_FETCH_URL}/api/chats/${chat.chatId}/participants`);
                    participants.forEach(p => {
                        participantCache[p.id] = `${p.name} ${p.surname}`;
                    });
                })(),
                loadMessages(chat.chatId, messagePage)
            ]);
            console.log(messages);
            renderMessages(messages);

            const unreadMessages = messages.filter(msg => !msg.read && msg.senderId !== currentUserId);
            await markMessagesAsRead(unreadMessages);

            if (messages.length === pageSize) {
                messagePage++;
            }

        } catch (error) {
            console.error("Ошибка открытия чата:", error);
            messagesEl.innerHTML = `<p class="placeholder">Не удалось загрузить данные чата.</p>`;
            chatTitleEl.textContent = 'Ошибка';
        }

        messageInput.focus();
    }

    async function loadMessages(chatId, page) {
        if (isLoading || !hasMoreMessages) return [];
        isLoading = true;
        try {
            const data = await apiFetch(`${API_FETCH_URL}/api/messages?chatId=${chatId}&page=${page}&size=${pageSize}`);
            if (!Array.isArray(data) || data.length < pageSize) {
                hasMoreMessages = false;
            }
            return data;
        } catch (error) {
            console.error('Ошибка загрузки сообщений:', error);
            return [];
        } finally {
            isLoading = false;
        }
    }

    async function renderMessages(messages) {
        messagesEl.innerHTML = ''; // Очищаем контейнер
        if (!messages || messages.length === 0) {
            messagesEl.innerHTML = '<p class="placeholder">Сообщений пока нет. Напишите первым!</p>';
            return;
        }

        const fragment = document.createDocumentFragment();
        for (const msg of messages) {
            const isSentByMe = msg.senderId === currentUserId;
            const msgDiv = await createMessageElement(msg, isSentByMe);
            fragment.appendChild(msgDiv);
        }

        messagesEl.appendChild(fragment); // Добавляем сообщения в DOM
        messagesEl.scrollTop = messagesEl.scrollHeight; // Прокручиваем вниз
    }



    async function createMessageElement(msg, isSentByMe) {
        const msgDiv = document.createElement('div');
        msgDiv.className = `message ${isSentByMe ? 'sent' : 'received'}`;
        msgDiv.dataset.messageId = msg.id;

        const messageType = msg.type || msg.messageType;
        const senderName = isSentByMe ? '' : (participantCache[msg.senderId] || `Пользователь #${msg.senderId}`);
        const senderHtml = senderName ? `<div class="message-sender">${senderName}</div>` : '';

        let attachmentsHtml = '';
        if (msg.attachments && msg.attachments.length > 0) {
            attachmentsHtml = '<div class="attachments-container">';

            const attachmentItemsHtml = await Promise.all(msg.attachments.map(async (att) => {
                const getLinkUrl = `${API_STORAGE_URL}/download/by-id?id=${att.fileId}`;

                try {
                    const realDownloadUrl = await apiFetch(getLinkUrl);

                    if (att.mimeType && att.mimeType.startsWith('image/')) {
                        // Для изображений создаем элемент с ленивой загрузкой
                        return `
                    <div class="attachment-item image-attachment">
                        <a href="${realDownloadUrl.href}" target="_blank" rel="noopener noreferrer">
                            <div class="skeleton skeleton-tile"></div> <!-- Скелетон для изображения -->
                            <img src="${realDownloadUrl.href}" alt="Вложение" class="attachment-image lazy-load" data-src="${realDownloadUrl.href}">
                        </a>
                    </div>`;
                    } else {
                        // Для других типов файлов отображаем стандартную ссылку для скачивания
                        return `
                    <div class="attachment-item file-attachment">
                        <div class="file-icon">📁</div>
                        <div class="file-info">
                            <span class="file-name">${msg.content || 'Файл'}</span>
                            <a href="${realDownloadUrl.href}" class="file-download-link" download>Скачать</a>
                        </div>
                    </div>`;
                    }
                } catch (error) {
                    // В случае ошибки при загрузке файла
                    console.error(`Не удалось получить ссылку для вложения (fileId: ${att.fileId}):`, error);
                    return `<div class="attachment-item file-attachment error">Не удалось загрузить вложение</div>`;
                }
            }));

            attachmentsHtml += attachmentItemsHtml.join('');
            attachmentsHtml += '</div>';
        }

        const contentHtml = messageType === 'TEXT'
            ? `<div class="message-content">${msg.content}</div>`
            : (msg.content && attachmentsHtml ? `<div class="message-content">${msg.content}</div>` : '');

        const statusText = isSentByMe ? (msg.read ? 'Прочитано' : 'Доставлено') : '';
        const statusClass = isSentByMe && msg.read ? 'read' : '';

        msgDiv.innerHTML = `
    ${senderHtml}
    ${attachmentsHtml}
    ${contentHtml}
    <div class="message-meta">
        <span>${formatDate(msg.createdAt)}</span>
        <span class="message-status ${statusClass}">${statusText}</span>
    </div>
    `;

        // Подключаем IntersectionObserver для ленивой загрузки изображений
        const images = msgDiv.querySelectorAll('.lazy-load');
        images.forEach(img => {
            const observer = new IntersectionObserver((entries, observer) => {
                entries.forEach(entry => {
                    if (entry.isIntersecting) {
                        const image = entry.target;
                        image.src = image.getAttribute('data-src');
                        image.onload = () => {
                            const skeleton = image.previousElementSibling;
                            if (skeleton) skeleton.remove(); // Убираем скелетон
                        };
                        observer.disconnect(); // Отключаем observer после загрузки изображения
                    }
                });
            });

            observer.observe(img);
        });

        return msgDiv;
    }


    async function addMessageToUI(msg, isSentByMe, prepend = false) {
        const placeholder = messagesEl.querySelector('.placeholder');
        if (placeholder) placeholder.remove();

        const wasScrolledToBottom = messagesEl.scrollHeight - messagesEl.clientHeight <= messagesEl.scrollTop + 1;

        const msgDiv = await createMessageElement(msg, isSentByMe);

        // Добавляем сообщение в начало или в конец
        if (prepend) {
            messagesEl.prepend(msgDiv);
        } else {
            messagesEl.appendChild(msgDiv);
        }

        // Если скроллили до низа, то прокручиваем вниз
        if (wasScrolledToBottom && !prepend) {
            messagesEl.scrollTop = messagesEl.scrollHeight;
        }
    }


    async function startChatWithUser(user) {
        console.log(`Попытка начать чат с пользователем ID: ${user.id}`);
        try {
            const chat = await apiFetch(`${API_FETCH_URL}/api/chats/private/${user.id}`, {
                method: 'POST',
            });

            userSearchModal.classList.add('hidden');

            const existingChatItem = chatListEl.querySelector(`[data-chat-id='${chat.chatId}']`);
            if (!existingChatItem) {
                const newChatItem = await createChatItem(chat);
                chatListEl.prepend(newChatItem);
            }
            openChat(chat);
        } catch (error) {
            alert(`Не удалось создать чат: ${error.message}`);
        }
    }

    function setFormEnabled(enabled) {
        messageInput.disabled = !enabled;
        sendBtn.disabled = !enabled;
        attachFileBtn.disabled = !enabled;
    }

    // Отображение превью
    function addAttachmentToPreview(file) {
        const tempId = `temp-${Date.now()}`;
        const previewEl = document.createElement('div');
        previewEl.className = 'attachment-preview-item';
        previewEl.dataset.fileId = tempId;

        const isImage = file.type.startsWith('image/');
        const previewContent = isImage
            ? `<img src="${URL.createObjectURL(file)}" alt="${file.name}">`
            : `<span>📁 ${file.name}</span>`;

        previewEl.innerHTML = `
        ${previewContent}
        <button class="remove-attachment-btn">&times;</button>
    `;

        previewEl.querySelector('.remove-attachment-btn').addEventListener('click', () => {
            removeAttachmentFromPreview(tempId);
        });

        attachmentPreviewContainer.appendChild(previewEl);

        pendingAttachments.push({
            file,
            mimeType: file.type,
            tempId
        });
    }


    // Удаление превью
    function removeAttachmentFromPreview(tempId) {
        pendingAttachments = pendingAttachments.filter(att => att.tempId !== tempId);
        const previewEl = attachmentPreviewContainer.querySelector(`[data-file-id='${tempId}']`);
        if (previewEl) previewEl.remove();
    }

    //Загрузка вложений
    async function loadAttachments(type) {
        // Рисуем скелетон
        if (type === "IMAGE" || type === "VIDEO") {
            attachmentsContent.innerHTML = `
      <div class="skeleton-grid">
        ${Array(12).fill('<div class="skeleton skeleton-tile"></div>').join("")}
      </div>`;
        } else {
            attachmentsContent.innerHTML = `
      <div class="skeleton-list">
        ${Array(6).fill('<div class="skeleton skeleton-row"></div>').join("")}
      </div>`;
        }

        try {
            const url = `http://localhost:8087/api/attachments/find-by-type-and-chat-id?mediaType=${type}&chatId=${activeChatId}`;
            const attachments = await apiFetch(url);

            if (!attachments || attachments.length === 0) {
                attachmentsContent.innerHTML = "<p>Нет вложений в этой категории</p>";
                return;
            }

            const items = await Promise.all(attachments.map(async att => {
                const getLinkUrl = `${API_STORAGE_URL}/download/by-id?id=${att.fileId}`;
                try {
                    const realDownloadUrl = await apiFetch(getLinkUrl);
                    if (type === "IMAGE") {
                        return `<div class="attachment-item">
                    <a href="${realDownloadUrl.href}" target="_blank">
                      <img src="${realDownloadUrl.href}" alt="Изображение">
                    </a>
                  </div>`;
                    } else if (type === "VIDEO") {
                        return `<div class="attachment-item">
                    <video src="${realDownloadUrl.href}" controls></video>
                  </div>`;
                    } else if (type === "AUDIO") {
                        return `<div class="attachment-list-item">
                    <audio controls src="${realDownloadUrl.href}"></audio>
                    <a href="${realDownloadUrl.href}" download>Скачать</a>
                  </div>`;
                    } else {
                        return `<div class="attachment-list-item">
                    <span>${att.mimeType || "Файл"}</span>
                    <a href="${realDownloadUrl.href}" download>Скачать</a>
                  </div>`;
                    }
                } catch {
                    return `<div class="attachment-list-item error">Ошибка загрузки файла</div>`;
                }
            }));

            // Отображаем грид или список
            if (type === "IMAGE" || type === "VIDEO") {
                attachmentsContent.innerHTML = `<div class="attachments-grid">${items.join("")}</div>`;
            } else {
                attachmentsContent.innerHTML = `<div class="attachments-list">${items.join("")}</div>`;
            }
        } catch (e) {
            console.error("Ошибка загрузки вложений:", e);
            attachmentsContent.innerHTML = "<p>Не удалось загрузить вложения</p>";
        }
    }

    function renderPendingMessage(content, attachments, tempId) {
        return `
        <div class="message sent pending" data-temp-id="${tempId}">
            ${content ? `<div class="message-content">${content}</div>` : ""}
            ${attachments?.length ? renderAttachmentPreview(attachments) : ""}
            <div class="message-meta">
                <span>Отправка...</span>
                <span class="message-status sending">⏳</span>
            </div>
        </div>
    `;
    }


    async function renderAttachmentPreview(attachments) {
        return `
        <div class="attachments-container">
            ${attachments.map(a => {
            if (a.mimeType.startsWith("image/")) {
                // Для изображений показываем скелетон до загрузки
                return `
                        <div class="attachment-item">
                            <div class="image-skeleton skeleton"></div>
                            <img src="${a.src}" alt="Изображение" class="attachment-image" style="display:none;">
                        </div>`;
            } else {
                return `
                        <div class="attachment-item">
                            <span>${a.mimeType}</span>
                        </div>`;
            }
        }).join("")}
        </div>
    `;
    }

    const allImages = document.querySelectorAll('.attachment-image');
    allImages.forEach(img => {
        img.onload = () => handleImageLoad(img);
        img.src = img.dataset.src;  // Применяем реальный src
    });

    function handleImageLoad(imgElement) {
        imgElement.style.display = "block"; // Показываем изображение
        const skeleton = imgElement.previousElementSibling;
        if (skeleton && skeleton.classList.contains("image-skeleton")) {
            skeleton.remove(); // Убираем скелетон
        }
    }


    function generateTempId() {
        return 'temp-' + Date.now() + '-' + Math.floor(Math.random() * 10000);
    }

    // --- Обработчики событий ---

    closeChatBtn.addEventListener('click', () => {
        chatWindowEl.classList.add('hidden');
        activeChatId = null;
        [...chatListEl.children].forEach(li => li.classList.remove('active'));
    });


    function debounce(func, delay) {
        let timeout;
        return function (...args) {
            const context = this;
            clearTimeout(timeout);
            timeout = setTimeout(() => func.apply(context, args), delay);
        };
    }

    chatListEl.addEventListener('scroll', debounce(() => {
        if (chatListEl.scrollTop + chatListEl.clientHeight >= chatListEl.scrollHeight - 100) {
            loadChats();
        }
    }, 300));

    messagesEl.addEventListener('scroll', async () => {
        if (messagesEl.scrollTop === 0 && hasMoreMessages && !isLoading) {

            const scrollHeightBefore = messagesEl.scrollHeight;

            try {
                const messages = await loadMessages(activeChatId, messagePage);

                if (messages && messages.length > 0) {

                    const fragment = document.createDocumentFragment();


                    for (const msg of messages) {
                        const isSentByMe = msg.senderId === currentUserId;
                        const msgDiv = await createMessageElement(msg, isSentByMe);
                        fragment.appendChild(msgDiv); // Добавляем в конец буфера, сохраняя порядок
                    }

                    messagesEl.prepend(fragment);

                    messagesEl.scrollTop = messagesEl.scrollHeight - scrollHeightBefore;

                    messagePage++;
                }
            } catch (error) {
                console.error("Ошибка при подгрузке старых сообщений:", error);
            }
        }
    });


    // Отправка сообщения по форме
    messageForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const content = messageInput.value.trim();

        if (!content && pendingAttachments.length === 0) return;

        // 1. Загружаем вложения
        const uploadedAttachments = [];
        for (let att of pendingAttachments) {
            try {
                const formData = new FormData();
                formData.append('file', att.file);

                const response = await fetch(`${API_STORAGE_URL}/upload`, {
                    method: 'POST',
                    headers: {'Authorization': `Bearer ${localStorage.getItem('accessToken')}`},
                    body: formData
                });

                if (!response.ok) throw new Error("Ошибка при загрузке");

                const result = await response.json(); // { id: ... }
                uploadedAttachments.push({
                    mimeType: att.mimeType,
                    fileId: result.id
                });

            } catch (err) {
                console.error("Ошибка загрузки файла:", err);
                alert(`Не удалось загрузить файл: ${att.file.name}`);
            }
        }

        // 2. Отправляем сообщение
        chatManager.sendMessageWithAttachments(content, uploadedAttachments);

        // 3. Чистим форму
        messageInput.value = '';
        attachmentPreviewContainer.innerHTML = '';
        pendingAttachments = [];
    });

    attachFileBtn.addEventListener('click', () => fileInput.click());

    fileInput.addEventListener('change', (e) => {
        const files = e.target.files;
        if (files && files.length > 0) {
            [...files].forEach(file => addAttachmentToPreview(file));
            fileInput.value = ''; // сбрасываем input
        }
    });

    findUserBtn.addEventListener('click', loadAndShowUsers);

    closeModalBtn.addEventListener('click', () => userSearchModal.classList.add('hidden'));

    userSearchModal.addEventListener('click', (e) => {
        if (e.target === userSearchModal) {
            userSearchModal.classList.add('hidden');
        }
    });

    logoutBtn.addEventListener('click', () => {
        window.location.href = '/logout';
    });

    //Вложения
    attachmentsBtn.addEventListener("click", () => {
        if (!activeChatId) return;
        attachmentsModal.classList.remove("hidden");
        loadAttachments("IMAGE"); // по умолчанию картинки
    });

    closeAttachmentsBtn.addEventListener("click", () => {
        attachmentsModal.classList.add("hidden");
    });

    attachmentsTabs.forEach(tab => {
        tab.addEventListener("click", () => {
            attachmentsTabs.forEach(t => t.classList.remove("active"));
            tab.classList.add("active");
            loadAttachments(tab.dataset.type);
        });
    });

    // =================================================================
    // ИНИЦИАЛИЗАЦИЯ
    // =================================================================

    async function initializeApp() {
        try {
            const me = await apiFetch(`${API_USERS_URL}/users/me`);
            currentUserId = me.id;
            participantCache[me.id] = `${me.name} ${me.surname}`;

            statusEl.textContent = 'Загрузка чатов...';
            loadChats();
            chatManager.start();

        } catch (error) {
            console.error("Ошибка инициализации:", error);
            statusEl.textContent = "Не удалось загрузить данные пользователя. Пожалуйста, войдите снова.";
        }
    }

    initializeApp();
});