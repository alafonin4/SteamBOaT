package alafonin4.SteamBOaT.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity(name = "_user")
@Getter
@Setter
public class User {
    @Id
    @Column(name = "chatId", nullable = false)
    private Long chatId;

    @Column(name = "firstName", nullable = false)
    private String name;

    @Column(name = "user_name", nullable = false, unique = true)
    private String userName;

}
