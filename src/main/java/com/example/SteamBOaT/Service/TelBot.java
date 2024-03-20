package alafonin4.SteamBOaT.Service;

import alafonin4.SteamBOaT.Entity.Order;
import alafonin4.SteamBOaT.Entity.PaymentMethod;
import alafonin4.SteamBOaT.Entity.User;
import alafonin4.SteamBOaT.Repository.OrderRepository;
import alafonin4.SteamBOaT.Repository.UserRepository;
import alafonin4.SteamBOaT.config.BotConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;

@Service
public class TelBot extends TelegramLongPollingBot {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private OrderRepository orderRepository;
    BotConfig config;
    Map<Long, Integer> currentInds;
    Map<Long, Order> currentOrder;
    Map<Long, Boolean> isName;
    Map<Long, Boolean> isDigit;


    public TelBot(BotConfig config) {
        this.config = config;
        this.currentInds = new HashMap<>();
        this.currentOrder = new HashMap<>();
        this.isName = new HashMap<>();
        this.isDigit = new HashMap<>();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            long chatId = update.getMessage().getChatId();
            String messageText = update.getMessage().getText();

            switch (messageText) {
                case "/start":
                    registerUser(update.getMessage());
                    sendMessage(chatId, "Welcome to the bot! Use /top_up to add funds or /rep_history to view your orders.");
                    break;
                case "/top_up":
                    updateBalance(chatId);
                    break;
                case "/rep_history":
                    sendOrderInfo(chatId, 0); // Display the first order
                    break;
                default:
                    if (isName.get(chatId) && !isDigit.get(chatId)) {
                        currentOrder.get(chatId).setSteamId(messageText);
                        isName.put(chatId, false);
                        isDigit.put(chatId, true);
                        ConfirmationsSteamId(chatId);
                        break;
                    } else if (!isName.get(chatId) && isDigit.get(chatId)) {
                        int c = Integer.parseInt(messageText);
                        if (c < 0) {
                            sendMessage(chatId, "Вы ввели число не доступное для зачисления");
                            break;
                        } else if (c < 100) {
                            sendMessage(chatId, "Минимальная сумма зачисления 100 рублей");
                            break;
                        }
                        currentOrder.get(chatId).setSum(c);
                        currentOrder.get(chatId).setSumWithCommission(c * 1.05);
                        isName.put(chatId, false);
                        isDigit.put(chatId, false);

                        ConfirmationsAmountOfReplenishment(chatId);
                        break;
                    }
                    sendMessage(chatId, "Sorry, command was not recognized");
            }
        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long messageId = update.getCallbackQuery().getMessage().getMessageId();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            switch (callbackData) {
                case "Previous Page":
                    int prevOrderIndex = currentInds.get(chatId) - 3;
                    sendOrderInfo(chatId, prevOrderIndex);
                    break;
                case "Next Page":
                    int nextOrderIndex = currentInds.get(chatId) + 3;
                    sendOrderInfo(chatId, nextOrderIndex);
                    break;
                case "Yes":
                    choosePaymentMethod(chatId);
                    break;
                case "Confirm":
                    sendMessage(chatId, "Введите сумму пополнения");
                    break;
                case "Cancel":
                    sendMessage(chatId, "Вы отменили заказ");
                    isName.put(chatId, false);
                    isDigit.put(chatId, false);
                    break;
                case "Pay":
                    sendMessage(chatId, "Вы успешно оплатили заказ");
                    currentOrder.get(chatId).setStatus(true);
                    break;
                case "Bank card":
                    currentOrder.get(chatId).setMethod(PaymentMethod.BankCard);
                    sendToPay(chatId);
                    saveInDataBase(chatId);
                    break;
                case "No id":
                    sendMessage(chatId, "Введите ваш логин Steam");
                    isName.put(chatId, true);
                    isDigit.put(chatId, false);
                    break;
                case "No number":
                    sendMessage(chatId, "Введите сумму пополнения");
                    isName.put(chatId, false);
                    isDigit.put(chatId, true);
                    break;
                case "PPS":
                    currentOrder.get(chatId).setMethod(PaymentMethod.PPS);
                    sendToPay(chatId);
                    saveInDataBase(chatId);
                    break;
                default:
                    sendMessage(chatId, "Sorry, command was not recognized");
            }
        }
    }
    public void registerUser(Message msg) {
        if(userRepository.findById(msg.getChatId()).isEmpty()){

            var chatId = msg.getChatId();
            var chat = msg.getChat();

            User user = new User();

            user.setChatId(chatId);
            user.setName(chat.getFirstName());
            user.setUserName(chat.getUserName());

            userRepository.save(user);
            currentInds.put(chatId, 0);
            isName.put(chatId, false);
            isDigit.put(chatId, false);
        }
    }

    private InlineKeyboardMarkup SetKeyboardToPay() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();

        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();

        var prButton = new InlineKeyboardButton();
        prButton.setText("Оплатить");
        prButton.setCallbackData("Pay");
        row.add(prButton);

        rowList.add(row);
        markup.setKeyboard(rowList);
        return markup;
    }

    private void sendToPay(Long chatId) {

        String str = "Нажмите на кнопку:";
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(str);
        InlineKeyboardMarkup markup = SetKeyboardToPay();
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    private InlineKeyboardMarkup SetKeyboardForConfirmationSteamId() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();

        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();

        var prButton = new InlineKeyboardButton();
        prButton.setText("Да");
        prButton.setCallbackData("Confirm");
        row.add(prButton);

        var pButton = new InlineKeyboardButton();
        pButton.setText("Нет");
        pButton.setCallbackData("No id");
        row.add(pButton);

        var nButton = new InlineKeyboardButton();
        nButton.setText("Отменить заказ");
        nButton.setCallbackData("Cancel");
        row.add(nButton);

        rowList.add(row);
        markup.setKeyboard(rowList);
        return markup;
    }
    private void ConfirmationsSteamId(Long chatId) {

        String str = "Подтвердите логин Steam:\n" +
                "Логин STEAM: " + currentOrder.get(chatId).getSteamId();
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(str);
        InlineKeyboardMarkup markup = SetKeyboardForConfirmationSteamId();
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private InlineKeyboardMarkup SetKeyboardForConfirmationsAmountOfReplenishment() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();

        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();

        var prButton = new InlineKeyboardButton();
        prButton.setText("Да");
        prButton.setCallbackData("Yes");
        row.add(prButton);

        var nButton = new InlineKeyboardButton();
        nButton.setText("Нет");
        nButton.setCallbackData("No number");
        row.add(nButton);

        var button = new InlineKeyboardButton();
        button.setText("Отменить заказ");
        button.setCallbackData("Cancel");
        row.add(button);

        rowList.add(row);
        markup.setKeyboard(rowList);
        return markup;
    }

    private void ConfirmationsAmountOfReplenishment(Long chatId) {

        String str = "Подтвердите оплату:\n" +
                "Информация по оплате\n\nПополнение STEAM \n\n" +
                " Логин: " + currentOrder.get(chatId).getSteamId() + "\n" +
                " Сумма оплаты: " + currentOrder.get(chatId).getSumWithCommission() + "\n" +
                " Сумма пополнения: " + currentOrder.get(chatId).getSum();
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(str);
        InlineKeyboardMarkup markup = SetKeyboardForConfirmationsAmountOfReplenishment();
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private InlineKeyboardMarkup SetKeyboardForChoosingPaymentMethod() {
        InlineKeyboardMarkup marup = new InlineKeyboardMarkup();

        List<List<InlineKeyboardButton>> rList = new ArrayList<>();
        List<InlineKeyboardButton> r = new ArrayList<>();

        var pButton = new InlineKeyboardButton();
        pButton.setText("Банковская карта");
        pButton.setCallbackData("Bank card");
        r.add(pButton);

        var button = new InlineKeyboardButton();
        button.setText("СБП");
        button.setCallbackData("PPS");
        r.add(button);

        rList.add(r);
        marup.setKeyboard(rList);
        return marup;
    }
    private void choosePaymentMethod(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Выберите вариант оплаты:");
        InlineKeyboardMarkup marup = SetKeyboardForChoosingPaymentMethod();
        message.setReplyMarkup(marup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    private void saveInDataBase(Long chatId) {
        orderRepository.save(currentOrder.get(chatId));
    }
    public void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void updateBalance(long chatId) {
        currentOrder.put(chatId, new Order());
        Optional<User> u = userRepository.findById(chatId);
        if (u.isPresent()){
            User ur = u.get();
            currentOrder.get(chatId).setUser(ur);
            currentOrder.get(chatId).setStatus(false);
            sendMessage(chatId, "Введите ваш логин Steam");
            isName.put(chatId, true);
            isDigit.put(chatId, false);
        }
    }

    private InlineKeyboardMarkup SetKeyboardForHistory(int orderIndex, int size) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();

        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();

        var prButton = new InlineKeyboardButton();
        prButton.setText("Previous Page");
        prButton.setCallbackData("Previous Page");

        var nButton = new InlineKeyboardButton();
        nButton.setText("Next Page");
        nButton.setCallbackData("Next Page");

        if (orderIndex - 3 + 1 > 0) {
            row.add(prButton);
        }

        if (orderIndex + 3 - 1 < size - 1) {
            row.add(nButton);
        }

        rowList.add(row);
        markup.setKeyboard(rowList);
        return markup;
    }

    private void sendOrderInfo(long chatId, int orderIndex) {
        currentInds.put(chatId, orderIndex);
        List<Order> orders = orderRepository.findByUser_ChatIdOrderByCreatedAtDesc(chatId);
        if (orders.isEmpty()) {
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText("Вы не создали ещё ни одного заказа!");
            try {
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
            return;
        }

        StringBuilder ors = new StringBuilder();
        for (int i = orderIndex; i < Math.min(orderIndex + 3, orders.size()); i++) {
            ors.append(orders.get(i) + "\n\n");
        }

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(ors.toString());

        InlineKeyboardMarkup markup = SetKeyboardForHistory(orderIndex, orders.size());
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getBotUsername() {
        return this.config.getBotName();
    }

    @Override
    public String getBotToken() {
        return this.config.getToken();
    }
}
