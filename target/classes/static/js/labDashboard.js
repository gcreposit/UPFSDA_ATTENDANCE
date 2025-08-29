console.log("Lab Dashboard JS loaded", new Date().toLocaleTimeString());
console.log(allRtspUrlData);


if (allRtspUrlData.length > 0) {
    // Dynamically set the values for total labs monitored and active
    const labsCount = allRtspUrlData.length;

    document.getElementById("total-labs-monitored").innerHTML = `Total Labs Monitored:${labsCount > 0 ? labsCount : 0}`;
    document.getElementById("total-labs-active").innerHTML = `Total Labs Active: ${labsCount > 0 ? labsCount : 0}`;
    // Generate the cards dynamically based on the JSON data
    generateLabCards(allRtspUrlData);
    addAlertCard("Unauthorized person detected", "14:45", "danger");
    addAlertCard("Authorized person entered", "14:50", "success");

}



if (typeof SockJS !== 'undefined') {
    connect();
} else {
    console.error("SockJS not loaded.");
}
function connect() {
    const socket = new SockJS('/ws-alerts');
    stompClient = Stomp.over(socket);

    stompClient.connect({}, function (frame) {
        console.log('Connected: ' + frame);

        stompClient.subscribe('/topic/alerts', function (message) {
            const alert = JSON.parse(message.body);
            console.log('Alert received:', alert);
            // handle alert
            showAlert(alert)
        });
    });
}

function showAlert(alert) {
    // Get current time if not provided
    const currentTime = alert.time || new Date().toLocaleTimeString();

    // Determine alert type based on message content
    let alertType = determineAlertType(alert.message);

    // Use the updated styled addAlertCard function
    addAlertCard(alert.message, currentTime, alertType);
}

function determineAlertType(message) {
    const lowerMessage = message.toLowerCase();

    // Check for success/positive messages
    if (lowerMessage.includes('authorized') ||
        lowerMessage.includes('normal') ||
        lowerMessage.includes('assigned person present') ||
        lowerMessage.includes('all clear')) {
        return 'success';
    }

    // Check for warning messages
    if (lowerMessage.includes('warning') ||
        lowerMessage.includes('caution')) {
        return 'warning';
    }

    // Check for info messages
    if (lowerMessage.includes('new person detected') &&
        !lowerMessage.includes('overcrowd') &&
        !lowerMessage.includes('unauthorized')) {
        return 'info';
    }

    // Default to danger for critical alerts
    // (person missing, overcrowd, unauthorized, etc.)
    return 'danger';
}

function addAlertCard(message, time, type = 'danger') {
    const container = document.getElementById('alert-container');

    if (!container) {
        console.error('Alert container not found!');
        return;
    }

    const alertStyles = getAlertStyles(type);

    const card = document.createElement('div');
    card.className = 'col-md-12 alert-item';
    card.innerHTML = `
    <div class="d-flex align-items-center justify-content-between mb-2 p-2 rounded"
         style="background: ${alertStyles.background}; border-left: 5px solid ${alertStyles.border}; box-shadow: 0 1px 4px rgba(0,0,0,0.05); min-height: 48px;">

        <!-- Left: Icon + Time -->
        <div class="d-flex align-items-center" style="min-width: 110px;">
            <i class="bi bi-${alertStyles.icon} ${alertStyles.textClass} me-2 fs-5"></i>
            <div class="small fw-semibold text-muted">${time}</div>
        </div>

        <!-- Center: Full Message (unchanged) -->
        <div class="flex-grow-1 px-3">
            <div class="fw-semibold ${alertStyles.textClass}" style="white-space: nowrap;">
                ${message}
            </div>
        </div>

        <!-- Right: Dismiss button -->
        <div class="d-flex align-items-center justify-content-end" style="min-width: 40px;">
            <button class="btn btn-sm btn-outline-secondary border-0" onclick="removeAlert(this)">
                <i class="bi bi-x"></i>
            </button>
        </div>
    </div>
`;

    // Smooth entry animation
    card.style.opacity = '0';
    card.style.transform = 'translateY(-10px)';
    container.prepend(card);

    setTimeout(() => {
        card.style.transition = 'all 0.3s ease';
        card.style.opacity = '1';
        card.style.transform = 'translateY(0)';
    }, 10);
}


function getAlertStyles(type) {
    switch(type) {
        case 'danger':
            return {
                background: '#ffe8e8',
                border: '#dc3545',
                icon: 'exclamation-triangle-fill text-danger',
                textClass: 'text-danger'
            };
        case 'success':
            return {
                background: '#e8f7e8',
                border: '#198754',
                icon: 'check-circle-fill text-success',
                textClass: 'text-success'
            };
        case 'warning':
            return {
                background: '#fff3cd',
                border: '#ffc107',
                icon: 'exclamation-circle-fill text-warning',
                textClass: 'text-warning'
            };
        case 'info':
            return {
                background: '#d1ecf1',
                border: '#0dcaf0',
                icon: 'info-circle-fill text-info',
                textClass: 'text-info'
            };
        default:
            return {
                background: '#ffe8e8',
                border: '#dc3545',
                icon: 'exclamation-triangle-fill text-danger',
                textClass: 'text-danger'
            };
    }
}

function removeAlert(button) {
    const alertCard = button.closest('.alert-item');
    if (alertCard) {
        alertCard.style.transition = 'all 0.3s ease';
        alertCard.style.opacity = '0';
        alertCard.style.transform = 'translateY(-10px)';

        setTimeout(() => {
            if (alertCard.parentNode) {
                alertCard.remove();
            }
        }, 300);
    }
}



    // Function to generate the cards dynamically
    function generateLabCards(data) {
    const container = document.getElementById('lab-cards-container');

    data.forEach(lab => {
    // Create a card element for each lab
    const card = document.createElement('div');
    card.classList.add('col-md-4');  // Three cards per row

    // Card HTML structure
    card.innerHTML = `
            <div class="card p-3 ${getCardClass(lab.crowdThreshold)}">
                <div class="d-flex align-items-center mb-2">
                    <span class="me-2">
                        <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" fill="${getSvgColor(lab.crowdThreshold)}"
                             class="bi bi-shield-fill-check" viewBox="0 0 16 16">
                            <path d="M8 0c-.69 0-1.37.07-2 .21C3.07.58 1.5 2.2 1.5 4.09c0 5.52 4.5 9.91 6.5 11.91 2-2 6.5-6.39 6.5-11.91 0-1.89-1.57-3.51-4.5-3.88A8.12 8.12 0 0 0 8 0zm3.354 5.354a.5.5 0 0 1 0 .708l-3 3a.5.5 0 0 1-.708 0l-1.5-1.5a.5.5 0 1 1 .708-.708L8 7.293l2.646-2.647a.5.5 0 0 1 .708 0z"/>
                        </svg>
                    </span>
                    <div>
                        <div class="fw-semibold">${lab.labName}</div>
                        <div class="text-muted small">District Laboratory</div>
                    </div>
                    <div class="ms-auto">
                        <span class="lab-status-dot ${getDotClass(lab.crowdThreshold)}"></span>
                        <span class="${getTextClass(lab.crowdThreshold)} fw-semibold">${getStatusText(lab.crowdThreshold)}</span>
                    </div>
                </div>
                <div class="mb-2 text-muted small">Total Assigned Person <span class="fw-bold">${lab.totalAssignedPerson}</span></div>
                <div class="mb-3 text-muted small">Total Crowed Allowed <span class="fw-bold">${lab.crowdThreshold}</span></div>
                <a href="/upfsdaMonitoring/labProfilePage?labName=${lab.labName}&id=${lab.id}" class="btn btn-green w-100">View Details</a>
            </div>
        `;
    container.appendChild(card);
});
}

    // Helper function to determine card class (for color)
    function getCardClass(crowdThreshold) {
    if (crowdThreshold == 0) {
    return 'card-warning';  // For Warning status
} else if (crowdThreshold < 0) {
    return 'card-critical';  // For Critical status
} else {
    return 'card-active';  // For Active status
}
}

    // Helper function to determine SVG color
    function getSvgColor(crowdThreshold) {
    if (crowdThreshold == 0) {
    return '#ffd600';  // Yellow for Warning
} else if (crowdThreshold < 0) {
    return '#ff4d4f';  // Red for Critical
} else {
    return '#27c46b';  // Green for Active
}
}

    // Helper function to determine dot class
    function getDotClass(crowdThreshold) {
    if (crowdThreshold == 0) {
    return 'dot-yellow';  // Yellow for Warning
} else if (crowdThreshold < 0) {
    return 'dot-red';  // Red for Critical
} else {
    return 'dot-green';  // Green for Active
}
}

    // Helper function to determine text class
    function getTextClass(crowdThreshold) {
    if (crowdThreshold == 0) {
    return 'text-warning';  // Warning Text
} else if (crowdThreshold < 0) {
    return 'text-danger';  // Critical Text
} else {
    return 'text-success';  // Active Text
}
}

    // Helper function to determine status text
    function getStatusText(crowdThreshold) {
    if (crowdThreshold == 0) {
    return 'Warning';  // Warning Text
} else if (crowdThreshold < 0) {
    return 'Critical';  // Critical Text
} else {
    return 'Active';  // Active Text
}
}


