console.log('DETECTION TIMELINE loaded', new Date().toLocaleTimeString());




// Timeline Detection System
class TimelineDetectionSystem {
    constructor() {
        this.detectionData = [];
        this.currentDate = null;
        this.timelineHours = [];
        this.selectedHour = null;
        this.isPlaying = false;
        this.playSpeed = 1;
        this.playInterval = null;

        this.initializeTimeline();
        this.setupEventListeners();
        this.setTodaysDate(); // Set today's date on initialization
    }

    initializeTimeline() {
        // Initialize 24-hour timeline
        this.timelineHours = Array.from({length: 24}, (_, i) => ({
            hour: i,
            detectionCount: 0,
            detections: []
        }));
    }

    // Set today's date and load today's data
    setTodaysDate() {
        const today = new Date();
        const todayStr = today.toISOString().split('T')[0]; // Format: YYYY-MM-DD

        // Update date picker
        const datePicker = document.querySelector('input[type="date"]');
        if (datePicker) {
            datePicker.value = todayStr;
        }

        this.currentDate = todayStr;

        // Load today's data immediately
        this.loadDataForDate(todayStr);

        console.log('Set current date to today:', todayStr);
    }

    setupEventListeners() {
        // Speed control
        // const speedRange = document.getElementById('speedRange');
        // const speedValue = document.getElementById('speedValue');
        //
        // if (speedRange) {
        //     speedRange.addEventListener('input', (e) => {
        //         this.playSpeed = parseInt(e.target.value);
        //         speedValue.textContent = this.playSpeed + 'x';
        //
        //         // Restart playback with new speed if playing
        //         if (this.isPlaying) {
        //             this.stopPlayback();
        //             this.startPlayback();
        //         }
        //     });
        // }

        // Play button

        // const playBtn = document.getElementById('playButton'); // or use a more specific selector
        // if (playBtn && playBtn.innerHTML.includes('Play')) {
        //     playBtn.addEventListener('click', () => {
        //         this.togglePlayback();
        //     });
        // }

        // Date picker
        const datePicker = document.querySelector('input[type="date"]');
        if (datePicker) {
            datePicker.addEventListener('change', (e) => {
                this.loadDataForDate(e.target.value);
            });
        }
    }

    // Parse timestamp from detection data
    parseTimestamp(timestamp) {
        // Format: "20250701_122120" -> YYYYMMDD_HHMMSS
        if (!timestamp || timestamp.length < 15) return null;

        const dateStr = timestamp.substring(0, 8); // 20250701
        const timeStr = timestamp.substring(9, 15); // 122120

        const year = dateStr.substring(0, 4);
        const month = dateStr.substring(4, 6);
        const day = dateStr.substring(6, 8);

        const hour = timeStr.substring(0, 2);
        const minute = timeStr.substring(2, 4);
        const second = timeStr.substring(4, 6);

        return {
            date: `${year}-${month}-${day}`,
            time: `${hour}:${minute}:${second}`,
            hour: parseInt(hour),
            minute: parseInt(minute),
            second: parseInt(second),
            fullDateTime: new Date(`${year}-${month}-${day}T${hour}:${minute}:${second}`)
        };
    }

    // Process detection data and organize by timeline
    processDetectionData(dataArray) {
        console.log("Processing detection data:", dataArray);

        if (!Array.isArray(dataArray) || dataArray.length === 0) {
            this.displayEmptyTimeline();
            return;
        }

        // Reset timeline
        this.timelineHours = Array.from({length: 24}, (_, i) => ({
            hour: i,
            detectionCount: 0,
            detections: []
        }));

        // Filter data for current selected date
        const selectedDate = this.currentDate;
        const filteredData = dataArray.filter(detection => {
            const parsedTime = this.parseTimestamp(detection.timestamp);
            return parsedTime && parsedTime.date === selectedDate;
        });

        console.log(`Filtered ${filteredData.length} detections for date: ${selectedDate}`);

        // Process each detection
        filteredData.forEach(detection => {
            const parsedTime = this.parseTimestamp(detection.timestamp);
            if (parsedTime) {
                // Add to timeline
                const hourIndex = parsedTime.hour;
                this.timelineHours[hourIndex].detectionCount++;
                this.timelineHours[hourIndex].detections.push({
                    ...detection,
                    parsedTime: parsedTime
                });
            }
        });

        // Sort detections within each hour by time
        this.timelineHours.forEach(hour => {
            hour.detections.sort((a, b) =>
                a.parsedTime.fullDateTime - b.parsedTime.fullDateTime
            );
        });

        this.detectionData = filteredData;
        this.renderTimeline();
        this.updateTotalDetections();

        // Auto-select first hour with detections or current hour if no detections
        this.autoSelectRelevantHour();
    }

    // Auto-select most relevant hour
    autoSelectRelevantHour() {
        // First try to find hours with detections
        const hoursWithDetections = this.timelineHours.filter(hour => hour.detectionCount > 0);

        if (hoursWithDetections.length > 0) {
            // If we have detections, select the first hour with detections
            this.selectHour(hoursWithDetections[0].hour);
        } else {
            // If no detections, select current hour if it's today
            const today = new Date();
            const todayStr = today.toISOString().split('T')[0];

            if (this.currentDate === todayStr) {
                // Select current hour for today
                this.selectHour(today.getHours());
            } else {
                // Select first hour for other dates
                this.selectHour(0);
            }
        }
    }

    // Render the timeline UI
    renderTimeline() {
        const timelineContainer = document.querySelector('.timeline-box .bg-light, .timeline-box [style*="background:#f8f9fa"]');

        if (!timelineContainer) {
            console.error('Timeline container not found');
            return;
        }

        let timelineHTML = '';

        this.timelineHours.forEach(hour => {
            const hourStr = hour.hour.toString().padStart(2, '0');
            const hasDetections = hour.detectionCount > 0;
            const isSelected = this.selectedHour === hour.hour;

            let rowClass = 'timeline-row mb-2 cursor-pointer';
            if (isSelected) rowClass += ' timeline-selected';

            timelineHTML += `
                <div class="${rowClass}" data-hour="${hour.hour}" onclick="timelineSystem.selectHour(${hour.hour})">
                    <span class="${hasDetections ? 'text-success fw-bold' : 'text-muted'}">${hourStr}</span>
                    ${hasDetections ? `<span class="ms-2 ${isSelected ? 'text-success' : 'text-warning'}">${hour.detectionCount}</span>` : ''}
                </div>
            `;
        });

        timelineContainer.innerHTML = timelineHTML;
    }

    // Select specific hour and display its detections
    selectHour(hour) {
        this.selectedHour = hour;
        this.renderTimeline();
        this.displayHourDetections(hour);
        this.updateTimeDisplay(hour);
    }

    // Display detections for selected hour
    displayHourDetections(hour) {
        const hourData = this.timelineHours[hour];
        const frameContainer = document.querySelector('.row.g-3');
        const hourTitle = document.querySelector('h6');
        const detectionCount = document.querySelector('.text-muted.small');

        if (!frameContainer) return;

        // Update hour title and count
        if (hourTitle) {
            hourTitle.textContent = `Detection Frames - ${hour.toString().padStart(2, '0')}:00 Hour`;
        }

        if (detectionCount) {
            detectionCount.textContent = `${hourData.detectionCount} detections found`;
        }

        // Clear existing frames
        frameContainer.innerHTML = '';

        if (hourData.detections.length === 0) {
            frameContainer.innerHTML = `
                <div class="col-12 text-center text-muted py-4">
                    <i class="bi bi-camera-video fs-1"></i>
                    <p>No detections found for ${hour.toString().padStart(2, '0')}:00 hour</p>
                </div>
            `;
            return;
        }

        // Create frame cards for each detection
        hourData.detections.forEach(detection => {
            const frameCard = this.createFrameCard(detection);
            frameContainer.appendChild(frameCard);
        });
    }

    // Create frame card element
    createFrameCard(detection) {
        const col = document.createElement('div');
        col.className = 'col-md-3';

        const timeStr = detection.parsedTime.time;

        col.innerHTML = `
            <div class="card frame-card cursor-pointer" onclick="timelineSystem.openFrameModal('${detection.frameImgPath}', '${timeStr}')">
                <div class="position-relative">
                    <img src="/path/to/placeholder.jpg" class="card-img-top frame-image" 
                         data-frame-path="${detection.frameImgPath}" alt="Detection Frame">
                    <span class="badge bg-dark badge-time">${timeStr}</span>
                </div>
            </div>
        `;

        // Load the actual image
        this.loadFrameImage(col.querySelector('.frame-image'), detection.frameImgPath);

        return col;
    }

    // Load frame image
    async loadFrameImage(imgElement, framePath) {
        try {
            // Use your existing getProfileImage function
            if (typeof getProfileImage === 'function') {
                const imageSrc = await getProfileImage(framePath);
                imgElement.src = imageSrc;
            } else {
                imgElement.src = framePath; // Fallback
            }
        } catch (error) {
            console.error('Error loading frame image:', error);
            imgElement.src = '/assets/placeholder-frame.jpg'; // Fallback placeholder
        }
    }

    // Open frame modal
    openFrameModal(framePath, timeStr) {
        const modal = new bootstrap.Modal(document.getElementById('imageModal'));
        const modalImage = document.getElementById('modalImage');
        const modalLabel = document.getElementById('imageModalLabel');

        if (modalImage && modalLabel) {
            this.loadFrameImage(modalImage, framePath);
            modalLabel.textContent = `Detection Frame - ${timeStr}`;
            modal.show();
        }
    }

    // Update total detections count
    updateTotalDetections() {
        const totalCount = this.timelineHours.reduce((sum, hour) => sum + hour.detectionCount, 0);
        const totalElement = document.querySelector('.text-muted b');
        if (totalElement) {
            totalElement.textContent = totalCount.toString();
        }
    }

    // Update time display
    updateTimeDisplay(hour) {
        const timeDisplay = document.querySelector('.ms-auto.text-muted b');
        if (timeDisplay) {
            const startHour = hour.toString().padStart(2, '0');
            const endHour = ((hour + 1) % 24).toString().padStart(2, '0');
            timeDisplay.textContent = `${startHour}:00 - ${endHour}:59`;
        }
    }

    // Playback functionality
    togglePlayback() {
        if (this.isPlaying) {
            this.stopPlayback();
        } else {
            this.startPlayback();
        }
    }

    startPlayback() {
        const playBtn = document.getElementById('playButton'); // or use a more specific selector
        if (playBtn) {
            playBtn.innerHTML = '<i class="bi bi-pause-fill"></i> Pause';
        }

        this.isPlaying = true;

        const hourData = this.timelineHours[this.selectedHour];
        if (!hourData || hourData.detections.length === 0) {
            console.warn("No frames to play in selected hour.");
            this.stopPlayback();
            return;
        }

        let index = 0;
        const intervalTime = 2000 / this.playSpeed; // e.g., 2s per frame / speed multiplier

        this.playInterval = setInterval(() => {
            const detection = hourData.detections[index];

            if (detection) {
                this.displaySingleFrame(detection);
                index++;
            } else {
                this.stopPlayback(); // Stop when done
            }
        }, intervalTime);
    }
    displaySingleFrame(detection) {
        const frameContainer = document.querySelector('.row.g-3');
        if (!frameContainer) return;

        const timeStr = detection.parsedTime.time;
        frameContainer.innerHTML = ''; // Clear previous frame

        const card = this.createFrameCard(detection);
        frameContainer.appendChild(card);

        // Update time display (optional)
        const timeDisplay = document.querySelector('.ms-auto.text-muted b');
        if (timeDisplay) {
            timeDisplay.textContent = `${timeStr}`;
        }
    }


    stopPlayback() {
        const playBtn = document.querySelector('.btn-success');
        if (playBtn) {
            playBtn.innerHTML = '<i class="bi bi-play-fill"></i> Play';
        }

        this.isPlaying = false;
        if (this.playInterval) {
            clearInterval(this.playInterval);
            this.playInterval = null;
        }
    }

    // Display empty timeline
    displayEmptyTimeline() {
        const timelineContainer = document.querySelector('.timeline-box .bg-light, .timeline-box [style*="background:#f8f9fa"]');
        if (timelineContainer) {
            timelineContainer.innerHTML = `
                <div class="text-center text-muted py-4">
                    <i class="bi bi-clock fs-1"></i>
                    <p>No detection data available for ${this.currentDate || 'selected date'}</p>
                </div>
            `;
        }

        const frameContainer = document.querySelector('.row.g-3');
        if (frameContainer) {
            frameContainer.innerHTML = `
                <div class="col-12 text-center text-muted py-4">
                    <i class="bi bi-camera-video fs-1"></i>
                    <p>No frames to display for ${this.currentDate || 'selected date'}</p>
                </div>
            `;
        }
    }

    // Load data for specific date
    loadDataForDate(dateStr) {
        console.log('Loading data for date:', dateStr);
        this.currentDate = dateStr;

        // Convert date format for API call (YYYY-MM-DD to YYYYMMDD)
        const apiDateFormat = dateStr.replace(/-/g, '');

        // Make API call to get data for the specific date
        if (typeof sam === 'function') {
            sam("DETECTION_TIMELINE", {date: apiDateFormat});
        }

        // If we already have data, filter and process it
        if (this.detectionData && this.detectionData.length > 0) {
            this.processDetectionData(this.detectionData);
        }
    }

    // Get today's date in format needed for filtering
    getTodayDateString() {
        const today = new Date();
        return today.toISOString().split('T')[0]; // YYYY-MM-DD format
    }

    // Check if given date is today
    isToday(dateStr) {
        return dateStr === this.getTodayDateString();
    }
}

// Initialize timeline system
const timelineSystem = new TimelineDetectionSystem();

// Enhanced update function for detection timeline
function updateDetectionTimeline(newData) {
    console.log("Updating detection timeline with new data:", newData);

    // Update the detected frames display (existing functionality)
    displayDetectionTimeLine(newData);

    // Store the raw data and process it for timeline
    timelineSystem.detectionData = newData;
    timelineSystem.processDetectionData(newData);
}

// Enhanced display function for detected frames
async function displayDetectionTimeLine(dataArray) {
    console.log("Displaying DETECTION_FRAMES with data:", dataArray);

    const objectContainer = document.getElementById('identifiedFrames');
    const frameCountBadge = document.getElementById('frameCount');
    const frameStatus = document.getElementById('frameStatus');

    // Clear existing content
    if (objectContainer) objectContainer.innerHTML = '';

    // Check if data is array and has content
    if (!Array.isArray(dataArray) || dataArray.length === 0) {
        if (frameCountBadge) frameCountBadge.textContent = '0 frames';
        if (frameStatus) frameStatus.innerHTML = ' No frames detection data available';
        return;
    }

    // Filter data for current date if timeline system has a current date
    let filteredData = dataArray;
    if (timelineSystem.currentDate) {
        filteredData = dataArray.filter(detection => {
            const parsedTime = timelineSystem.parseTimestamp(detection.timestamp);
            return parsedTime && parsedTime.date === timelineSystem.currentDate;
        });
    }

    let objectImages = [];
    let totalObjectCount = 0;

    // Process each detection record in the filtered array
    for (const detectionData of filteredData) {
        // Skip if no object images in this record
        if (!detectionData.frameImgPath || detectionData.frameImgPath.trim() === '') {
            continue;
        }

        // Split image paths by comma
        const imagePaths = detectionData.frameImgPath.split(',').filter(path => path.trim() !== '');
        objectImages.push(...imagePaths);
        totalObjectCount += imagePaths.length;
    }

    // Create loading placeholders for object images
    if (objectContainer) {
        objectImages.forEach(() => {
            objectContainer.appendChild(createLoadingPlaceholder());
        });
    }

    // Load object images
    let frameLoadedBadge = 0;
    for (let i = 0; i < objectImages.length && objectContainer; i++) {
        const imagePath = objectImages[i].trim();

        try {
            const imageSrc = await getProfileImage(imagePath);
            const placeholder = objectContainer.children[i];
            const img = createFrameImageElement(imageSrc, imagePath);
            objectContainer.replaceChild(img, placeholder);
            frameLoadedBadge++;
        } catch (error) {
            console.error(`Error loading frames image ${imagePath}:`, error);
            const placeholder = objectContainer.children[i];
            const errorPlaceholder = createErrorPlaceholder();
            objectContainer.replaceChild(errorPlaceholder, placeholder);
        }
    }

    // Update counts and status
    if (frameCountBadge) frameCountBadge.textContent = `${frameLoadedBadge} frames`;

    // Update status message
    if (frameStatus) {
        if (frameLoadedBadge > 0) {
            frameStatus.innerHTML = ' frames successfully identified';
        } else {
            frameStatus.innerHTML = ' No frames detected';
        }
    }

    console.log(`Processed: ${frameLoadedBadge} object images for date: ${timelineSystem.currentDate}`);
}

function createFrameImageElement(imageSrc, originalPath) {
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

// Helper functions (add these if they don't exist)
function createLoadingPlaceholder() {
    const placeholder = document.createElement('div');
    placeholder.className = 'person-img me-2 mb-2 bg-light d-flex align-items-center justify-content-center';
    placeholder.innerHTML = '<i class="bi bi-hourglass-split text-muted"></i>';
    return placeholder;
}

function createErrorPlaceholder() {
    const placeholder = document.createElement('div');
    placeholder.className = 'person-img me-2 mb-2 bg-danger-subtle d-flex align-items-center justify-content-center';
    placeholder.innerHTML = '<i class="bi bi-exclamation-triangle text-danger"></i>';
    return placeholder;
}

console.log('Timeline Detection System initialized successfully');