// Location Tracking JavaScript functionality

// Global variables
let map;
let userMarkers = {};
let userPaths = {};
let currentUser = null;
let isTracking = false;
let trackingSession = null;
let watchId = null;
let mockUsers = [];
let locationHistory = [];

// Initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', function () {
    // Wait for Google Maps to load
    if (typeof google !== 'undefined') {
        initializeLocationTracking();
    } else {
        // Google Maps not loaded yet, wait for callback
        window.initMap = initializeLocationTracking;
    }
});

function initializeLocationTracking() {
    console.log('Initializing Location Tracking...');

    // Get current user info
    currentUser = localStorage.getItem('username') || 'user';

    // Initialize map
    initializeMap();

    // Load mock data
    loadMockData();

    // Initialize event listeners
    initializeEventListeners();

    // Set up UI based on user role
    setupUserInterface();

    // Start mock location updates
    startMockLocationUpdates();

    console.log('Location Tracking initialized successfully');
}

function initializeMap() {
    // Default location (Lucknow, India)
    const defaultLocation = { lat: 26.8467, lng: 80.9462 };

    map = new google.maps.Map(document.getElementById('map'), {
        zoom: 12,
        center: defaultLocation,
        mapTypeId: google.maps.MapTypeId.ROADMAP,
        styles: [
            {
                featureType: 'poi',
                elementType: 'labels',
                stylers: [{ visibility: 'off' }]
            }
        ]
    });

    // Add a marker for Lucknow
    const lucknowMarker = new google.maps.Marker({
        position: defaultLocation,
        map: map,
        title: 'Lucknow, India',
        icon: {
            url: 'data:image/svg+xml;charset=UTF-8,' + encodeURIComponent(`
                <svg width="32" height="32" viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <circle cx="16" cy="16" r="12" fill="#ff6b35" stroke="#fff" stroke-width="3"/>
                    <circle cx="16" cy="16" r="6" fill="#fff"/>
                </svg>
            `),
            scaledSize: new google.maps.Size(32, 32)
        }
    });

    // Add info window for Lucknow marker
    const infoWindow = new google.maps.InfoWindow({
        content: `
            <div class="marker-info">
                <h6>Lucknow, India</h6>
                <p><strong>Default Location</strong></p>
                <p>Latitude: 26.8467</p>
                <p>Longitude: 80.9462</p>
            </div>
        `
    });

    lucknowMarker.addListener('click', () => {
        infoWindow.open(map, lucknowMarker);
    });
}

function loadMockData() {
    // Mock users data (Located around Lucknow, India)
    mockUsers = [
        {
            id: 'user1',
            name: 'Rahul Sharma',
            role: 'Developer',
            isActive: true,
            lastSeen: new Date(),
            currentLocation: { lat: 26.8505, lng: 80.9490 }, // Near Hazratganj
            sessionStart: new Date(Date.now() - 2 * 60 * 60 * 1000), // 2 hours ago
            distanceTraveled: 5.2
        },
        {
            id: 'user2',
            name: 'Priya Singh',
            role: 'Designer',
            isActive: false,
            lastSeen: new Date(Date.now() - 30 * 60 * 1000), // 30 minutes ago
            currentLocation: { lat: 26.8420, lng: 80.9520 }, // Near Gomti Nagar
            sessionStart: null,
            distanceTraveled: 0
        },
        {
            id: 'user3',
            name: 'Amit Verma',
            role: 'Manager',
            isActive: true,
            lastSeen: new Date(),
            currentLocation: { lat: 26.8380, lng: 80.9380 }, // Near Indira Nagar
            sessionStart: new Date(Date.now() - 4 * 60 * 60 * 1000), // 4 hours ago
            distanceTraveled: 8.7
        },
        {
            id: 'user',
            name: 'Lokesh Kumar',
            role: 'UI/UX Designer',
            isActive: false,
            lastSeen: new Date(),
            currentLocation: { lat: 26.8467, lng: 80.9462 }, // Central Lucknow
            sessionStart: null,
            distanceTraveled: 0
        }
    ];

    // Mock location history
    locationHistory = generateMockLocationHistory();
}

function generateMockLocationHistory() {
    const history = [];
    const baseLocation = { lat: 26.8467, lng: 80.9462 }; // Lucknow, India
    const now = new Date();

    // Generate 24 hours of location data
    for (let i = 0; i < 24; i++) {
        const timestamp = new Date(now.getTime() - (23 - i) * 60 * 60 * 1000);

        // Create some movement pattern around Lucknow
        const latOffset = (Math.random() - 0.5) * 0.02;
        const lngOffset = (Math.random() - 0.5) * 0.02;

        history.push({
            timestamp: timestamp,
            location: {
                lat: baseLocation.lat + latOffset,
                lng: baseLocation.lng + lngOffset
            },
            isActive: Math.random() > 0.3, // 70% chance of being active
            sessionId: Math.floor(i / 3) // Group into sessions
        });
    }

    return history;
}

function initializeEventListeners() {
    // Tracking toggle
    const trackingToggle = document.getElementById('trackingToggle');
    if (trackingToggle) {
        trackingToggle.addEventListener('change', handleTrackingToggle);
    }

    // Admin controls
    const userSelect = document.getElementById('userSelect');
    if (userSelect) {
        userSelect.addEventListener('change', handleUserSelection);
    }

    const viewUserHistoryBtn = document.getElementById('viewUserHistoryBtn');
    if (viewUserHistoryBtn) {
        viewUserHistoryBtn.addEventListener('click', viewSelectedUserHistory);
    }

    // Map view toggle
    const mapViewButtons = document.querySelectorAll('input[name="mapView"]');
    mapViewButtons.forEach(button => {
        button.addEventListener('change', handleMapViewChange);
    });

    // Map controls
    document.getElementById('centerMapBtn')?.addEventListener('click', centerMap);
    document.getElementById('refreshMapBtn')?.addEventListener('click', refreshMap);

    // History filter
    document.getElementById('historyTimeFilter')?.addEventListener('change', updateLocationHistory);
}

function setupUserInterface() {
    const isAdmin = currentUser === 'MasterAdmin';

    if (isAdmin) {
        // Show admin controls
        document.getElementById('adminControls').style.display = 'block';
        document.getElementById('userStatusCards').style.display = 'flex';
        document.getElementById('liveUsersPanel').style.display = 'block';

        // Hide user controls
        document.getElementById('userControls').style.display = 'none';
        document.getElementById('personalTrackingPanel').style.display = 'none';

        // Populate user select
        populateUserSelect();

        // Update admin statistics
        updateAdminStatistics();

        // Render live users
        renderLiveUsers();
    } else {
        // Show user controls
        document.getElementById('userControls').style.display = 'block';
        document.getElementById('personalTrackingPanel').style.display = 'block';

        // Hide admin controls
        document.getElementById('adminControls').style.display = 'none';
        document.getElementById('userStatusCards').style.display = 'none';
        document.getElementById('liveUsersPanel').style.display = 'none';

        // Update personal tracking info
        updatePersonalTrackingInfo();
    }

    // Update location history
    updateLocationHistory();
}

function populateUserSelect() {
    const userSelect = document.getElementById('userSelect');
    if (!userSelect) return;

    userSelect.innerHTML = '<option value="">Select a user to view history</option>';

    mockUsers.forEach(user => {
        if (user.id !== 'MasterAdmin') {
            const option = document.createElement('option');
            option.value = user.id;
            option.textContent = `${user.name} (${user.role})`;
            userSelect.appendChild(option);
        }
    });
}

function handleTrackingToggle(event) {
    const isEnabled = event.target.checked;
    const statusElement = document.getElementById('trackingStatus');

    if (isEnabled) {
        startTracking();
        statusElement.textContent = 'Go Offline';
    } else {
        stopTracking();
        statusElement.textContent = 'Go Live';
    }
}

function startTracking() {
    if (isTracking) return;

    isTracking = true;
    trackingSession = {
        id: Date.now(),
        startTime: new Date(),
        locations: []
    };

    // Update user status
    const user = mockUsers.find(u => u.id === currentUser);
    if (user) {
        user.isActive = true;
        user.sessionStart = new Date();
    }

    // Start location watching
    if (navigator.geolocation) {
        watchId = navigator.geolocation.watchPosition(
            handleLocationUpdate,
            handleLocationError,
            {
                enableHighAccuracy: true,
                timeout: 10000,
                maximumAge: 60000
            }
        );
    }

    // Update UI
    updatePersonalTrackingInfo();
    document.getElementById('myTrackingStatus').textContent = 'Active';
    document.getElementById('myTrackingStatus').className = 'badge bg-success';

    showNotification('Success', 'Location tracking started', 'success');
}

function stopTracking() {
    if (!isTracking) return;

    isTracking = false;

    // Stop location watching
    if (watchId) {
        navigator.geolocation.clearWatch(watchId);
        watchId = null;
    }

    // Update user status
    const user = mockUsers.find(u => u.id === currentUser);
    if (user) {
        user.isActive = false;
        user.sessionStart = null;
    }

    // Save session
    if (trackingSession) {
        trackingSession.endTime = new Date();
        // In a real app, you'd save this to the server
    }

    // Update UI
    updatePersonalTrackingInfo();
    document.getElementById('myTrackingStatus').textContent = 'Offline';
    document.getElementById('myTrackingStatus').className = 'badge bg-secondary';

    showNotification('Info', 'Location tracking stopped', 'info');
}

function handleLocationUpdate(position) {
    const location = {
        lat: position.coords.latitude,
        lng: position.coords.longitude,
        timestamp: new Date(),
        accuracy: position.coords.accuracy
    };

    // Add to current session
    if (trackingSession) {
        trackingSession.locations.push(location);
    }

    // Update user's current location
    const user = mockUsers.find(u => u.id === currentUser);
    if (user) {
        user.currentLocation = { lat: location.lat, lng: location.lng };
        user.lastSeen = new Date();
    }

    // Update map if showing live view
    updateUserMarkers();
    updatePersonalTrackingInfo();
}

function handleLocationError(error) {
    console.error('Location error:', error);
    let message = 'Location access denied';

    switch (error.code) {
        case error.PERMISSION_DENIED:
            message = 'Location access denied by user';
            break;
        case error.POSITION_UNAVAILABLE:
            message = 'Location information unavailable';
            break;
        case error.TIMEOUT:
            message = 'Location request timed out';
            break;
    }

    showNotification('Error', message, 'error');
}

function updateUserMarkers() {
    // Clear existing markers
    Object.values(userMarkers).forEach(marker => marker.setMap(null));
    userMarkers = {};

    // Add markers for each user
    mockUsers.forEach(user => {
        if (user.currentLocation) {
            const marker = new google.maps.Marker({
                position: user.currentLocation,
                map: map,
                title: user.name,
                icon: {
                    url: user.isActive ?
                        'data:image/svg+xml;charset=UTF-8,' + encodeURIComponent(`
                            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                                <circle cx="12" cy="12" r="8" fill="#28a745" stroke="#fff" stroke-width="2"/>
                                <circle cx="12" cy="12" r="4" fill="#fff"/>
                            </svg>
                        `) :
                        'data:image/svg+xml;charset=UTF-8,' + encodeURIComponent(`
                            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                                <circle cx="12" cy="12" r="8" fill="#6c757d" stroke="#fff" stroke-width="2"/>
                                <circle cx="12" cy="12" r="4" fill="#fff"/>
                            </svg>
                        `),
                    scaledSize: new google.maps.Size(24, 24)
                }
            });

            // Add info window
            const infoWindow = new google.maps.InfoWindow({
                content: `
                    <div class="marker-info">
                        <h6>${user.name}</h6>
                        <p><strong>Role:</strong> ${user.role}</p>
                        <p><strong>Status:</strong> ${user.isActive ? 'Active' : 'Offline'}</p>
                        <p><strong>Last Seen:</strong> ${formatDateTime(user.lastSeen)}</p>
                        ${user.isActive ? `<p><strong>Distance:</strong> ${user.distanceTraveled} km</p>` : ''}
                    </div>
                `
            });

            marker.addListener('click', () => {
                infoWindow.open(map, marker);
            });

            userMarkers[user.id] = marker;
        }
    });
}

function updateAdminStatistics() {
    const activeUsers = mockUsers.filter(u => u.isActive).length;
    const offlineUsers = mockUsers.filter(u => !u.isActive).length;
    const totalDistance = mockUsers.reduce((sum, u) => sum + u.distanceTraveled, 0);
    const activeTime = mockUsers
        .filter(u => u.isActive && u.sessionStart)
        .reduce((sum, u) => sum + (Date.now() - u.sessionStart.getTime()), 0);

    document.getElementById('activeUsers').textContent = activeUsers;
    document.getElementById('offlineUsers').textContent = offlineUsers;
    document.getElementById('totalDistance').textContent = `${totalDistance.toFixed(1)} km`;
    document.getElementById('activeTime').textContent = `${Math.floor(activeTime / (1000 * 60 * 60))}h`;
}

function renderLiveUsers() {
    const liveUsersList = document.getElementById('liveUsersList');
    if (!liveUsersList) return;

    const html = mockUsers.map(user => {
        const statusClass = user.isActive ? 'text-success' : 'text-secondary';
        const statusText = user.isActive ? 'Active' : 'Offline';
        const sessionTime = user.isActive && user.sessionStart ?
            formatDuration(Date.now() - user.sessionStart.getTime()) : '0h 0m';

        return `
            <div class="live-user-item" onclick="focusOnUser('${user.id}')">
                <div class="user-avatar">
                    <div class="avatar-circle ${user.isActive ? 'active' : 'offline'}">
                        ${user.name.charAt(0)}
                    </div>
                </div>
                <div class="user-info">
                    <h6 class="user-name">${user.name}</h6>
                    <p class="user-role">${user.role}</p>
                    <div class="user-status">
                        <span class="status ${statusClass}">${statusText}</span>
                        ${user.isActive ? `<span class="session-time">${sessionTime}</span>` : ''}
                    </div>
                </div>
                <div class="user-actions">
                    <button class="btn btn-sm btn-outline-primary" onclick="viewUserHistory('${user.id}'); event.stopPropagation();">
                        <i class="fas fa-history"></i>
                    </button>
                </div>
            </div>
        `;
    }).join('');

    liveUsersList.innerHTML = html;
}

function updatePersonalTrackingInfo() {
    const user = mockUsers.find(u => u.id === currentUser);
    if (!user) return;

    const sessionDuration = user.isActive && user.sessionStart ?
        formatDuration(Date.now() - user.sessionStart.getTime()) : '00:00:00';

    document.getElementById('sessionDuration').textContent = sessionDuration;
    document.getElementById('distanceTraveled').textContent = `${user.distanceTraveled.toFixed(1)} km`;

    // Update current location
    if (user.currentLocation) {
        // In a real app, you'd reverse geocode this
        document.getElementById('currentLocation').textContent =
            `${user.currentLocation.lat.toFixed(4)}, ${user.currentLocation.lng.toFixed(4)}`;
    }

    // Update recent sessions
    updateRecentSessions();
}

function updateRecentSessions() {
    const recentSessions = document.getElementById('recentSessions');
    if (!recentSessions) return;

    // Mock recent sessions
    const sessions = [
        { date: new Date(Date.now() - 24 * 60 * 60 * 1000), duration: '4h 30m', distance: '12.5 km' },
        { date: new Date(Date.now() - 2 * 24 * 60 * 60 * 1000), duration: '6h 15m', distance: '18.2 km' },
        { date: new Date(Date.now() - 3 * 24 * 60 * 60 * 1000), duration: '3h 45m', distance: '8.7 km' }
    ];

    const html = sessions.map(session => `
        <div class="session-item">
            <div class="session-date">${formatDate(session.date)}</div>
            <div class="session-stats">
                <span class="duration">${session.duration}</span>
                <span class="distance">${session.distance}</span>
            </div>
        </div>
    `).join('');

    recentSessions.innerHTML = html;
}

function updateLocationHistory() {
    const historyList = document.getElementById('locationHistoryList');
    if (!historyList) return;

    const filter = document.getElementById('historyTimeFilter')?.value || '24h';
    let filteredHistory = locationHistory;

    // Apply time filter
    const now = new Date();
    switch (filter) {
        case '7d':
            filteredHistory = locationHistory.filter(h =>
                (now - h.timestamp) <= 7 * 24 * 60 * 60 * 1000);
            break;
        case '30d':
            filteredHistory = locationHistory.filter(h =>
                (now - h.timestamp) <= 30 * 24 * 60 * 60 * 1000);
            break;
        case '24h':
        default:
            filteredHistory = locationHistory.filter(h =>
                (now - h.timestamp) <= 24 * 60 * 60 * 1000);
    }

    const html = filteredHistory.slice(0, 10).map(item => `
        <div class="history-item ${item.isActive ? 'active' : 'inactive'}" 
             onclick="showLocationDetail('${item.timestamp.toISOString()}')">
            <div class="history-time">${formatTime(item.timestamp)}</div>
            <div class="history-status">
                <span class="status-indicator ${item.isActive ? 'active' : 'inactive'}"></span>
                ${item.isActive ? 'Active' : 'Offline'}
            </div>
            <div class="history-location">
                ${item.location.lat.toFixed(4)}, ${item.location.lng.toFixed(4)}
            </div>
        </div>
    `).join('');

    historyList.innerHTML = html || '<div class="no-history">No location history available</div>';
}

function startMockLocationUpdates() {
    // Simulate location updates for active users
    setInterval(() => {
        mockUsers.forEach(user => {
            if (user.isActive && user.currentLocation) {
                // Simulate small movements
                const latOffset = (Math.random() - 0.5) * 0.001;
                const lngOffset = (Math.random() - 0.5) * 0.001;

                user.currentLocation.lat += latOffset;
                user.currentLocation.lng += lngOffset;
                user.lastSeen = new Date();

                // Update distance (rough calculation)
                user.distanceTraveled += Math.random() * 0.1;
            }
        });

        // Update UI if admin
        if (currentUser === 'MasterAdmin') {
            updateUserMarkers();
            updateAdminStatistics();
            renderLiveUsers();
        } else {
            updatePersonalTrackingInfo();
        }
    }, 30000); // Update every 30 seconds
}

// Event handlers
function handleUserSelection(event) {
    const selectedUserId = event.target.value;
    // Enable/disable view history button
    const viewBtn = document.getElementById('viewUserHistoryBtn');
    if (viewBtn) {
        viewBtn.disabled = !selectedUserId;
    }
}

function viewSelectedUserHistory() {
    const userSelect = document.getElementById('userSelect');
    const selectedUserId = userSelect?.value;

    if (selectedUserId) {
        viewUserHistory(selectedUserId);
    }
}

function handleMapViewChange(event) {
    const view = event.target.value;

    if (view === 'live') {
        document.getElementById('mapTitle').textContent = 'Live Location Map';
        updateUserMarkers();
    } else {
        document.getElementById('mapTitle').textContent = 'Location History Map';
        // Show history view
    }
}

function centerMap() {
    if (currentUser !== 'MasterAdmin') {
        // Center on current user
        const user = mockUsers.find(u => u.id === currentUser);
        if (user && user.currentLocation) {
            map.setCenter(user.currentLocation);
            map.setZoom(15);
        }
    } else {
        // Center on all users
        const bounds = new google.maps.LatLngBounds();
        mockUsers.forEach(user => {
            if (user.currentLocation) {
                bounds.extend(user.currentLocation);
            }
        });
        map.fitBounds(bounds);
    }
}

function refreshMap() {
    updateUserMarkers();
    if (currentUser === 'MasterAdmin') {
        updateAdminStatistics();
        renderLiveUsers();
    } else {
        updatePersonalTrackingInfo();
    }
    showNotification('Success', 'Map refreshed', 'success');
}

function focusOnUser(userId) {
    const user = mockUsers.find(u => u.id === userId);
    if (user && user.currentLocation) {
        map.setCenter(user.currentLocation);
        map.setZoom(15);

        // Open info window
        const marker = userMarkers[userId];
        if (marker) {
            google.maps.event.trigger(marker, 'click');
        }
    }
}

function viewUserHistory(userId) {
    const user = mockUsers.find(u => u.id === userId);
    if (!user) return;

    // Switch to history view
    document.getElementById('historyView').checked = true;
    document.getElementById('mapTitle').textContent = `${user.name} - Location History`;

    // Show user's path on map
    showUserPath(userId);

    showNotification('Info', `Showing history for ${user.name}`, 'info');
}

function showUserPath(userId) {
    // Clear existing paths
    Object.values(userPaths).forEach(path => path.setMap(null));
    userPaths = {};

    // Generate mock path for user
    const user = mockUsers.find(u => u.id === userId);
    if (!user) return;

    const pathCoordinates = generateMockPath(user.currentLocation);

    const userPath = new google.maps.Polyline({
        path: pathCoordinates,
        geodesic: true,
        strokeColor: '#FF6B35',
        strokeOpacity: 1.0,
        strokeWeight: 3
    });

    userPath.setMap(map);
    userPaths[userId] = userPath;

    // Fit map to path
    const bounds = new google.maps.LatLngBounds();
    pathCoordinates.forEach(coord => bounds.extend(coord));
    map.fitBounds(bounds);
}

function generateMockPath(startLocation) {
    const path = [startLocation];
    let currentLat = startLocation.lat;
    let currentLng = startLocation.lng;

    // Generate 20 points for the path
    for (let i = 0; i < 20; i++) {
        currentLat += (Math.random() - 0.5) * 0.01;
        currentLng += (Math.random() - 0.5) * 0.01;
        path.push({ lat: currentLat, lng: currentLng });
    }

    return path;
}

function showLocationDetail(timestamp) {
    const modal = new bootstrap.Modal(document.getElementById('locationDetailModal'));
    const content = document.getElementById('locationDetailContent');

    const historyItem = locationHistory.find(h => h.timestamp.toISOString() === timestamp);
    if (!historyItem) return;

    content.innerHTML = `
        <div class="location-detail">
            <h6>Location Details</h6>
            <div class="row">
                <div class="col-md-6">
                    <strong>Timestamp:</strong> ${formatDateTime(historyItem.timestamp)}<br>
                    <strong>Status:</strong> ${historyItem.isActive ? 'Active' : 'Offline'}<br>
                    <strong>Session ID:</strong> ${historyItem.sessionId}
                </div>
                <div class="col-md-6">
                    <strong>Latitude:</strong> ${historyItem.location.lat.toFixed(6)}<br>
                    <strong>Longitude:</strong> ${historyItem.location.lng.toFixed(6)}<br>
                    <strong>Coordinates:</strong> ${historyItem.location.lat.toFixed(4)}, ${historyItem.location.lng.toFixed(4)}
                </div>
            </div>
        </div>
    `;

    modal.show();
}

// Utility functions
function formatDateTime(date) {
    return date.toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}

function formatDate(date) {
    return date.toLocaleDateString('en-US', {
        month: 'short',
        day: 'numeric'
    });
}

function formatTime(date) {
    return date.toLocaleTimeString('en-US', {
        hour: '2-digit',
        minute: '2-digit'
    });
}

function formatDuration(milliseconds) {
    const hours = Math.floor(milliseconds / (1000 * 60 * 60));
    const minutes = Math.floor((milliseconds % (1000 * 60 * 60)) / (1000 * 60));
    return `${hours}h ${minutes}m`;
}

// Make functions globally available
window.initMap = initializeLocationTracking;
window.focusOnUser = focusOnUser;
window.viewUserHistory = viewUserHistory;
window.showLocationDetail = showLocationDetail;