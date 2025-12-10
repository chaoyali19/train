package order.controller;

import edu.fudan.common.util.Response;
import order.entity.DemoUser;
import order.service.DemoUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.ResponseEntity.ok;

/**
 * Demo User Controller
 * @author fdse
 */
@RestController
@RequestMapping("/api/v1/demouserservice")
@CrossOrigin(origins = "*")
public class DemoUserController {

    @Autowired
    private DemoUserService demoUserService;

    private static final Logger LOGGER = LoggerFactory.getLogger(DemoUserController.class);

    @GetMapping(path = "/welcome")
    public String home() {
        return "Welcome to [ Demo User Service ] !";
    }

    /**
     * 获取所有用户
     */
    @GetMapping(path = "/users")
    public HttpEntity getAllUsers(@RequestHeader HttpHeaders headers) {
        LOGGER.info("[getAllUsers][Get All Users]");
        return ok(demoUserService.getAllUsers(headers));
    }

    /**
     * 根据ID获取用户
     */
    @GetMapping(path = "/users/{id}")
    public HttpEntity getUserById(@PathVariable Integer id, @RequestHeader HttpHeaders headers) {
        LOGGER.info("[getUserById][Get User By Id][Id: {}]", id);
        return ok(demoUserService.getUserById(id, headers));
    }

    /**
     * 根据用户名获取用户
     */
    @GetMapping(path = "/users/username/{username}")
    public HttpEntity getUserByUsername(@PathVariable String username, @RequestHeader HttpHeaders headers) {
        LOGGER.info("[getUserByUsername][Get User By Username][Username: {}]", username);
        return ok(demoUserService.getUserByUsername(username, headers));
    }

    /**
     * 根据邮箱获取用户
     */
    @GetMapping(path = "/users/email/{email}")
    public HttpEntity getUserByEmail(@PathVariable String email, @RequestHeader HttpHeaders headers) {
        LOGGER.info("[getUserByEmail][Get User By Email][Email: {}]", email);
        return ok(demoUserService.getUserByEmail(email, headers));
    }

    /**
     * 根据手机号获取用户
     */
    @GetMapping(path = "/users/phone/{phone}")
    public HttpEntity getUserByPhone(@PathVariable String phone, @RequestHeader HttpHeaders headers) {
        LOGGER.info("[getUserByPhone][Get User By Phone][Phone: {}]", phone);
        return ok(demoUserService.getUserByPhone(phone, headers));
    }

    /**
     * 根据年龄获取用户
     */
    @GetMapping(path = "/users/age/{age}")
    public HttpEntity getUsersByAge(@PathVariable Integer age, @RequestHeader HttpHeaders headers) {
        LOGGER.info("[getUsersByAge][Get Users By Age][Age: {}]", age);
        return ok(demoUserService.getUsersByAge(age, headers));
    }

    /**
     * 根据年龄范围获取用户
     */
    @GetMapping(path = "/users/age")
    public HttpEntity getUsersByAgeRange(@RequestParam Integer minAge, 
                                        @RequestParam Integer maxAge, 
                                        @RequestHeader HttpHeaders headers) {
        LOGGER.info("[getUsersByAgeRange][Get Users By Age Range][MinAge: {}, MaxAge: {}]", minAge, maxAge);
        return ok(demoUserService.getUsersByAgeRange(minAge, maxAge, headers));
    }

    /**
     * 根据用户名模糊搜索
     */
    @GetMapping(path = "/users/search/username")
    public HttpEntity searchUsersByUsername(@RequestParam String username, @RequestHeader HttpHeaders headers) {
        LOGGER.info("[searchUsersByUsername][Search Users By Username][Username: {}]", username);
        return ok(demoUserService.searchUsersByUsername(username, headers));
    }

    /**
     * 根据邮箱模糊搜索
     */
    @GetMapping(path = "/users/search/email")
    public HttpEntity searchUsersByEmail(@RequestParam String email, @RequestHeader HttpHeaders headers) {
        LOGGER.info("[searchUsersByEmail][Search Users By Email][Email: {}]", email);
        return ok(demoUserService.searchUsersByEmail(email, headers));
    }

    /**
     * 根据多个条件过滤查询用户
     */
    @GetMapping(path = "/users/filter")
    public HttpEntity getUsersWithFilters(@RequestParam(required = false) String username,
                                         @RequestParam(required = false) String email,
                                         @RequestParam(required = false) String phone,
                                         @RequestParam(required = false) Integer minAge,
                                         @RequestParam(required = false) Integer maxAge,
                                         @RequestHeader HttpHeaders headers) {
        LOGGER.info("[getUsersWithFilters][Get Users With Filters][Username: {}, Email: {}, Phone: {}, MinAge: {}, MaxAge: {}]", 
                   username, email, phone, minAge, maxAge);
        return ok(demoUserService.getUsersWithFilters(username, email, phone, minAge, maxAge, headers));
    }

    /**
     * 统计年龄范围内的用户数量
     */
    @GetMapping(path = "/users/count/age")
    public HttpEntity countUsersByAgeRange(@RequestParam Integer minAge, 
                                          @RequestParam Integer maxAge, 
                                          @RequestHeader HttpHeaders headers) {
        LOGGER.info("[countUsersByAgeRange][Count Users By Age Range][MinAge: {}, MaxAge: {}]", minAge, maxAge);
        return ok(demoUserService.countUsersByAgeRange(minAge, maxAge, headers));
    }

    /**
     * 根据邮箱和用户名获取用户
     */
    @GetMapping(path = "/users/email-username")
    public HttpEntity getUserByEmailAndUsername(@RequestParam String email, 
                                               @RequestParam String username, 
                                               @RequestHeader HttpHeaders headers) {
        LOGGER.info("[getUserByEmailAndUsername][Get User By Email And Username][Email: {}, Username: {}]", email, username);
        return ok(demoUserService.getUserByEmailAndUsername(email, username, headers));
    }

    /**
     * 创建新用户
     */
    @PostMapping(path = "/users")
    public HttpEntity createUser(@RequestBody DemoUser user, @RequestHeader HttpHeaders headers) {
        LOGGER.info("[createUser][Create User][Username: {}]", user.getUsername());
        return ok(demoUserService.createUser(user, headers));
    }

    /**
     * 更新用户信息
     */
    @PutMapping(path = "/users")
    public HttpEntity updateUser(@RequestBody DemoUser user, @RequestHeader HttpHeaders headers) {
        LOGGER.info("[updateUser][Update User][Id: {}]", user.getId());
        return ok(demoUserService.updateUser(user, headers));
    }

    /**
     * 删除用户
     */
    @DeleteMapping(path = "/users/{id}")
    public HttpEntity deleteUser(@PathVariable Integer id, @RequestHeader HttpHeaders headers) {
        LOGGER.info("[deleteUser][Delete User][Id: {}]", id);
        return ok(demoUserService.deleteUser(id, headers));
    }

    /**
     * 获取按创建时间排序的前20000条用户记录
     */
    @GetMapping(path = "/users/top20000")
    public HttpEntity getTop20000UsersByCreatedAt(@RequestHeader HttpHeaders headers) {
        LOGGER.info("[getTop20000UsersByCreatedAt][Get Top 20000 Users By Created At]");
        return ok(demoUserService.getTop20000UsersByCreatedAt(headers));
    }

    /**
     * 获取按创建时间排序的前N条用户记录
     */
    @GetMapping(path = "/users/top")
    public HttpEntity getTopNUsersByCreatedAt(@RequestParam(defaultValue = "20000") int limit, 
                                             @RequestHeader HttpHeaders headers) {
        LOGGER.info("[getTopNUsersByCreatedAt][Get Top {} Users By Created At]", limit);
        return ok(demoUserService.getTopNUsersByCreatedAt(limit, headers));
    }

    /**
     * 根据用户名模糊查询并限制数量
     */
    @GetMapping(path = "/users/search/username/limit")
    public HttpEntity getUsersByUsernameContainingWithLimit(@RequestParam String username,
                                                           @RequestParam(defaultValue = "20000") int limit,
                                                           @RequestHeader HttpHeaders headers) {
        LOGGER.info("[getUsersByUsernameContainingWithLimit][Get Users By Username Containing][Username: {}, Limit: {}]", username, limit);
        return ok(demoUserService.getUsersByUsernameContainingWithLimit(username, limit, headers));
    }

    /**
     * 慢查询1：使用LOWER函数包装字段
     */
    @GetMapping(path = "/users/search/username/limit/slow")
    public HttpEntity getUsersByUsernameContainingWithLimitSlow(@RequestParam String username,
                                                               @RequestParam(defaultValue = "20000") int limit,
                                                               @RequestHeader HttpHeaders headers) {
        LOGGER.info("[getUsersByUsernameContainingWithLimitSlow][Get Users By Username Containing Slow][Username: {}, Limit: {}]", username, limit);
        return ok(demoUserService.getUsersByUsernameContainingWithLimitSlow(username, limit, headers));
    }

    /**
     * 慢查询2：使用OR条件增加复杂度
     */
    @GetMapping(path = "/users/search/username/limit/slower")
    public HttpEntity getUsersByUsernameContainingWithLimitSlower(@RequestParam String username,
                                                                 @RequestParam(defaultValue = "20000") int limit,
                                                                 @RequestHeader HttpHeaders headers) {
        LOGGER.info("[getUsersByUsernameContainingWithLimitSlower][Get Users By Username Containing Slower][Username: {}, Limit: {}]", username, limit);
        return ok(demoUserService.getUsersByUsernameContainingWithLimitSlower(username, limit, headers));
    }

    /**
     * 慢查询3：使用子查询
     */
    @GetMapping(path = "/users/search/username/limit/slowest")
    public HttpEntity getUsersByUsernameContainingWithLimitSlowest(@RequestParam String username,
                                                                  @RequestParam(defaultValue = "20000") int limit,
                                                                  @RequestHeader HttpHeaders headers) {
        LOGGER.info("[getUsersByUsernameContainingWithLimitSlowest][Get Users By Username Containing Slowest][Username: {}, Limit: {}]", username, limit);
        return ok(demoUserService.getUsersByUsernameContainingWithLimitSlowest(username, limit, headers));
    }
} 