
document.addEventListener('DOMContentLoaded', () => {
    const loginForm = document.getElementById('login-form');
    const loginButton = document.getElementById('login-button');
    const errorMessageDiv = document.getElementById('error-message');
    const usernameInput = document.getElementById('username');
    const passwordInput = document.getElementById('password');

    const AUTH_API_URL = 'http://localhost:8085/auth/login';

    loginForm.addEventListener('submit', async (event) => {
        event.preventDefault();

        const username = usernameInput.value;
        const password = passwordInput.value;

        errorMessageDiv.classList.add('hidden');
        loginButton.disabled = true;
        loginButton.textContent = 'Вход...';

        try {
            const response = await fetch(AUTH_API_URL, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ username, password }),
            });

            if (response.ok) {
                const data = await response.json();
                localStorage.setItem('accessToken', data.accessToken);
                localStorage.setItem('refreshToken', data.refreshToken);
                window.location.href = '/';

            } else if (response.status === 401) {
                showError('Неверное имя пользователя или пароль.');
            } else {
                showError('Произошла ошибка на сервере. Попробуйте позже.');
            }

        } catch (error) {
            console.error('Ошибка сети:', error);
            showError('Не удалось подключиться к серверу аутентификации.');
        } finally {
            if (!localStorage.getItem('accessToken')) {
                loginButton.disabled = false;
                loginButton.textContent = 'Войти';
            }
        }
    });

    function showError(message) {
        errorMessageDiv.textContent = message;
        errorMessageDiv.classList.remove('hidden');
    }
});