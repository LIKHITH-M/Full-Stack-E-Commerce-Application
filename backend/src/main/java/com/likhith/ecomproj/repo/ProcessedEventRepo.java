package com.likhith.ecomproj.repo;

import com.likhith.ecomproj.model.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedEventRepo extends JpaRepository<ProcessedEvent, Long> {

    boolean existsByEventIdAndEventType(String eventId, String eventType);
}
