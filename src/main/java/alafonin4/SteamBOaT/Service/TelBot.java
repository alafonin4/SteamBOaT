package alafonin4.SteamBOaT.Service;

import alafonin4.SteamBOaT.Entity.Order;
import alafonin4.SteamBOaT.Entity.PaymentMethod;
import alafonin4.SteamBOaT.Entity.User;
import alafonin4.SteamBOaT.Repository.OrderRepository;
import alafonin4.SteamBOaT.Repository.UserRepository;
import alafonin4.SteamBOaT.config.BotConfig;
import org.antlr.v4.runtime.misc.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "Перезагрузка бота"));
        listOfCommands.add(new BotCommand("/add_funds", "Пополнение баланса"));
        listOfCommands.add(new BotCommand("/history", "История пополнений"));
        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            long chatId = update.getMessage().getChatId();
            String messageText = update.getMessage().getText();

            switch (messageText) {
                case "/start":
                    registerUser(update.getMessage());
                    sendMessage(chatId, "Добро пожаловать! Используйте команду /add_funds для пополнения баланса " +
                            "или команду /history для просмотра истории ваших пополнений.");
                    break;
                case "/add_funds":
                    updateBalance(chatId);
                    break;
                case "/history":
                    sendOrderInfo(chatId, 0); // Display the first order
                    break;
                default:
                    if (isName.get(chatId) && !isDigit.get(chatId) && !messageText.startsWith("/")) {
                        Pattern pattern = Pattern.compile("^[A-Za-z0-9_]{3,32}$");
                        Matcher matcher = pattern.matcher(messageText);
                        Boolean isMatch = matcher.find();
                        if (!isMatch) {
                            sendMessage(chatId, "Вы ввели не корректный логин Steam.\n" +
                                    "Он должен состоять из английских букв, цифр или знака подчеркивания" +
                                    " и быть длиной от 3 до 32 символов.");
                            break;
                        }
                        currentOrder.get(chatId).setSteamId(messageText);
                        isName.put(chatId, false);
                        isDigit.put(chatId, true);
                        ConfirmationsSteamId(chatId);
                        break;
                    } else if (!isName.get(chatId) && isDigit.get(chatId) && !messageText.startsWith("/")) {

                        String input = messageText.replace(",", ".");
                        double result;
                        try {
                            result = Double.parseDouble(input);
                        } catch (Exception e) {
                            sendMessage(chatId, "Вы ввели не число.");
                            break;
                        }

                        double c = result;
                        if (c < 0) {
                            sendMessage(chatId, "Вы ввели число не доступное для зачисления");
                            break;
                        } else if (c < 100) {
                            sendMessage(chatId, "Минимальная сумма зачисления 100 рублей");
                            break;
                        }
                        BigDecimal r = new BigDecimal(c);
                        r = r.setScale(2, RoundingMode.DOWN);
                        System.out.println(Double.parseDouble(String.valueOf(r)));
                        currentOrder.get(chatId).setSum(Double.parseDouble(String.valueOf(r)));

                        BigDecimal re = new BigDecimal(c * 1.05);
                        re = re.setScale(2, RoundingMode.DOWN);
                        currentOrder.get(chatId).setSumWithCommission(Double.parseDouble(String.valueOf(re)));
                        isName.put(chatId, false);
                        isDigit.put(chatId, false);

                        ConfirmationsAmountOfReplenishment(chatId);
                        break;
                    }
                    sendMessage(chatId, "Извините, команда не распознана.");
                    break;
            }
        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long messageId = update.getCallbackQuery().getMessage().getMessageId();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            switch (callbackData) {
                case "Previous Page":
                    int prevOrderIndex = currentInds.get(chatId) - 3;

                    EditMessageText messageText = new EditMessageText();
                    Pair<String, Integer> p = getThreeOrders(chatId, prevOrderIndex);

                    messageText.setChatId(String.valueOf(chatId));
                    messageText.setText(p.a);
                    messageText.setReplyMarkup(SetKeyboardForHistory(currentInds.get(chatId), p.b));
                    messageText.setMessageId((int) messageId);

                    try {
                        execute(messageText);
                    } catch (TelegramApiException e) {
                    }

                    break;
                case "Next Page":
                    int nextOrderIndex = currentInds.get(chatId) + 3;

                    EditMessageText messText = new EditMessageText();
                    Pair<String, Integer> pair = getThreeOrders(chatId, nextOrderIndex);

                    messText.setChatId(String.valueOf(chatId));
                    messText.setText(pair.a);
                    messText.setReplyMarkup(SetKeyboardForHistory(currentInds.get(chatId), pair.b));
                    messText.setMessageId((int) messageId);

                    try {
                        execute(messText);
                    } catch (TelegramApiException e) {
                    }
                    break;
                case "Yes":
                    EditMessageText yesText = new EditMessageText();
                    yesText.setChatId(String.valueOf(chatId));
                    BigDecimal r = new BigDecimal(currentOrder.get(chatId).getSum());
                    r = r.setScale(2, RoundingMode.DOWN);
                    String str = "Подтвердите оплату:\n" +
                            "Информация по оплате\n\nПополнение STEAM \n\n" +
                            " Логин: " + currentOrder.get(chatId).getSteamId() + "\n" +
                            " Сумма оплаты: " + currentOrder.get(chatId).getSumWithCommission() + "\n" +
                            " Сумма пополнения: " + Double.parseDouble(String.valueOf(r));
                    yesText.setText(str);
                    yesText.setMessageId((int) messageId);

                    try {
                        execute(yesText);
                    } catch (TelegramApiException e) {
                    }
                    choosePaymentMethod(chatId);
                    break;
                case "Confirm":
                    EditMessageText confirmText = new EditMessageText();
                    confirmText.setChatId(String.valueOf(chatId));
                    String str2 = "Подтвердите логин Steam:\n" +
                            "Логин STEAM: " + currentOrder.get(chatId).getSteamId();
                    confirmText.setText(str2);
                    confirmText.setMessageId((int) messageId);

                    try {
                        execute(confirmText);
                    } catch (TelegramApiException e) {
                    }
                    sendMessage(chatId, "Введите сумму пополнения");
                    break;
                case "Cancel by steamId":
                    EditMessageText cancelIdText = new EditMessageText();
                    cancelIdText.setChatId(String.valueOf(chatId));
                    String str5 = "Подтвердите логин Steam:\n" +
                            "Логин STEAM: " + currentOrder.get(chatId).getSteamId();
                    cancelIdText.setText(str5);
                    cancelIdText.setMessageId((int) messageId);

                    try {
                        execute(cancelIdText);
                    } catch (TelegramApiException e) {
                    }
                    sendMessage(chatId, "Вы отменили заказ");
                    isName.put(chatId, false);
                    isDigit.put(chatId, false);
                    break;
                case "Cancel by sum":
                    EditMessageText cancelSumText = new EditMessageText();
                    cancelSumText.setChatId(String.valueOf(chatId));
                    String str1 = "Подтвердите оплату:\n" +
                            "Информация по оплате\n\nПополнение STEAM \n\n" +
                            " Логин: " + currentOrder.get(chatId).getSteamId() + "\n" +
                            " Сумма оплаты: " + currentOrder.get(chatId).getSumWithCommission() + "\n" +
                            " Сумма пополнения: " + currentOrder.get(chatId).getSum();
                    cancelSumText.setText(str1);
                    cancelSumText.setMessageId((int) messageId);

                    try {
                        execute(cancelSumText);
                    } catch (TelegramApiException e) {
                    }
                    sendMessage(chatId, "Вы отменили заказ");
                    isName.put(chatId, false);
                    isDigit.put(chatId, false);
                    break;
                case "Cancel by choose method":
                    EditMessageText cancelMethodText = new EditMessageText();
                    cancelMethodText.setChatId(String.valueOf(chatId));
                    cancelMethodText.setText("Выберите вариант оплаты:");
                    cancelMethodText.setMessageId((int) messageId);

                    try {
                        execute(cancelMethodText);
                    } catch (TelegramApiException e) {
                    }
                    sendMessage(chatId, "Вы отменили заказ");
                    isName.put(chatId, false);
                    isDigit.put(chatId, false);
                    break;
                case "Pay":
                    EditMessageText payText = new EditMessageText();
                    payText.setChatId(String.valueOf(chatId));
                    payText.setText("Вы успешно оплатили заказ");
                    payText.setMessageId((int) messageId);

                    try {
                        execute(payText);
                    } catch (TelegramApiException e) {
                    }
                    currentOrder.get(chatId).setStatus(true);
                    break;
                case "Bank card":
                    currentOrder.get(chatId).setMethod(PaymentMethod.BankCard);
                    EditMessageText mText = new EditMessageText();
                    mText.setChatId(String.valueOf(chatId));
                    mText.setText("Вы точно хотите оплатить?");
                    mText.setReplyMarkup(SetKeyboardToPay());
                    mText.setMessageId((int) messageId);

                    try {
                        execute(mText);
                    } catch (TelegramApiException e) {
                    }
                    saveInDataBase(chatId);
                    break;
                case "No id":
                    EditMessageText noIdText = new EditMessageText();
                    noIdText.setChatId(String.valueOf(chatId));
                    String str4 = "Подтвердите логин Steam:\n" +
                            "Логин STEAM: " + currentOrder.get(chatId).getSteamId();
                    noIdText.setText(str4);
                    noIdText.setMessageId((int) messageId);

                    try {
                        execute(noIdText);
                    } catch (TelegramApiException e) {
                    }
                    sendMessage(chatId, "Введите ваш логин Steam");
                    isName.put(chatId, true);
                    isDigit.put(chatId, false);
                    break;
                case "No number":
                    EditMessageText noNumberText = new EditMessageText();
                    noNumberText.setChatId(String.valueOf(chatId));
                    String str6 = "Подтвердите оплату:\n" +
                            "Информация по оплате\n\nПополнение STEAM \n\n" +
                            " Логин: " + currentOrder.get(chatId).getSteamId() + "\n" +
                            " Сумма оплаты: " + currentOrder.get(chatId).getSumWithCommission() + "\n" +
                            " Сумма пополнения: " + currentOrder.get(chatId).getSum();
                    noNumberText.setText(str6);
                    noNumberText.setMessageId((int) messageId);

                    try {
                        execute(noNumberText);
                    } catch (TelegramApiException e) {
                    }

                    sendMessage(chatId, "Введите сумму пополнения");
                    isName.put(chatId, false);
                    isDigit.put(chatId, true);
                    break;
                case "PPS":
                    currentOrder.get(chatId).setMethod(PaymentMethod.PPS);
                    EditMessageText mTet = new EditMessageText();
                    mTet.setChatId(String.valueOf(chatId));
                    mTet.setText("Вы точно хотите оплатить?");
                    mTet.setReplyMarkup(SetKeyboardToPay());
                    mTet.setMessageId((int) messageId);

                    try {
                        execute(mTet);
                    } catch (TelegramApiException e) {
                    }
                    saveInDataBase(chatId);
                    break;
                case "Back":
                    EditMessageText backText = new EditMessageText();
                    backText.setChatId(String.valueOf(chatId));
                    backText.setText("Выберите вариант оплаты:");
                    backText.setReplyMarkup(SetKeyboardForChoosingPaymentMethod());
                    backText.setMessageId((int) messageId);

                    try {
                        execute(backText);
                    } catch (TelegramApiException e) {
                    }
                    saveInDataBase(chatId);
                    break;
                default:
                    sendMessage(chatId, "Извините, команда не распознана.");
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

        var rButton = new InlineKeyboardButton();
        rButton.setText("Назад");
        rButton.setCallbackData("Back");
        row.add(rButton);

        rowList.add(row);
        markup.setKeyboard(rowList);
        return markup;
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
        nButton.setCallbackData("Cancel by steamId");
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
        button.setCallbackData("Cancel by sum");
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

        var nButton = new InlineKeyboardButton();
        nButton.setText("Отменить заказ");
        nButton.setCallbackData("Cancel by choose method");
        r.add(nButton);

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
    private Pair<String, Integer> getThreeOrders(long chatId, int orderIndex) {
        currentInds.put(chatId, orderIndex);
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

    private void sendOrderInfo(long chatId, int orderIndex) {
        Pair<String, Integer> orders = getThreeOrders(chatId, orderIndex);
        if (orders.a.equals("")) {
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

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(orders.a);

        InlineKeyboardMarkup markup = SetKeyboardForHistory(orderIndex, orders.b);
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
