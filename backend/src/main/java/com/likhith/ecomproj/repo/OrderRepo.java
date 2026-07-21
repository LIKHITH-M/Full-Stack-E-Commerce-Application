package com.likhith.ecomproj.repo;

import com.likhith.ecomproj.model.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepo extends JpaRepository<OrderEntity, Integer> {
    List<OrderEntity> findByUsername(String username);
}
