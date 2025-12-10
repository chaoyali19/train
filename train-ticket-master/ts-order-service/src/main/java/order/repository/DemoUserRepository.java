package order.repository;

import order.entity.DemoUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Demo User Repository
 * @author fdse
 */
@Repository
public interface DemoUserRepository extends JpaRepository<DemoUser, Integer> {

    @Override
    Optional<DemoUser> findById(Integer id);

    @Override
    List<DemoUser> findAll();

    // 根据用户名查询
    List<DemoUser> findByUsername(String username);

    // 根据邮箱查询
    Optional<DemoUser> findByEmail(String email);

    // 根据手机号查询
    Optional<DemoUser> findByPhone(String phone);

    // 根据年龄查询
    List<DemoUser> findByAge(Integer age);

    // 根据年龄范围查询
    List<DemoUser> findByAgeBetween(Integer minAge, Integer maxAge);

    // 根据用户名模糊查询
    List<DemoUser> findByUsernameContaining(String username);

    // 根据邮箱模糊查询
    List<DemoUser> findByEmailContaining(String email);

    // 根据创建时间查询
    List<DemoUser> findByCreatedAtBetween(Date startDate, Date endDate);

    // 按创建时间升序排序查询所有用户
    List<DemoUser> findAllByOrderByCreatedAtAsc();

    // 按创建时间降序排序查询所有用户
    List<DemoUser> findAllByOrderByCreatedAtDesc();

    // 自定义SQL查询：按created_at排序，限制20000条记录
    @Query(value = "SELECT * FROM demo_users ORDER BY created_at ASC LIMIT 20000", nativeQuery = true)
    List<DemoUser> findTop20000ByOrderByCreatedAtAsc();

    // 自定义SQL查询：按created_at排序，限制指定数量条记录
    @Query(value = "SELECT * FROM demo_users ORDER BY created_at ASC LIMIT :limit", nativeQuery = true)
    List<DemoUser> findTopNByOrderByCreatedAtAsc(@Param("limit") int limit);

    // 自定义SQL查询：按用户名模糊查询，限制指定数量条记录
    @Query(value = "SELECT * FROM demo_users WHERE username LIKE %:username% ORDER BY created_at ASC LIMIT :limit", nativeQuery = true)
    List<DemoUser> findByUsernameContainingWithLimit(@Param("username") String username, @Param("limit") int limit);

    // 低效查询：使用函数包装字段，避免索引使用
    @Query(value = "SELECT * FROM demo_users WHERE LOWER(username) LIKE LOWER(%:username%) ORDER BY created_at ASC LIMIT :limit", nativeQuery = true)
    List<DemoUser> findByUsernameContainingWithLimitSlow(@Param("username") String username, @Param("limit") int limit);

    // 更慢的查询：使用OR条件增加查询复杂度
    @Query(value = "SELECT * FROM demo_users WHERE (username LIKE %:username% OR email LIKE %:username% OR phone LIKE %:username%) ORDER BY created_at ASC LIMIT :limit", nativeQuery = true)
    List<DemoUser> findByUsernameContainingWithLimitSlower(@Param("username") String username, @Param("limit") int limit);

    // 最慢的查询：使用子查询和复杂的条件
    @Query(value = "SELECT * FROM demo_users WHERE id IN (SELECT id FROM demo_users WHERE username LIKE %:username%) ORDER BY created_at ASC LIMIT :limit", nativeQuery = true)
    List<DemoUser> findByUsernameContainingWithLimitSlowest(@Param("username") String username, @Param("limit") int limit);

    // 自定义查询：根据多个条件查询
    @Query("SELECT u FROM DemoUser u WHERE " +
           "(:username IS NULL OR u.username = :username) " +
           "AND (:email IS NULL OR u.email = :email) " +
           "AND (:phone IS NULL OR u.phone = :phone) " +
           "AND (:minAge IS NULL OR u.age >= :minAge) " +
           "AND (:maxAge IS NULL OR u.age <= :maxAge)")
    List<DemoUser> findUsersWithFilters(
            @Param("username") String username,
            @Param("email") String email,
            @Param("phone") String phone,
            @Param("minAge") Integer minAge,
            @Param("maxAge") Integer maxAge);

    // 统计用户数量
    @Query("SELECT COUNT(u) FROM DemoUser u WHERE u.age >= :minAge AND u.age <= :maxAge")
    long countUsersByAgeRange(@Param("minAge") Integer minAge, @Param("maxAge") Integer maxAge);

    // 根据邮箱和用户名查询
    Optional<DemoUser> findByEmailAndUsername(String email, String username);

    @Override
    void deleteById(Integer id);
} 