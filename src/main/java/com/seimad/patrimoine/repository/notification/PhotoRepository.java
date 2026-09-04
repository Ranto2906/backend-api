package com.seimad.patrimoine.repository.notification;

import com.seimad.patrimoine.entity.notification.Photo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PhotoRepository extends JpaRepository<Photo, UUID> {

    List<Photo> findByEntiteTypeAndEntiteIdOrderByDateCreationAsc(String entiteType, UUID entiteId);

    Optional<Photo> findByEntiteTypeAndEntiteIdAndTypePhoto(String entiteType, UUID entiteId, String typePhoto);

    void deleteByEntiteTypeAndEntiteId(String entiteType, UUID entiteId);
}
