document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('register-form');
    const registerButton = document.getElementById('register-button');
    const messageBox = document.getElementById('message-box');

    form.addEventListener('submit', async (event) => {
        // Предотвращаем стандартную отправку формы
        event.preventDefault();

        // Очищаем предыдущие сообщения и стили
        messageBox.classList.add('hidden');
        messageBox.classList.remove('success-message', 'error-message');
        messageBox.textContent = '';

        // Блокируем кнопку на время запроса
        registerButton.disabled = true;
        registerButton.textContent = 'Регистрация...';

        // Собираем данные из формы в объект
        const formData = new FormData(form);
        const userRegisterDto = {
            name: formData.get('name'),
            surname: formData.get('surname'),
            username: formData.get('username'),
            password: formData.get('password'),
            email: formData.get('email')
        };

        try {
            // Отправляем POST-запрос на сервер
            const response = await fetch('http://localhost:8085/auth/register', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(userRegisterDto),
            });

            // Обрабатываем ответ
            if (response.ok) { // Проверяем успешный статус (например, 200 OK или 201 Created)
                const jwtResponse = await response.json();

                // Сохраняем токены в локальное хранилище
                if (jwtResponse.accessToken && jwtResponse.refreshToken) {
                    localStorage.setItem('accessToken', jwtResponse.accessToken);
                    localStorage.setItem('refreshToken', jwtResponse.refreshToken);

                    // Переадресовываем на главную страницу
                    window.location.href = '/index';
                } else {
                    throw new Error('Ответ сервера не содержит токенов.');
                }
            } else {
                // Если статус ответа не "успех", пытаемся получить текст ошибки
                const errorData = await response.json().catch(() => ({ message: 'Произошла неизвестная ошибка' }));
                const errorMessage = errorData.message || `Ошибка ${response.status}: ${response.statusText}`;
                throw new Error(errorMessage);
            }
        } catch (error) {
            // Показываем сообщение об ошибке
            messageBox.textContent = error.message || 'Не удалось подключиться к серверу.';
            messageBox.classList.add('error-message');
            messageBox.classList.remove('hidden');
        } finally {
            // Включаем кнопку обратно в любом случае (кроме успешной переадресации)
            registerButton.disabled = false;
            registerButton.textContent = 'Зарегистрироваться';
        }
    });
});