// Global variables for markers and paths
let userMarkers = {};
let userPaths = {};
let map;

function initMap() {
    map = new google.maps.Map(document.getElementById("map"), {
        center: {lat: 27.7, lng: 85.3}, // Default center
        zoom: 8
    });
}

async function loadLocationHistoryFromDB(username) {
    try {
        const res = await fetch(`/api/data/history/${username}`);
        if (!res.ok) throw new Error("Failed to fetch location history");

        const data = await res.json();

        if (!Array.isArray(data) || data.length === 0) {
            console.warn("No location history found for", username);
            return;
        }

        // Clear old markers & paths
        Object.values(userMarkers).forEach(marker => marker.setMap(null));
        userMarkers = {};
        if (userPaths[username]) {
            userPaths[username].setMap(null);
        }

        const pathCoordinates = [];

        data.forEach((loc, index) => {
            const lngValue = loc.lng !== undefined ? loc.lng : loc.lon;
            const position = {lat: parseFloat(loc.lat), lng: parseFloat(lngValue)};
            pathCoordinates.push(position);

            let color = "#007bff"; // blue
            if (index === 0) color = "#28a745"; // green start
            if (index === data.length - 1) color = "#dc3545"; // red end

            const marker = new google.maps.Marker({
                position,
                map,
                title: `${loc.userName || loc.username} - ${loc.timestamp}`,
                icon: {
                    url: 'data:image/svg+xml;charset=UTF-8,' + encodeURIComponent(`
                        <svg width="24" height="24" viewBox="0 0 24 24" fill="none"
                             xmlns="http://www.w3.org/2000/svg">
                            <circle cx="12" cy="12" r="8" fill="${color}" stroke="#fff" stroke-width="2"/>
                            <circle cx="12" cy="12" r="4" fill="#fff"/>
                        </svg>
                    `),
                    scaledSize: new google.maps.Size(24, 24)
                }
            });

            const infoWindow = new google.maps.InfoWindow({
                content: `
                    <div>
                        <strong>${loc.userName || loc.username}</strong><br>
                        Lat: ${loc.lat}, Lng: ${lngValue}<br>
                        Time: ${new Date(loc.timestamp).toLocaleString()}
                    </div>
                `
            });
            marker.addListener("click", () => infoWindow.open(map, marker));

            userMarkers[`marker_${index}`] = marker;
        });

        if (pathCoordinates.length > 0) {
            const bounds = new google.maps.LatLngBounds();
            pathCoordinates.forEach(coord => bounds.extend(coord));
            map.fitBounds(bounds);
        } else {
            console.warn("No path coordinates to fit bounds");
        }

    } catch (err) {
        console.error("Error loading location history:", err);
    }
}

// Example: load data for a user
loadLocationHistoryFromDB("1269_Rahul");
