package com.intunes.repository;

import com.intunes.domain.Community;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface CommunityRepository extends JpaRepository<Community, Long> {
    Optional<Community> findByArchetypeKey(String archetypeKey);

    /** Caller owns the transaction; ON CONFLICT avoids poisoning it during concurrent creation. */
    @Modifying
    @Query(value = """
            insert into communities (archetype_key, name, description) values (:key, :name, :description)
            on conflict (archetype_key) do nothing
            """, nativeQuery = true)
    int insertIfAbsent(@Param("key") String key, @Param("name") String name, @Param("description") String description);
}
