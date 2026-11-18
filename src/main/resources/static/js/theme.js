// Theme toggle — persiste em localStorage, aplica sem flash
(function () {
    var STORAGE_KEY = 'dc-theme';

    function applyTheme(theme) {
        document.documentElement.setAttribute('data-theme', theme);
        var btn = document.getElementById('theme-toggle');
        if (btn) {
            btn.setAttribute('aria-label', theme === 'dark' ? 'Mudar para modo claro' : 'Mudar para modo escuro');
            btn.setAttribute('title',      theme === 'dark' ? 'Modo claro' : 'Modo escuro');
            btn.textContent = theme === 'dark' ? '☀' : '🌙';
        }
    }

    function getTheme() {
        var saved = localStorage.getItem(STORAGE_KEY);
        if (saved) return saved;
        return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
    }

    function toggle() {
        var current = document.documentElement.getAttribute('data-theme') || 'light';
        var next = current === 'dark' ? 'light' : 'dark';
        localStorage.setItem(STORAGE_KEY, next);
        applyTheme(next);
    }

    // Apply immediately (before paint) to avoid flash
    applyTheme(getTheme());

    document.addEventListener('DOMContentLoaded', function () {
        applyTheme(getTheme()); // re-apply after DOM ready (updates button label)
        var btn = document.getElementById('theme-toggle');
        if (btn) btn.addEventListener('click', toggle);
    });
})();
