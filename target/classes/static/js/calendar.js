// Calendar JavaScript functionality

// Global variables
let currentDate = new Date();
let currentView = 'month';
let events = [];
let eventIdCounter = 1;

// Initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', function () {
    initializeCalendar();
});

function initializeCalendar() {
    console.log('Initializing Calendar...');

    // Load mock events
    loadMockEvents();

    // Initialize event listeners
    initializeEventListeners();

    // Render current view
    renderCurrentView();

    // Update upcoming events
    updateUpcomingEvents();

    console.log('Calendar initialized successfully');
}

function loadMockEvents() {
    // Mock events data (using current dates)
    const today = new Date();
    const currentMonth = today.getMonth();
    const currentYear = today.getFullYear();

    events = [
        {
            id: 1,
            title: "Team Standup",
            description: "Daily team standup meeting",
            date: `${currentYear}-${String(currentMonth + 1).padStart(2, '0')}-15`,
            startTime: "09:00",
            endTime: "09:30",
            category: "meeting",
            allDay: false,
            createdAt: new Date()
        },
        {
            id: 2,
            title: "Client Presentation",
            description: "Present Q1 project results to client",
            date: `${currentYear}-${String(currentMonth + 1).padStart(2, '0')}-16`,
            startTime: "14:00",
            endTime: "15:30",
            category: "meeting",
            allDay: false,
            createdAt: new Date()
        },
        {
            id: 3,
            title: "Design Review",
            description: "Review new UI designs with the team",
            date: `${currentYear}-${String(currentMonth + 1).padStart(2, '0')}-17`,
            startTime: "10:00",
            endTime: "11:00",
            category: "work",
            allDay: false,
            createdAt: new Date()
        },
        {
            id: 4,
            title: "Project Deadline",
            description: "Final submission for Project Alpha",
            date: `${currentYear}-${String(currentMonth + 1).padStart(2, '0')}-20`,
            startTime: "",
            endTime: "",
            category: "reminder",
            allDay: true,
            createdAt: new Date()
        },
        {
            id: 5,
            title: "Doctor Appointment",
            description: "Annual health checkup",
            date: `${currentYear}-${String(currentMonth + 1).padStart(2, '0')}-18`,
            startTime: "15:30",
            endTime: "16:30",
            category: "personal",
            allDay: false,
            createdAt: new Date()
        },
        {
            id: 6,
            title: "Team Building Event",
            description: "Company team building activities",
            date: `${currentYear}-${String(currentMonth + 1).padStart(2, '0')}-25`,
            startTime: "09:00",
            endTime: "17:00",
            category: "work",
            allDay: false,
            createdAt: new Date()
        }
    ];

    eventIdCounter = Math.max(...events.map(e => e.id)) + 1;
}

function initializeEventListeners() {
    // Navigation buttons
    document.getElementById('prevBtn')?.addEventListener('click', navigatePrevious);
    document.getElementById('nextBtn')?.addEventListener('click', navigateNext);
    document.getElementById('todayBtn')?.addEventListener('click', goToToday);

    // View toggle
    const viewButtons = document.querySelectorAll('input[name="calendarView"]');
    viewButtons.forEach(button => {
        button.addEventListener('change', handleViewChange);
    });

    // Add event button
    document.getElementById('addEventBtn')?.addEventListener('click', showAddEventModal);

    // Event form
    document.getElementById('saveEventBtn')?.addEventListener('click', handleSaveEvent);
    document.getElementById('deleteEventBtn')?.addEventListener('click', handleDeleteEvent);
    document.getElementById('editEventBtn')?.addEventListener('click', handleEditEvent);

    // All day checkbox
    document.getElementById('allDayEvent')?.addEventListener('change', handleAllDayToggle);
}

function navigatePrevious() {
    switch (currentView) {
        case 'month':
            currentDate.setMonth(currentDate.getMonth() - 1);
            break;
        case 'week':
            currentDate.setDate(currentDate.getDate() - 7);
            break;
        case 'day':
            currentDate.setDate(currentDate.getDate() - 1);
            break;
    }
    renderCurrentView();
}

function navigateNext() {
    switch (currentView) {
        case 'month':
            currentDate.setMonth(currentDate.getMonth() + 1);
            break;
        case 'week':
            currentDate.setDate(currentDate.getDate() + 7);
            break;
        case 'day':
            currentDate.setDate(currentDate.getDate() + 1);
            break;
    }
    renderCurrentView();
}

function goToToday() {
    currentDate = new Date();
    renderCurrentView();
}

function handleViewChange(event) {
    currentView = event.target.value;
    renderCurrentView();
}

function renderCurrentView() {
    updateCurrentMonthDisplay();

    // Hide all views
    document.getElementById('monthViewContainer').style.display = 'none';
    document.getElementById('weekViewContainer').style.display = 'none';
    document.getElementById('dayViewContainer').style.display = 'none';

    // Show current view
    switch (currentView) {
        case 'month':
            document.getElementById('monthViewContainer').style.display = 'block';
            renderMonthView();
            break;
        case 'week':
            document.getElementById('weekViewContainer').style.display = 'block';
            renderWeekView();
            break;
        case 'day':
            document.getElementById('dayViewContainer').style.display = 'block';
            renderDayView();
            break;
    }
}

function updateCurrentMonthDisplay() {
    const monthNames = [
        'January', 'February', 'March', 'April', 'May', 'June',
        'July', 'August', 'September', 'October', 'November', 'December'
    ];

    let displayText = '';
    switch (currentView) {
        case 'month':
            displayText = `${monthNames[currentDate.getMonth()]} ${currentDate.getFullYear()}`;
            break;
        case 'week':
            const weekStart = getWeekStart(currentDate);
            const weekEnd = new Date(weekStart);
            weekEnd.setDate(weekEnd.getDate() + 6);
            displayText = `${formatDate(weekStart)} - ${formatDate(weekEnd)}`;
            break;
        case 'day':
            displayText = formatDate(currentDate);
            break;
    }

    document.getElementById('currentMonth').textContent = displayText;
}

function renderMonthView() {
    const calendarDays = document.getElementById('calendarDays');
    if (!calendarDays) return;

    const year = currentDate.getFullYear();
    const month = currentDate.getMonth();

    // Get first day of month and number of days
    const firstDay = new Date(year, month, 1);
    const lastDay = new Date(year, month + 1, 0);
    const daysInMonth = lastDay.getDate();
    const startingDayOfWeek = firstDay.getDay();

    // Get previous month's last days
    const prevMonth = new Date(year, month, 0);
    const daysInPrevMonth = prevMonth.getDate();

    let html = '';
    let dayCount = 1;
    let nextMonthDay = 1;

    // Generate 6 weeks (42 days)
    for (let week = 0; week < 6; week++) {
        for (let day = 0; day < 7; day++) {
            const cellIndex = week * 7 + day;
            let cellDate, cellDay, isCurrentMonth = true, isToday = false;

            if (cellIndex < startingDayOfWeek) {
                // Previous month days
                cellDay = daysInPrevMonth - startingDayOfWeek + cellIndex + 1;
                cellDate = new Date(year, month - 1, cellDay);
                isCurrentMonth = false;
            } else if (dayCount <= daysInMonth) {
                // Current month days
                cellDay = dayCount;
                cellDate = new Date(year, month, cellDay);
                dayCount++;

                // Check if today
                const today = new Date();
                isToday = cellDate.toDateString() === today.toDateString();
            } else {
                // Next month days
                cellDay = nextMonthDay;
                cellDate = new Date(year, month + 1, cellDay);
                nextMonthDay++;
                isCurrentMonth = false;
            }

            // Get events for this day
            const dayEvents = getEventsForDate(cellDate);

            html += `
                <div class="calendar-day ${isCurrentMonth ? 'current-month' : 'other-month'} ${isToday ? 'today' : ''}" 
                     data-date="${formatDateForData(cellDate)}" onclick="handleDayClick('${formatDateForData(cellDate)}')">
                    <div class="day-number">${cellDay}</div>
                    <div class="day-events">
                        ${dayEvents.slice(0, 3).map(event => `
                            <div class="event-dot ${event.category}" title="${event.title}" onclick="showEventDetail(${event.id}); event.stopPropagation();"></div>
                        `).join('')}
                        ${dayEvents.length > 3 ? `<div class="more-events">+${dayEvents.length - 3}</div>` : ''}
                    </div>
                </div>
            `;
        }
    }

    calendarDays.innerHTML = html;
}

function renderWeekView() {
    // Week view implementation
    const weekStart = getWeekStart(currentDate);
    const weekDays = document.getElementById('weekDays');
    const weekEvents = document.getElementById('weekEvents');

    if (!weekDays || !weekEvents) return;

    // Render week days header
    let daysHtml = '';
    for (let i = 0; i < 7; i++) {
        const day = new Date(weekStart);
        day.setDate(day.getDate() + i);
        const isToday = day.toDateString() === new Date().toDateString();

        daysHtml += `
            <div class="week-day ${isToday ? 'today' : ''}" data-date="${formatDateForData(day)}">
                <div class="day-name">${getDayName(day.getDay())}</div>
                <div class="day-number">${day.getDate()}</div>
            </div>
        `;
    }
    weekDays.innerHTML = daysHtml;

    // Render time slots
    renderTimeSlots('timeSlots');

    // Render events
    renderWeekEvents();
}

function renderDayView() {
    const dayViewDate = document.getElementById('dayViewDate');
    if (dayViewDate) {
        dayViewDate.textContent = formatDate(currentDate);
    }

    // Render time slots
    renderTimeSlots('dayTimeSlots');

    // Render day events
    renderDayEvents();
}

function renderTimeSlots(containerId) {
    const container = document.getElementById(containerId);
    if (!container) return;

    let html = '';
    for (let hour = 0; hour < 24; hour++) {
        const timeString = `${hour.toString().padStart(2, '0')}:00`;
        html += `
            <div class="time-slot" data-time="${timeString}">
                <div class="time-label">${formatTime(timeString)}</div>
                <div class="time-content" onclick="handleTimeSlotClick('${timeString}')"></div>
            </div>
        `;
    }
    container.innerHTML = html;
}

function renderWeekEvents() {
    // Implementation for rendering events in week view
    // This would position events based on their time and duration
}

function renderDayEvents() {
    const dayEvents = document.getElementById('dayEvents');
    if (!dayEvents) return;

    const todayEvents = getEventsForDate(currentDate);

    let html = '';
    todayEvents.forEach(event => {
        const startTime = event.allDay ? 'All Day' : formatTime(event.startTime);
        const endTime = event.allDay ? '' : ` - ${formatTime(event.endTime)}`;

        html += `
            <div class="day-event ${event.category}" onclick="showEventDetail(${event.id})">
                <div class="event-time">${startTime}${endTime}</div>
                <div class="event-title">${event.title}</div>
                ${event.description ? `<div class="event-description">${event.description}</div>` : ''}
            </div>
        `;
    });

    dayEvents.innerHTML = html || '<div class="no-events">No events for this day</div>';
}

function getEventsForDate(date) {
    const dateString = formatDateForData(date);
    return events.filter(event => event.date === dateString);
}

function handleDayClick(dateString) {
    showAddEventModal(dateString);
}

function handleTimeSlotClick(timeString) {
    const dateString = formatDateForData(currentDate);
    showAddEventModal(dateString, timeString);
}

function showAddEventModal(dateString = '', timeString = '') {
    const modal = new bootstrap.Modal(document.getElementById('eventModal'));
    const form = document.getElementById('eventForm');

    // Reset form
    form.reset();
    document.getElementById('eventId').value = '';
    document.getElementById('eventModalTitle').textContent = 'Add New Event';
    document.getElementById('deleteEventBtn').style.display = 'none';

    // Set date and time if provided
    if (dateString) {
        document.getElementById('eventDate').value = dateString;
    }
    if (timeString) {
        document.getElementById('eventStartTime').value = timeString;
        // Set end time to 1 hour later
        const endTime = new Date(`2000-01-01 ${timeString}`);
        endTime.setHours(endTime.getHours() + 1);
        document.getElementById('eventEndTime').value = endTime.toTimeString().slice(0, 5);
    }

    modal.show();
}

function showEventDetail(eventId) {
    const event = events.find(e => e.id === eventId);
    if (!event) return;

    const modal = new bootstrap.Modal(document.getElementById('eventDetailModal'));
    const content = document.getElementById('eventDetailContent');

    const timeDisplay = event.allDay ? 'All Day' : `${formatTime(event.startTime)} - ${formatTime(event.endTime)}`;

    content.innerHTML = `
        <div class="event-detail">
            <div class="event-detail-header">
                <h5>${event.title}</h5>
                <span class="badge bg-${getCategoryColor(event.category)}">${event.category}</span>
            </div>
            
            ${event.description ? `
                <div class="event-detail-section">
                    <h6>Description</h6>
                    <p>${event.description}</p>
                </div>
            ` : ''}
            
            <div class="event-detail-section">
                <h6>Details</h6>
                <div class="row">
                    <div class="col-md-6">
                        <strong>Date:</strong> ${formatDate(new Date(event.date))}<br>
                        <strong>Time:</strong> ${timeDisplay}<br>
                        <strong>Category:</strong> ${event.category}
                    </div>
                    <div class="col-md-6">
                        <strong>Created:</strong> ${formatDateTime(event.createdAt)}
                    </div>
                </div>
            </div>
        </div>
    `;

    // Set up edit button
    const editBtn = document.getElementById('editEventBtn');
    editBtn.onclick = () => {
        modal.hide();
        editEvent(eventId);
    };

    modal.show();
}

function editEvent(eventId) {
    const event = events.find(e => e.id === eventId);
    if (!event) return;

    const modal = new bootstrap.Modal(document.getElementById('eventModal'));
    const form = document.getElementById('eventForm');

    // Populate form with event data
    document.getElementById('eventId').value = event.id;
    document.getElementById('eventTitle').value = event.title;
    document.getElementById('eventDescription').value = event.description;
    document.getElementById('eventDate').value = event.date;
    document.getElementById('eventStartTime').value = event.startTime;
    document.getElementById('eventEndTime').value = event.endTime;
    document.getElementById('eventCategory').value = event.category;
    document.getElementById('allDayEvent').checked = event.allDay;

    document.getElementById('eventModalTitle').textContent = 'Edit Event';
    document.getElementById('deleteEventBtn').style.display = 'inline-block';

    handleAllDayToggle(); // Update time fields visibility

    modal.show();
}

function handleSaveEvent() {
    const eventId = document.getElementById('eventId').value;
    const title = document.getElementById('eventTitle').value.trim();
    const description = document.getElementById('eventDescription').value.trim();
    const date = document.getElementById('eventDate').value;
    const startTime = document.getElementById('eventStartTime').value;
    const endTime = document.getElementById('eventEndTime').value;
    const category = document.getElementById('eventCategory').value;
    const allDay = document.getElementById('allDayEvent').checked;

    if (!title || !date) {
        showNotification('Error', 'Title and date are required', 'error');
        return;
    }

    if (!allDay && (!startTime || !endTime)) {
        showNotification('Error', 'Start and end times are required for non-all-day events', 'error');
        return;
    }

    const eventData = {
        title,
        description,
        date,
        startTime: allDay ? '' : startTime,
        endTime: allDay ? '' : endTime,
        category,
        allDay
    };

    if (eventId) {
        // Update existing event
        const eventIndex = events.findIndex(e => e.id === parseInt(eventId));
        if (eventIndex !== -1) {
            events[eventIndex] = {...events[eventIndex], ...eventData};
            showNotification('Success', 'Event updated successfully!', 'success');
        }
    } else {
        // Create new event
        const newEvent = {
            id: eventIdCounter++,
            ...eventData,
            createdAt: new Date()
        };
        events.push(newEvent);
        showNotification('Success', 'Event created successfully!', 'success');
    }

    // Close modal and refresh view
    bootstrap.Modal.getInstance(document.getElementById('eventModal')).hide();
    renderCurrentView();
    updateUpcomingEvents();
}

function handleDeleteEvent() {
    const eventId = document.getElementById('eventId').value;
    if (!eventId) return;

    if (confirm('Are you sure you want to delete this event?')) {
        events = events.filter(e => e.id !== parseInt(eventId));

        bootstrap.Modal.getInstance(document.getElementById('eventModal')).hide();
        renderCurrentView();
        updateUpcomingEvents();

        showNotification('Success', 'Event deleted successfully!', 'success');
    }
}

function handleEditEvent() {
    // This is handled by the event detail modal
}

function handleAllDayToggle() {
    const allDayChecked = document.getElementById('allDayEvent').checked;
    const startTimeField = document.getElementById('eventStartTime');
    const endTimeField = document.getElementById('eventEndTime');

    startTimeField.disabled = allDayChecked;
    endTimeField.disabled = allDayChecked;

    if (allDayChecked) {
        startTimeField.value = '';
        endTimeField.value = '';
    }
}

function updateUpcomingEvents() {
    const upcomingEvents = document.getElementById('upcomingEvents');
    if (!upcomingEvents) return;

    // Get events for the next 7 days
    const today = new Date();
    const nextWeek = new Date();
    nextWeek.setDate(today.getDate() + 7);

    const upcoming = events
        .filter(event => {
            const eventDate = new Date(event.date);
            return eventDate >= today && eventDate <= nextWeek;
        })
        .sort((a, b) => new Date(a.date) - new Date(b.date))
        .slice(0, 5);

    if (upcoming.length === 0) {
        upcomingEvents.innerHTML = '<p class="text-muted">No upcoming events</p>';
        return;
    }

    const html = upcoming.map(event => {
        const timeDisplay = event.allDay ? 'All Day' : `${formatTime(event.startTime)} - ${formatTime(event.endTime)}`;

        return `
            <div class="upcoming-event" onclick="showEventDetail(${event.id})">
                <div class="event-date">
                    <div class="date-day">${new Date(event.date).getDate()}</div>
                    <div class="date-month">${getMonthName(new Date(event.date).getMonth()).slice(0, 3)}</div>
                </div>
                <div class="event-info">
                    <h6 class="event-title">${event.title}</h6>
                    <p class="event-time">${timeDisplay}</p>
                    <span class="badge bg-${getCategoryColor(event.category)}">${event.category}</span>
                </div>
            </div>
        `;
    }).join('');

    upcomingEvents.innerHTML = html;
}

// Utility functions
function getWeekStart(date) {
    const d = new Date(date);
    const day = d.getDay();
    const diff = d.getDate() - day;
    return new Date(d.setDate(diff));
}

function getDayName(dayIndex) {
    const days = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
    return days[dayIndex];
}

function getMonthName(monthIndex) {
    const months = [
        'January', 'February', 'March', 'April', 'May', 'June',
        'July', 'August', 'September', 'October', 'November', 'December'
    ];
    return months[monthIndex];
}

function formatDate(date) {
    return date.toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'long',
        day: 'numeric'
    });
}

function formatDateForData(date) {
    return date.toISOString().split('T')[0];
}

function formatTime(timeString) {
    if (!timeString) return '';
    const [hours, minutes] = timeString.split(':');
    const date = new Date();
    date.setHours(parseInt(hours), parseInt(minutes));
    return date.toLocaleTimeString('en-US', {
        hour: 'numeric',
        minute: '2-digit',
        hour12: true
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

function getCategoryColor(category) {
    const colors = {
        'meeting': 'primary',
        'appointment': 'info',
        'reminder': 'warning',
        'personal': 'success',
        'work': 'secondary'
    };
    return colors[category] || 'secondary';
}

// Make functions globally available
window.showEventDetail = showEventDetail;
window.handleDayClick = handleDayClick;
window.handleTimeSlotClick = handleTimeSlotClick;