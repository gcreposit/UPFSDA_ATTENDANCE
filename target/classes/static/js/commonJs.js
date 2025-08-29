const urlParams = new URLSearchParams(window.location.search);
console.log("URL Parameters:", urlParams.toString());
const labName = urlParams.get('labName');
const labId = urlParams.get('id');  // Extract 'id' parameter


async function sam(PAGE) {
    // Prepare FormData
    const formData = new FormData();
    console.log("labId", labId);
    formData.append("id", labId);  // Append the extracted id

// Send the form data to the backend
    const endPoint = "/monitoring/api/data/fetchLabDetectionData";

    const result = await fetchData(endPoint, formData);

    console.log("Lab Detection Data Result: ", result);
    if (PAGE === "LAB_PROFILE")
        updatePersonDisplay(result);
    else if (PAGE === "DETECTION_TIMELINE"){
        console.log("Detection Timeline Data: ", result);
        updateDetectionTimeline(result);
    }

}


// Function to get profile image DATA simply
async function getProfileImage(imagePath) {
    try {
        const response = await fetch('/monitoring/api/data/', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            body: `filePath=${encodeURIComponent(imagePath)}`
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const blob = await response.blob();
        return URL.createObjectURL(blob);
    } catch (error) {
        console.error('Error fetching profile image:', error);
        return 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTAwIiBoZWlnaHQ9IjEwMCIgdmlld0JveD0iMCAwIDEwMCAxMDAiIGZpbGw9Im5vbmUiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+CjxyZWN0IHdpZHRoPSIxMDAiIGhlaWdodD0iMTAwIiBmaWxsPSIjRjNGNEY2Ii8+CjxjaXJjbGUgY3g9IjUwIiBjeT0iMzciIHI9IjEyIiBmaWxsPSIjOUNBM0FGIi8+CjxwYXRoIGQ9Im0yOCA2OWMwLTggOC0xNiAxNi0xNmg4YzggMCAxNiA4IDE2IDE2djEwaC00MHoiIGZpbGw9IiM5Q0EzQUYiLz4KPC9zdmc+';
    }
}


// Function to create loading placeholder
function createLoadingPlaceholder() {
    const placeholder = document.createElement('div');
    placeholder.className = 'loading-placeholder me-2 mb-2';
    placeholder.innerHTML = '<div class="spinner-border spinner-border-sm text-primary" role="status"></div>';
    return placeholder;
}

// Function to create error placeholder
function createErrorPlaceholder() {
    const placeholder = document.createElement('div');
    placeholder.className = 'person-img error-img me-2 mb-2';
    placeholder.innerHTML = '<i class="bi bi-exclamation-triangle"></i>';
    return placeholder;
}