package com.vladko.autoshopcore.parts.repository;

import com.vladko.autoshopcore.parts.entity.Part;
import com.vladko.autoshopcore.shared.repository.BaseRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PartRepository extends BaseRepository<Part, Integer> {

    Optional<Part> findByArticleNumber(String articleNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Part p where p.id = :id")
    Optional<Part> findByIdForUpdate(@Param("id") Integer id);
}
