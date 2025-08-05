package com.example.demo.service;

import com.example.demo.dto.EmployeeRequest;
import com.example.demo.dto.EmployeeResponse;
import com.example.demo.entity.Employee;
import com.example.demo.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private FaceRecognitionService faceRecognitionService;

    @Mock
    private LocationService locationService;

    @InjectMocks
    private EmployeeService employeeService;

    private EmployeeRequest validRequest;
    private Employee savedEmployee;

    @BeforeEach
    void setUp() {
        validRequest = createValidEmployeeRequest();
        savedEmployee = createSavedEmployee();
    }

    @Test
    void createEmployee_ShouldCreateEmployeeSuccessfully() {
        // Arrange
        when(employeeRepository.existsByIdentityCardNo(anyString())).thenReturn(false);
        when(locationService.isValidDistrict(anyString())).thenReturn(true);
        when(locationService.isValidTehsilForDistrict(anyString(), anyString())).thenReturn(true);
        when(fileStorageService.storeEmployeeFiles(anyString(), any(), any()))
                .thenReturn(FileStorageService.FileStorageResult.success("face/path.jpg", "signature/path.jpg"));
        when(employeeRepository.save(any(Employee.class))).thenReturn(savedEmployee);

        // Act
        EmployeeResponse response = employeeService.createEmployee(validRequest);

        // Assert
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getId()).isEqualTo(savedEmployee.getId());
        assertThat(response.getName()).isEqualTo(savedEmployee.getName());
        assertThat(response.getIdentityCardNo()).isEqualTo(savedEmployee.getIdentityCardNo());

        verify(employeeRepository).save(any(Employee.class));
        verify(faceRecognitionService).sendToFaceRecognitionAPI(anyString(), anyString(), anyString());
    }

    @Test
    void createEmployee_ShouldThrowExceptionForDuplicateIdentityCard() {
        // Arrange
        when(employeeRepository.existsByIdentityCardNo(anyString())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> employeeService.createEmployee(validRequest))
                .isInstanceOf(com.example.demo.serviceimpl.EmployeeServiceImpl.IdentityCardDuplicateException.class)
                .hasMessageContaining("Identity card number already exists");

        verify(employeeRepository, never()).save(any());
    }

    @Test
    void createEmployee_ShouldThrowExceptionForInvalidDistrict() {
        // Arrange
        when(employeeRepository.existsByIdentityCardNo(anyString())).thenReturn(false);
        when(locationService.isValidDistrict(anyString())).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> employeeService.createEmployee(validRequest))
                .isInstanceOf(com.example.demo.serviceimpl.EmployeeServiceImpl.InvalidLocationException.class)
                .hasMessageContaining("Invalid district");

        verify(employeeRepository, never()).save(any());
    }

    @Test
    void createEmployee_ShouldThrowExceptionForInvalidTehsil() {
        // Arrange
        when(employeeRepository.existsByIdentityCardNo(anyString())).thenReturn(false);
        when(locationService.isValidDistrict(anyString())).thenReturn(true);
        when(locationService.isValidTehsilForDistrict(anyString(), anyString())).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> employeeService.createEmployee(validRequest))
                .isInstanceOf(com.example.demo.serviceimpl.EmployeeServiceImpl.InvalidLocationException.class)
                .hasMessageContaining("Invalid tehsil");

        verify(employeeRepository, never()).save(any());
    }

    @Test
    void createEmployee_ShouldThrowExceptionForFileStorageFailure() {
        // Arrange
        when(employeeRepository.existsByIdentityCardNo(anyString())).thenReturn(false);
        when(locationService.isValidDistrict(anyString())).thenReturn(true);
        when(locationService.isValidTehsilForDistrict(anyString(), anyString())).thenReturn(true);
        when(fileStorageService.storeEmployeeFiles(anyString(), any(), any()))
                .thenThrow(new FileStorageService.FileStorageException("File storage failed"));

        // Act & Assert
        assertThatThrownBy(() -> employeeService.createEmployee(validRequest))
                .isInstanceOf(FileStorageService.FileStorageException.class)
                .hasMessageContaining("File storage failed");

        verify(employeeRepository, never()).save(any());
    }

    @Test
    void isIdentityCardUnique_ShouldReturnTrueForUniqueCard() {
        when(employeeRepository.existsByIdentityCardNo("ABC123")).thenReturn(false);

        boolean result = employeeService.isIdentityCardUnique("ABC123");

        assertThat(result).isTrue();
    }

    @Test
    void isIdentityCardUnique_ShouldReturnFalseForExistingCard() {
        when(employeeRepository.existsByIdentityCardNo("ABC123")).thenReturn(true);

        boolean result = employeeService.isIdentityCardUnique("ABC123");

        assertThat(result).isFalse();
    }

    @Test
    void isIdentityCardUnique_ShouldReturnFalseForNullOrEmptyCard() {
        boolean resultNull = employeeService.isIdentityCardUnique(null);
        boolean resultEmpty = employeeService.isIdentityCardUnique("");
        boolean resultBlank = employeeService.isIdentityCardUnique("   ");

        assertThat(resultNull).isFalse();
        assertThat(resultEmpty).isFalse();
        assertThat(resultBlank).isFalse();
    }

    @Test
    void findByIdentityCardNo_ShouldReturnEmployeeWhenFound() {
        when(employeeRepository.findByIdentityCardNo("ABC123")).thenReturn(Optional.of(savedEmployee));

        Employee result = employeeService.findByIdentityCardNo("ABC123");

        assertThat(result).isEqualTo(savedEmployee);
    }

    @Test
    void findByIdentityCardNo_ShouldThrowExceptionWhenNotFound() {
        when(employeeRepository.findByIdentityCardNo("ABC123")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.findByIdentityCardNo("ABC123"))
                .isInstanceOf(com.example.demo.serviceimpl.EmployeeServiceImpl.EmployeeNotFoundException.class)
                .hasMessageContaining("Employee not found with identity card");
    }

    @Test
    void findById_ShouldReturnEmployeeWhenFound() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(savedEmployee));

        Employee result = employeeService.findById(1L);

        assertThat(result).isEqualTo(savedEmployee);
    }

    @Test
    void findById_ShouldThrowExceptionWhenNotFound() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.findById(1L))
                .isInstanceOf(com.example.demo.serviceimpl.EmployeeServiceImpl.EmployeeNotFoundException.class)
                .hasMessageContaining("Employee not found with ID");
    }

    private EmployeeRequest createValidEmployeeRequest() {
        EmployeeRequest request = new EmployeeRequest();
        request.setName("John Doe");
        request.setDateOfBirth("15/06/1990");
        request.setIdentityCardNo("ABC123456");
        request.setAddress("123 Main Street, City");
        request.setAssignedLocation("Sampler");
        request.setDistrict("Agra");
        request.setTehsil("Agra");
        request.setMobileNumber("9876543210");
        request.setEmailAddress("john.doe@example.com");
        request.setUploadFacePhoto(new MockMultipartFile("face", "face.jpg", "image/jpeg", "face content".getBytes()));
        request.setUploadSignature(
                new MockMultipartFile("signature", "signature.png", "image/png", "signature content".getBytes()));
        return request;
    }

    private Employee createSavedEmployee() {
        Employee employee = new Employee();
        employee.setId(1L);
        employee.setName("John Doe");
        employee.setIdentityCardNo("ABC123456");
        employee.setDateOfBirth("15/06/1990");
        employee.setAssignedLocation("Sampler");
        employee.setDistrict("Agra");
        employee.setTehsil("Agra");
        employee.setUploadFacePhotoImgPath("john_doe_08-04-2025/Face/face.jpg");
        employee.setUploadSignatureImgPath("john_doe_08-04-2025/Signature/signature.png");
        return employee;
    }
}