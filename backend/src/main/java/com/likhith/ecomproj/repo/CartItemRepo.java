package com.likhith.ecomproj.repo;

import com.likhith.ecomproj.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepo extends JpaRepository<CartItem, Long> {

    List<CartItem> findByUsername(String username);

    Optional<CartItem> findByUsernameAndProductId(String username, int productId);

    @Transactional
    void deleteByUsername(String username);

    @Transactional
    void deleteByUsernameAndProductId(String username, int productId);
}
