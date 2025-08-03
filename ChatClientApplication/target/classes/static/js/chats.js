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

    const findUserBtn = document.getElementById('findUserBtn');
    const closeModalBtn = document.getElementById('closeModalBtn');
    const userListContainer = document.getElementById('userListContainer');

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

            this.stompClient.subscribe(`/user/queue/messages`, (message) => {
                try {
                    const msg = JSON.parse(message.body);

                    if (msg.chatId === activeChatId) {
                        const isSentByMe = msg.senderId === currentUserId;

                        addMessageToUI(msg, isSentByMe);

                        if (!isSentByMe) {
                            markMessagesAsRead([msg]);
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

        sendMessage: function (content) {
            if (this.stompClient && this.isConnected && activeChatId) {

                const chatMessage = {
                    chatId: activeChatId,
                    content: content
                };

                this.stompClient.send("/app/chat.send", {}, JSON.stringify(chatMessage));

                messageInput.value = '';
            } else {
                alert("Нет подключения для отправки сообщения.");
            }
        },
    };

    function renderUsers(users) {
        userListContainer.innerHTML = '';
        if (!users || users.length === 0) {
            userListContainer.innerHTML = '<p class="placeholder">Пользователи не найдены.</p>';
            return;
        }
        users.forEach(user => {
            if (user.id === currentUserId) return; // Пропускаем себя

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
        if (activeChatId === chat.chatId) return;

        activeChatId = chat.chatId;
        messagePage = 0;
        hasMoreMessages = true;
        participantCache = {};

        [...chatListEl.children].forEach(li => {
            li.classList.toggle('active', li.dataset.chatId == activeChatId);
        });

        chatTitleEl.textContent = 'Загрузка...';
        messagesEl.innerHTML = '<p class="placeholder">Загрузка данных...</p>';
        chatWindowEl.classList.remove('hidden');

        try {
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

            const messages = await loadMessages(chat.chatId, messagePage);

            renderMessages(messages);

            const unreadMessages = messages.filter(msg => !msg.read && msg.senderId !== currentUserId);

            // 2. Вызываем нашу новую функцию для отправки
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

    function renderMessages(messages) {
        messagesEl.innerHTML = '';
        if (!messages || messages.length === 0) {
            messagesEl.innerHTML = '<p class="placeholder">Сообщений пока нет. Напишите первым!</p>';
            return;
        }
        messages.forEach(msg => {
            const isSentByMe = msg.senderId === currentUserId;
            addMessageToUI(msg, isSentByMe, true);
        });
        messagesEl.scrollTop = messagesEl.scrollHeight;
    }

    function addMessageToUI(msg, isSentByMe, prepend = false) {
        const placeholder = messagesEl.querySelector('.placeholder');
        if (placeholder) placeholder.remove();

        const wasScrolledToBottom = messagesEl.scrollHeight - messagesEl.clientHeight <= messagesEl.scrollTop + 1;

        const msgDiv = document.createElement('div');
        msgDiv.className = `message ${isSentByMe ? 'sent' : 'received'}`;
        msgDiv.dataset.messageId = msg.id;

        const senderName = isSentByMe ? 'Вы' : (participantCache[msg.senderId] || `Пользователь #${msg.senderId}`);

        if (msg.isPending) {
            msgDiv.innerHTML = `
            <div class="message-content">${msg.content}</div>
            <div class="message-meta">
                <span>Отправка...</span>
                <span class="message-status pending-animation"></span> 
            </div>
        `;
        } else {
            const statusText = isSentByMe ? (msg.read ? 'Прочитано' : 'Доставлено') : '';
            const statusClass = isSentByMe && msg.read ? 'read' : '';

            msgDiv.innerHTML = `
            <div class="message-sender">${senderName}</div>
            <div class="message-content">${msg.content}</div>
            <div class="message-meta">
                <span>${formatDate(msg.createdAt)}</span>
                <span class="message-status ${statusClass}">${statusText}</span>
            </div>
        `;
        }

        if (prepend) {
            messagesEl.prepend(msgDiv);
        } else {
            messagesEl.appendChild(msgDiv);
        }

        if (wasScrolledToBottom) {
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

    // --- Обработчики событий ---

    closeChatBtn.addEventListener('click', () => {
        chatWindowEl.classList.add('hidden');
        activeChatId = null;
        [...chatListEl.children].forEach(li => li.classList.remove('active'));
    });

    // В файле chats.js

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
        if (content) {
            console.log(content)
            chatManager.sendMessage(content);
            messageInput.value = '';
        }
    });

    findUserBtn.addEventListener('click', loadAndShowUsers);

    closeModalBtn.addEventListener('click', () => userSearchModal.classList.add('hidden'));

    userSearchModal.addEventListener('click', (e) => {
        if (e.target === userSearchModal) {
            userSearchModal.classList.add('hidden');
        }
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