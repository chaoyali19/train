package order.service;

import edu.fudan.common.util.Response;
import order.entity.DemoUser;
import order.repository.DemoUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Demo User Service Implementation
 * @author fdse
 */
@Service
public class DemoUserServiceImpl implements DemoUserService {

    @Autowired
    private DemoUserRepository demoUserRepository;

    @Value("${demo.users.query.limit:20000}")
    private int demoUsersQueryLimit;

    private static volatile String queryLimit = "10000";

    private static final Logger LOGGER = LoggerFactory.getLogger(DemoUserServiceImpl.class);

    // 获取 queryLimit 的 long 值
    private long getQueryLimitAsLong() {
        try {
            return Long.parseLong(queryLimit);
        } catch (NumberFormatException e) {
            LOGGER.warn("[getQueryLimitAsLong][Invalid queryLimit value: {}], using default: 10000", queryLimit);
            return 10000L;
        }
    }


    @Override
    public Response getAllUsers(HttpHeaders headers) {
        LOGGER.info("[getAllUsers][Get All Users]");
        try {
            List<DemoUser> users = demoUserRepository.findAll();
            return new Response(1, "Success", users);
        } catch (Exception e) {
            LOGGER.error("[getAllUsers][Error]", e);
            return new Response(0, "Failed to get all users: " + e.getMessage(), null);
        }
    }

    @Override
    public Response getUserById(Integer id, HttpHeaders headers) {
        LOGGER.info("[getUserById][Get User By Id][Id: {}]", id);
        try {
            Optional<DemoUser> user = demoUserRepository.findById(id);
            if (user.isPresent()) {
                return new Response(1, "Success", user.get());
            } else {
                return new Response(0, "User not found", null);
            }
        } catch (Exception e) {
            LOGGER.error("[getUserById][Error]", e);
            return new Response(0, "Failed to get user: " + e.getMessage(), null);
        }
    }

    @Override
    public Response getUserByUsername(String username, HttpHeaders headers) {
        LOGGER.info("[getUserByUsername][Get User By Username][Username: {}]", username);
        try {
            List<DemoUser> users = demoUserRepository.findByUsername(username);
            if (!users.isEmpty()) {
                return new Response(1, "Success", users);
            } else {
                return new Response(0, "No users found with username: " + username, null);
            }
        } catch (Exception e) {
            LOGGER.error("[getUserByUsername][Error]", e);
            return new Response(0, "Failed to get user by username: " + e.getMessage(), null);
        }
    }

    @Override
    public Response getUserByEmail(String email, HttpHeaders headers) {
        LOGGER.info("[getUserByEmail][Get User By Email][Email: {}]", email);
        try {
            Optional<DemoUser> user = demoUserRepository.findByEmail(email);
            if (user.isPresent()) {
                return new Response(1, "Success", user.get());
            } else {
                return new Response(0, "User not found with email: " + email, null);
            }
        } catch (Exception e) {
            LOGGER.error("[getUserByEmail][Error]", e);
            return new Response(0, "Failed to get user by email: " + e.getMessage(), null);
        }
    }

    @Override
    public Response getUserByPhone(String phone, HttpHeaders headers) {
        LOGGER.info("[getUserByPhone][Get User By Phone][Phone: {}]", phone);
        try {
            Optional<DemoUser> user = demoUserRepository.findByPhone(phone);
            if (user.isPresent()) {
                return new Response(1, "Success", user.get());
            } else {
                return new Response(0, "User not found with phone: " + phone, null);
            }
        } catch (Exception e) {
            LOGGER.error("[getUserByPhone][Error]", e);
            return new Response(0, "Failed to get user by phone: " + e.getMessage(), null);
        }
    }

    @Override
    public Response getUsersByAge(Integer age, HttpHeaders headers) {
        LOGGER.info("[getUsersByAge][Get Users By Age][Age: {}]", age);
        try {
            List<DemoUser> users = demoUserRepository.findByAge(age);
            if (!users.isEmpty()) {
                return new Response(1, "Success", users);
            } else {
                return new Response(0, "No users found with age: " + age, null);
            }
        } catch (Exception e) {
            LOGGER.error("[getUsersByAge][Error]", e);
            return new Response(0, "Failed to get users by age: " + e.getMessage(), null);
        }
    }

    @Override
    public Response getUsersByAgeRange(Integer minAge, Integer maxAge, HttpHeaders headers) {
        LOGGER.info("[getUsersByAgeRange][Get Users By Age Range][MinAge: {}, MaxAge: {}]", minAge, maxAge);
        try {
            List<DemoUser> users = demoUserRepository.findByAgeBetween(minAge, maxAge);
            if (!users.isEmpty()) {
                return new Response(1, "Success", users);
            } else {
                return new Response(0, "No users found in age range: " + minAge + "-" + maxAge, null);
            }
        } catch (Exception e) {
            LOGGER.error("[getUsersByAgeRange][Error]", e);
            return new Response(0, "Failed to get users by age range: " + e.getMessage(), null);
        }
    }

    @Override
    public Response searchUsersByUsername(String username, HttpHeaders headers) {
        LOGGER.info("[searchUsersByUsername][Search Users By Username][Username: {}]", username);
        try {
            List<DemoUser> users = demoUserRepository.findByUsernameContaining(username);
            if (!users.isEmpty()) {
                return new Response(1, "Success", users);
            } else {
                return new Response(0, "No users found containing username: " + username, null);
            }
        } catch (Exception e) {
            LOGGER.error("[searchUsersByUsername][Error]", e);
            return new Response(0, "Failed to search users by username: " + e.getMessage(), null);
        }
    }

    @Override
    public Response searchUsersByEmail(String email, HttpHeaders headers) {
        LOGGER.info("[searchUsersByEmail][Search Users By Email][Email: {}]", email);
        try {
            List<DemoUser> users = demoUserRepository.findByEmailContaining(email);
            if (!users.isEmpty()) {
                return new Response(1, "Success", users);
            } else {
                return new Response(0, "No users found containing email: " + email, null);
            }
        } catch (Exception e) {
            LOGGER.error("[searchUsersByEmail][Error]", e);
            return new Response(0, "Failed to search users by email: " + e.getMessage(), null);
        }
    }

    @Override
    public Response getUsersWithFilters(String username, String email, String phone, 
                                      Integer minAge, Integer maxAge, HttpHeaders headers) {
        LOGGER.info("[getUsersWithFilters][Get Users With Filters][Username: {}, Email: {}, Phone: {}, MinAge: {}, MaxAge: {}]", 
                   username, email, phone, minAge, maxAge);
        try {
            List<DemoUser> users = demoUserRepository.findUsersWithFilters(username, email, phone, minAge, maxAge);
            if (!users.isEmpty()) {
                return new Response(1, "Success", users);
            } else {
                return new Response(0, "No users found with the specified filters", null);
            }
        } catch (Exception e) {
            LOGGER.error("[getUsersWithFilters][Error]", e);
            return new Response(0, "Failed to get users with filters: " + e.getMessage(), null);
        }
    }

    @Override
    public Response countUsersByAgeRange(Integer minAge, Integer maxAge, HttpHeaders headers) {
        LOGGER.info("[countUsersByAgeRange][Count Users By Age Range][MinAge: {}, MaxAge: {}]", minAge, maxAge);
        try {
            long count = demoUserRepository.countUsersByAgeRange(minAge, maxAge);
            return new Response(1, "Success", count);
        } catch (Exception e) {
            LOGGER.error("[countUsersByAgeRange][Error]", e);
            return new Response(0, "Failed to count users by age range: " + e.getMessage(), null);
        }
    }

    @Override
    public Response createUser(DemoUser user, HttpHeaders headers) {
        LOGGER.info("[createUser][Create User][Username: {}]", user.getUsername());
        try {
            DemoUser savedUser = demoUserRepository.save(user);
            return new Response(1, "User created successfully", savedUser);
        } catch (Exception e) {
            LOGGER.error("[createUser][Error]", e);
            return new Response(0, "Failed to create user: " + e.getMessage(), null);
        }
    }

    @Override
    public Response updateUser(DemoUser user, HttpHeaders headers) {
        LOGGER.info("[updateUser][Update User][Id: {}]", user.getId());
        try {
            if (demoUserRepository.existsById(user.getId())) {
                DemoUser updatedUser = demoUserRepository.save(user);
                return new Response(1, "User updated successfully", updatedUser);
            } else {
                return new Response(0, "User not found", null);
            }
        } catch (Exception e) {
            LOGGER.error("[updateUser][Error]", e);
            return new Response(0, "Failed to update user: " + e.getMessage(), null);
        }
    }

    @Override
    public Response deleteUser(Integer id, HttpHeaders headers) {
        LOGGER.info("[deleteUser][Delete User][Id: {}]", id);
        try {
            if (demoUserRepository.existsById(id)) {
                demoUserRepository.deleteById(id);
                return new Response(1, "User deleted successfully", null);
            } else {
                return new Response(0, "User not found", null);
            }
        } catch (Exception e) {
            LOGGER.error("[deleteUser][Error]", e);
            return new Response(0, "Failed to delete user: " + e.getMessage(), null);
        }
    }

    @Override
    public Response getUserByEmailAndUsername(String email, String username, HttpHeaders headers) {
        LOGGER.info("[getUserByEmailAndUsername][Get User By Email And Username][Email: {}, Username: {}]", email, username);
        try {
            Optional<DemoUser> user = demoUserRepository.findByEmailAndUsername(email, username);
            if (user.isPresent()) {
                return new Response(1, "Success", user.get());
            } else {
                return new Response(0, "User not found with email: " + email + " and username: " + username, null);
            }
        } catch (Exception e) {
            LOGGER.error("[getUserByEmailAndUsername][Error]", e);
            return new Response(0, "Failed to get user by email and username: " + e.getMessage(), null);
        }
    }

    @Override
    public Response getTop20000UsersByCreatedAt(HttpHeaders headers) {
        LOGGER.info("[getTop20000UsersByCreatedAt][Get Top {} Users By Created At]", demoUsersQueryLimit);
        return getTopNUsersByCreatedAt(demoUsersQueryLimit, headers);
    }

    @Override
    public Response getTopNUsersByCreatedAt(int limit, HttpHeaders headers) {
        LOGGER.info("[getTopNUsersByCreatedAt][Get Top {} Users By Created At]", limit);
        try {
            List<DemoUser> users = demoUserRepository.findTopNByOrderByCreatedAtAsc(limit);
            if (!users.isEmpty()) {
                return new Response(1, "Success", users);
            } else {
                return new Response(0, "No users found", null);
            }
        } catch (Exception e) {
            LOGGER.error("[getTopNUsersByCreatedAt][Error]", e);
            return new Response(0, "Failed to get top " + limit + " users by created at: " + e.getMessage(), null);
        }
    }

    @Override
    public Response getUsersByUsernameContainingWithLimit(String username, int limit, HttpHeaders headers) {
        LOGGER.info("[getUsersByUsernameContainingWithLimit][Get Users By Username Containing][Username: {}, Limit: {}]", username, limit);
        try {
            List<DemoUser> users = demoUserRepository.findByUsernameContainingWithLimit(username, limit);
            if (!users.isEmpty()) {
                return new Response(1, "Success", users);
            } else {
                return new Response(0, "No users found containing username: " + username, null);
            }
        } catch (Exception e) {
            LOGGER.error("[getUsersByUsernameContainingWithLimit][Error]", e);
            return new Response(0, "Failed to get users by username containing: " + e.getMessage(), null);
        }
    }

    @Override
    public Response getUsersByUsernameContainingWithLimitSlow(String username, int limit, HttpHeaders headers) {
        LOGGER.info("[getUsersByUsernameContainingWithLimitSlow][Get Users By Username Containing Slow][Username: {}, Limit: {}]", username, limit);
        try {
            List<DemoUser> users = demoUserRepository.findByUsernameContainingWithLimitSlow(username, limit);
            if (!users.isEmpty()) {
                return new Response(1, "Success (Slow Query)", users);
            } else {
                return new Response(0, "No users found containing username: " + username, null);
            }
        } catch (Exception e) {
            LOGGER.error("[getUsersByUsernameContainingWithLimitSlow][Error]", e);
            return new Response(0, "Failed to get users by username containing (slow): " + e.getMessage(), null);
        }
    }

    @Override
    public Response getUsersByUsernameContainingWithLimitSlower(String username, int limit, HttpHeaders headers) {
        LOGGER.info("[getUsersByUsernameContainingWithLimitSlower][Get Users By Username Containing Slower][Username: {}, Limit: {}]", username, limit);
        try {
            List<DemoUser> users = demoUserRepository.findByUsernameContainingWithLimitSlower(username, limit);
            if (!users.isEmpty()) {
                return new Response(1, "Success (Slower Query)", users);
            } else {
                return new Response(0, "No users found containing username: " + username, null);
            }
        } catch (Exception e) {
            LOGGER.error("[getUsersByUsernameContainingWithLimitSlower][Error]", e);
            return new Response(0, "Failed to get users by username containing (slower): " + e.getMessage(), null);
        }
    }

    @Override
    public Response getUsersByUsernameContainingWithLimitSlowest(String username, int limit, HttpHeaders headers) {
        LOGGER.info("[getUsersByUsernameContainingWithLimitSlowest][Get Users By Username Containing Slowest][Username: {}, Limit: {}]", username, limit);
        try {
            List<DemoUser> users = demoUserRepository.findByUsernameContainingWithLimitSlowest(username, limit);
            if (!users.isEmpty()) {
                return new Response(1, "Success (Slowest Query)", users);
            } else {
                return new Response(0, "No users found containing username: " + username, null);
            }
        } catch (Exception e) {
            LOGGER.error("[getUsersByUsernameContainingWithLimitSlowest][Error]", e);
            return new Response(0, "Failed to get users by username containing (slowest): " + e.getMessage(), null);
        }
    }
} 