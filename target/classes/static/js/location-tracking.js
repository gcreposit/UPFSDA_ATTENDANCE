/***** Location Tracking JavaScript – UPDATED *****/

/* ========================
   Global state
======================== */
let map;
let userMarkers = {};     // live markers keyed by userId
let historyMarkers = {};  // "latest" markers keyed by userId (for history view)
let userPaths = {};       // polylines keyed by userId
let markers = {};         // kept for backwards compat in renderLocationHistory
let currentUser = null;
let isTracking = false;
let trackingSession = null;
let watchId = null;

let mockUsers = [];       // [{ id, username, name, isActive, distanceTraveled, currentLocation, lastSeen, sessionStart }]
let locationHistory = []; // [{ username, location:{lat,lng}, isActive, timestamp:Date, sessionId }]


let stompClient;
let reconnectTimer = null;

function connectLiveSocket() {
    // create SockJS + STOMP
    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);

    // quiet logs (optional)
    stompClient.debug = null;

    // IMPORTANT: set heartbeat BEFORE connect
    stompClient.heartbeat.outgoing = 10000; // client -> broker
    stompClient.heartbeat.incoming = 10000; // broker -> client

    // connect + subscribe
    stompClient.connect(
        {}, // headers if needed
        () => {
            // connected
            if (reconnectTimer) { clearTimeout(reconnectTimer); reconnectTimer = null; }

            stompClient.subscribe('/topic/location.latest', msg => {
                const ev = JSON.parse(msg.body);
                onLivePoint(ev);
            });
        },
        (err) => {
            console.error('WS error', err);
            // simple backoff: try again in 5s
            if (!reconnectTimer) {
                reconnectTimer = setTimeout(() => {
                    reconnectTimer = null;
                    connectLiveSocket();
                }, 5000);
            }
        }
    );
}

function onLivePoint(ev) {
    const uid = ev.userName || ev.username || ev.id || ev.name;
    const loc = ev.location || ((ev.lat != null && ev.lon != null)
        ? {lat: Number(ev.lat), lng: Number(ev.lon)} : null);
    const ts = ev.timestamp ? new Date(ev.timestamp) : new Date();
    if (!uid || !loc) return;

    const active = isRecentlyActive(ts);

    // merge user
    let found = false;
    mockUsers = mockUsers.map(u => {
        if (userId(u) === uid) {
            found = true;
            return {...u, currentLocation: loc, lastSeen: ts, isActive: active};
        }
        return u;
    });
    if (!found) {
        mockUsers.push({
            _uid: uid, username: uid, name: uid,
            isActive: active, distanceTraveled: 0,
            currentLocation: loc, lastSeen: ts, sessionStart: null
        });
    }

    // live marker (userMarkers)
    if (userMarkers[uid]) {
        userMarkers[uid].setPosition(loc);
        userMarkers[uid].setIcon({url: markerIconSvg(active), scaledSize: new google.maps.Size(24, 24)});
    } else {
        const m = new google.maps.Marker({
            position: loc, map, title: uid,
            icon: {url: markerIconSvg(active), scaledSize: new google.maps.Size(24, 24)}
        });
        const iw = new google.maps.InfoWindow();
        m.addListener('click', () => {
            iw.setContent(`
        <div class="marker-info">
          <h6>${uid}</h6>
          <p><strong>Status:</strong> ${active ? 'Active' : 'Offline'}</p>
          <p><strong>Last Seen:</strong> ${formatDateTime(ts)}</p>
        </div>
      `);
            iw.open(map, m);
        });
        userMarkers[uid] = m;
    }

    // history “latest” marker (historyMarkers) — keep it in sync, too
    if (historyMarkers[uid]) {
        historyMarkers[uid].setPosition(loc);
        historyMarkers[uid].setIcon({url: markerIconSvg(active), scaledSize: new google.maps.Size(24, 24)});
    }

    // side-panels
    if (currentUser === 'MasterAdmin') {
        updateAdminStatistics();
        renderLiveUsers();
    } else {
        updatePersonalTrackingInfo();
    }

    // append to in-memory history
    locationHistory.unshift({username: uid, userName: uid, location: loc, timestamp: ts, isActive: active});
    if (locationHistory.length > 500) locationHistory.length = 500;
}

function markerIconSvg(active) {
    const color = active ? '#28a745' : '#6c757d';
    return 'data:image/svg+xml;charset=UTF-8,' + encodeURIComponent(`
    <svg width="24" height="24" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
      <circle cx="12" cy="12" r="9" fill="${color}" stroke="#ffffff" stroke-width="2"/>
    </svg>
  `);
}

/* ========================
   Boot
======================== */
document.addEventListener('DOMContentLoaded', function () {
    if (typeof google !== 'undefined') {
        initializeLocationTracking();
    } else {
        // If the Google Maps callback is configured to call `initMap`, wire it
        window.initMap = initializeLocationTracking;
    }
});

// Expose a single init for Maps callback (kept for safety if script tag uses &callback=initMap)
window.initMap = initializeLocationTracking;

/* ========================
   Utilities (IDs / names / formatting)
======================== */
const DEFAULT_CENTER = {lat: 26.8467, lng: 80.9462}; // Lucknow

function userId(u) {
    // A stable key for users across the app
    return u?.username || u?.id || u?.email || u?.name || 'unknown';
}

function userName(u) {
    return u?.username || u?.name || u?.id || 'User';
}

function isRecentlyActive(ts, minutes = 5) {
    if (!ts) return false;
    const t = ts instanceof Date ? ts : new Date(ts);
    return (Date.now() - t.getTime()) < minutes * 60 * 1000;
}

function findUserById(idStr) {
    return mockUsers.find(u => userId(u) === idStr);
}

function formatDateTime(date) {
    if (!date) return 'N/A';
    try {
        return new Date(date).toLocaleString('en-US', {
            year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit'
        });
    } catch {
        return 'N/A';
    }
}

function formatDate(date) {
    if (!date) return '';
    return new Date(date).toLocaleDateString('en-US', {month: 'short', day: 'numeric'});
}

function formatTime(date) {
    if (!date) return '';
    return new Date(date).toLocaleTimeString('en-US', {hour: '2-digit', minute: '2-digit'});
}

function formatDuration(ms) {
    const hours = Math.floor(ms / (1000 * 60 * 60));
    const minutes = Math.floor((ms % (1000 * 60 * 60)) / (1000 * 60));
    const seconds = Math.floor((ms % (1000 * 60)) / 1000);
    return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
}

/* ========================
   Notifications (simple, no framework required)
======================== */
function showNotification(title, message, type = 'info') {
    // type: 'success' | 'info' | 'error'
    try {
        const containerId = 'notify-container';
        let container = document.getElementById(containerId);
        if (!container) {
            container = document.createElement('div');
            container.id = containerId;
            container.style.position = 'fixed';
            container.style.top = '16px';
            container.style.right = '16px';
            container.style.zIndex = '2000';
            document.body.appendChild(container);
        }

        const card = document.createElement('div');
        card.style.minWidth = '260px';
        card.style.maxWidth = '360px';
        card.style.marginBottom = '10px';
        card.style.padding = '12px 14px';
        card.style.borderRadius = '8px';
        card.style.boxShadow = '0 4px 12px rgba(0,0,0,0.15)';
        card.style.color = '#fff';
        card.style.fontFamily = 'system-ui, -apple-system, Segoe UI, Roboto, Arial, sans-serif';
        card.style.background = type === 'success' ? '#198754' : type === 'error' ? '#dc3545' : '#0d6efd';
        card.innerHTML = `<div style="font-weight:600;margin-bottom:4px">${title}</div><div>${message}</div>`;
        container.appendChild(card);
        setTimeout(() => card.remove(), 3500);
    } catch (e) {
        console.log(`[${type.toUpperCase()}] ${title}: ${message}`);
    }
}

/* ========================
   Map
======================== */
function initializeMap() {
    map = new google.maps.Map(document.getElementById('map'), {
        zoom: 12,
        center: DEFAULT_CENTER,
        mapTypeId: google.maps.MapTypeId.ROADMAP,
        styles: [{featureType: 'poi', elementType: 'labels', stylers: [{visibility: 'off'}]}]
    });

    // Add a static marker for Lucknow
    const lucknowMarker = new google.maps.Marker({
        position: DEFAULT_CENTER, map, title: 'Lucknow, India', icon: {
            url: 'data:image/svg+xml;charset=UTF-8,' + encodeURIComponent(`
        <svg width="32" height="32" viewBox="0 0 32 32" xmlns="http://www.w3.org/2000/svg">
          <circle cx="16" cy="16" r="12" fill="#ff6b35" stroke="#fff" stroke-width="3"/>
          <circle cx="16" cy="16" r="6" fill="#fff"/>
        </svg>
      `), scaledSize: new google.maps.Size(32, 32)
        }
    });

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
    lucknowMarker.addListener('click', () => infoWindow.open(map, lucknowMarker));
}

/* ========================
   App init
======================== */
async function initializeLocationTracking() {
    // Get current user identity (fallback to 'user')
    currentUser = localStorage.getItem('username') || 'user';

    // Init map
    initializeMap();

    // Load backend data
    await loadMockData();

    // Wire events
    initializeEventListeners();

    // Build UI based on role
    setupUserInterface();

    connectLiveSocket();          // go real-time

}

async function loadMockData() {
    try {
        // 1) Users (as before)
        const usersResponse = await fetch('/api/data/employeesDetails');
        if (!usersResponse.ok) throw new Error(`Error fetching users: ${usersResponse.statusText}`);
        const usersData = await usersResponse.json();

        // 2) Location history from backend table
        //    Default last 24h (controller already defaults if params are omitted)
        const historyResponse = await fetch('/api/data/location-history');
        if (!historyResponse.ok) throw new Error(`Error fetching location history: ${historyResponse.statusText}`);
        const historyData = await historyResponse.json();

        // Normalize to your internal shape
        locationHistory = (historyData || []).map(item => ({
            ...item, // the backend gives userName; keep a lowercase alias for existing code paths
            username: item.userName,
            location: item.location ?? ((item.lat != null && item.lon != null) ? {lat: item.lat, lng: item.lon} : null),
            timestamp: new Date(item.timestamp),
            isActive: item.isActive ?? false,      // backend may not have this – fine
            sessionId: item.sessionId ?? undefined // optional
        })).filter(h => h.location && !isNaN(h.timestamp?.getTime?.()));

        // 3) Latest per user (for “historyMarkers” on first render)
        const latestResponse = await fetch('/api/data/location-latest');
        if (!latestResponse.ok) throw new Error(`Error fetching latest locations: ${latestResponse.statusText}`);
        const latestData = await latestResponse.json();

        const latestHistory = (latestData || []).map(item => ({
            username: item.userName,
            location: item.location ?? ((item.lat != null && item.lon != null) ? { lat: item.lat, lng: item.lon } : null),
            timestamp: new Date(item.timestamp),
            // was: isActive: false
            isActive: isRecentlyActive(item.timestamp)
        }));

        // Build user models
        const latestMap = {};
        latestHistory.forEach(loc => {
            const key = loc.username;
            if (!latestMap[key] || loc.timestamp > latestMap[key].timestamp) latestMap[key] = loc;
        });

        mockUsers = (usersData || []).map(raw => {
            const uid = userId(raw);
            const lastKnown = latestMap[uid] || null;
            return {
                ...raw,
                _uid: uid,
                isActive: raw.active ?? false,
                distanceTraveled: Number(raw.distanceTraveled) || 0,
                currentLocation: lastKnown?.location ?? null,
                lastSeen: lastKnown?.timestamp ?? null,
                sessionStart: raw.sessionStart ? new Date(raw.sessionStart) : null
            };
        });

        // populateUserSelect(mockUsers);

        populateUserSelect();

        renderLocationHistory(Object.values(latestMap)); // show "latest markers" only
        setMarkersVisible(userMarkers, false);
        setMarkersVisible(historyMarkers, true);
    } catch (error) {
        console.error('Error loading data:', error);
        showNotification('Error', 'Failed to load data from server.', 'error');
    }
}

/* ========================
   Marker/History rendering
======================== */

function renderLocationHistory(history) {
    const bounds = new google.maps.LatLngBounds();

    (history || []).forEach(item => {
        const uid = item.userName || item.username || item.userId || item.id || item.name;
        if (item.location && typeof item.location.lat === 'number' && typeof item.location.lng === 'number') {
            const position = {lat: item.location.lat, lng: item.location.lng};

            if (historyMarkers[uid]) {
                historyMarkers[uid].setPosition(position);
            } else {
                const marker = new google.maps.Marker({
                    position,
                    map,
                    title: uid,
                    icon: {
                        url: 'data:image/svg+xml;charset=UTF-8,' + encodeURIComponent(`
      <svg width="24" height="24" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
        <circle cx="12" cy="12" r="8" fill="${item.isActive ? '#28a745' : '#6c757d'}" stroke="#ffffff" stroke-width="2"/>
        <circle cx="12" cy="12" r="4" fill="#ffffff"/>
      </svg>
    `),
                        scaledSize: new google.maps.Size(24, 24)
                    }
                });

                const infoWindow = new google.maps.InfoWindow({
                    content: `
            <div>
              <strong>${uid || 'Unknown'}</strong><br>
              Last Seen: ${item.timestamp ? new Date(item.timestamp).toLocaleString() : 'N/A'}<br>
            </div>
          `
                });
                marker.addListener('click', () => infoWindow.open(map, marker));
                historyMarkers[uid] = marker;
                markers[uid] = marker;
            }
            bounds.extend(position);
        }
    });

    if (!bounds.isEmpty()) map.fitBounds(bounds);
}

/* ========================
   Event listeners
======================== */
function initializeEventListeners() {
    // Tracking toggle
    const trackingToggle = document.getElementById('trackingToggle');
    if (trackingToggle) trackingToggle.addEventListener('change', handleTrackingToggle);

    // Admin controls
    const userSelect = document.getElementById('userSelect');
    if (userSelect) userSelect.addEventListener('change', handleUserSelection);

    const viewUserHistoryBtn = document.getElementById('viewUserHistoryBtn');
    if (viewUserHistoryBtn) viewUserHistoryBtn.addEventListener('click', viewSelectedUserHistory);

    // Map view toggle
    const mapViewButtons = document.querySelectorAll('input[name="mapView"]');
    mapViewButtons.forEach(button => button.addEventListener('change', handleMapViewChange));

    // Map controls
    document.getElementById('centerMapBtn')?.addEventListener('click', centerMap);
    document.getElementById('refreshMapBtn')?.addEventListener('click', refreshMap);

    // History filter
    document.getElementById('historyTimeFilter')?.addEventListener('change', updateLocationHistory);
}

/* ========================
   UI setup
======================== */
function setupUserInterface() {
    const isAdmin = currentUser === 'MasterAdmin';

    const $ = id => document.getElementById(id);

    if (isAdmin) {
        // Show admin areas
        $('adminControls') && ($('adminControls').style.display = 'block');
        $('userStatusCards') && ($('userStatusCards').style.display = 'flex');
        $('liveUsersPanel') && ($('liveUsersPanel').style.display = 'block');

        // Hide user-only areas
        $('userControls') && ($('userControls').style.display = 'none');
        $('personalTrackingPanel') && ($('personalTrackingPanel').style.display = 'none');

        populateUserSelect();
        updateAdminStatistics();
        renderLiveUsers();
    } else {
        // Show user areas
        $('userControls') && ($('userControls').style.display = 'block');
        $('personalTrackingPanel') && ($('personalTrackingPanel').style.display = 'block');

        // Hide admin areas
        $('adminControls') && ($('adminControls').style.display = 'none');
        $('userStatusCards') && ($('userStatusCards').style.display = 'none');
        $('liveUsersPanel') && ($('liveUsersPanel').style.display = 'none');

        updatePersonalTrackingInfo();
    }

    updateLocationHistory();
}

function populateUserSelect() {
    const userSelect = document.getElementById('userSelect');
    if (!userSelect) return;

    userSelect.innerHTML = '<option value="">Select a user to view history</option>';

    mockUsers.forEach(u => {
        const uid = userId(u);
        if (uid !== 'MasterAdmin') {
            const option = document.createElement('option');
            option.value = uid;
            option.textContent = userName(u);
            userSelect.appendChild(option);
        }
    });
}

/* ========================
   Tracking (start/stop)
======================== */
function handleTrackingToggle(event) {
    const isEnabled = event.target.checked;
    const statusElement = document.getElementById('trackingStatus');

    if (isEnabled) {
        startTracking();
        if (statusElement) statusElement.textContent = 'Go Offline';
    } else {
        stopTracking();
        if (statusElement) statusElement.textContent = 'Go Live';
    }
}

function startTracking() {
    if (isTracking) return;

    isTracking = true;
    trackingSession = {
        id: Date.now(), startTime: new Date(), locations: []
    };

    // Update user status
    const me = findUserById(currentUser);
    if (me) {
        me.isActive = true;
        me.sessionStart = new Date();
    }

    // Start location watch
    if (navigator.geolocation) {
        watchId = navigator.geolocation.watchPosition(handleLocationUpdate, handleLocationError, {
            enableHighAccuracy: true,
            timeout: 10000,
            maximumAge: 60000
        });
    }

    // UI
    updatePersonalTrackingInfo();
    const chip = document.getElementById('myTrackingStatus');
    if (chip) {
        chip.textContent = 'Active';
        chip.className = 'badge bg-success';
    }

    showNotification('Success', 'Location tracking started', 'success');
}

function stopTracking() {
    if (!isTracking) return;

    isTracking = false;

    // Stop geolocation
    if (watchId) {
        navigator.geolocation.clearWatch(watchId);
        watchId = null;
    }

    // Update user status
    const me = findUserById(currentUser);
    if (me) {
        me.isActive = false;
        me.sessionStart = null;
    }

    // Close session
    if (trackingSession) {
        trackingSession.endTime = new Date();
        // TODO: persist to backend if needed
    }

    // UI
    updatePersonalTrackingInfo();
    const chip = document.getElementById('myTrackingStatus');
    if (chip) {
        chip.textContent = 'Offline';
        chip.className = 'badge bg-secondary';
    }

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
    if (trackingSession) trackingSession.locations.push(location);

    // Update my user object
    const me = findUserById(currentUser);
    if (me) {
        me.currentLocation = {lat: location.lat, lng: location.lng};
        me.lastSeen = new Date();
    }

    // Update map/view
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

/* ========================
   Live markers / admin stats
======================== */
function updateUserMarkers() {
    // Clear existing live markers
    Object.values(userMarkers).forEach(marker => marker.setMap(null));
    userMarkers = {};

    mockUsers.forEach(u => {
        if (u.currentLocation && typeof u.currentLocation.lat === 'number' && typeof u.currentLocation.lng === 'number') {
            const marker = new google.maps.Marker({
                position: u.currentLocation, map, title: userName(u), icon: {
                    url: 'data:image/svg+xml;charset=UTF-8,' + encodeURIComponent(`
            <svg width="24" height="24" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
              <circle cx="12" cy="12" r="8" fill="${u.isActive ? '#28a745' : '#6c757d'}" stroke="#ffffff" stroke-width="2"/>
              <circle cx="12" cy="12" r="4" fill="#ffffff"/>
            </svg>
          `), scaledSize: new google.maps.Size(24, 24)
                }
            });

            const infoWindow = new google.maps.InfoWindow({
                content: `
          <div class="marker-info">
            <h6>${userName(u)}</h6>
            <p><strong>Role:</strong> Employee</p>
            <p><strong>Status:</strong> ${u.isActive ? 'Active' : 'Offline'}</p>
            <p><strong>Last Seen:</strong> ${formatDateTime(u.lastSeen)}</p>
          </div>
        `
            });
            marker.addListener('click', () => infoWindow.open(map, marker));

            userMarkers[userId(u)] = marker;
        }
    });
}

function updateAdminStatistics() {
    const activeUsers = mockUsers.filter(u => u.isActive).length;
    const offlineUsers = mockUsers.filter(u => !u.isActive).length;
    const totalDistance = mockUsers.reduce((sum, u) => sum + (Number(u.distanceTraveled) || 0), 0);
    const activeTime = mockUsers
        .filter(u => u.isActive && u.sessionStart)
        .reduce((sum, u) => sum + (Date.now() - u.sessionStart.getTime()), 0);

    const $ = id => document.getElementById(id);
    $('activeUsers') && ($('activeUsers').textContent = activeUsers);
    $('offlineUsers') && ($('offlineUsers').textContent = offlineUsers);
    $('totalDistance') && ($('totalDistance').textContent = `${totalDistance.toFixed(1)} km`);
    $('activeTime') && ($('activeTime').textContent = `${Math.floor(activeTime / (1000 * 60 * 60))}h`);
}

function renderLiveUsers() {
    const liveUsersList = document.getElementById('liveUsersList');
    if (!liveUsersList) return;

    const html = mockUsers.map(u => {
        const statusClass = u.isActive ? 'text-success' : 'text-secondary';
        const statusText = u.isActive ? 'Active' : 'Offline';
        const sessionTime = u.isActive && u.sessionStart ? formatDuration(Date.now() - u.sessionStart.getTime()) : '00:00:00';

        return `
      <div class="live-user-item" onclick="focusOnUser('${userId(u)}')">
        <div class="user-avatar">
          <div class="avatar-circle ${u.isActive ? 'active' : 'offline'}">
            ${userName(u).charAt(0)}
          </div>
        </div>
        <div class="user-info">
          <h6 class="user-name">${userName(u)}</h6>
          <p class="user-role">Employee</p>
          <div class="user-status">
            <span class="status ${statusClass}">${statusText}</span>
            ${u.isActive ? `<span class="session-time">${sessionTime}</span>` : ''}
          </div>
        </div>
        <div class="user-actions">
          <button class="btn btn-sm btn-outline-primary" onclick="viewUserHistory('${userId(u)}'); event.stopPropagation();">
            <i class="fas fa-history"></i>
          </button>
        </div>
      </div>
    `;
    }).join('');

    liveUsersList.innerHTML = html;
}

/* ========================
   Personal tracking panel
======================== */
function updatePersonalTrackingInfo() {
    const me = findUserById(currentUser);
    if (!me) return;

    const sessionDuration = me.isActive && me.sessionStart ? formatDuration(Date.now() - me.sessionStart.getTime()) : '00:00:00';

    const $ = id => document.getElementById(id);
    $('sessionDuration') && ($('sessionDuration').textContent = sessionDuration);
    $('distanceTraveled') && ($('distanceTraveled').textContent = `${(Number(me.distanceTraveled) || 0).toFixed(1)} km`);

    if (me.currentLocation) {
        $('currentLocation') && ($('currentLocation').textContent = `${me.currentLocation.lat.toFixed(4)}, ${me.currentLocation.lng.toFixed(4)}`);
    }

    updateRecentSessions();
}

function updateRecentSessions() {
    const recentSessions = document.getElementById('recentSessions');
    if (!recentSessions) return;

    // Example (replace with real session history when available)
    const sessions = [{
        date: new Date(Date.now() - 24 * 60 * 60 * 1000),
        duration: '4h 30m',
        distance: '12.5 km'
    }, {
        date: new Date(Date.now() - 2 * 24 * 60 * 60 * 1000),
        duration: '6h 15m',
        distance: '18.2 km'
    }, {date: new Date(Date.now() - 3 * 24 * 60 * 60 * 1000), duration: '3h 45m', distance: '8.7 km'}];

    const html = sessions.map(s => `
    <div class="session-item">
      <div class="session-date">${formatDate(s.date)}</div>
      <div class="session-stats">
        <span class="duration">${s.duration}</span>
        <span class="distance">${s.distance}</span>
      </div>
    </div>
  `).join('');

    recentSessions.innerHTML = html;
}

/* ========================
   History list + filter
======================== */
async function updateLocationHistory() {
    const historyList = document.getElementById('locationHistoryList');
    if (!historyList) return;

    const key = document.getElementById('historyTimeFilter')?.value || '24h';
    const hours = {'24h': 24, '7d': 7 * 24, '30d': 30 * 24}[key] || 24;

    try {
        const to = new Date();
        const from = new Date(to.getTime() - hours * 60 * 60 * 1000);

        const url = `/api/data/location-history?from=${encodeURIComponent(from.toISOString())}&to=${encodeURIComponent(to.toISOString())}`;
        const resp = await fetch(url);
        if (!resp.ok) throw new Error('history fetch failed');
        const data = await resp.json();

        locationHistory = (data || []).map(item => ({
            ...item,
            username: item.userName,
            location: item.location ?? ((item.lat != null && item.lon != null) ? {lat: item.lat, lng: item.lon} : null),
            timestamp: new Date(item.timestamp)
        })).filter(h => h.location && !isNaN(h.timestamp?.getTime?.()));
    } catch (e) {
        console.error(e);
        // keep existing locationHistory if fetch fails
    }

    // Newest first
    const filtered = [...locationHistory].sort((a, b) => b.timestamp - a.timestamp).slice(0, 10);

    const html = filtered.map(item => {
        const lat = item.location?.lat, lng = item.location?.lng;
        return `
      <div class="history-item ${item.isActive ? 'active' : 'inactive'}"
           onclick="showLocationDetail('${item.timestamp.toISOString()}')">
        <div class="history-time">${formatTime(item.timestamp)}</div>
        <div class="history-status">
          <span class="status-indicator ${item.isActive ? 'active' : 'inactive'}"></span>
          ${item.isActive ? 'Active' : 'Offline'}
        </div>
        <div class="history-location">
          ${typeof lat === 'number' && typeof lng === 'number' ? `${lat.toFixed(4)}, ${lng.toFixed(4)}` : 'Unknown'}
        </div>
      </div>`;
    }).join('');

    historyList.innerHTML = html || '<div class="no-history">No location history available</div>';
}

/* ========================
   Event handlers / view controls
======================== */
function handleUserSelection(event) {
    const selectedUserId = event.target.value;
    const viewBtn = document.getElementById('viewUserHistoryBtn');
    if (viewBtn) viewBtn.disabled = !selectedUserId;
}

function viewSelectedUserHistory() {
    const userSelect = document.getElementById('userSelect');
    const selectedUserId = userSelect?.value;
    if (selectedUserId) viewUserHistory(selectedUserId);
}

function handleMapViewChange(event) {
    const view = event.target.value;
    const title = document.getElementById('mapTitle');

    if (view === 'live') {
        title && (title.textContent = 'Live Location Map');
        setMarkersVisible(historyMarkers, false);
        setMarkersVisible(userMarkers, true);
        Object.values(userPaths).forEach(p => p.setMap(null));
        userPaths = {};
    } else {
        title && (title.textContent = 'Location History Map');
        setMarkersVisible(userMarkers, false);
        setMarkersVisible(historyMarkers, true);
    }
}

function centerMap() {
    if (currentUser !== 'MasterAdmin') {
        const me = findUserById(currentUser);
        if (me?.currentLocation) {
            map.setCenter(me.currentLocation);
            map.setZoom(15);
        }
    } else {
        const bounds = new google.maps.LatLngBounds();
        let hasAny = false;
        mockUsers.forEach(u => {
            if (u.currentLocation) {
                bounds.extend(u.currentLocation);
                hasAny = true;
            }
        });
        if (hasAny) map.fitBounds(bounds);
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

function focusOnUser(uid) {
    const u = findUserById(uid);
    if (u?.currentLocation) {
        map.setCenter(u.currentLocation);
        map.setZoom(15);

        const marker = userMarkers[uid] || historyMarkers[uid] || markers[uid];
        if (marker) google.maps.event.trigger(marker, 'click');
    }
}

function viewUserHistory(uid) {
    const u = findUserById(uid);
    if (!u) return;

    const historyRadio = document.getElementById('historyView');
    if (historyRadio) historyRadio.checked = true;

    const title = document.getElementById('mapTitle');
    title && (title.textContent = `${userName(u)} - Location History`);

    showUserPath(uid);
    showNotification('Info', `Showing history for ${userName(u)}`, 'info');
}

async function showUserPath(uid) {
    // Clear existing paths
    Object.values(userPaths).forEach(p => p.setMap(null));
    userPaths = {};

    // Get current filter window
    const key = document.getElementById('historyTimeFilter')?.value || '24h';
    const hours = {'24h': 24, '7d': 7 * 24, '30d': 30 * 24}[key] || 24;
    const to = new Date();
    const from = new Date(to.getTime() - hours * 60 * 60 * 1000);

    // Fetch this user's points from backend
    const url = `/api/data/location-history?userName=${encodeURIComponent(uid)}&from=${encodeURIComponent(from.toISOString())}&to=${encodeURIComponent(to.toISOString())}`;
    const resp = await fetch(url);
    if (!resp.ok) {
        showNotification('Error', 'Failed to load user path', 'error');
        return;
    }
    const data = await resp.json();

    const coords = data
        .map(p => p.location || (p.lat != null && p.lon != null ? {lat: p.lat, lng: p.lon} : null))
        .filter(Boolean);

    if (!coords.length) {
        showNotification('Info', 'No points in this window', 'info');
        return;
    }

    const poly = new google.maps.Polyline({
        path: coords,
        geodesic: true,
        strokeColor: '#FF6B35',
        strokeOpacity: 1.0,
        strokeWeight: 3
    });
    poly.setMap(map);
    userPaths[uid] = poly;

    // Fit bounds to this path
    const bounds = new google.maps.LatLngBounds();
    coords.forEach(c => bounds.extend(c));
    map.fitBounds(bounds);
}

window.addEventListener('beforeunload', () => {
    try {
        stompClient && stompClient.disconnect(() => {
        });
    } catch {
    }
});

function generateMockPath(startLocation) {
    const base = startLocation || DEFAULT_CENTER;
    const path = [base];
    let currentLat = base.lat;
    let currentLng = base.lng;

    for (let i = 0; i < 20; i++) {
        currentLat += (Math.random() - 0.5) * 0.01;
        currentLng += (Math.random() - 0.5) * 0.01;
        path.push({lat: currentLat, lng: currentLng});
    }
    return path;
}

function setMarkersVisible(collection, visible) {
    Object.values(collection).forEach(m => m && m.setMap(visible ? map : null));
}

function showLocationDetail(timestampISO) {
    const modalEl = document.getElementById('locationDetailModal');
    const content = document.getElementById('locationDetailContent');
    if (!modalEl || !content) return;

    const historyItem = locationHistory.find(h => h.timestamp.toISOString() === timestampISO);
    if (!historyItem) return;

    const lat = historyItem.location?.lat;
    const lng = historyItem.location?.lng;

    content.innerHTML = `
    <div class="location-detail">
      <h6>Location Details</h6>
      <div class="row">
        <div class="col-md-6">
          <strong>Timestamp:</strong> ${formatDateTime(historyItem.timestamp)}<br>
          <strong>Status:</strong> ${historyItem.isActive ? 'Active' : 'Offline'}<br>
          <strong>Session ID:</strong> ${historyItem.sessionId || 'N/A'}
        </div>
        <div class="col-md-6">
          <strong>Latitude:</strong> ${typeof lat === 'number' ? lat.toFixed(6) : 'N/A'}<br>
          <strong>Longitude:</strong> ${typeof lng === 'number' ? lng.toFixed(6) : 'N/A'}<br>
          <strong>Coordinates:</strong> ${typeof lat === 'number' && typeof lng === 'number' ? `${lat.toFixed(4)}, ${lng.toFixed(4)}` : 'N/A'}
        </div>
      </div>
    </div>
  `;

    // Bootstrap modal if available; fallback to simple alert
    if (window.bootstrap && bootstrap.Modal) {
        const modal = bootstrap.Modal.getOrCreateInstance(modalEl);
        modal.show();
    } else {
        alert(`${formatDateTime(historyItem.timestamp)}\nLat: ${lat}\nLng: ${lng}`);
    }
}