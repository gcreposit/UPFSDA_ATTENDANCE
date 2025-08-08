//package com.example.demo.controller;
//
//import com.example.demo.dto.EmployeeResponse;
//import com.example.demo.entity.Employee;
//import com.example.demo.service.EmployeeService;
//import com.example.demo.service.FileStorageService;
//import com.example.demo.service.LocationService;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.http.MediaType;
//import org.springframework.mock.web.MockMultipartFile;
//import org.springframework.test.web.servlet.MockMvc;
//
//import java.util.Arrays;
//import java.util.List;
//
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.Mockito.when;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//@WebMvcTest(DataApiController.class)
//class DataApiControllerTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    @MockBean
//    private EmployeeService employeeService;
//
//    @MockBean
//    private LocationService locationService;
//
//    @Test
//    void createEmployee_ShouldReturnCreatedEmployee() throws Exception {
//        // Arrange
//        EmployeeResponse mockResponse = EmployeeResponse.success(1L, "John Doe", "ABC123", "Employee created successfully");
//        when(employeeService.createEmployee(any())).thenReturn(mockResponse);
//
//        MockMultipartFile facePhoto = new MockMultipartFile("uploadFacePhoto", "face.jpg", "image/jpeg", "face content".getBytes());
//        MockMultipartFile signature = new MockMultipartFile("uploadSignature", "signature.png", "image/png", "signature content".getBytes());
//
//        // Act & Assert
//        mockMvc.perform(multipart("/api/data/employees")
//                .file(facePhoto)
//                .file(signature)
//                .param("name", "John Doe")
//                .param("dateOfBirth", "15/06/1990")
//                .param("identityCardNo", "ABC123")
//                .param("address", "123 Main Street")
//                .param("assignedLocation", "Sampler")
//                .param("district", "Agra")
//                .param("tehsil", "Agra")
//                .contentType(MediaType.MULTIPART_FORM_DATA))
//                .andExpect(status().isCreated())
//                .andExpect(jsonPath("$.success").value(true))
//                .andExpect(jsonPath("$.id").value(1))
//                .andExpect(jsonPath("$.name").value("John Doe"))
//                .andExpect(jsonPath("$.identityCardNo").value("ABC123"));
//    }
//
//    @Test
//    void createEmployee_ShouldReturnValidationError() throws Exception {
//        MockMultipartFile facePhoto = new MockMultipartFile("uploadFacePhoto", "face.jpg", "image/jpeg", "face content".getBytes());
//        MockMultipartFile signature = new MockMultipartFile("uploadSignature", "signature.png", "image/png", "signature content".getBytes());
//
//        mockMvc.perform(multipart("/api/data/employees")
//                .file(facePhoto)
//                .file(signature)
//                .param("name", "") // Empty name should cause validation error
//                .param("dateOfBirth", "15/06/1990")
//                .param("identityCardNo", "ABC123")
//                .param("address", "123 Main Street")
//                .param("assignedLocation", "Sampler")
//                .param("district", "Agra")
//                .param("tehsil", "Agra")
//                .contentType(MediaType.MULTIPART_FORM_DATA))
//                .andExpect(status().isBadRequest())
//                .andExpect(jsonPath("$.success").value(false))
//                .andExpect(jsonPath("$.message").value("Validation failed"));
//    }
//
//    @Test
//    void createEmployee_ShouldReturnConflictForDuplicateIdentityCard() throws Exception {
//        // Arrange
//        when(employeeService.createEmployee(any()))
//            .thenThrow(new EmployeeService.IdentityCardDuplicateException("Identity card number already exists"));
//
//        MockMultipartFile facePhoto = new MockMultipartFile("uploadFacePhoto", "face.jpg", "image/jpeg", "face content".getBytes());
//        MockMultipartFile signature = new MockMultipartFile("uploadSignature", "signature.png", "image/png", "signature content".getBytes());
//
//        // Act & Assert
//        mockMvc.perform(multipart("/api/data/employees")
//                .file(facePhoto)
//                .file(signature)
//                .param("name", "John Doe")
//                .param("dateOfBirth", "15/06/1990")
//                .param("identityCardNo", "ABC123")
//                .param("address", "123 Main Street")
//                .param("assignedLocation", "Sampler")
//                .param("district", "Agra")
//                .param("tehsil", "Agra")
//                .contentType(MediaType.MULTIPART_FORM_DATA))
//                .andExpect(status().isConflict())
//                .andExpect(jsonPath("$.success").value(false))
//                .andExpect(jsonPath("$.message").value("Identity card number already exists"));
//    }
//
//    @Test
//    void createEmployee_ShouldReturnBadRequestForFileStorageError() throws Exception {
//        // Arrange
//        when(employeeService.createEmployee(any()))
//            .thenThrow(new FileStorageService.FileStorageException("File upload failed"));
//
//        MockMultipartFile facePhoto = new MockMultipartFile("uploadFacePhoto", "face.jpg", "image/jpeg", "face content".getBytes());
//        MockMultipartFile signature = new MockMultipartFile("uploadSignature", "signature.png", "image/png", "signature content".getBytes());
//
//        // Act & Assert
//        mockMvc.perform(multipart("/api/data/employees")
//                .file(facePhoto)
//                .file(signature)
//                .param("name", "John Doe")
//                .param("dateOfBirth", "15/06/1990")
//                .param("identityCardNo", "ABC123")
//                .param("address", "123 Main Street")
//                .param("assignedLocation", "Sampler")
//                .param("district", "Agra")
//                .param("tehsil", "Agra")
//                .contentType(MediaType.MULTIPART_FORM_DATA))
//                .andExpect(status().isInternalServerError())
//                .andExpect(jsonPath("$.success").value(false))
//                .andExpect(jsonPath("$.message").value("File upload failed: File upload failed"));
//    }
//
//    @Test
//    void getDistricts_ShouldReturnDistrictList() throws Exception {
//        // Arrange
//        List<String> mockDistricts = Arrays.asList("Agra", "Lucknow", "Kanpur");
//        when(locationService.getAllDistricts()).thenReturn(mockDistricts);
//
//        // Act & Assert
//        mockMvc.perform(get("/api/data/districts"))
//                .andExpect(status().isOk())
//                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
//                .andExpect(jsonPath("$").isArray())
//                .andExpect(jsonPath("$.length()").value(3))
//                .andExpect(jsonPath("$[0]").value("Agra"))
//                .andExpect(jsonPath("$[1]").value("Lucknow"))
//                .andExpect(jsonPath("$[2]").value("Kanpur"));
//    }
//
//    @Test
//    void getDistricts_ShouldReturnErrorOnException() throws Exception {
//        // Arrange
//        when(locationService.getAllDistricts()).thenThrow(new RuntimeException("Database error"));
//
//        // Act & Assert
//        mockMvc.perform(get("/api/data/districts"))
//                .andExpect(status().isInternalServerError())
//                .andExpect(jsonPath("$.success").value(false))
//                .andExpect(jsonPath("$.message").value("Failed to fetch districts"));
//    }
//
//    @Test
//    void getTehsils_ShouldReturnTehsilList() throws Exception {
//        // Arrange
//        List<String> mockTehsils = Arrays.asList("Agra", "Fatehabad", "Kiraoli");
//        when(locationService.getTehsilsByDistrict("Agra")).thenReturn(mockTehsils);
//
//        // Act & Assert
//        mockMvc.perform(get("/api/data/tehsils")
//                .param("district", "Agra"))
//                .andExpect(status().isOk())
//                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
//                .andExpect(jsonPath("$").isArray())
//                .andExpect(jsonPath("$.length()").value(3))
//                .andExpect(jsonPath("$[0]").value("Agra"))
//                .andExpect(jsonPath("$[1]").value("Fatehabad"))
//                .andExpect(jsonPath("$[2]").value("Kiraoli"));
//    }
//
//    @Test
//    void getTehsils_ShouldReturnBadRequestForMissingDistrict() throws Exception {
//        mockMvc.perform(get("/api/data/tehsils"))
//                .andExpect(status().isBadRequest())
//                .andExpect(jsonPath("$.success").value(false))
//                .andExpect(jsonPath("$.message").value("District parameter is required"));
//    }
//
//    @Test
//    void getTehsils_ShouldReturnBadRequestForEmptyDistrict() throws Exception {
//        mockMvc.perform(get("/api/data/tehsils")
//                .param("district", ""))
//                .andExpect(status().isBadRequest())
//                .andExpect(jsonPath("$.success").value(false))
//                .andExpect(jsonPath("$.message").value("District parameter is required"));
//    }
//
//    @Test
//    void getEmployee_ShouldReturnEmployee() throws Exception {
//        // Arrange
//        Employee mockEmployee = new Employee();
//        mockEmployee.setId(1L);
//        mockEmployee.setName("John Doe");
//        mockEmployee.setIdentityCardNo("ABC123");
//
//        when(employeeService.findById(1L)).thenReturn(mockEmployee);
//
//        // Act & Assert
//        mockMvc.perform(get("/api/data/employees/1"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.id").value(1))
//                .andExpect(jsonPath("$.name").value("John Doe"))
//                .andExpect(jsonPath("$.identityCardNo").value("ABC123"));
//    }
//
//    @Test
//    void getEmployee_ShouldReturnNotFoundForNonExistentEmployee() throws Exception {
//        // Arrange
//        when(employeeService.findById(1L))
//            .thenThrow(new EmployeeService.EmployeeNotFoundException("Employee not found"));
//
//        // Act & Assert
//        mockMvc.perform(get("/api/data/employees/1"))
//                .andExpect(status().isNotFound())
//                .andExpect(jsonPath("$.success").value(false))
//                .andExpect(jsonPath("$.message").value("Employee not found"));
//    }
//
//    @Test
//    void checkIdentityCardUniqueness_ShouldReturnUniqueTrue() throws Exception {
//        // Arrange
//        when(employeeService.isIdentityCardUnique("ABC123")).thenReturn(true);
//
//        // Act & Assert
//        mockMvc.perform(get("/api/data/employees/check-identity/ABC123"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.unique").value(true))
//                .andExpect(jsonPath("$.message").value("Identity card is available"));
//    }
//
//    @Test
//    void checkIdentityCardUniqueness_ShouldReturnUniqueFalse() throws Exception {
//        // Arrange
//        when(employeeService.isIdentityCardUnique("ABC123")).thenReturn(false);
//
//        // Act & Assert
//        mockMvc.perform(get("/api/data/employees/check-identity/ABC123"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.unique").value(false))
//                .andExpect(jsonPath("$.message").value("Identity card already exists"));
//    }
//}