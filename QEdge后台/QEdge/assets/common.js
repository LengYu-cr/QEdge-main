const Icons = {
    menu: '<svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="3" y1="6" x2="21" y2="6"></line><line x1="3" y1="12" x2="21" y2="12"></line><line x1="3" y1="18" x2="21" y2="18"></line></svg>',
    check: '<svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"></polyline></svg>',
    x: '<svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>',
    info: '<svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="16" x2="12" y2="12"></line><line x1="12" y1="8" x2="12.01" y2="8"></line></svg>',
    warning: '<svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path><line x1="12" y1="9" x2="12" y2="13"></line><line x1="12" y1="17" x2="12.01" y2="17"></line></svg>',
    users: '<svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle><path d="M23 21v-2a4 4 0 0 0-3-3.87"></path><path d="M16 3.13a4 4 0 0 1 0 7.75"></path></svg>',
    home: '<svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path><polyline points="9 22 9 12 15 12 15 22"></polyline></svg>',
    shield: '<svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path></svg>',
    dashboard: '<svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="7"></rect><rect x="14" y="3" width="7" height="7"></rect><rect x="14" y="14" width="7" height="7"></rect><rect x="3" y="14" width="7" height="7"></rect></svg>',
    settings: '<svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="3"></circle><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"></path></svg>',
    logout: '<svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path><polyline points="16 17 21 12 16 7"></polyline><line x1="21" y1="12" x2="9" y2="12"></line></svg>',
    search: '<svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>',
    trash: '<svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path></svg>',
    edit: '<svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path></svg>',
    download: '<svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path><polyline points="7 10 12 15 17 10"></polyline><line x1="12" y1="15" x2="12" y2="3"></line></svg>',
    ban: '<svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><line x1="4.93" y1="4.93" x2="19.07" y2="19.07"></line></svg>',
    checkCircle: '<svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path><polyline points="22 4 12 14.01 9 11.01"></polyline></svg>',
    gift: '<svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 12 20 22 4 22 4 12"></polyline><rect x="2" y="7" width="20" height="5"></rect><line x1="12" y1="22" x2="12" y2="7"></line><path d="M12 7H7.5a2.5 2.5 0 0 1 0-5C11 2 12 7 12 7z"></path><path d="M12 7h4.5a2.5 2.5 0 0 0 0-5C13 2 12 7 12 7z"></path></svg>',
    upload: '<svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path><polyline points="17 8 12 3 7 8"></polyline><line x1="12" y1="3" x2="12" y2="15"></line></svg>',
    user: '<svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path><circle cx="12" cy="7" r="4"></circle></svg>',
    lock: '<svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect><path d="M7 11V7a5 5 0 0 1 10 0v4"></path></svg>',
    mail: '<svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"></path><polyline points="22,6 12,13 2,6"></polyline></svg>',
    bell: '<svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path><path d="M13.73 21a2 2 0 0 1-3.46 0"></path></svg>',
    file: '<svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M13 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z"></path><polyline points="13 2 13 9 20 9"></polyline></svg>',
    zap: '<svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"></polygon></svg>',
    activity: '<svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"></polyline></svg>',
    qrCode: '<svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="7"></rect><rect x="14" y="3" width="7" height="7"></rect><rect x="14" y="14" width="7" height="7"></rect><rect x="3" y="14" width="7" height="7"></rect></svg>',
    arrowLeft: '<svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="19" y1="12" x2="5" y2="12"></line><polyline points="12 19 5 12 12 5"></polyline></svg>',
    arrowRight: '<svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="5" y1="12" x2="19" y2="12"></line><polyline points="12 5 19 12 12 19"></polyline></svg>',
    star: '<svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"></polygon></svg>',
    externalLink: '<svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"></path><polyline points="15 3 21 3 21 9"></polyline><line x1="10" y1="14" x2="21" y2="3"></line></svg>'
};

function icon(name, size = '') {
    const sizeClass = size ? ' icon-' + size : '';
    return `<span class="icon${sizeClass}">${Icons[name] || ''}</span>`;
}

const Sidebar = {
    sidebar: null,
    overlay: null,

    init() {
        this.sidebar = document.querySelector('.sidebar');
        this.overlay = document.querySelector('.sidebar-overlay');

        if (!this.sidebar || !this.overlay) return;

        const menuBtn = document.querySelector('.topbar-menu-btn');
        if (menuBtn) {
            menuBtn.addEventListener('click', () => this.toggle());
        }

        this.overlay.addEventListener('click', () => this.close());

        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape') {
                this.close();
            }
        });
    },

    open() {
        if (this.sidebar) this.sidebar.classList.add('open');
        if (this.overlay) this.overlay.classList.add('active');
        document.body.style.overflow = 'hidden';
    },

    close() {
        if (this.sidebar) this.sidebar.classList.remove('open');
        if (this.overlay) this.overlay.classList.remove('active');
        document.body.style.overflow = '';
    },

    toggle() {
        if (this.sidebar && this.sidebar.classList.contains('open')) {
            this.close();
        } else {
            this.open();
        }
    }
};

const Toast = {
    container: null,

    init() {
        this.container = document.createElement('div');
        this.container.className = 'toast-container';
        document.body.appendChild(this.container);
    },

    show(message, type = 'success', duration = 3000) {
        if (!this.container) {
            this.init();
        }

        const toast = document.createElement('div');
        toast.className = `toast toast-${type}`;

        let iconName = 'check';
        switch (type) {
            case 'success':
                iconName = 'check';
                break;
            case 'error':
                iconName = 'x';
                break;
            case 'info':
                iconName = 'info';
                break;
            case 'warning':
                iconName = 'warning';
                break;
        }

        toast.innerHTML = `${icon(iconName)}<span>${message}</span>`;
        this.container.appendChild(toast);

        setTimeout(() => {
            toast.style.animation = 'toastOut 0.3s ease-out';
            setTimeout(() => {
                if (toast.parentNode) {
                    toast.parentNode.removeChild(toast);
                }
            }, 300);
        }, duration);
    },

    success(message, duration) {
        this.show(message, 'success', duration);
    },

    error(message, duration) {
        this.show(message, 'error', duration);
    },

    info(message, duration) {
        this.show(message, 'info', duration);
    },

    warning(message, duration) {
        this.show(message, 'warning', duration);
    }
};

const Modal = {
    overlay: null,
    modal: null,

    init() {
        this.overlay = document.createElement('div');
        this.overlay.className = 'modal-overlay';
        this.overlay.style.display = 'none';
        this.overlay.innerHTML = `
            <div class="modal">
                <div class="modal-header">
                    <h3 class="modal-title" id="modal-title">提示</h3>
                    <button class="modal-close" onclick="Modal.close()" aria-label="关闭">${icon('x', 'sm')}</button>
                </div>
                <div class="modal-body" id="modal-content"></div>
                <div class="modal-footer">
                    <button class="btn btn-secondary" onclick="Modal.close()">取消</button>
                    <button class="btn btn-primary" id="modal-confirm">确认</button>
                </div>
            </div>
        `;
        document.body.appendChild(this.overlay);

        this.overlay.addEventListener('click', (e) => {
            if (e.target === this.overlay) {
                this.close();
            }
        });

        this.modal = this.overlay.querySelector('.modal');
    },

    show(options) {
        if (!this.overlay) {
            this.init();
        }

        const title = options.title || '提示';
        const content = options.content || '';
        const confirmText = options.confirmText || '确认';
        const onConfirm = options.onConfirm || null;
        const showCancel = options.showCancel !== false;

        document.getElementById('modal-title').textContent = title;
        document.getElementById('modal-content').innerHTML = content;
        document.getElementById('modal-confirm').textContent = confirmText;

        const cancelBtn = this.overlay.querySelector('.modal-footer .btn-secondary');
        if (cancelBtn) {
            cancelBtn.style.display = showCancel ? '' : 'none';
        }

        const confirmBtn = document.getElementById('modal-confirm');
        confirmBtn.onclick = () => {
            if (onConfirm) {
                onConfirm();
            }
            this.close();
        };

        this.overlay.style.display = 'flex';
    },

    close() {
        if (this.overlay) {
            this.overlay.style.display = 'none';
        }
    },

    confirm(message, onConfirm, title = '确认') {
        this.show({
            title: title,
            content: `<p>${message}</p>`,
            onConfirm: onConfirm
        });
    },

    alert(message, onClose, title = '提示') {
        this.show({
            title: title,
            content: `<p>${message}</p>`,
            confirmText: '确定',
            showCancel: false,
            onConfirm: onClose
        });
    }
};

const Loading = {
    overlay: null,

    init() {
        this.overlay = document.createElement('div');
        this.overlay.className = 'loading-overlay';
        this.overlay.style.display = 'none';
        this.overlay.innerHTML = '<div class="loading-spinner"></div>';
        document.body.appendChild(this.overlay);
    },

    show() {
        if (!this.overlay) {
            this.init();
        }
        this.overlay.style.display = 'flex';
    },

    hide() {
        if (this.overlay) {
            this.overlay.style.display = 'none';
        }
    }
};

function formatDate(dateStr) {
    if (!dateStr || dateStr === '0000-00-00 00:00:00') {
        return '暂无';
    }
    const date = new Date(dateStr);
    return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
    });
}

function formatNumber(num) {
    if (!num || num === 0) {
        return '0';
    }
    return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',');
}

function isEmpty(obj) {
    if (!obj) return true;
    if (Array.isArray(obj) && obj.length === 0) return true;
    if (typeof obj === 'object' && Object.keys(obj).length === 0) return true;
    return false;
}

function debounce(func, wait) {
    let timeout;
    return function(...args) {
        clearTimeout(timeout);
        timeout = setTimeout(() => func.apply(this, args), wait);
    };
}

function throttle(func, limit) {
    let inThrottle;
    return function(...args) {
        if (!inThrottle) {
            func.apply(this, args);
            inThrottle = true;
            setTimeout(() => (inThrottle = false), limit);
        }
    };
}

function getCSRFToken() {
    const token = document.querySelector('input[name="csrf_token"], #csrf_token');
    return token ? token.value : '';
}

async function fetchApi(url, options = {}) {
    const defaultOptions = {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
        },
        credentials: 'same-origin'
    };

    const mergedOptions = { ...defaultOptions, ...options };

    try {
        const response = await fetch(url, mergedOptions);
        const data = await response.json();
        return data;
    } catch (error) {
        console.error('API请求失败:', error);
        Toast.error('网络请求失败，请稍后重试');
        throw error;
    }
}

function safeParseJSON(str) {
    try {
        return JSON.parse(str);
    } catch (e) {
        console.error('JSON解析失败:', str);
        return null;
    }
}

function validateQQ(qq) {
    return /^[1-9][0-9]{4,10}$/.test(qq);
}

function validateEmail(email) {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

function validatePhone(phone) {
    return /^1[3-9]\d{9}$/.test(phone);
}

function sanitizeInput(input) {
    if (!input) return '';
    return input.replace(/[<>&]/g, {
        '<': '&lt;',
        '>': '&gt;',
        '&': '&amp;'
    });
}

function getUrlParam(name) {
    const urlParams = new URLSearchParams(window.location.search);
    return urlParams.get(name);
}

function setUrlParam(name, value) {
    const url = new URL(window.location.href);
    url.searchParams.set(name, value);
    window.history.pushState({}, '', url);
}

function removeUrlParam(name) {
    const url = new URL(window.location.href);
    url.searchParams.delete(name);
    window.history.pushState({}, '', url);
}

function scrollToTop() {
    window.scrollTo({
        top: 0,
        behavior: 'smooth'
    });
}

function copyToClipboard(text) {
    if (navigator.clipboard && window.isSecureContext) {
        navigator.clipboard.writeText(text).then(() => {
            Toast.success('已复制到剪贴板');
        }).catch(() => {
            Toast.error('复制失败');
        });
    } else {
        const textArea = document.createElement('textarea');
        textArea.value = text;
        textArea.style.position = 'fixed';
        textArea.style.left = '-9999px';
        document.body.appendChild(textArea);
        textArea.focus();
        textArea.select();
        try {
            document.execCommand('copy');
            Toast.success('已复制到剪贴板');
        } catch (err) {
            Toast.error('复制失败');
        }
        document.body.removeChild(textArea);
    }
}

document.addEventListener('DOMContentLoaded', () => {
    Toast.init();
    Modal.init();
    Loading.init();
    Sidebar.init();
});
