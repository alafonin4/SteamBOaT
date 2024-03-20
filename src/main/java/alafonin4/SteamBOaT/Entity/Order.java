package alafonin4.SteamBOaT.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity(name = "_order")
@Getter
@Setter
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "chatId")
    private User user;

    @Column(name = "steam_id", nullable = false)
    private String steamId;

    @Column(name = "status", nullable = false)
    private Boolean status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "sum", nullable = false)
    private Double sum;

    @Column(name = "sum_with_commission", nullable = false)
    private Double sumWithCommission;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 50)
    private PaymentMethod method;

    @Override
    public String toString() {
        String s = status ? "Оплачен" : "Не оплачен";
        //String m = m
        return "Id заказа: " + Id + "\n" +
                "Логин Steam: " + steamId + "\n" +
                "Статус заказа: " + s + "\n" +
                "Сумма с комиссией: " + sumWithCommission + "\n" +
                "Метод оплаты: " + method;
    }
}
