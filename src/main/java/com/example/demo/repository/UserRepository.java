package com.example.demo.repository;

import com.example.demo.entity.User;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    @Cacheable(value = "users", key = "#username")
    Optional<User> findByUsername(String username);
    
    @Cacheable(value = "users", key = "#email")
    Optional<User> findByEmail(String email);
    
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.username = :username")
    boolean existsByUsername(@Param("username") String username);
    
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.email = :email")
    boolean existsByEmail(@Param("email") String email);
    
    @Override
    @CacheEvict(value = "users", allEntries = true)
    <S extends User> S save(S entity);
    
    @Override
    @CacheEvict(value = "users", allEntries = true)
    void deleteById(Long id);
}