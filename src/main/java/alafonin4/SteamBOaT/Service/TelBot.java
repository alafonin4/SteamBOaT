package alafonin4.SteamBOaT.Service;

import alafonin4.SteamBOaT.Button;
import alafonin4.SteamBOaT.Entity.Order;
import alafonin4.SteamBOaT.Entity.PaymentMethod;
import alafonin4.SteamBOaT.Entity.User;
import alafonin4.SteamBOaT.KeyboardMarkupBuilder;
import alafonin4.SteamBOaT.Repository.UserRepository;
import alafonin4.SteamBOaT.config.BotConfig;
import com.vdurmont.emoji.EmojiParser;
import org.antlr.v4.runtime.misc.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TelBot extends TelegramLongPollingBot {

    private static final String ADD_FUNDS = EmojiParser.parseToUnicode(":money_with_wings:" + "Пополнить баланс");
    private static final String HISTORY = EmojiParser.parseToUnicode(":calendar:" + "История пополнений");
    private static final String FEEDBACK = EmojiParser.parseToUnicode(":speech_balloon:" + "Отзыв на бота");
    private static final String SUPPORT = EmojiParser.parseToUnicode(":sos:" + "Поддержка");
    private static final String AGREEMENT = EmojiParser.parseToUnicode(":page_facing_up:" + "Оферта");
    @Autowired
    private UserRepository userRepository;
    BotConfig config;
    Map<Long, Integer> currentInds;
    Map<Long, Order> currentOrder;
    Map<Long, Boolean> isName;
    Map<Long, Boolean> isDigit;
    @Autowired
    OrderService orderService;

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
        listOfCommands.add(new BotCommand("/support", "Поддержка"));
        listOfCommands.add(new BotCommand("/feedback", "Отзыв на бот"));
        listOfCommands.add(new BotCommand("/agreement", "Оферта"));

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

            if (messageText.equals(ADD_FUNDS)) {
                updateBalance(chatId);
            } else if (messageText.equals(HISTORY)) {
                currentInds.put(chatId, 0);
                sendOrderInfo(chatId, 0);
            } else if (messageText.equals(FEEDBACK)) {
                feedback(chatId);
            } else if (messageText.equals(SUPPORT)) {
                support(chatId);
            } else if (messageText.equals(AGREEMENT)) {
                sendFile(chatId);
            } else {
            switch (messageText) {
                case "/start":
                    registerUser(update.getMessage());
                    sendMessage(chatId, EmojiParser.parseToUnicode("Добро пожаловать!\n" +
                            "Для пополнения баланса используйте соответствующую кнопку. " +
                            "Если в процессе пополнения возникнут вопросы, пишите в поддержку.\n" +
                            "Если кнопки скрыты, используйте значок \uD83C\uDF9B рядом с клавиатурой, чтобы их открыть."));
                    break;
                case "/add_funds":
                    updateBalance(chatId);
                    break;
                case "/agreement":
                    sendFile(chatId);
                    break;
                case "/history":
                    currentInds.put(chatId, 0);
                    sendOrderInfo(chatId, 0);
                    break;
                case "/support":
                    support(chatId);
                    break;
                case "/feedback":
                    feedback(chatId);
                    break;
                default:
                    if (isName.get(chatId) && !isDigit.get(chatId) && !messageText.startsWith("/")) {
                        isSteamIdCorrect(chatId, messageText);
                        break;
                    } else if (!isName.get(chatId) && isDigit.get(chatId) && !messageText.startsWith("/")) {
                        isSumCorrect(chatId, messageText);
                        break;
                    }
                    sendMessage(chatId, "Извините, команда не распознана.");
                    break;
            }
            }
        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long messageId = update.getCallbackQuery().getMessage().getMessageId();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            switch (callbackData) {
                case "Previous Page":
                    sendPreviousPage(chatId, messageId, currentInds.get(chatId));
                    currentInds.put(chatId, currentInds.get(chatId) - 3);
                    break;
                case "Next Page":
                    sendNextPage(chatId, messageId, currentInds.get(chatId));
                    currentInds.put(chatId, currentInds.get(chatId) + 3);
                    break;
                case "Confirm":
                    goToEnterSumLevelFromConfirmationSteamId(chatId, messageId);
                    break;
                case "250rubles":
                    currentOrder.get(chatId).setSum(250d);
                    enterSumFromKeyboard(chatId, messageId, 250);
                    ConfirmationsAmountOfReplenishment(chatId);
                    break;
                case "500rubles":
                    currentOrder.get(chatId).setSum(500d);
                    enterSumFromKeyboard(chatId, messageId, 500);
                    ConfirmationsAmountOfReplenishment(chatId);
                    break;
                case "1000rubles":
                    currentOrder.get(chatId).setSum(1000d);
                    enterSumFromKeyboard(chatId, messageId, 1000);
                    ConfirmationsAmountOfReplenishment(chatId);
                    break;
                case "No id":
                    goBackToEnterSteamIdLevel(chatId, messageId);
                    break;
                case "Cancel by steamId":
                    cancelCurrentOrderAtSteamIdRequestLevel(chatId, messageId);
                    break;
                case "Yes":
                    goToChoosingPaymentMethodLevelFromConfirmationAmountOfReplenishment(chatId, messageId);
                    choosePaymentMethod(chatId);
                    break;
                case "No number":
                    goBackToEnterSumLevel(chatId, messageId);
                    break;
                case "Cancel by sum":
                    cancelCurrentOrderAtSumRequestLevel(chatId, messageId);
                    break;
                case "Bank card":
                    setBankCard(chatId, messageId);
                    break;
                case "PPS":
                    setPPS(chatId, messageId);
                    break;
                case "Cancel by choose method":
                    orderService.delete(currentOrder.get(chatId));
                    cancelCurrentOrderAtChoosingPaymentMethodRequestLevel(chatId, messageId);
                    break;
                case "Pay":
                    PayCurrentOrder(chatId, messageId);
                    break;
                case "Back":
                    goBackToChoosingPaymentMethodLevel(chatId, messageId);
                    orderService.saveInDataBase(currentOrder.get(chatId));
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

    public void enterSumFromKeyboard(long chatId, long messageId, double sum) {
        BigDecimal re = new BigDecimal(sum * 1.05);
        re = re.setScale(2, RoundingMode.DOWN);
        currentOrder.get(chatId).setSumWithCommission(Double.parseDouble(String.valueOf(re)));
        isName.put(chatId, false);
        isDigit.put(chatId, false);
        EditMessageText cancelSumText = new EditMessageText();
        cancelSumText.setChatId(String.valueOf(chatId));
        String str1 = "Введите сумму пополнения или выберите один из популярных вариантов";
        cancelSumText.setText(str1);
        cancelSumText.setMessageId((int) messageId);

        try {
            execute(cancelSumText);
        } catch (TelegramApiException e) {
        }
    }
    private void isSteamIdCorrect(long chatId, String messageText) {
        Pattern pattern = Pattern.compile("^[A-Za-z0-9_]{3,32}$");
        Matcher matcher = pattern.matcher(messageText);
        Boolean isMatch = matcher.find();
        if (!isMatch) {
            sendMessage(chatId, "Вы ввели некорректный логин Steam.\n" +
                    "Он должен состоять из английских букв, цифр или знака подчеркивания" +
                    " и быть длиной от 3 до 32 символов.");
            return;
        }
        currentOrder.get(chatId).setSteamId(messageText);
        isName.put(chatId, false);
        isDigit.put(chatId, true);
        ConfirmationsSteamId(chatId);
    }
    private void isSumCorrect(long chatId, String messageText) {
        String input = messageText.replace(",", ".");
        double result;
        try {
            result = Double.parseDouble(input);
        } catch (Exception e) {
            sendMessage(chatId, "Вы ввели не число.");
            return;
        }

        double c = result;
        if (c < 0) {
            sendMessage(chatId, "Вы ввели число не доступное для зачисления");
            return;
        } else if (c < 100) {
            sendMessage(chatId, "Минимальная сумма зачисления 100 рублей");
            return;
        }
        BigDecimal r = new BigDecimal(c);
        r = r.setScale(2, RoundingMode.DOWN);
        currentOrder.get(chatId).setSum(Double.parseDouble(String.valueOf(r)));

        BigDecimal re = new BigDecimal(c * 1.05);
        re = re.setScale(2, RoundingMode.DOWN);
        currentOrder.get(chatId).setSumWithCommission(Double.parseDouble(String.valueOf(re)));
        isName.put(chatId, false);
        isDigit.put(chatId, false);

        ConfirmationsAmountOfReplenishment(chatId);
    }
    private void sendPreviousPage(long chatId, long messageId, Integer current) {
        int prevOrderIndex = current - 3;

        EditMessageText messageText = new EditMessageText();
        Pair<String, Integer> p = orderService.getThreeOrders(chatId, prevOrderIndex);

        messageText.setChatId(String.valueOf(chatId));
        messageText.setText(p.a);
        messageText.setReplyMarkup(KeyboardMarkupBuilder.setKeyboardForHistory(prevOrderIndex, p.b));
        messageText.setMessageId((int) messageId);


        try {
            execute(messageText);
        } catch (TelegramApiException e) {
        }
    }
    private void sendNextPage(long chatId, long messageId, Integer current) {
        int nextOrderIndex = current + 3;

        EditMessageText messText = new EditMessageText();
        Pair<String, Integer> pair = orderService.getThreeOrders(chatId, nextOrderIndex);

        messText.setChatId(String.valueOf(chatId));
        messText.setText(pair.a);
        messText.setReplyMarkup(KeyboardMarkupBuilder.setKeyboardForHistory(nextOrderIndex, pair.b));
        messText.setMessageId((int) messageId);

        try {
            execute(messText);
        } catch (TelegramApiException e) {
        }
    }
    private void setBankCard(long chatId, long messageId) {
        currentOrder.get(chatId).setMethod(PaymentMethod.BankCard);
        EditMessageText mText = new EditMessageText();
        mText.setChatId(String.valueOf(chatId));
        mText.setText("Вы точно хотите оплатить?");
        Button payButton = new Button("Оплатить", "Pay");
        Button backButton = new Button("Назад", "Back");
        List<Button> buttons = new ArrayList<>();
        buttons.add(payButton);
        buttons.add(backButton);
        mText.setReplyMarkup(KeyboardMarkupBuilder.setKeyboard(buttons));
        mText.setMessageId((int) messageId);

        try {
            execute(mText);
        } catch (TelegramApiException e) {
        }
        orderService.saveInDataBase(currentOrder.get(chatId));
    }
    private void setPPS(long chatId, long messageId) {
        currentOrder.get(chatId).setMethod(PaymentMethod.PPS);
        EditMessageText mTet = new EditMessageText();
        mTet.setChatId(String.valueOf(chatId));
        mTet.setText("Вы точно хотите оплатить?");
        Button payButton = new Button("Оплатить", "Pay");
        Button backButton = new Button("Назад", "Back");
        List<Button> buttons = new ArrayList<>();
        buttons.add(payButton);
        buttons.add(backButton);
        mTet.setReplyMarkup(KeyboardMarkupBuilder.setKeyboard(buttons));
        mTet.setMessageId((int) messageId);

        try {
            execute(mTet);
        } catch (TelegramApiException e) {
        }
        orderService.saveInDataBase(currentOrder.get(chatId));
    }
    private void cancelCurrentOrderAtSteamIdRequestLevel(long chatId, long messageId) {
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
        sendMessage(chatId, "Вы отменили пополнение");
        isName.put(chatId, false);
        isDigit.put(chatId, false);
    }
    private void cancelCurrentOrderAtSumRequestLevel(long chatId, long messageId) {
        EditMessageText cancelSumText = new EditMessageText();
        cancelSumText.setChatId(String.valueOf(chatId));
        String str1 = "Подтвердите оплату:\n" +
                "Информация по оплате\n\nПополнение STEAM \n\n" +
                " Логин: " + currentOrder.get(chatId).getSteamId() + "\n" +
                " Сумма оплаты: " + currentOrder.get(chatId).getSumWithCommission() + "\u20BD\n" +
                " Сумма пополнения: " + currentOrder.get(chatId).getSum() + "\u20BD";
        cancelSumText.setText(str1);
        cancelSumText.setMessageId((int) messageId);

        try {
            execute(cancelSumText);
        } catch (TelegramApiException e) {
        }
        sendMessage(chatId, "Вы отменили пополнение");
        isName.put(chatId, false);
        isDigit.put(chatId, false);
    }
    private void cancelCurrentOrderAtChoosingPaymentMethodRequestLevel(long chatId, long messageId) {
        EditMessageText cancelMethodText = new EditMessageText();
        cancelMethodText.setChatId(String.valueOf(chatId));
        cancelMethodText.setText("Выберите вариант оплаты:");
        cancelMethodText.setMessageId((int) messageId);

        try {
            execute(cancelMethodText);
        } catch (TelegramApiException e) {
        }
        sendMessage(chatId, "Вы отменили пополнение");
        isName.put(chatId, false);
        isDigit.put(chatId, false);
    }
    private void goBackToChoosingPaymentMethodLevel(long chatId, long messageId) {
        EditMessageText backText = new EditMessageText();
        backText.setChatId(String.valueOf(chatId));
        backText.setText("Выберите вариант оплаты:");
        Button bankButton = new Button("Банковская карта", "Bank card");
        Button PPSButton = new Button("СБП", "PPS");
        Button cancelButton = new Button("Отменить пополнение", "Cancel by choose method");
        List<Button> buttons = new ArrayList<>();
        buttons.add(bankButton);
        buttons.add(PPSButton);
        buttons.add(cancelButton);
        backText.setReplyMarkup(KeyboardMarkupBuilder.setKeyboard(buttons));
        backText.setMessageId((int) messageId);

        try {
            execute(backText);
        } catch (TelegramApiException e) {
        }
    }
    private void goBackToEnterSteamIdLevel(long chatId, long messageId) {
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
    }
    private void goBackToEnterSumLevel(long chatId, long messageId) {
        EditMessageText noNumberText = new EditMessageText();
        noNumberText.setChatId(String.valueOf(chatId));
        String str6 = "Подтвердите оплату:\n" +
                "Информация по оплате\n\nПополнение STEAM \n\n" +
                " Логин: " + currentOrder.get(chatId).getSteamId() + "\n" +
                " Сумма оплаты: " + currentOrder.get(chatId).getSumWithCommission() + "\u20BD\n" +
                " Сумма пополнения: " + currentOrder.get(chatId).getSum() + "\u20BD";
        noNumberText.setText(str6);
        noNumberText.setMessageId((int) messageId);

        try {
            execute(noNumberText);
        } catch (TelegramApiException e) {
        }

        sendMessage(chatId, "Введите сумму пополнения или выберите один из популярных вариантов:");
        isName.put(chatId, false);
        isDigit.put(chatId, true);
    }
    private void goToEnterSumLevelFromConfirmationSteamId(long chatId, long messageId) {
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

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Введите сумму пополнения или выберите один из популярных вариантов:");
        Button bankButton = new Button("250\u20BD", "250rubles");
        Button PPSButton = new Button("500\u20BD", "500rubles");
        Button cancelButton = new Button("1000\u20BD", "1000rubles");
        List<Button> buttons = new ArrayList<>();
        buttons.add(bankButton);
        buttons.add(PPSButton);
        buttons.add(cancelButton);
        message.setReplyMarkup(KeyboardMarkupBuilder.setKeyboard(buttons));

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    private void goToChoosingPaymentMethodLevelFromConfirmationAmountOfReplenishment(long chatId, long messageId) {
        EditMessageText yesText = new EditMessageText();
        yesText.setChatId(String.valueOf(chatId));
        BigDecimal r = new BigDecimal(currentOrder.get(chatId).getSum());
        r = r.setScale(2, RoundingMode.DOWN);
        String str = "Подтвердите оплату:\n" +
                "Информация по оплате\n\nПополнение STEAM \n\n" +
                " Логин: " + currentOrder.get(chatId).getSteamId() + "\n" +
                " Сумма оплаты: " + currentOrder.get(chatId).getSumWithCommission() + "\u20BD\n" +
                " Сумма пополнения: " + Double.parseDouble(String.valueOf(r)) + "\u20BD";
        yesText.setText(str);
        yesText.setMessageId((int) messageId);

        try {
            execute(yesText);
        } catch (TelegramApiException e) {
        }
    }
    private void PayCurrentOrder(long chatId, long messageId) {
        EditMessageText payText = new EditMessageText();
        payText.setChatId(String.valueOf(chatId));
        payText.setText("Транзакция проведена успешно! В течении 5-10 минут баланс будет пополнен.");
        payText.setMessageId((int) messageId);

        try {
            execute(payText);
        } catch (TelegramApiException e) {
        }
        currentOrder.get(chatId).setStatus(true);
        orderService.saveInDataBase(currentOrder.get(chatId));
    }
    private void support(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Нажмите на кнопку, чтобы перейти на чат с поддержкой:");
        Button chatButton = new Button("Чат с поддержкой", "Chat", "https://t.me/alafonin4");
        List<Button> buttons = new ArrayList<>();
        buttons.add(chatButton);
        message.setReplyMarkup(KeyboardMarkupBuilder.setKeyboard(buttons));
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    private void feedback(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Нажмите на кнопку, чтобы пройти опрос про пользование бота:");
        Button chatButton = new Button("Отзыв на бота", "Feedback",
                "https://docs.google.com/forms/d/e/1FAIpQLSfLAWTncu_RwefxJI24X0jXotqKPCQZFFvcNbswfbVHZxPQ7w/viewform?usp=sharing");
        List<Button> buttons = new ArrayList<>();
        buttons.add(chatButton);
        message.setReplyMarkup(KeyboardMarkupBuilder.setKeyboard(buttons));
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    private void sendFile(Long chatId) {
        SendDocument sendDocument = new SendDocument();
        sendDocument.setChatId(String.valueOf(chatId));
        File file = new File("D:\\Univercity\\3_курс\\2_семестр\\Майнор\\Политика_в отношении_обработки_персональных_данных.pdf");
        InputFile inputFile = new InputFile(file);
        sendDocument.setDocument(inputFile);
        try {
            execute(sendDocument);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void ConfirmationsSteamId(Long chatId) {

        String str = "Подтвердите логин Steam:\n" +
                "Логин STEAM: " + currentOrder.get(chatId).getSteamId();
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(str);
        Button yesButton = new Button("Да", "Confirm");
        Button noButton = new Button("Нет", "No id");
        Button cancelButton = new Button("Отменить пополнение", "Cancel by steamId");
        List<Button> buttons = new ArrayList<>();
        buttons.add(yesButton);
        buttons.add(noButton);
        buttons.add(cancelButton);
        InlineKeyboardMarkup markup = KeyboardMarkupBuilder.setKeyboard(buttons);
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    private void ConfirmationsAmountOfReplenishment(Long chatId) {

        String str = "Подтвердите оплату:\n" +
                "Информация по оплате\n\nПополнение STEAM \n\n" +
                " Логин: " + currentOrder.get(chatId).getSteamId() + "\n" +
                " Сумма оплаты: " + currentOrder.get(chatId).getSumWithCommission() + "\u20BD\n" +
                " Сумма пополнения: " + currentOrder.get(chatId).getSum() + "\u20BD";
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(str);
        Button yesButton = new Button("Да", "Yes");
        Button noButton = new Button("Нет", "No number");
        Button cancelButton = new Button("Отменить пополнение", "Cancel by sum");
        List<Button> buttons = new ArrayList<>();
        buttons.add(yesButton);
        buttons.add(noButton);
        buttons.add(cancelButton);
        InlineKeyboardMarkup markup = KeyboardMarkupBuilder.setKeyboard(buttons);
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    private void choosePaymentMethod(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Выберите вариант оплаты:");
        Button bankButton = new Button("Банковская карта", "Bank card");
        Button PPSButton = new Button("СБП", "PPS");
        Button cancelButton = new Button("Отменить пополнение", "Cancel by choose method");
        List<Button> buttons = new ArrayList<>();
        buttons.add(bankButton);
        buttons.add(PPSButton);
        buttons.add(cancelButton);
        InlineKeyboardMarkup marup = KeyboardMarkupBuilder.setKeyboard(buttons);
        message.setReplyMarkup(marup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    public void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);
        message.enableHtml(true);
        message.setDisableWebPagePreview(true);

        message.setReplyMarkup(KeyboardMarkupBuilder.setReplyKeyboard());
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
            sendMessage(chatId, EmojiParser.parseToUnicode("Введите ваш логин Steam\n\n" +
                    ":warning::warning::warning::warning:\n" +
                    "Обратите внимание на следующее: логин в Steam - это информация, которую вы указываете при входе в Steam. " +
                    "Указав неверные данные, денежные средства поступят на баланс другому пользователю. " +
                    "Посмотреть логин вы можете <a href=\"https://store.steampowered.com/account/\">здесь</a>."));
            isName.put(chatId, true);
            isDigit.put(chatId, false);
        }
    }
    private void sendOrderInfo(long chatId, int orderIndex) {
        Pair<String, Integer> orders = orderService.getThreeOrders(chatId, orderIndex);
        if (orders.a.equals("")) {
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText("Вы не создали ещё ни единого пополнения!");
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

        InlineKeyboardMarkup markup = KeyboardMarkupBuilder.setKeyboardForHistory(orderIndex, orders.b);
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
