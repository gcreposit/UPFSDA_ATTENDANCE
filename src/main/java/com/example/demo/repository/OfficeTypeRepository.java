package com.example.demo.repository;

import com.example.demo.entity.OfficeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OfficeTypeRepository extends JpaRepository<OfficeType,Long> {

}
