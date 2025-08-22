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

            this.stompClient.subscribe(`/user/queue/messages`, async (message) => { // !!! Делаем обработчик async !!!
                try {
                    const newMsg = JSON.parse(message.body);

                    await updateOrFetchChatInList(newMsg);

                    if (newMsg.chatId === activeChatId) {
                        const isSentByMe = newMsg.senderId === currentUserId;
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

        sendMessageWithAttachments: function(content, attachments) {
            if (this.stompClient && this.isConnected && activeChatId) {
                const chatMessage = {
                    chatId: activeChatId,
                    content: content,
                    attachments: attachments // attachments - это массив [{ attachmentId: ..., mimeType: ... }]
                };
                this.stompClient.send("/app/chat.send", {}, JSON.stringify(chatMessage));
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
        }
        else {
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
        messagesEl.innerHTML = '';
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

        messagesEl.appendChild(fragment);

        messagesEl.scrollTop = messagesEl.scrollHeight;
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
                        // Используем полученную ссылку для отображения картинки
                        return `
                        <div class="attachment-item image-attachment">
                            <a href="${realDownloadUrl.href}" target="_blank" rel="noopener noreferrer">
                                <img src="${realDownloadUrl.href}" alt="Вложение" loading="lazy">
                            </a>
                        </div>`;
                    } else {
                        // Используем полученную ссылку для кнопки "Скачать"
                        // Атрибут `download` заставит браузер скачать файл, а не пытаться открыть его.
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
                    // Единый блок обработки ошибок для всех типов вложений
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

        return msgDiv;
    }

    async function addMessageToUI(msg, isSentByMe, prepend = false) {
        const placeholder = messagesEl.querySelector('.placeholder');
        if (placeholder) placeholder.remove();

        const wasScrolledToBottom = messagesEl.scrollHeight - messagesEl.clientHeight <= messagesEl.scrollTop + 1;

        const msgDiv = await createMessageElement(msg, isSentByMe);

        if (prepend) {
            messagesEl.prepend(msgDiv);
        } else {
            messagesEl.appendChild(msgDiv);
        }
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
    function addAttachmentToPreview(file, fileId, mimeType) {
        const previewEl = document.createElement('div');
        previewEl.className = 'attachment-preview-item';
        previewEl.dataset.fileId = fileId;

        const isImage = mimeType.startsWith('image/');
        const previewContent = isImage
            ? `<img src="${URL.createObjectURL(file)}" alt="${file.name}">`
            : `<span>📁 ${file.name}</span>`;

        previewEl.innerHTML = `
            ${previewContent}
            <button class="remove-attachment-btn">&times;</button>
        `;

        // Добавляем обработчик на кнопку удаления
        previewEl.querySelector('.remove-attachment-btn').addEventListener('click', () => {
            removeAttachmentFromPreview(fileId);
        });

        attachmentPreviewContainer.appendChild(previewEl);
    }

    // Удаление превью
    function removeAttachmentFromPreview(fileId) {
        pendingAttachments = pendingAttachments.filter(att => att.attachmentId !== fileId);
        const previewEl = attachmentPreviewContainer.querySelector(`[data-file-id='${fileId}']`);
        if (previewEl) {
            previewEl.remove();
        }
    }

    // Главная функция загрузки файла
    async function uploadFile(file) {
        setFormEnabled(false); // Блокируем форму
        const tempId = `temp-${Date.now()}`;
        const tempPreviewEl = document.createElement('div');
        tempPreviewEl.className = 'attachment-preview-item loading';
        tempPreviewEl.dataset.fileId = tempId;
        tempPreviewEl.innerHTML = `<span>⏳ Загрузка: ${file.name}</span>`;
        attachmentPreviewContainer.appendChild(tempPreviewEl);

        const formData = new FormData();
        formData.append('file', file);

        try {
            const response = await fetch(`${API_STORAGE_URL}/upload`, {
                method: 'POST',
                headers: { 'Authorization': `Bearer ${localStorage.getItem('accessToken')}` },
                body: formData
            });

            if (!response.ok) throw new Error('Ошибка сервера при загрузке файла.');

            const result = await response.json(); // Ожидаем { id: ..., path: ... }

            tempPreviewEl.remove();
            addAttachmentToPreview(file, result.id, file.type);

            // Добавляем информацию о файле в массив ожидания
            pendingAttachments.push({
                mimeType: file.type,
                fileId: result.id
            });

        } catch (error) {
            console.error('Ошибка загрузки файла:', error);
            tempPreviewEl.remove(); // Удаляем временное превью при ошибке
            alert('Не удалось загрузить файл.');
        } finally {
            setFormEnabled(true); // Разблокируем форму в любом случае
            fileInput.value = ''; // Сбрасываем input
        }
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
            const messages = await loadMessages(activeChatId, messagePage);
            if (messages.length > 0) {
                messages.forEach(msg => {
                    addMessageToUI(msg, false, true);
                });
                // Добавляем в начало
                messagesEl.scrollTop = messagesEl.scrollHeight - scrollHeightBefore; // Восстанавливаем позицию
                messagePage++;
            }
        }
    });

    // Отправка сообщения по форме
    messageForm.addEventListener('submit', (e) => {
        e.preventDefault();
        const content = messageInput.value.trim();

        if (content || pendingAttachments.length > 0) {
            chatManager.sendMessageWithAttachments(content, pendingAttachments);
            messageInput.value = '';
            attachmentPreviewContainer.innerHTML = '';
            pendingAttachments = [];
        }
    });

    attachFileBtn.addEventListener('click', () => fileInput.click());

    fileInput.addEventListener('change', (e) => {
        const files = e.target.files;
        if (files && files.length > 0) {
            uploadFile(files[0]);
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