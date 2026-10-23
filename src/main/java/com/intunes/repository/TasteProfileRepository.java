package com.intunes.repository;

import com.intunes.domain.TasteProfile;
import com.intunes.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TasteProfileRepository extends JpaRepository<TasteProfile, Long> {
    Optional<TasteProfile> findByUserAndGenre(User user, String genre);
}
