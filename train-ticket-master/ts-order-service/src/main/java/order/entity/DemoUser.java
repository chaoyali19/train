package order.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.ToString;

import javax.persistence.*;
import java.util.Date;

/**
 * Demo User Entity
 * @author fdse
 */
@Data
@Table(name = "demo_users")
@Entity
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class DemoUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 100)
    private String username;

    @Column(length = 100)
    private String email;

    @Column(length = 20)
    private String phone;

    private Integer age;

    @Column(name = "created_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    public DemoUser() {
        this.createdAt = new Date();
    }

    public DemoUser(String username, String email, String phone, Integer age) {
        this.username = username;
        this.email = email;
        this.phone = phone;
        this.age = age;
        this.createdAt = new Date();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        DemoUser other = (DemoUser) obj;
        return id.equals(other.getId())
                && username.equals(other.getUsername())
                && email.equals(other.getEmail())
                && phone.equals(other.getPhone())
                && age.equals(other.getAge());
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + (id == null ? 0 : id.hashCode());
        return result;
    }
} 