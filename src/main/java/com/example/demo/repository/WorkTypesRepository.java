package com.example.demo.repository;

import com.example.demo.entity.WorkTypes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface WorkTypesRepository extends JpaRepository<WorkTypes,Long> {

    @Query(value = "SELECT DISTINCT work_type FROM work_types ORDER BY work_type ",nativeQuery = true)
    List<String> fetchAllWorkTypes();

}
