package order.service;

import edu.fudan.common.util.Response;
import order.entity.DemoUser;
import org.springframework.http.HttpHeaders;

/**
 * Demo User Service Interface
 * @author fdse
 */
public interface DemoUserService {

    Response getAllUsers(HttpHeaders headers);

    Response getUserById(Integer id, HttpHeaders headers);

    Response getUserByUsername(String username, HttpHeaders headers);

    Response getUserByEmail(String email, HttpHeaders headers);

    Response getUserByPhone(String phone, HttpHeaders headers);

    Response getUsersByAge(Integer age, HttpHeaders headers);

    Response getUsersByAgeRange(Integer minAge, Integer maxAge, HttpHeaders headers);

    Response searchUsersByUsername(String username, HttpHeaders headers);

    Response searchUsersByEmail(String email, HttpHeaders headers);

    Response getUsersWithFilters(String username, String email, String phone, 
                                Integer minAge, Integer maxAge, HttpHeaders headers);

    Response countUsersByAgeRange(Integer minAge, Integer maxAge, HttpHeaders headers);

    Response createUser(DemoUser user, HttpHeaders headers);

    Response updateUser(DemoUser user, HttpHeaders headers);

    Response deleteUser(Integer id, HttpHeaders headers);

    Response getUserByEmailAndUsername(String email, String username, HttpHeaders headers);

    Response getTop20000UsersByCreatedAt(HttpHeaders headers);

    Response getTopNUsersByCreatedAt(int limit, HttpHeaders headers);

    Response getUsersByUsernameContainingWithLimit(String username, int limit, HttpHeaders headers);

    // 慢查询方法
    Response getUsersByUsernameContainingWithLimitSlow(String username, int limit, HttpHeaders headers);
    Response getUsersByUsernameContainingWithLimitSlower(String username, int limit, HttpHeaders headers);
    Response getUsersByUsernameContainingWithLimitSlowest(String username, int limit, HttpHeaders headers);
} 