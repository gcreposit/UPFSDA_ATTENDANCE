package com.example.demo.repository;

import com.example.demo.entity.ExtraWork;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExtraWorkRepository extends JpaRepository<ExtraWork, Long> {


}
