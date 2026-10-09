package com.jandi.band_backend.promo.repository;

import com.jandi.band_backend.promo.entity.PromoPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PromoPhotoRepository extends JpaRepository<PromoPhoto, Integer> {
    Optional<PromoPhoto> findByPromoIdAndIsCurrentTrue(Integer promoId);
    
    @Query("SELECT pp FROM PromoPhoto pp WHERE pp.promo.id = :promoId AND pp.deletedAt IS NULL")
    List<PromoPhoto> findByPromoIdAndNotDeleted(@Param("promoId") Integer promoId);
    
    @Query("SELECT pp FROM PromoPhoto pp WHERE pp.promo.id IN :promoIds AND pp.deletedAt IS NULL")
    List<PromoPhoto> findByPromoIdsAndNotDeleted(@Param("promoIds") List<Integer> promoIds);
    
    @Query("SELECT pp FROM PromoPhoto pp WHERE pp.promo.id = :promoId AND pp.imageUrl = :imageUrl AND pp.deletedAt IS NULL")
    PromoPhoto findByPromoIdAndImageUrlAndNotDeleted(@Param("promoId") Integer promoId, @Param("imageUrl") String imageUrl);

    @Modifying
    @Query(value = "UPDATE promo_photo SET uploader_user_id = -1 WHERE uploader_user_id = :userId", nativeQuery = true)
    int anonymizeByUserId(@Param("userId") Integer userId);
}
