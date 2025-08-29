console.log('Lab Profile Page Data loaded', new Date().toLocaleTimeString());

console.log(allLabDetectionData)
//  Common Function is used to fetch data from  Lab Detection Data
sam("LAB_PROFILE");

const timelineLink = document.getElementById('timeline-link');

if (timelineLink && labId) {
    timelineLink.href = `/upfsdaMonitoring/timeLineNavigation?id=${labId}`;
    console.log("Updated timeline link:", timelineLink.href);
}








// Function to create image element
function createImageElement(imageSrc, isAuthorized, originalPath) {
    const img = document.createElement('img');
    img.src = imageSrc;
    img.className = `person-img me-2 mb-2 ${isAuthorized ? 'auth' : 'unauth'}`;
    img.alt = isAuthorized ? 'Authorized Person' : 'Unauthorized Person';
    img.dataset.originalPath = originalPath;

    // Add click event to open modal
    img.addEventListener('click', function () {
        const modal = new bootstrap.Modal(document.getElementById('imageModal'));
        document.getElementById('modalImage').src = this.src;
        document.getElementById('imageModalLabel').textContent =
            isAuthorized ? 'Authorized Person' : 'Unauthorized Person';
        modal.show();
    });

    return img;
}


// Function to create object image element
function createObjectImageElement(imageSrc, originalPath) {
    const img = document.createElement('img');
    img.src = imageSrc;
    img.className = 'person-img me-2 mb-2';
    img.alt = 'Identified Object';
    img.dataset.originalPath = originalPath;

    // Add click event to open modal
    img.addEventListener('click', function () {
        const modal = new bootstrap.Modal(document.getElementById('imageModal'));
        document.getElementById('modalImage').src = this.src;
        document.getElementById('imageModalLabel').textContent = 'Identified Object';
        modal.show();
    });

    return img;
}




// Function to process and display person images
async function displayPersonImages(dataArray) {
    console.log("Displaying person images with data:", dataArray);

    const authorizedContainer = document.getElementById('authorizedPersons');
    const unauthorizedContainer = document.getElementById('unauthorizedPersons');
    const authorizedCountBadge = document.getElementById('authorizedCount');
    const unauthorizedCountBadge = document.getElementById('unauthorizedCount');
    const authorizedStatus = document.getElementById('authorizedStatus');
    const unauthorizedStatus = document.getElementById('unauthorizedStatus');

    // Clear existing content
    authorizedContainer.innerHTML = '';
    unauthorizedContainer.innerHTML = '';

    // Check if data is array and has content
    if (!Array.isArray(dataArray) || dataArray.length === 0) {
        authorizedCountBadge.textContent = '0 Detected';
        unauthorizedCountBadge.textContent = '0 Detected';
        authorizedStatus.innerHTML = '<i class="bi bi-info-circle"></i> No detection data available';
        unauthorizedStatus.innerHTML = '<i class="bi bi-info-circle"></i> No detection data available';
        return;
    }

    let authorizedImages = [];
    let unauthorizedImages = [];
    let totalAuthorizedCount = 0;
    let totalUnauthorizedCount = 0;

    // Process each detection record in the array
    for (const detectionData of dataArray) {
        // Skip if no person images in this record
        if (!detectionData.personImgPath || detectionData.personImgPath.trim() === '') {
            continue;
        }

        // Split image paths by comma
        const imagePaths = detectionData.personImgPath.split(',').filter(path => path.trim() !== '');
        const isAuthorized = detectionData.personStatus === 'AUTHORIZED';

        // Categorize images
        if (isAuthorized) {
            authorizedImages.push(...imagePaths);
            totalAuthorizedCount += imagePaths.length;
        } else {
            unauthorizedImages.push(...imagePaths);
            totalUnauthorizedCount += imagePaths.length;
        }
    }

    // Create loading placeholders for authorized images
    authorizedImages.forEach(() => {
        authorizedContainer.appendChild(createLoadingPlaceholder());
    });

    // Create loading placeholders for unauthorized images
    unauthorizedImages.forEach(() => {
        unauthorizedContainer.appendChild(createLoadingPlaceholder());
    });
    // Load authorized images
    let authorizedLoadedCount = 0;
    for (let i = 0; i < authorizedImages.length; i++) {
        const imagePath = authorizedImages[i].trim();

        try {
            const imageSrc = await getProfileImage(imagePath);
            const placeholder = authorizedContainer.children[i];
            const img = createImageElement(imageSrc, true, imagePath);
            authorizedContainer.replaceChild(img, placeholder);
            authorizedLoadedCount++;
        } catch (error) {
            console.error(`Error loading authorized image ${imagePath}:`, error);
            const placeholder = authorizedContainer.children[i];
            const errorPlaceholder = createErrorPlaceholder();
            authorizedContainer.replaceChild(errorPlaceholder, placeholder);
        }
    }

    // Load unauthorized images
    let unauthorizedLoadedCount = 0;
    for (let i = 0; i < unauthorizedImages.length; i++) {
        const imagePath = unauthorizedImages[i].trim();

        try {
            const imageSrc = await getProfileImage(imagePath);
            const placeholder = unauthorizedContainer.children[i];
            const img = createImageElement(imageSrc, false, imagePath);
            unauthorizedContainer.replaceChild(img, placeholder);
            unauthorizedLoadedCount++;
        } catch (error) {
            console.error(`Error loading unauthorized image ${imagePath}:`, error);
            const placeholder = unauthorizedContainer.children[i];
            const errorPlaceholder = createErrorPlaceholder();
            unauthorizedContainer.replaceChild(errorPlaceholder, placeholder);
        }
    }

    // Update counts and status
    authorizedCountBadge.textContent = `${authorizedLoadedCount} Detected`;
    unauthorizedCountBadge.textContent = `${unauthorizedLoadedCount} Detected`;

    // Update status messages
    if (authorizedLoadedCount > 0) {
        authorizedStatus.innerHTML = '<i class="bi bi-check-circle"></i> All authorized personnel detected';
    } else {
        authorizedStatus.innerHTML = '<i class="bi bi-info-circle"></i> No authorized persons detected';
    }

    if (unauthorizedLoadedCount > 0) {
        unauthorizedStatus.innerHTML = '<i class="bi bi-exclamation-triangle"></i> Unauthorized access detected';
    } else {
        unauthorizedStatus.innerHTML = '<i class="bi bi-info-circle"></i> No unauthorized access detected';
    }

    console.log(`Processed: ${authorizedLoadedCount} authorized, ${unauthorizedLoadedCount} unauthorized images`);
}


// Function to process and display object images
async function displayObjectImages(dataArray) {
    console.log("Displaying object images with data:", dataArray);

    const objectContainer = document.getElementById('identifiedObjects');
    const objectCountBadge = document.getElementById('objectCount');
    const objectStatus = document.getElementById('objectStatus');

    // Clear existing content
    objectContainer.innerHTML = '';

    // Check if data is array and has content
    if (!Array.isArray(dataArray) || dataArray.length === 0) {
        objectCountBadge.textContent = '0 Objects';
        objectStatus.innerHTML = ' No object detection data available';
        return;
    }

    let objectImages = [];
    let totalObjectCount = 0;

    // Process each detection record in the array
    for (const detectionData of dataArray) {
        // Skip if no object images in this record
        if (!detectionData.objectImgPath || detectionData.objectImgPath.trim() === '') {
            continue;
        }

        // Split image paths by comma
        const imagePaths = detectionData.objectImgPath.split(',').filter(path => path.trim() !== '');
        objectImages.push(...imagePaths);
        totalObjectCount += imagePaths.length;
    }

    // Create loading placeholders for object images
    objectImages.forEach(() => {
        objectContainer.appendChild(createLoadingPlaceholder());
    });

    // Load object images
    let objectLoadedCount = 0;
    for (let i = 0; i < objectImages.length; i++) {
        const imagePath = objectImages[i].trim();

        try {
            const imageSrc = await getProfileImage(imagePath);
            const placeholder = objectContainer.children[i];
            const img = createObjectImageElement(imageSrc, imagePath);
            objectContainer.replaceChild(img, placeholder);
            objectLoadedCount++;
        } catch (error) {
            console.error(`Error loading object image ${imagePath}:`, error);
            const placeholder = objectContainer.children[i];
            const errorPlaceholder = createErrorPlaceholder();
            objectContainer.replaceChild(errorPlaceholder, placeholder);
        }
    }

    // Update counts and status
    objectCountBadge.textContent = `${objectLoadedCount} Objects`;

    // Update status message
    if (objectLoadedCount > 0) {
        objectStatus.innerHTML = ' Objects successfully identified';
    } else {
        objectStatus.innerHTML = ' No objects detected';
    }

    console.log(`Processed: ${objectLoadedCount} object images`);
}





// Function to update display with new data (call this when you get new data from your API)
function updatePersonDisplay(newData) {
    displayPersonImages(newData);
    displayObjectImages(newData);

}

