package alafonin4.SteamBOaT.Repository;

import alafonin4.SteamBOaT.Entity.Order;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends CrudRepository<Order, Long> {
    List<Order> findByUser_ChatId(Long chatId);
    List<Order> findByUser_ChatIdOrderByCreatedAtDesc(Long chatId);
}
