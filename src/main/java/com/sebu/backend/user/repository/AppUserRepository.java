package com.sebu.backend.user.repository;
import com.sebu.backend.user.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AppUserRepository extends JpaRepository<AppUser, Long> { }
