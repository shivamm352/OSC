package com.springosc.product.dao;

import com.springosc.product.entity.RecentlyViewed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecentlyViewedRepository extends JpaRepository<RecentlyViewed, Integer> {
    boolean existsByUserIdAndProductId(String userId, String productId);

}
