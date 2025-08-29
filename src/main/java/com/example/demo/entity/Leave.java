package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "leave_requests") // safer than 'leave'
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Leave {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Username of the employee applying for leave
    @Column(name = "username", nullable = false)
    private String username;

    private String officeName;

    // Leave start and end dates
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    // Full day or Half day leave
    @Column(name = "duration_type", nullable = false)
    private String durationType; // e.g., FULL_DAY / HALF_DAY

    private String leaveType;

    // Reason for applying
    @Column(name = "reason", length = 500)
    private String reason;

    // Status of leave (Pending, Approved, Rejected, Cancelled)
    @Column(name = "status", nullable = false)
    private String status = "APPROVED";

    // Approver's username (manager/admin)
    @Column(name = "approved_by")
    private String approvedBy;

    // Audit fields
    @Column(name = "applied_on", updatable = false)
    private LocalDateTime appliedOn = LocalDateTime.now();

    @Column(name = "updated_on")
    private LocalDateTime updatedOn = LocalDateTime.now();

}
