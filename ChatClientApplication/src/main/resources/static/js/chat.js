const API_URL = 'http://localhost:8086';
let chatId = null;
const chatManager = {
    stompClient: null,
    isConnecting: false,
    isConnected: false,
    retryCount: 0,
    maxRetries: 3,

    start: function() {
        if (this.isConnected || this.isConnecting) {
            return;
        }
        this.connect();
    },

    connect: function() {
        this.isConnecting = true;
        const accessToken = localStorage.getItem('accessToken');

        if (!accessToken) {
            logout();
            return;
        }

        const socket = new SockJS(`${API_URL}/ws-chat?token=${accessToken}`);
        this.stompClient = Stomp.over(socket);
        this.stompClient.debug = null;

        this.stompClient.connect({},
            (frame) => {this.onConnectSuccess(frame); console.log('Connected: ' + frame)},
            (error) => this.onConnectError(error)
        );
    },

    onConnectSuccess: function(frame) {
        this.isConnecting = false;
        this.isConnected = true;
        this.retryCount = 0;

        this.stompClient.subscribe(`/user/queue/messages`, async function(message) {
            try {
                const msg = JSON.parse(message.body);
                chatId = msg.chatId;
                const isSentByUser = false;
                console.log(`Полученное сообщение: ${msg}`);
                await addMessageToUI(msg, isSentByUser);
            } catch (error) {
                console.error('Ошибка при обработке сообщения:', error);
            }
        });
    },

    onConnectError: async function(error) {
        this.isConnecting = false;
        this.isConnected = false;

        if (this.retryCount >= this.maxRetries) {
            alert("Не удалось подключиться к серверу. Пожалуйста, войдите снова.");
            logout();
            return;
        }

        this.retryCount++;

        console.log(`Соединение потеряно. Пытаюсь обновить токен и переподключиться (попытка ${this.retryCount})...`);

        try {
            await handleTokenRefresh();
            console.log("Токен успешно обновлен. Переподключаюсь...");

            setTimeout(() => {
                this.connect();
            }, 500 * this.retryCount);

        } catch (refreshError) {
            console.error("Не удалось обновить токен. Выход из системы.", refreshError);
            alert("Ваша сессия истекла. Пожалуйста, войдите снова.");
            logout();
        }
    },

    sendMessage: function(content, toUserId) {
        if (this.stompClient && this.isConnected) {
            const chatMessage = {
                fromUserId: null,
                toUserId: toUserId,
                content: content,
                timestamp: Date.now()
            };
            this.stompClient.send("/app/chat.send", {}, JSON.stringify(chatMessage));
        } else {
            alert("Нет подключения к чату для отправки сообщения.");
        }
    },

    disconnect: function() {
        if (this.stompClient) {
            this.stompClient.disconnect(() => {
                this.isConnected = false;
            });
        }
    }
};

async function getUserInitials(userId) {
    let token = localStorage.getItem('accessToken')
    try {
        const response = await fetch(`http://localhost:8085/api/users/initials/${userId}`,{
            headers : {
                'Authorization' : `Bearer ${token}`
            }
        });
        if (!response.ok) {
            console.error('Ошибка при получении инициалов:', response.status);
            return '??'; // Возвращаем заглушку при ошибке
        }
        return await response.text();
    } catch (error) {
        console.error('Ошибка при запросе инициалов:', error);
        return '??';
    }
}

// Функция для форматирования даты в читаемый вид
function formatTimestamp(timestamp) {
    if (!timestamp) return 'неизвестно';
    const date = new Date(timestamp);
    return date.toLocaleString('ru-RU');
}

async function addMessageToUI(messageData, isSentByUser) {
    const messagesDiv = document.getElementById('messages');
    if (!messagesDiv) return;

    const messageElement = document.createElement('div');
    messageElement.className = `message ${isSentByUser ? 'sent' : 'received'}`;
    console.log(`Полученное сообщение: ${messageData}`);
    const initials = await getUserInitials(messageData.senderId);

    // Создаём HTML-структуру сообщения
    messageElement.innerHTML = `
        <div class="message-header">
            <span>${isSentByUser ? 'Вы' : `Отправитель: ${initials}`}</span>
        </div>
        <div class="message-content">${messageData.content}</div>
        <div class="message-time">
            Отправлено: ${formatTimestamp(messageData.createdAt)}
            ${messageData.updatedAt ? `(Изменено: ${formatTimestamp(messageData.updatedAt)})` : ''}
        </div>
        <div class="message-status">
            Статус: ${messageData.isRead ? `Прочитано ${formatTimestamp(messageData.readAt)}` : 'Не прочитано'}
        </div>
    `;

    messagesDiv.appendChild(messageElement);
    messagesDiv.scrollTop = messagesDiv.scrollHeight; // Автопрокрутка к новому сообщению
}

document.addEventListener('DOMContentLoaded', () => {
    chatManager.start();

    const sendForm = document.getElementById('send-form');
    console.log(sendForm);
    if (sendForm) {
        sendForm.addEventListener('submit', (e) => {
            e.preventDefault();
            const input = document.getElementById('message-input');
            const recipientInput = document.getElementById('recipient-id-input');
            if (input && recipientInput && input.value && recipientInput.value) {
                chatManager.sendMessage(input.value, recipientInput.value);
                //addMessageToUI(`Вы: ${input.value}`, true);
                input.value = '';
            }
        });
    }
});