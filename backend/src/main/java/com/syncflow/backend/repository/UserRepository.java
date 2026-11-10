package com.syncflow.backend.repository;

import com.syncflow.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // Tìm kiếm user dựa vào email (dùng khi đăng nhập hoặc kiểm tra trùng lặp)
    Optional<User> findByEmail(String email);
    
    // Kiểm tra xem email đã tồn tại trong hệ thống chưa
    boolean existsByEmail(String email);
}