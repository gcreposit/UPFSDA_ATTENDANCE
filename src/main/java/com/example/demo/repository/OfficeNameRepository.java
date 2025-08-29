package com.example.demo.repository;

import com.example.demo.entity.OfficeName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OfficeNameRepository extends JpaRepository<OfficeName,Long> {


}
