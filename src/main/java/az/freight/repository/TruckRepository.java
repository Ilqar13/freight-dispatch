package az.freight.repository;

import az.freight.entity.Truck;
import az.freight.enums.TruckStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TruckRepository extends JpaRepository<Truck, Long> {

    Page<Truck> findByStatus(TruckStatus status, Pageable pageable);

    Optional<Truck> findByPlate(String plate);

    boolean existsByVin(String vin);

    /** PostGIS ilə təyinat nöqtəsinə ən yaxın boş və uyğun maşınlar (metrlə məsafə üzrə sıralanır). */
    @Query(value = """
            SELECT t.*
            FROM truck t
            WHERE t.status = 'FREE'
              AND t.type IN (:types)
              AND t.capacity_tons >= :tons
            ORDER BY CAST(t.geom AS geography)
                     <-> CAST(ST_SetSRID(ST_MakePoint(:lon, :lat), 4326) AS geography)
            LIMIT :limit
            """, nativeQuery = true)
    List<Truck> findNearestFree(@Param("types") Collection<String> types,
                                @Param("tons") double tons,
                                @Param("lon") double lon,
                                @Param("lat") double lat,
                                @Param("limit") int limit);

    /**
     * Əvəzedici maşın axtarışı: nasazlıq nöqtəsinə ən yaxın boş və uyğun maşınlar, sətirlər kilidlənmiş şəkildə.
     * SKIP LOCKED: başqa əməliyyatın artıq kilidlədiyi maşın atlanır, ona görə eyni maşını iki tərəf götürə bilməz.
     */
    @Query(value = """
            SELECT t.*
            FROM truck t
            WHERE t.status = 'FREE'
              AND t.type IN (:types)
              AND t.capacity_tons >= :tons
            ORDER BY CAST(t.geom AS geography)
                     <-> CAST(ST_SetSRID(ST_MakePoint(:lon, :lat), 4326) AS geography)
            LIMIT :limit
            FOR UPDATE OF t SKIP LOCKED
            """, nativeQuery = true)
    List<Truck> findNearestFreeForUpdate(@Param("types") Collection<String> types,
                                         @Param("tons") double tons,
                                         @Param("lon") double lon,
                                         @Param("lat") double lat,
                                         @Param("limit") int limit);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Truck t where t.id = :id")
    Optional<Truck> findForUpdate(@Param("id") Long id);
}
