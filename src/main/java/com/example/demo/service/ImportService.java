package com.example.demo.service;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional
public class ImportService {

    // Configuration
// Configuration
    private static final String SEARCH_FOLDER_DIRECTORY = "/Users/apple1/Downloads/UPNEDA/UPNEDA 2024/";
    private static final String UPLOAD_EXCEL_DIRECTORY = "/Users/apple1/Downloads/AllCSV/";

    // File extensions
    private static final Set<String> IMAGE_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png");
    private static final Set<String> VIDEO_EXTENSIONS = Set.of(".mp4", ".mov", ".avi");

    // Social media platforms
    private static final String IMAGE_PLATFORMS = "Insta,X,Facebook";
    private static final String VIDEO_PLATFORMS = "Insta,X,Facebook,Youtube";

    // CSV Headers
    private static final String CSV_HEADER = "id,approval_date,approved_by,created_by,creation_date,post_description,post_name,post_type,posted_by,posted_on,schedule_post_by,schedule_post_date,status,upload_path,social_media_platform";

    private static final AtomicLong idGenerator = new AtomicLong(1);

    // Remove the main method from service class - not needed in Spring
    // public static void main(String[] args) { ... }

    public void generateCSV() throws IOException {
        Path searchPath = Paths.get(SEARCH_FOLDER_DIRECTORY);
        Path outputDir = Paths.get(UPLOAD_EXCEL_DIRECTORY);

        // Create output directory if it doesn't exist
        Files.createDirectories(outputDir);

        // Generate timestamp for filename
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String csvFileName = "UPNEDA_Posts_" + timestamp + ".csv";
        Path csvPath = outputDir.resolve(csvFileName);

        List<String> csvRows = new ArrayList<>();
        csvRows.add(CSV_HEADER);

        // Process each month folder
        try (Stream<Path> monthFolders = Files.list(searchPath)) {
            monthFolders
                    .filter(Files::isDirectory)
                    .filter(path -> path.getFileName().toString().startsWith("UPNEDA"))
                    .sorted()
                    .forEach(monthFolder -> {
                        try {
                            processMonthFolder(monthFolder, csvRows);
                        } catch (IOException e) {
                            System.err.println("Error processing month folder: " + monthFolder + " - " + e.getMessage());
                        }
                    });
        }

        // Write CSV file
        Files.write(csvPath, csvRows, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        System.out.println("CSV file generated: " + csvPath);
    }

    private void processMonthFolder(Path monthFolder, List<String> csvRows) throws IOException {
        String monthName = monthFolder.getFileName().toString();
        System.out.println("Processing month folder: " + monthName);

        try (Stream<Path> dayFolders = Files.list(monthFolder)) {
            dayFolders
                    .filter(Files::isDirectory)
                    .sorted(this::compareDayFolders)
                    .forEach(dayFolder -> {
                        try {
                            processDayFolder(dayFolder, monthName, csvRows);
                        } catch (IOException e) {
                            System.err.println("Error processing day folder: " + dayFolder + " - " + e.getMessage());
                        }
                    });
        }
    }

    private void processDayFolder(Path dayFolder, String monthName, List<String> csvRows) throws IOException {
        String dayName = dayFolder.getFileName().toString();
        System.out.println("  Processing day folder: " + dayName);

        // Get all media files in the day folder
        List<Path> imageFiles = new ArrayList<>();
        List<Path> videoFiles = new ArrayList<>();

        try (Stream<Path> files = Files.list(dayFolder)) {
            files.filter(Files::isRegularFile)
                    .forEach(file -> {
                        String fileName = file.getFileName().toString().toLowerCase();
                        String extension = getFileExtension(fileName);

                        if (IMAGE_EXTENSIONS.contains(extension)) {
                            imageFiles.add(file);
                        } else if (VIDEO_EXTENSIONS.contains(extension)) {
                            videoFiles.add(file);
                        }
                    });
        }

        // Generate CSV rows for this day
        if (!imageFiles.isEmpty() || !videoFiles.isEmpty()) {
            String postedOn = convertDateFormat(dayName, monthName);

            // Create image post row if images exist
            if (!imageFiles.isEmpty()) {
                String imagePaths = imageFiles.stream()
                        .map(this::generateRelativePath)
                        .collect(Collectors.joining(","));

                String imageRow = createCSVRow(
                        dayName + " Images",
                        "Image",
                        postedOn,
                        imagePaths,
                        IMAGE_PLATFORMS
                );
                csvRows.add(imageRow);
            }

            // Create video post row if videos exist
            if (!videoFiles.isEmpty()) {
                String videoPaths = videoFiles.stream()
                        .map(this::generateRelativePath)
                        .collect(Collectors.joining(","));

                String videoRow = createCSVRow(
                        dayName + " Videos",
                        "Video",
                        postedOn,
                        videoPaths,
                        VIDEO_PLATFORMS
                );
                csvRows.add(videoRow);
            }
        }
    }

    private String createCSVRow(String postName, String postType, String postedOn,
                                String uploadPath, String platforms) {
        return String.join(",",
                String.valueOf(idGenerator.getAndIncrement()), // id
                "", // approval_date
                "", // approved_by
                "System", // created_by
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), // creation_date
                "", // post_description
                "\"" + postName + "\"", // post_name (quoted to handle spaces)
                postType, // post_type
                "", // posted_by
                postedOn, // posted_on
                "", // schedule_post_by
                "", // schedule_post_date
                "active", // status
                "\"" + uploadPath + "\"", // upload_path (quoted to handle commas)
                "\"" + platforms + "\"" // social_media_platform (quoted to handle commas)
        );
    }

    private String generateRelativePath(Path filePath) {
        // Generate relative path from UPNEDA 2024 folder
        Path searchRoot = Paths.get(SEARCH_FOLDER_DIRECTORY);
        Path relativePath = searchRoot.relativize(filePath);
        return "/UPNEDA 2024/" + relativePath.toString().replace("\\", "/");
    }

    private String convertDateFormat(String dayName, String monthName) {
        try {
            // Extract day number from folder name (e.g., "1 APRIL" -> "1")
            String dayNumber = dayName.split(" ")[0];

            // Extract month name from folder name (e.g., "UPNEDA April" -> "April")
            String month = monthName.replace("UPNEDA ", "");

            // Convert month name to number
            int monthNumber = switch (month.toLowerCase()) {
                case "january" -> 1;
                case "february" -> 2;
                case "march" -> 3;
                case "april" -> 4;
                case "may" -> 5;
                case "june" -> 6;
                case "july" -> 7;
                case "august" -> 8;
                case "september" -> 9;
                case "october" -> 10;
                case "november" -> 11;
                case "december" -> 12;
                default -> 4; // Default to April if not found
            };

            // Return date in dd/mm/yyyy format
            return String.format("%02d/%02d/2024", Integer.parseInt(dayNumber), monthNumber);
        } catch (Exception e) {
            System.err.println("Error converting date format for: " + dayName + " - " + e.getMessage());
            return "01/04/2024"; // Default fallback to 1st April 2024
        }
    }


    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        return (lastDotIndex == -1) ? "" : fileName.substring(lastDotIndex);
    }

    private int compareDayFolders(Path a, Path b) {
        try {
            // Extract day numbers for proper sorting
            String dayA = a.getFileName().toString().split(" ")[0];
            String dayB = b.getFileName().toString().split(" ")[0];

            int numA = Integer.parseInt(dayA);
            int numB = Integer.parseInt(dayB);

            return Integer.compare(numA, numB);
        } catch (NumberFormatException e) {
            // Fallback to string comparison
            return a.getFileName().toString().compareTo(b.getFileName().toString());
        }
    }
}