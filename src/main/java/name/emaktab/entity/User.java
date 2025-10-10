package name.emaktab.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity(name = "users")
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String telegramId;
    private String username;
    private String password;
    @Column(columnDefinition = "text")
    private String cookie;
    private LocalDateTime cookieExpiry;
    private LocalDateTime lastLogin;
    private String lastLoginMessage;

}
