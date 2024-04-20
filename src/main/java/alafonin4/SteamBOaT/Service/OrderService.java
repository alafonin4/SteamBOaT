package alafonin4.SteamBOaT.Service;

import alafonin4.SteamBOaT.Entity.Order;
import alafonin4.SteamBOaT.Repository.OrderRepository;
import org.antlr.v4.runtime.misc.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderService {

    @Autowired
    OrderRepository orderRepository;

    public void saveInDataBase(Order order) {
        orderRepository.save(order);
    }
    public void delete(Order order) {
        if (orderRepository.existsById(order.getId())) {
            orderRepository.delete(order);
        }
    }
    public Pair<String, Integer> getThreeOrders(long chatId, int orderIndex) {
        List<Order> orders = orderRepository.findByUser_ChatIdOrderByCreatedAtDesc(chatId);
        if (orders.isEmpty()) {
            return new Pair<>("", 0);
        }

        StringBuilder ors = new StringBuilder();
        for (int i = orderIndex; i < Math.min(orderIndex + 3, orders.size()); i++) {
            ors.append(orders.get(i) + "\n\n");
        }
        return new Pair<>(ors.toString(), orders.size());
    }
}
