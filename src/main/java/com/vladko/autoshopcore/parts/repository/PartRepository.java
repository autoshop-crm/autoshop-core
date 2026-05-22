package com.vladko.autoshopcore.parts.repository;

import com.vladko.autoshopcore.parts.entity.Part;
import com.vladko.autoshopcore.shared.repository.BaseRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Collection;
import java.util.List;

public interface PartRepository extends BaseRepository<Part, Integer> {

    Optional<Part> findByArticleNumber(String articleNumber);

    List<Part> findAllByArticleNumberIn(Collection<String> articleNumbers);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Part p where p.id = :id")
    Optional<Part> findByIdForUpdate(@Param("id") Integer id);
}
