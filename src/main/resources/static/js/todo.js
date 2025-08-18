// To-Do List JavaScript functionality

// Global variables
let tasks = [];
let taskIdCounter = 1;
let currentFilter = 'all';

// Initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', function () {
    initializeTodoApp();
});

function initializeTodoApp() {
    console.log('Initializing To-Do App...');

    // Load mock tasks
    loadMockTasks();

    // Initialize event listeners
    initializeEventListeners();

    // Render tasks
    renderTasks();

    // Update statistics
    updateTaskStatistics();

    console.log('To-Do App initialized successfully');
}

function loadMockTasks() {
    // Mock tasks data
    tasks = [
        {
            id: 1,
            title: "Complete project proposal",
            description: "Finish the Q1 project proposal for the new client",
            priority: "high",
            category: "work",
            dueDate: "2024-01-15",
            completed: false,
            createdAt: new Date('2024-01-10T09:00:00'),
            completedAt: null
        },
        {
            id: 2,
            title: "Team meeting preparation",
            description: "Prepare slides and agenda for tomorrow's team meeting",
            priority: "medium",
            category: "meeting",
            dueDate: "2024-01-12",
            completed: false,
            createdAt: new Date('2024-01-11T14:30:00'),
            completedAt: null
        },
        {
            id: 3,
            title: "Review design mockups",
            description: "Review and provide feedback on the new UI mockups",
            priority: "medium",
            category: "work",
            dueDate: "2024-01-13",
            completed: true,
            createdAt: new Date('2024-01-09T11:15:00'),
            completedAt: new Date('2024-01-11T16:45:00')
        },
        {
            id: 4,
            title: "Update portfolio website",
            description: "Add recent projects to personal portfolio",
            priority: "low",
            category: "personal",
            dueDate: "2024-01-20",
            completed: false,
            createdAt: new Date('2024-01-08T10:00:00'),
            completedAt: null
        },
        {
            id: 5,
            title: "Client presentation",
            description: "Present the final design to the client",
            priority: "high",
            category: "urgent",
            dueDate: "2024-01-10",
            completed: false,
            createdAt: new Date('2024-01-05T09:30:00'),
            completedAt: null
        }
    ];

    taskIdCounter = Math.max(...tasks.map(t => t.id)) + 1;
}

function initializeEventListeners() {
    // Add task form
    const addTaskForm = document.getElementById('addTaskForm');
    if (addTaskForm) {
        addTaskForm.addEventListener('submit', handleAddTask);
    }

    // Task filters
    const filterButtons = document.querySelectorAll('input[name="taskFilter"]');
    filterButtons.forEach(button => {
        button.addEventListener('change', handleFilterChange);
    });

    // Search functionality
    const searchInput = document.getElementById('taskSearch');
    if (searchInput) {
        searchInput.addEventListener('input', debounce(handleSearch, 300));
    }

    // Delete task button in modal
    const deleteTaskBtn = document.getElementById('deleteTaskBtn');
    if (deleteTaskBtn) {
        deleteTaskBtn.addEventListener('click', handleDeleteTask);
    }
}

function handleAddTask(event) {
    event.preventDefault();

    const title = document.getElementById('taskTitle').value.trim();
    const description = document.getElementById('taskDescription').value.trim();
    const priority = document.getElementById('taskPriority').value;
    const category = document.getElementById('taskCategory').value;
    const dueDate = document.getElementById('taskDueDate').value;

    if (!title) {
        showNotification('Error', 'Task title is required', 'error');
        return;
    }

    const newTask = {
        id: taskIdCounter++,
        title: title,
        description: description,
        priority: priority,
        category: category,
        dueDate: dueDate,
        completed: false,
        createdAt: new Date(),
        completedAt: null
    };

    tasks.unshift(newTask); // Add to beginning of array

    // Clear form
    document.getElementById('addTaskForm').reset();

    // Re-render tasks
    renderTasks();
    updateTaskStatistics();

    showNotification('Success', 'Task added successfully!', 'success');
}

function handleFilterChange(event) {
    currentFilter = event.target.value;
    renderTasks();
}

function handleSearch(event) {
    const searchTerm = event.target.value.toLowerCase();
    renderTasks(searchTerm);
}

function renderTasks(searchTerm = '') {
    const tasksList = document.getElementById('tasksList');
    const noTasksMessage = document.getElementById('noTasksMessage');

    if (!tasksList) return;

    // Filter tasks based on current filter and search term
    let filteredTasks = tasks.filter(task => {
        // Apply filter
        let matchesFilter = true;
        switch (currentFilter) {
            case 'pending':
                matchesFilter = !task.completed;
                break;
            case 'completed':
                matchesFilter = task.completed;
                break;
            case 'overdue':
                matchesFilter = !task.completed && task.dueDate && new Date(task.dueDate) < new Date();
                break;
            case 'all':
            default:
                matchesFilter = true;
        }

        // Apply search
        let matchesSearch = true;
        if (searchTerm) {
            matchesSearch = task.title.toLowerCase().includes(searchTerm) ||
                task.description.toLowerCase().includes(searchTerm) ||
                task.category.toLowerCase().includes(searchTerm);
        }

        return matchesFilter && matchesSearch;
    });

    // Show/hide no tasks message
    if (filteredTasks.length === 0) {
        tasksList.style.display = 'none';
        noTasksMessage.style.display = 'block';
    } else {
        tasksList.style.display = 'block';
        noTasksMessage.style.display = 'none';
    }

    // Render tasks
    tasksList.innerHTML = filteredTasks.map(task => createTaskHTML(task)).join('');

    // Add event listeners to task items
    addTaskEventListeners();
}

function createTaskHTML(task) {
    const isOverdue = !task.completed && task.dueDate && new Date(task.dueDate) < new Date();
    const priorityClass = `priority-${task.priority}`;
    const statusClass = task.completed ? 'completed' : (isOverdue ? 'overdue' : 'pending');

    return `
        <div class="task-item ${statusClass}" data-task-id="${task.id}">
            <div class="task-checkbox">
                <input type="checkbox" ${task.completed ? 'checked' : ''} 
                       onchange="toggleTaskComplete(${task.id})" 
                       class="form-check-input">
            </div>
            <div class="task-content" onclick="showTaskDetail(${task.id})">
                <div class="task-header">
                    <h6 class="task-title ${task.completed ? 'text-decoration-line-through' : ''}">${task.title}</h6>
                    <div class="task-badges">
                        <span class="badge bg-${getPriorityColor(task.priority)} priority-badge">${task.priority}</span>
                        <span class="badge bg-secondary category-badge">${task.category}</span>
                    </div>
                </div>
                ${task.description ? `<p class="task-description">${task.description}</p>` : ''}
                <div class="task-meta">
                    <div class="task-dates">
                        ${task.dueDate ? `<span class="due-date ${isOverdue ? 'text-danger' : ''}">
                            <i class="fas fa-calendar me-1"></i>Due: ${formatDate(task.dueDate)}
                        </span>` : ''}
                        <span class="created-date">
                            <i class="fas fa-clock me-1"></i>Created: ${formatDateTime(task.createdAt)}
                        </span>
                    </div>
                    <div class="task-actions">
                        <button class="btn btn-sm btn-outline-primary" onclick="editTask(${task.id}); event.stopPropagation();">
                            <i class="fas fa-edit"></i>
                        </button>
                        <button class="btn btn-sm btn-outline-danger" onclick="deleteTask(${task.id}); event.stopPropagation();">
                            <i class="fas fa-trash"></i>
                        </button>
                    </div>
                </div>
            </div>
        </div>
    `;
}

function addTaskEventListeners() {
    // Event listeners are added inline in the HTML for simplicity
    // In a production app, you might want to use event delegation
}

function toggleTaskComplete(taskId) {
    const task = tasks.find(t => t.id === taskId);
    if (task) {
        task.completed = !task.completed;
        task.completedAt = task.completed ? new Date() : null;

        renderTasks();
        updateTaskStatistics();

        const message = task.completed ? 'Task marked as completed!' : 'Task marked as pending!';
        showNotification('Success', message, 'success');
    }
}

function showTaskDetail(taskId) {
    const task = tasks.find(t => t.id === taskId);
    if (!task) return;

    const modal = new bootstrap.Modal(document.getElementById('taskDetailModal'));
    const content = document.getElementById('taskDetailContent');

    const isOverdue = !task.completed && task.dueDate && new Date(task.dueDate) < new Date();

    content.innerHTML = `
        <div class="task-detail">
            <div class="task-detail-header">
                <h5>${task.title}</h5>
                <div class="task-detail-badges">
                    <span class="badge bg-${getPriorityColor(task.priority)}">${task.priority} Priority</span>
                    <span class="badge bg-secondary">${task.category}</span>
                    <span class="badge bg-${task.completed ? 'success' : (isOverdue ? 'danger' : 'warning')}">
                        ${task.completed ? 'Completed' : (isOverdue ? 'Overdue' : 'Pending')}
                    </span>
                </div>
            </div>
            
            ${task.description ? `
                <div class="task-detail-section">
                    <h6>Description</h6>
                    <p>${task.description}</p>
                </div>
            ` : ''}
            
            <div class="task-detail-section">
                <h6>Details</h6>
                <div class="row">
                    <div class="col-md-6">
                        <strong>Priority:</strong> ${task.priority}<br>
                        <strong>Category:</strong> ${task.category}<br>
                        ${task.dueDate ? `<strong>Due Date:</strong> ${formatDate(task.dueDate)}<br>` : ''}
                    </div>
                    <div class="col-md-6">
                        <strong>Created:</strong> ${formatDateTime(task.createdAt)}<br>
                        ${task.completedAt ? `<strong>Completed:</strong> ${formatDateTime(task.completedAt)}<br>` : ''}
                        <strong>Status:</strong> ${task.completed ? 'Completed' : 'Pending'}
                    </div>
                </div>
            </div>
        </div>
    `;

    // Set up delete button
    const deleteBtn = document.getElementById('deleteTaskBtn');
    deleteBtn.onclick = () => {
        deleteTask(taskId);
        modal.hide();
    };

    modal.show();
}

function editTask(taskId) {
    // For now, just show the task detail
    // In a full implementation, you'd open an edit form
    showTaskDetail(taskId);
}

function deleteTask(taskId) {
    if (confirm('Are you sure you want to delete this task?')) {
        tasks = tasks.filter(t => t.id !== taskId);
        renderTasks();
        updateTaskStatistics();
        showNotification('Success', 'Task deleted successfully!', 'success');
    }
}

function handleDeleteTask() {
    // This would be called from the modal
    // Implementation depends on how you track the current task
}

function updateTaskStatistics() {
    const total = tasks.length;
    const completed = tasks.filter(t => t.completed).length;
    const pending = tasks.filter(t => !t.completed).length;
    const overdue = tasks.filter(t => !t.completed && t.dueDate && new Date(t.dueDate) < new Date()).length;

    // Update stat cards
    const totalElement = document.getElementById('totalTasks');
    const pendingElement = document.getElementById('pendingTasks');
    const completedElement = document.getElementById('completedTasks');
    const overdueElement = document.getElementById('overdueTasks');

    if (totalElement) totalElement.textContent = total;
    if (pendingElement) pendingElement.textContent = pending;
    if (completedElement) completedElement.textContent = completed;
    if (overdueElement) overdueElement.textContent = overdue;
}

// Utility functions
function getPriorityColor(priority) {
    switch (priority) {
        case 'high':
            return 'danger';
        case 'medium':
            return 'warning';
        case 'low':
            return 'info';
        default:
            return 'secondary';
    }
}

function formatDate(dateString) {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric'
    });
}

function formatDateTime(date) {
    return date.toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}

// Debounce function for search
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

// Make functions globally available
window.toggleTaskComplete = toggleTaskComplete;
window.showTaskDetail = showTaskDetail;
window.editTask = editTask;
window.deleteTask = deleteTask;