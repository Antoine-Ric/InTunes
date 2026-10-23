package com.intunes.repository;

import com.intunes.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    // TODO: query methods as needed, e.g. findByUsername
}
