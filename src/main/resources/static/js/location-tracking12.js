// Global variables
let map;
let userMarkers = {};
let userPaths = {};
let currentUser = null;
let isTracking = false;
let trackingSession = null;
let watchId = null;
let users = [];
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

    // Get current user info (example: fetching from localStorage)
    currentUser = localStorage.getItem('username') || 'user';

    // Initialize map
    initializeMap();

    // Load users data
    loadUsersData();

    // Initialize event listeners
    initializeEventListeners();

    // Set up UI based on user role
    setupUserInterface();

    // Start real-time location updates
    startRealTimeLocationUpdates();

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

async function loadUsersData() {
    try {
        // Fetch user data from your real API endpoint
        const response = await fetch('/api/users'); // Replace with your real API URL
        if (!response.ok) {
            throw new Error('Failed to fetch users data');
        }
        users = await response.json(); // Assuming the response is an array of users
        loadLocationHistory(); // Load the location history after fetching users
        updateUserMarkers(); // Update map with user markers
        setupUserInterface(); // Update UI based on fetched data
    } catch (error) {
        console.error('Error fetching users data:', error);
    }
}

async function loadLocationHistory() {
    try {
        // Fetch location history from your real API endpoint
        const response = await fetch('/api/location-history'); // Replace with your real API URL
        if (!response.ok) {
            throw new Error('Failed to fetch location history');
        }
        locationHistory = await response.json(); // Assuming the response is an array of location history
        updateLocationHistory(); // Update location history view
    } catch (error) {
        console.error('Error fetching location history:', error);
    }
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

    users.forEach(user => {
        if (user.id !== 'MasterAdmin') {
            const option = document.createElement('option');
            option.value = user.id;
            option.textContent = `${user.name} (${user.role})`;
            userSelect.appendChild(option);
        }
    });
}

function updateUserMarkers() {
    // Clear existing markers
    Object.values(userMarkers).forEach(marker => marker.setMap(null));
    userMarkers = {};

    // Add markers for each user
    users.forEach(user => {
        if (user.currentLocation) {
            const marker = new google.maps.Marker({
                position: user.currentLocation,
                map: map,
                title: user.name,
                icon: {
                    url: user.isActive ? 'data:image/svg+xml;charset=UTF-8,' + encodeURIComponent(`
                        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                            <circle cx="12" cy="12" r="8" fill="#28a745" stroke="#fff" stroke-width="2"/>
                            <circle cx="12" cy="12" r="4" fill="#fff"/>
                        </svg>
                    `) : 'data:image/svg+xml;charset=UTF-8,' + encodeURIComponent(`
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

// Function to simulate real-time location updates
function startRealTimeLocationUpdates() {
    setInterval(() => {
        // Fetch updated user data (location) every 30 seconds
        loadUsersData();
    }, 30000); // 30 seconds interval
}

// Update functions for personal tracking, admin statistics, and location history will remain similar to your previous code.
