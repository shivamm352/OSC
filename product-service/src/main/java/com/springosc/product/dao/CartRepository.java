package com.springosc.product.dao;

import com.springosc.product.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartRepository extends JpaRepository<Cart, Integer> {

    Cart findByUserIdAndProductId(String userId, String productId);

    List<Cart> findByUserIdAndQuantityGreaterThan(String userId, int minQuantity);

}
