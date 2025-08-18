// Common JavaScript functionality for UPFSDA Attendance System

document.addEventListener('DOMContentLoaded', function () {
    // Initialize theme
    initTheme();

    // Initialize sidebar toggle
    initSidebarToggle();

    // Initialize tooltips
    initTooltips();

    // Initialize common event listeners
    initCommonEvents();

    // Initialize time updates
    initTimeUpdates();

    // Initialize authentication check
    initAuthCheck();

    // Prevent back button access after logout
    preventBackButtonAccess();

    // Check authentication when page becomes visible (tab switching)
    initVisibilityCheck();

    // Session timeout disabled - using JWT expiration (24 hours) instead
});

// Prevent back button access to protected pages after logout
function preventBackButtonAccess() {
    // Add state to history to detect back button
    window.history.pushState(null, null, window.location.href);

    window.addEventListener('popstate', function (event) {
        // Check if user is authenticated when using back button
        const token = localStorage.getItem('authToken');
        const protectedPaths = ['/attendance/dashboard', '/attendance/projects', '/attendance/leave', '/attendance/team'];
        const currentPath = window.location.pathname;

        if (protectedPaths.some(path => currentPath.startsWith(path)) && !token) {
            console.log('Back button detected on protected page without auth, redirecting to login');
            window.location.replace('/login?error=Session expired. Please login again.');
            return;
        }

        // Re-validate authentication
        if (token) {
            validateAuthToken(token);
        }
    });
}

// Sidebar toggle functionality
function initSidebarToggle() {
    const sidebarToggle = document.getElementById('sidebarToggle');
    const mobileMenuBtn = document.getElementById('mobileMenuBtn');
    const sidebar = document.getElementById('sidebar');
    const sidebarOverlay = document.getElementById('sidebarOverlay');
    const mainContent = document.querySelector('.main-content');

    // Desktop sidebar toggle (collapse/expand)
    if (sidebarToggle && sidebar) {
        sidebarToggle.addEventListener('click', function () {
            sidebar.classList.toggle('collapsed');
            if (mainContent) {
                mainContent.classList.toggle('sidebar-collapsed');
            }
        });
    }

    // Mobile sidebar toggle (show/hide)
    if (mobileMenuBtn && sidebar) {
        mobileMenuBtn.addEventListener('click', function () {
            sidebar.classList.toggle('show');
            if (sidebarOverlay) {
                sidebarOverlay.classList.toggle('show');
            }
        });
    }

    // Close sidebar when clicking overlay
    if (sidebarOverlay && sidebar) {
        sidebarOverlay.addEventListener('click', function () {
            sidebar.classList.remove('show');
            sidebarOverlay.classList.remove('show');
        });
    }

    // Close sidebar when clicking nav links on mobile
    const navLinks = document.querySelectorAll('.nav-link');
    navLinks.forEach(link => {
        link.addEventListener('click', function () {
            if (window.innerWidth <= 768) {
                sidebar.classList.remove('show');
                if (sidebarOverlay) {
                    sidebarOverlay.classList.remove('show');
                }
            }
        });
    });
}

// Initialize Bootstrap tooltips
function initTooltips() {
    const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    tooltipTriggerList.map(function (tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl);
    });
}

// Common event listeners
function initCommonEvents() {
    // Handle form submissions with loading states
    const forms = document.querySelectorAll('form[data-loading]');
    forms.forEach(form => {
        form.addEventListener('submit', function (e) {
            const submitBtn = form.querySelector('button[type="submit"]');
            if (submitBtn) {
                submitBtn.disabled = true;
                submitBtn.innerHTML = '<span class="spinner"></span> Processing...';
            }
        });
    });

    // Handle punch in/out buttons
    const punchButtons = document.querySelectorAll('.punch-btn');
    punchButtons.forEach(btn => {
        btn.addEventListener('click', handlePunchAction);
    });

    // Handle project timer buttons
    const timerButtons = document.querySelectorAll('.timer-btn');
    timerButtons.forEach(btn => {
        btn.addEventListener('click', handleTimerAction);
    });
}

// Initialize time updates
function initTimeUpdates() {
    // Update current time every second
    updateCurrentTime();
    setInterval(updateCurrentTime, 1000);

    // Update working hours for active employees
    updateWorkingHours();
    setInterval(updateWorkingHours, 60000); // Update every minute
}

// Update current time display
function updateCurrentTime() {
    const timeElements = document.querySelectorAll('.current-time');
    const now = new Date();
    const timeString = now.toLocaleTimeString('en-US', {
        hour12: false,
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
    });

    timeElements.forEach(element => {
        element.textContent = timeString;
    });
}

// Update working hours for employees who are currently working
function updateWorkingHours() {
    const workingHoursElements = document.querySelectorAll('.working-hours[data-punch-in]');

    workingHoursElements.forEach(element => {
        const punchInTime = element.dataset.punchIn;
        if (punchInTime && !element.dataset.punchOut) {
            const workingHours = calculateWorkingHours(punchInTime);
            element.textContent = workingHours;
        }
    });
}

// Calculate working hours from punch in time
function calculateWorkingHours(punchInTime) {
    const now = new Date();
    const punchIn = new Date();
    const [hours, minutes] = punchInTime.split(':');
    punchIn.setHours(parseInt(hours), parseInt(minutes), 0, 0);

    const diffMs = now - punchIn;
    const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
    const diffMinutes = Math.floor((diffMs % (1000 * 60 * 60)) / (1000 * 60));
    const diffSeconds = Math.floor((diffMs % (1000 * 60)) / 1000);

    return `${diffHours.toString().padStart(2, '0')}:${diffMinutes.toString().padStart(2, '0')}:${diffSeconds.toString().padStart(2, '0')}`;
}

// Handle punch in/out actions
async function handlePunchAction(event) {
    event.preventDefault();
    const button = event.target.closest('.punch-btn');
    const action = button.dataset.action;
    const employeeId = button.dataset.employeeId;

    try {
        button.disabled = true;
        button.innerHTML = '<span class="spinner"></span> Processing...';

        let result;
        if (action === 'punch-in') {
            result = await mockAPI.punchIn(employeeId);
        } else {
            result = await mockAPI.punchOut(employeeId);
        }

        if (result.success) {
            showNotification('Success', `Successfully ${action.replace('-', ' ')}ed!`, 'success');
            // Reload page to update UI
            setTimeout(() => window.location.reload(), 1000);
        } else {
            showNotification('Error', result.error, 'error');
        }
    } catch (error) {
        showNotification('Error', 'An error occurred. Please try again.', 'error');
    } finally {
        button.disabled = false;
        button.innerHTML = action === 'punch-in' ?
            '<i class="fas fa-play"></i> Punch In' :
            '<i class="fas fa-stop"></i> Punch Out';
    }
}

// Handle project timer actions
async function handleTimerAction(event) {
    event.preventDefault();
    const button = event.target.closest('.timer-btn');
    const action = button.dataset.action;
    const projectId = button.dataset.projectId;

    try {
        button.disabled = true;
        button.innerHTML = '<span class="spinner"></span> Processing...';

        // Simulate API call
        await mockAPI.delay(1000);

        showNotification('Success', `Project timer ${action}ed!`, 'success');

        // Update button state
        if (action === 'start') {
            button.dataset.action = 'stop';
            button.innerHTML = '<i class="fas fa-stop"></i> Stop Timer';
            button.classList.remove('btn-success');
            button.classList.add('btn-danger');
        } else {
            button.dataset.action = 'start';
            button.innerHTML = '<i class="fas fa-play"></i> Start Timer';
            button.classList.remove('btn-danger');
            button.classList.add('btn-success');
        }
    } catch (error) {
        showNotification('Error', 'An error occurred. Please try again.', 'error');
    } finally {
        button.disabled = false;
    }
}

// Show notification
function showNotification(title, message, type = 'info') {
    // Create notification element
    const notification = document.createElement('div');
    notification.className = `alert alert-${getBootstrapAlertClass(type)} alert-dismissible fade show notification-toast`;
    notification.innerHTML = `
        <strong>${title}:</strong> ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    `;

    // Add to notification container or body
    let container = document.querySelector('.notification-container');
    if (!container) {
        container = document.createElement('div');
        container.className = 'notification-container';
        document.body.appendChild(container);
    }

    container.appendChild(notification);

    // Auto remove after 5 seconds
    setTimeout(() => {
        if (notification.parentNode) {
            notification.remove();
        }
    }, 5000);
}

// Convert notification type to Bootstrap alert class
function getBootstrapAlertClass(type) {
    const typeMap = {
        'success': 'success',
        'error': 'danger',
        'warning': 'warning',
        'info': 'info'
    };
    return typeMap[type] || 'info';
}

// Format date for display
function formatDate(dateString) {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric'
    });
}

// Format time for display
function formatTime(timeString) {
    if (!timeString) return '-';
    const [hours, minutes] = timeString.split(':');
    const date = new Date();
    date.setHours(parseInt(hours), parseInt(minutes));
    return date.toLocaleTimeString('en-US', {
        hour: 'numeric',
        minute: '2-digit',
        hour12: true
    });
}

// Get status badge class
function getStatusBadgeClass(status) {
    const statusMap = {
        'Working': 'badge-success',
        'Completed': 'badge-info',
        'In Progress': 'badge-warning',
        'Planning': 'badge-secondary',
        'Ongoing': 'badge-primary',
        'Pending': 'badge-warning',
        'Approved': 'badge-success',
        'Denied': 'badge-danger'
    };
    return statusMap[status] || 'badge-secondary';
}

// Progress bar color based on percentage
function getProgressBarClass(progress) {
    if (progress >= 80) return 'bg-success';
    if (progress >= 60) return 'bg-info';
    if (progress >= 40) return 'bg-warning';
    return 'bg-danger';
}

// Utility function to debounce function calls
function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

// Theme management
function initTheme() {
    const themeToggle = document.getElementById('themeToggle');
    const themeIcon = document.getElementById('themeIcon');

    if (!themeToggle) return; // Not on a page with theme toggle

    // Load saved theme or default to light
    const savedTheme = localStorage.getItem('theme') || 'light';
    setTheme(savedTheme);

    themeToggle.addEventListener('click', function () {
        const currentTheme = document.documentElement.getAttribute('data-theme') || 'light';
        const newTheme = currentTheme === 'light' ? 'dark' : 'light';
        setTheme(newTheme);
    });
}

function setTheme(theme) {
    const themeIcon = document.getElementById('themeIcon');

    if (theme === 'dark') {
        document.documentElement.setAttribute('data-theme', 'dark');
        if (themeIcon) {
            themeIcon.classList.remove('fa-moon');
            themeIcon.classList.add('fa-sun');
        }
    } else {
        document.documentElement.removeAttribute('data-theme');
        if (themeIcon) {
            themeIcon.classList.remove('fa-sun');
            themeIcon.classList.add('fa-moon');
        }
    }

    localStorage.setItem('theme', theme);
}

// Authentication check for protected pages
function initAuthCheck() {
    // Skip auth check for login page
    if (window.location.pathname === '/login') {
        return;
    }

    // Check if user is on a protected page
    const protectedPaths = ['/attendance/dashboard', '/attendance/projects', '/attendance/leave', '/attendance/team'];
    const currentPath = window.location.pathname;

    if (protectedPaths.some(path => currentPath.startsWith(path))) {
        const token = localStorage.getItem('authToken');
        const username = localStorage.getItem('username');

        if (!token || !username) {
            console.log('No authentication data found, redirecting to login');
            // No token, redirect to login immediately
            window.location.replace('/login?error=Please login to access this page');
            return;
        }

        // Validate token with backend
        validateAuthToken(token);
    }
}

async function validateAuthToken(token) {
    try {
        console.log('Validating token:', token.substring(0, 20) + '...');

        const response = await fetch('/api/web/validate-token', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({token: token})
        });

        console.log('Token validation response status:', response.status);

        if (!response.ok) {
            throw new Error('Token validation request failed');
        }

        const data = await response.json();
        console.log('Token validation response:', data);

        if (!data.valid) {
            console.log('Token is invalid, clearing auth data and redirecting');
            // Token is invalid, clear all auth data and redirect
            localStorage.removeItem('authToken');
            localStorage.removeItem('username');
            sessionStorage.clear();

            // Use replace to prevent back button access
            window.location.replace('/login?error=Session expired. Please login again.');
            return;
        }

        console.log('Token is valid, checking role-based access');
        // Token is valid, check role-based access
        const username = localStorage.getItem('username');
        const currentPath = window.location.pathname;

        console.log('Current user:', username, 'Current path:', currentPath);

// Always redirect to dashboard unless already there
        if (currentPath !== '/attendance/dashboard') {
            console.log('Redirecting to dashboard page only');
            window.location.replace('/attendance/dashboard');
        } else {
            console.log('Already on dashboard — access granted');
        }

    } catch (error) {
        console.error('Token validation error:', error);
        // On error, clear auth data and redirect to login for safety
        localStorage.removeItem('authToken');
        localStorage.removeItem('username');
        sessionStorage.clear();
        window.location.replace('/login?error=Authentication error. Please login again.');
    }
}

// Logout functionality
function handleLogout() {
    if (confirm('Are you sure you want to logout?')) {
        // Clear all authentication data
        localStorage.removeItem('authToken');
        localStorage.removeItem('username');

        // Clear any session storage
        sessionStorage.clear();

        // Show logout message
        showNotification('Success', 'Logged out successfully!', 'success');

        // Redirect to login page
        setTimeout(() => {
            window.location.href = '/login?message=You have been logged out successfully';
        }, 1000);
    }
}

// Check authentication when page becomes visible (prevents tab switching bypass)
function initVisibilityCheck() {
    document.addEventListener('visibilitychange', function () {
        if (!document.hidden) {
            // Page became visible, re-check authentication
            const token = localStorage.getItem('authToken');
            const protectedPaths = ['/attendance/dashboard', '/attendance/projects', '/attendance/leave', '/attendance/team'];
            const currentPath = window.location.pathname;

            if (protectedPaths.some(path => currentPath.startsWith(path))) {
                if (!token) {
                    console.log('Page visible but no auth token, redirecting to login');
                    window.location.replace('/login?error=Session expired. Please login again.');
                } else {
                    // Re-validate token only (no client-side session timeout)
                    validateAuthToken(token);
                }
            }
        }
    });
}

// Client-side session timeout removed - using JWT expiration (24 hours) instead
// Users will only be logged out when JWT token expires or is invalid

// Make logout function globally available
window.handleLogout = handleLogout;

// Export functions for use in other scripts
window.attendanceUtils = {
    formatDate,
    formatTime,
    getStatusBadgeClass,
    getProgressBarClass,
    showNotification,
    debounce,
    setTheme
};