package com.example.demomodulithapp.ingestjob.repo;

import com.example.demomodulithapp.ingestjob.entity.AggregatedOrderEntity;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface AggregatedOrderRepository extends JpaRepository<AggregatedOrderEntity, Long> {

    @Transactional
    void deleteByDataDate(LocalDate dataDate);
}
