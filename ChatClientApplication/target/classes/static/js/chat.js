const API_URL = 'http://localhost:8086';

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

        this.stompClient.subscribe(`/user/queue/messages`, function(message) {
            const msg = JSON.parse(message.body);
            console.log("Received:", msg);
            addMessageToUI(`От ${msg.fromUserId}: ${msg.content}`, false);
        });
    },

    onConnectError: async function(error) {
        console.log(error);
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
            console.log(chatMessage);
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

function addMessageToUI(message, isSentByUser) {

    const messagesDiv = document.getElementById('messages');
    if (!messagesDiv) return;
    const messageElement = document.createElement('p');
    messageElement.textContent = message;
    messageElement.className = isSentByUser ? 'sent' : 'received';
    messagesDiv.appendChild(messageElement);
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
                addMessageToUI(`Вы: ${input.value}`, true);
                input.value = '';
            }
        });
    }
});