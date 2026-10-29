document.addEventListener('DOMContentLoaded', () => {
    const loginForm = document.querySelector('[data-validate-login]');
    if (loginForm) {
        loginForm.addEventListener('submit', (event) => {
            const email = loginForm.querySelector('#email');
            const password = loginForm.querySelector('#password');
            let valid = true;

            valid = validateRequired(email, 'Vui lòng nhập tài khoản.') && valid;
            valid = validateRequired(password, 'Vui lòng nhập mật khẩu.') && valid;
            if (email.value.trim() && !email.validity.valid) {
                showFieldError(email, 'Tài khoản phải là một địa chỉ email hợp lệ.');
                valid = false;
            }
            if (!valid) event.preventDefault();
        });

        loginForm.querySelectorAll('input').forEach((input) => {
            input.addEventListener('input', () => clearFieldError(input));
        });
    }

    document.querySelectorAll('[data-password-toggle]').forEach((button) => {
        button.addEventListener('click', () => {
            const input = document.getElementById(button.dataset.passwordToggle);
            const visible = input.type === 'text';
            input.type = visible ? 'password' : 'text';
            button.setAttribute('aria-label', visible ? 'Hiện mật khẩu' : 'Ẩn mật khẩu');
        });
    });

    document.querySelectorAll('[data-menu-toggle]').forEach((button) => {
        button.addEventListener('click', () => document.body.classList.toggle('menu-open'));
    });
    document.querySelectorAll('.nav-item').forEach((link) => {
        link.addEventListener('click', () => document.body.classList.remove('menu-open'));
    });

    document.querySelectorAll('[data-clear-input]').forEach((button) => {
        button.addEventListener('click', () => {
            const input = document.getElementById(button.dataset.clearInput);
            input.value = '';
            input.focus();
        });
    });

    document.querySelectorAll('[data-collapse-toggle]').forEach((button) => {
        const content = document.getElementById(button.dataset.collapseToggle);
        if (content.classList.contains('is-open')) button.setAttribute('aria-expanded', 'true');
        button.addEventListener('click', () => {
            const open = content.classList.toggle('is-open');
            button.setAttribute('aria-expanded', String(open));
        });
    });

    const dateElement = document.querySelector('[data-current-date]');
    if (dateElement) {
        dateElement.textContent = new Intl.DateTimeFormat('vi-VN', {
            weekday: 'long', day: '2-digit', month: 'long', year: 'numeric'
        }).format(new Date());
    }

    const toast = document.querySelector('[data-auto-dismiss]');
    if (toast) {
        setTimeout(() => toast.classList.add('is-hiding'), 4200);
        setTimeout(() => toast.remove(), 4600);
    }
});

function validateRequired(input, message) {
    if (!input.value.trim()) {
        showFieldError(input, message);
        return false;
    }
    clearFieldError(input);
    return true;
}

function showFieldError(input, message) {
    input.closest('.form-field').classList.add('has-error');
    const error = document.getElementById(`${input.id}-error`);
    if (error) error.textContent = message;
}

function clearFieldError(input) {
    input.closest('.form-field').classList.remove('has-error');
    const error = document.getElementById(`${input.id}-error`);
    if (error) error.textContent = '';
}
