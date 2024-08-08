package alafonin4.TolikBot.Service;

import alafonin4.TolikBot.Button;
import alafonin4.TolikBot.Entity.*;
import alafonin4.TolikBot.Entity.User;
import alafonin4.TolikBot.KeyboardMarkupBuilder;
import alafonin4.TolikBot.Repository.*;
import alafonin4.TolikBot.config.BotConfig;
import com.vdurmont.emoji.EmojiParser;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.polls.SendPoll;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class TelBot extends TelegramLongPollingBot {
    private static final String MODERATION = EmojiParser.parseToUnicode(":page_facing_up:" + "модерация");
    private static final String EMPLOYEE = EmojiParser.parseToUnicode(":page_facing_up:" + "управление сотрудниками");
    private static final String CHANGE = EmojiParser.parseToUnicode(":page_facing_up:" + "изменение списка товаров");
    private static final String SENDORDERIMAGE = EmojiParser.parseToUnicode(":page_facing_up:" + "отправить скрин заказа");
    private static final String ASKTOLIC = EmojiParser.parseToUnicode(":page_facing_up:" + "Задать вопрос");
    private static final String PRODUCT = EmojiParser.parseToUnicode(":page_facing_up:" + "Продукты");
    private static final String SENDREVIEWIMAGE = EmojiParser.parseToUnicode(":page_facing_up:" + "отправить скрин отзыва");
    //private static final String CHANGE = EmojiParser.parseToUnicode(":page_facing_up:" + "изменение списка товаров");
    private static final String URL_To_Rules = "https://disk.yandex.ru/d/9g5olrg5l-o-lA";
    private static final String URL_TO_TERMS_OF_USE = "https://disk.yandex.ru/i/Yj_1_zmT3eQtfA";
    private static final String TUSH = "Тушь для ресниц FUTURISTIC удлиняющая стойкая 5G volume эффект";
    private static final String Cream = "Тональный крем для лица SKIN FUTURE матовый";
    private static final String FIX = "Фиксатор макияжа FIXING SPRAY HYDRA увлажняющий тонизирующий спрей ";
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ImageRepository imageRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private QuestionRepository questionRepository;
    @Autowired
    private AnswerRepository answerRepository;
    @Autowired
    private ReservationRepository reservationRepository;
    @Autowired
    private ProductReservationRepository productReservationRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OrderImageRepository orderImageRepository;
    @Autowired
    private ReviewRepository reviewRepository;
    @Autowired
    private ReviewImageRepository reviewImageRepository;
    BotConfig config;
    Map<Long, Integer> currentInds;
    Map<Long, Integer> currentIndInList;
    Map<Long, Integer> curCat;
    Map<Long, Product> curProd;
    List<String> program;
    Map<Long, Boolean> hasInvited;
    Map<Long, String> usersName;
    Map<Long, List<ProductReservation>> currentProdResInOrder;
    Map<Long, List<ProductReservation>> currentProdResInReview;
    Map<Long, Role> usersRole;
    Map<Long, Reservation> reservationToPay;
    Map<Long, List<List<Product>>> curProdToR;
    Map<Long, List<Image>> curImageInOrder;
    Map<Long, List<Image>> curImageInReview;

    public TelBot(BotConfig config) {
        this.config = config;
        this.currentInds = new HashMap<>();
        this.curCat = new HashMap<>();
        this.hasInvited = new HashMap<>();
        this.usersName = new HashMap<>();
        this.usersRole = new HashMap<>();
        this.curProd = new HashMap<>();
        this.currentIndInList = new HashMap<>();
        this.currentProdResInOrder = new HashMap<>();
        this.currentProdResInReview = new HashMap<>();
        this.reservationToPay = new HashMap<>();
        this.curImageInOrder = new HashMap<>();
        this.curImageInReview = new HashMap<>();
        this.curProdToR = new HashMap<>();
        this.program = new ArrayList<>();
        this.program.add("А пока давай я расскажу тебе про программу!");
        this.program.add("Professor SkinGood – это уходовая косметика, вдохновленная лучшими достижениями Кореи. " +
                "В программе ты можешь попробовать 11 классных продуктов, которые Профессор создал специально для тебя.");
        this.program.add("Участвуя в программе, ты можешь попробовать любое количество продуктов! Все очень просто: " +
                "выбери продукт, попробуй его и оставь свой честный отзыв. \n" +
                "Как это работает? \n" +
                "• \uD83D\uDED2 Ты выбираешь продукт\n" +
                "• \uD83E\uDDF4 Делаешь заказ на Wildberries/Ozon и присылаешь мне скриншот, на котором видно " +
                "какой товар ты купила и стоимость покупки. Я забронирую для тебя возмещение\n" +
                "• \uD83D\uDCB0 Когда ты полноценно протестируешь продукт – оставь на него отзыв и пришли мне скрин " +
                "его размещения на сайте. Как только я приму твой отзыв – я отправлю тебе сертификат с возмещением");
        this.program.add("\uD83D\uDCA1 Кстати, если ты уже пробовала какой-то из продуктов-участников и готова " +
                "оставить на него отзыв – отлично. Делать заказ заново не обязательно. " +
                "Поделившись своим мнением, ты будешь участвовать в розыгрыше ежемесячных призов.");
        this.program.add("Самые активные участницы будут ежемесячно получать призы:\n" +
                "\uD83C\uDFC61 место: Один приз – сертификат в OZON на 10 000 рублей\n" +
                "\uD83C\uDFC62 место: Десять призов – сертификаты в OZON на 1000 рублей\n" +
                "\uD83C\uDFC63 место: Десять призов – сертификаты в OZON на 500 рублей\n" +
                "\n" +
                "Пробуй больше продуктов, создавай подробные отзывы и выигрывай призы!");
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.hasPollAnswer()) {
            System.out.println("poll");
            long chatId = update.getMessage().getChatId();
            ThanksForAnswer(chatId);
            sendScreen(chatId);
        }
        if (update.hasMessage() && update.getMessage().hasPhoto()) {
            long chatId = update.getMessage().getChatId();
            User user = userRepository.findById(chatId).get();
            if (user.getRole().equals(Role.Customer)) {
                if (user.getStage().equals(Stage.EnterImageOrder) || user.getStage().equals(Stage.EnterImageReview)) {
                    var photos = update.getMessage().getPhoto();

                    PhotoSize largestPhoto = photos.stream()
                            .max(Comparator.comparingInt(PhotoSize::getFileSize))
                            .orElse(null);
                    if (largestPhoto != null) {
                        String fileId = largestPhoto.getFileId();
                        try {
                            byte[] fileData = downloadFileAsBytes(fileId);
                            saveFileToDatabase(chatId, fileData, chatId,
                                    curCat.get(chatId) == 0 ? Category.Order : Category.Review);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    SendMessage sendMessage = new SendMessage();
                    sendMessage.setChatId(String.valueOf(chatId));
                    StringBuilder text = new StringBuilder();
                    List<List<Button>> buttons = new ArrayList<>();

                    if (user.getStage().equals(Stage.EnterImageOrder)) {
                        if (currentProdResInOrder.get(chatId).size() == 0) {
                            text.append("Вы уже отправили изображения заказа на все бронирования.\n");
                        } else {
                            text.append("Выберете товары присутствующие на скрине:\n");
                            int ind = 1;
                            List<Button> currentRow = new ArrayList<>();
                            for (var pr : currentProdResInOrder.get(chatId)) {
                                Button prButton = new Button(String.valueOf(ind), "order_" + pr.getId());
                                currentRow.add(prButton);
                                if (currentRow.size() == 3) {
                                    buttons.add(new ArrayList<>(currentRow));
                                    currentRow.clear();
                                }
                                text.append(ind).append(" ").append(pr.getProduct().getTitle())
                                        .append(" ").append(pr.getProduct().getShop()).append("\n\n");
                                ind++;
                            }

                            if (!currentRow.isEmpty()) {
                                buttons.add(new ArrayList<>(currentRow));
                                currentRow.clear();
                            }
                        }
                    }

                    if (user.getStage().equals(Stage.EnterImageReview)) {
                        if (currentProdResInReview.get(chatId).size() == 0) {
                            text.append("Вы уже отправили изображения отзывов ко всем заказам.\n");
                        } else {
                            text.append("Выберете товары присутствующие на скрине:\n");
                            int ind = 1;
                            List<Button> currentRow = new ArrayList<>();
                            for (var pr : currentProdResInReview.get(chatId)) {
                                Button prButton = new Button(String.valueOf(ind), "review_" + pr.getId());
                                currentRow.add(prButton);
                                if (currentRow.size() == 3) {
                                    buttons.add(new ArrayList<>(currentRow));
                                    currentRow.clear();
                                }
                                text.append(ind).append(" ").append(pr.getProduct().getTitle())
                                        .append(" ").append(pr.getProduct().getShop()).append("\n\n");
                                ind++;
                            }

                            if (!currentRow.isEmpty()) {
                                buttons.add(new ArrayList<>(currentRow));
                                currentRow.clear();
                            }
                        }
                    }
                    sendMessage.setText(text.toString());
                    InlineKeyboardMarkup markup = KeyboardMarkupBuilder.setKeyboardWithRaw(buttons);
                    sendMessage.setReplyMarkup(markup);
                    try {
                        execute(sendMessage);
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }

                } else {
                    sendMessage(chatId, "⚠️Ой, ты не выбрала никакой команды, отправляя изображение. " +
                            "❗️Пожалуйста, выбери соответствующую команду в меню или кнопку выше, а затем повтори " +
                            "отправку. \n" +
                            "⁉️Нужна помощь человека? Тогда отправь мне в ответ /help.");
                }
            } else {
                sendMessage(chatId, "Извините команда не распознана");
            }
        } else if (update.hasMessage() && update.getMessage().hasText()) {
            long chatId = update.getMessage().getChatId();
            String messageText = update.getMessage().getText();

            User user = new User();
            if (!messageText.equals("/start")) {
                user = userRepository.findById(chatId).get();
            }

            if (messageText.equals("/start")) {
                registerUser(update.getMessage());
                sendMessage(chatId, EmojiParser.parseToUnicode("Привет\uD83D\uDC4B Меня зовут Толик. " +
                        "Я – бот, и помогаю с участием в проекте «Эксперты Professor SkinGood». " +
                        "Здесь ты сможешь попробовать \uD83E\uDDF4уходовую косметику «Professor SkinGood», " +
                        "\uD83D\uDCAC поделиться своим мнением о ней и \uD83C\uDF89 выиграть призы! " +
                        "За твое мнение мы возместим стоимость покупки и каждый месяц будем разыгрывать " +
                        "20+ призов среди участников, в том числе сертификат на 10 000 рублей."));
                sendRulesAndTermsOfUse(chatId);
                return;
            }

            if (user.getRole().equals(Role.Customer)) {
                getTextUpdateFromCustomer(update);
            } else if (user.getRole().equals(Role.Moderator)) {
                getTextUpdateFromModerator(update);
            } else if (user.getRole().equals(Role.Admin)) {
                getTextUpdateFromAdmin(update);
            }
        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            var m = update.getCallbackQuery().getMessage();
            long messageId = update.getCallbackQuery().getMessage().getMessageId();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            User usern = userRepository.findById(chatId).get();
            if (usern.getRole().equals(Role.Customer)) {
                getCallBackQueryUpdateFromCustomer(update);
            } else if (usern.getRole().equals(Role.Moderator)) {
                getCallBackQueryUpdateFromModerator(update);
            } else if (usern.getRole().equals(Role.Admin)) {
                getCallBackQueryUpdateFromAdmin(update);
            }
        }
    }
    public String normalizeUsername(String username) {
        if (username.startsWith("@")) {
            return username.substring(1);
        }
        return username;
    }
    private void getTextUpdateFromCustomer(Update update) {
        long chatId = update.getMessage().getChatId();
        String messageText = update.getMessage().getText();

        User user = new User();
        if (!messageText.equals("/start")) {
            user = userRepository.findById(chatId).get();
        }
        if (messageText.equals(SENDORDERIMAGE)) {
            curCat.put(chatId, 0);
            user.setStage(Stage.EnterImageOrder);
            userRepository.save(user);
            sendScreen(chatId);
        } else if (messageText.equals(SENDREVIEWIMAGE)) {
            curCat.put(chatId, 1);
            user.setStage(Stage.EnterImageReview);
            userRepository.save(user);
            sendReview(chatId);
        } else if (messageText.equals(PRODUCT)) {
            showProductList(chatId);
        } else if (messageText.equals(ASKTOLIC)) {
            AskTolic(chatId);
        } else {
            switch (messageText) {
                case "/start":
                    registerUser(update.getMessage());
                    sendMessage(chatId, EmojiParser.parseToUnicode("Привет\uD83D\uDC4B Меня зовут Толик. " +
                            "Я – бот, и помогаю с участием в проекте «Эксперты Professor SkinGood». " +
                            "Здесь ты сможешь попробовать \uD83E\uDDF4уходовую косметику «Professor SkinGood», " +
                            "\uD83D\uDCAC поделиться своим мнением о ней и \uD83C\uDF89 выиграть призы! " +
                            "За твое мнение мы возместим стоимость покупки и каждый месяц будем разыгрывать " +
                            "20+ призов среди участников, в том числе сертификат на 10 000 рублей."));
                    sendRulesAndTermsOfUse(chatId);
                    break;
                case "/rules":
                    sendRules(chatId);
                    break;
                case "/policy":
                    sendTermsOfUse(chatId);
                    break;
                case "/info":
                    sendInfo(chatId);
                    break;
                case "/product":
                    showProductList(chatId);
                    break;
                case "/review":
                    curCat.put(chatId, 1);
                    user.setStage(Stage.EnterImageReview);
                    userRepository.save(user);
                    sendReview(chatId);
                    break;
                case "/screen":
                    curCat.put(chatId, 0);
                    user.setStage(Stage.EnterImageOrder);
                    userRepository.save(user);
                    sendScreen(chatId);
                    break;
                case "/help":
                    AskTolic(chatId);
                    break;
                default:
                    if (user.getStage().equals(Stage.EnterFirstName) && !messageText.startsWith("/")) {
                        SetUserName(chatId, normalizeUsername(messageText));
                        greatings(chatId);
                        friendInviteYou(chatId);
                        break;
                    } else if (user.getStage().equals(Stage.EnterUserNameOfFriend) && !messageText.startsWith("/")) {

                        SetUserNameOfFriend(chatId, messageText);
                        send(chatId);
                        sendFirstPageOfProgram(chatId);
                        break;
                    } else if (user.getStage().equals(Stage.AskingQuestion) && !messageText.startsWith("/")) {
                        user.setStage(Stage.DoingNothing);
                        userRepository.save(user);
                        if(userRepository.findById(chatId).isPresent()){
                            Question q = new Question();
                            q.setCreatedAt(LocalDateTime.now());
                            q.setStatus(Status.Unseen);
                            q.setQue(messageText);
                            User u = userRepository.findById(chatId).get();
                            q.setUser(u);
                            questionRepository.save(q);
                            sendMessage(chatId, "Я скоро вернусь! Передал твой вопрос человеку!");
                        }
                        break;
                    } else if (user.getStage().equals(Stage.AnsweringQuestion) && !messageText.startsWith("/")) {
                        User user2 = userRepository.findById(chatId).get();
                        user2.setStage(Stage.DoingNothing);
                        userRepository.save(user2);
                        Question unseenQuestion = questionRepository
                                .findFirstByStatusOrderByCreatedAtDesc(Status.Unseen).get();
                        unseenQuestion.setStatus(Status.Approved);
                        User u = unseenQuestion.getUser();
                        Answer answer = new Answer();
                        answer.setAnswer(messageText);
                        answer.setQuestion(unseenQuestion);
                        answer.setUser(u);
                        answer.setUs(userRepository.findById(chatId).get());
                        answerRepository.save(answer);
                        questionRepository.save(unseenQuestion);
                        sendMessage(u.getChatId(), "Ответ на ваш вопрос:\n" + messageText);
                        showListOfUnseenQuestions(chatId);
                        break;
                    }
                    sendMessage(chatId, "Извините, команда не распознана.");
                    break;
            }
        }
    }
    private void getCallBackQueryUpdateFromCustomer(Update update) {
        String callbackData = update.getCallbackQuery().getData();
        var m = update.getCallbackQuery().getMessage();
        long messageId = update.getCallbackQuery().getMessage().getMessageId();
        long chatId = update.getCallbackQuery().getMessage().getChatId();

        if (callbackData.startsWith("order_")) {
            String number = callbackData.substring(6);
            long indOfProductRes = Integer.parseInt(number);

            var ind = 0;
            for (var i:
                    currentProdResInOrder.get(chatId)) {
                if (i.getId().equals(indOfProductRes)) {
                    createOrder(chatId, ind);
                    break;
                }
                ind++;
            }
            //createOrder(chatId, ind);
            return;
        }
        if (callbackData.startsWith("review_")) {
            String number = callbackData.substring(7);
            long indOfProductRes = Integer.parseInt(number);

            var ind = 0;
            for (var i:
                    currentProdResInReview.get(chatId)) {
                if (i.getId().equals(indOfProductRes)) {
                    createReview(chatId, ind);
                    break;
                }
                ind++;
            }
            //createOrder(chatId, ind);
            return;
        }
        if (callbackData.startsWith("shop_")) {
            String number = callbackData.substring(5);
            long indOfProduct = Integer.parseInt(number);

            Reservation r = new Reservation();
            r.setUser(userRepository.findById(chatId).get());

            Product pr = productRepository.findById(indOfProduct).get();
            int count = pr.getCountAvailable() - 1;
            pr.setCountAvailable(count);
            productRepository.save(pr);
            r.setCreatedAt(LocalDateTime.now());
            r.setStatus(Status.Unseen);
            reservationRepository.save(r);
            ProductReservation productReservation = new ProductReservation();
            productReservation.setProduct(pr);
            productReservation.setReservation(r);
            productReservation.setQuantity(1);

            productReservationRepository.save(productReservation);

            var lpr =  currentProdResInOrder.get(chatId);
            lpr.add(productReservation);
            currentProdResInOrder.put(chatId, lpr);
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText("Забронировано " + pr.getTitle() + " на " + pr.getShop() + ".");

            try {
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
            return;
        }

        if (callbackData.startsWith("skipOrder_")) {
            int numberOfRes = currentIndInList.get(chatId);
            numberOfRes++;
            var l = currentProdResInOrder.get(chatId);
            if (numberOfRes > l.size() - 1) {
                numberOfRes = 0;
            }
            String text = "Отправьте скрин для товара: " + l.get(numberOfRes).getProduct().getTitle();

            curImageInOrder.put(chatId, new ArrayList<>());
            numberOfRes++;
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText(text);
            Button yesButton = new Button("Пропустить", "skipOrder_" + numberOfRes);
            List<Button> buttons = new ArrayList<>();
            buttons.add(yesButton);
            InlineKeyboardMarkup markup = KeyboardMarkupBuilder.setKeyboard(buttons);
            message.setReplyMarkup(markup);

            try {
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
            return;
        }
        switch (callbackData) {
            case "Confirm":
                editMessageAfterAgreeRulesAndTerms(chatId, messageId);
                setUserCommands(chatId);
                tryToGetAcquainted(chatId);
                break;
            case "AddToOrder":
                sendMessage(chatId, "Отправьте ещё изображение по этому заказу");
                break;
            case "FinishOrder":
                sendMessage(chatId, "Спасибо! Я покажу всё человеку и вернусь, не волнуйся, если это " +
                        "займет 1-2 дня.\n" +
                        "\n" +
                        "\uD83D\uDD14 В любой момент меня можно вызвать и задать вопрос через меню или " +
                        "написав сюда /help.");
                createOrder(chatId, currentIndInList.get(chatId));

                int inde = currentIndInList.get(chatId);
                var li = currentProdResInOrder.get(chatId);
                li.remove(inde);
                if (inde > li.size() - 1) {
                    inde = 0;
                    currentIndInList.put(chatId, inde);
                }

                curImageInOrder.put(chatId, new ArrayList<>());
                break;
            case "No":
                editMessageAfterChooseInvited(chatId, messageId);
                inviteFriend(chatId);
                startTalkingAboutProgram(chatId);
                break;
            case "Yes":
                User user1 = userRepository.findById(chatId).get();
                user1.setStage(Stage.EnterUserNameOfFriend);
                userRepository.save(user1);
                editMessageAfterChooseInvited(chatId, messageId);
                invitedByFriend(chatId);
                break;
            case "SendScreenshots":
                sendScreen(chatId);
                break;
            case "Next":
                int cur = currentInds.get(chatId);
                currentInds.put(chatId, cur + 1);
                getTextOfProgramPage(chatId, messageId);
                break;
            case "finishToChooseItems":
                List<List<Product>> products = curProdToR.get(chatId);
                String text = "Выберите магазин у товара: ";
                List<Button> buttons = new ArrayList<>();
                int ind = 0;
                int i = 1;
                for (var product:
                        products.get(ind)) {
                    if (i==1) {
                        text += product.getTitle();
                        i = 0;
                    }
                    Button newButton = new Button(product.getShop(), "shop_" + product.getId() + "_" + ind);
                    buttons.add(newButton);
                }

                if (products.size() > 1) {
                    Button newButton = new Button("Пропустить", "skip_" + ind);
                    buttons.add(newButton);
                }
                SendMessage message = new SendMessage();
                message.setChatId(String.valueOf(chatId));
                message.setText(text);
                InlineKeyboardMarkup markup = KeyboardMarkupBuilder.setKeyboard(buttons);
                message.setReplyMarkup(markup);

                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
                break;
            case "sentItAll":
                User u = userRepository.findById(chatId).get();
                u.setStage(Stage.EnterUserNameOfFriend);
                userRepository.save(u);
                sendMessage(chatId, "Спасибо! Я покажу всё человеку и вернусь. " +
                        "Не волнуйся, если это займет 1-2 дня. Я вернусь к тебе, как только человек проверит " +
                        "корректность скрина и учтет всю информацию.");
                sendMessage(chatId, "\uD83C\uDF8A Модерация всех твоих отзывов пройдена! \uD83C\uDF8A");
                sendTextsAfterModerationReview(chatId);
                break;
            case "newOrder":
                sendMessage(chatId, "Спасибо! Я покажу всё человеку и вернусь, не волнуйся, если это " +
                        "займет 1-2 дня.\n" +
                        "\n" +
                        "\uD83D\uDD14 В любой момент меня можно вызвать и задать вопрос через меню или " +
                        "написав сюда /help.");
                createOrder(chatId, currentIndInList.get(chatId));

                int index = currentIndInList.get(chatId);
                var l = currentProdResInOrder.get(chatId);
                l.remove(index);
                if (index > l.size() - 1) {
                    index = 0;
                    currentIndInList.put(chatId, index);
                }

                curImageInOrder.put(chatId, new ArrayList<>());
                String t = "\nОтправьте скрин для товара: " + l.get(index).getProduct().getTitle();

                index++;
                SendMessage mess = new SendMessage();
                mess.setChatId(String.valueOf(chatId));
                mess.setText(t);
                Button yesButton = new Button("Пропустить", "skipOrder_" + index);
                List<Button> butts = new ArrayList<>();
                butts.add(yesButton);
                InlineKeyboardMarkup ma = KeyboardMarkupBuilder.setKeyboard(butts);
                mess.setReplyMarkup(ma);

                try {
                    execute(mess);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
                break;
            case "WantMore":
                sendTextAfterMore(chatId);
                break;
            case "End":
                sendTextAfterEnd(chatId);
                break;
            case "AskQuestion":
                AskQuestion(chatId);
                break;
            case "endToDoReservations":
                endToDoReservations(chatId, m);
                break;
            default:
                sendMessage(chatId, "Извините, команда не распознана.");
                break;
        }
    }
    private void getTextUpdateFromModerator(Update update) {
        long chatId = update.getMessage().getChatId();
        String messageText = update.getMessage().getText();

        User user = new User();
        if (!messageText.equals("/start")) {
            user = userRepository.findById(chatId).get();
        }

        if (messageText.equals(MODERATION)) {
            moderation(chatId);
        } else {
            switch (messageText) {
                case "/start":
                    registerUser(update.getMessage());
                    sendMessage(chatId, EmojiParser.parseToUnicode("Привет\uD83D\uDC4B Меня зовут Толик. " +
                            "Я – бот, и помогаю с участием в проекте «Эксперты Professor SkinGood». " +
                            "Здесь ты сможешь попробовать \uD83E\uDDF4уходовую косметику «Professor SkinGood», " +
                            "\uD83D\uDCAC поделиться своим мнением о ней и \uD83C\uDF89 выиграть призы! " +
                            "За твое мнение мы возместим стоимость покупки и каждый месяц будем разыгрывать " +
                            "20+ призов среди участников, в том числе сертификат на 10 000 рублей."));
                    sendRulesAndTermsOfUse(chatId);
                    break;
                case "/rules":
                    sendRules(chatId);
                    break;
                case "/policy":
                    sendTermsOfUse(chatId);
                    break;
                case "/info":
                    sendInfo(chatId);
                    break;
                case "/moderation":
                    moderation(chatId);
                    break;
                default:
                    if (user.getStage().equals(Stage.EnterFirstName) && !messageText.startsWith("/")) {
                        SetUserName(chatId, messageText);
                        greatings(chatId);
                        friendInviteYou(chatId);
                        break;
                    } else if (user.getStage().equals(Stage.EnterUserNameOfFriend) && !messageText.startsWith("/")) {
                        SetUserNameOfFriend(chatId, messageText);
                        send(chatId);
                        sendFirstPageOfProgram(chatId);
                        break;
                    } else if (user.getStage().equals(Stage.EnterReasonManually) && !messageText.startsWith("/")) {
                        User user2 = userRepository.findById(chatId).get();
                        user2.setStage(Stage.DoingNothing);
                        userRepository.save(user2);
                        Order order = orderRepository.findFirstByStatusOrderByCreatedAtDesc(Status.Unseen).get();
                        order.setStatus(Status.Disapproved);
                        orderRepository.save(order);
                        sendMessage(order.getUser().getChatId(), "Заказ отклонен по причине:\n" + messageText);
                        break;
                    } else if (user.getStage().equals(Stage.EnterReasonManuallyToReview) && !messageText.startsWith("/")) {
                        User user2 = userRepository.findById(chatId).get();
                        user2.setStage(Stage.DoingNothing);
                        userRepository.save(user2);
                        Review order = reviewRepository.findFirstByStatusOrderByCreatedAtDesc(Status.Unseen).get();
                        order.setStatus(Status.Disapproved);
                        reviewRepository.save(order);
                        sendMessage(order.getUser().getChatId(), "Отзыв отклонен по причине:\n" + messageText);
                        break;
                    } else if (user.getStage().equals(Stage.AnsweringQuestion) && !messageText.startsWith("/")) {
                        User user2 = userRepository.findById(chatId).get();
                        user2.setStage(Stage.DoingNothing);
                        userRepository.save(user2);
                        Question unseenQuestion = questionRepository
                                .findFirstByStatusOrderByCreatedAtDesc(Status.Unseen).get();
                        unseenQuestion.setStatus(Status.Approved);
                        User u = unseenQuestion.getUser();
                        Answer answer = new Answer();
                        answer.setAnswer(messageText);
                        answer.setQuestion(unseenQuestion);
                        answer.setUser(u);
                        answer.setUs(userRepository.findById(chatId).get());
                        answerRepository.save(answer);
                        questionRepository.save(unseenQuestion);
                        sendMessage(u.getChatId(), "Ответ на ваш вопрос:\n" + messageText);
                        showListOfUnseenQuestions(chatId);
                        break;
                    } else if (user.getStage().equals(Stage.EnterCostOfItem) && !messageText.startsWith("/")) {
                        User user2 = userRepository.findById(chatId).get();
                        user2.setStage(Stage.DoingNothing);
                        userRepository.save(user2);
                        Integer cost = Integer.parseInt(messageText);
                        var res = reservationToPay.get(user2.getChatId());
                        var list = productReservationRepository.findByReservation(res);
                        for (var i:
                                list) {
                            i.setCost(cost);
                            productReservationRepository.save(i);
                        }
                        showListOfUnseenOrders(chatId);
                        break;
                    }
                    sendMessage(chatId, "Извините, команда не распознана.");
                    break;
            }
        }
    }
    private void getCallBackQueryUpdateFromModerator(Update update) {
        String callbackData = update.getCallbackQuery().getData();
        var m = update.getCallbackQuery().getMessage();
        long messageId = update.getCallbackQuery().getMessage().getMessageId();
        long chatId = update.getCallbackQuery().getMessage().getChatId();

        if (callbackData.startsWith("Approver_")) {
            String[] arr = (callbackData.substring(9)).split(" ");
            long numberOfRes = Integer.parseInt(arr[0]);
            long numberOfOrd = Integer.parseInt(arr[1]);
            Review or = reviewRepository.findById(numberOfOrd).get();
            Reservation res = reservationRepository.findById(numberOfRes).get();
            res.setStatus(Status.Approved);
            or.setStatus(Status.Approved);
            reviewRepository.save(or);
            reservationRepository.save(res);
            User u = or.getUser();

            User user2 = userRepository.findById(chatId).get();
            user2.setStage(Stage.DoingNothing);
            userRepository.save(user2);
            sendMessage(u.getChatId(), "\uD83C\uDF8A Модерация всех " +
                    "твоих отзывов пройдена! \uD83C\uDF8A\n");
            showListOfUnseenReviews(chatId);
            return;
        }
        if (callbackData.startsWith("Disapprover_")) {
            String arr = callbackData.substring(12);
            long numberOfRes = Integer.parseInt(arr);
            Review or = reviewRepository.findById(numberOfRes).get();
            User u = or.getUser();

            var prRes = or.getProductReservation().getReservation();
            var list = new ArrayList<ProductReservation>();
            ProductReservation pr = new ProductReservation();
            Reservation r = new Reservation();
            r.setUser(u);
            r.setStatus(prRes.getStatus());
            r.setCreatedAt(LocalDateTime.now());
            reservationRepository.save(r);

            pr.setReservation(r);
            pr.setProduct(or.getProductReservation().getProduct());
            pr.setQuantity(or.getProductReservation().getQuantity());
            productReservationRepository.save(pr);
            list.add(pr);
            currentProdResInReview.put(u.getChatId(), list);

            setReasonForRejectingReview(chatId, u.getChatId());
            return;
        }
        if (callbackData.startsWith("Approved_")) {
            String[] arr = (callbackData.substring(9)).split(" ");
            long numberOfRes = Integer.parseInt(arr[0]);
            long numberOfOrd = Integer.parseInt(arr[1]);
            Order or = orderRepository.findById(numberOfOrd).get();
            Reservation res = reservationRepository.findById(numberOfRes).get();
            res.setStatus(Status.Approved);
            or.setStatus(Status.Approved);
            orderRepository.save(or);
            reservationRepository.save(res);
            User u = or.getUser();

            sendMessage(chatId, "Введите стоимость товара из чека");
            User user2 = userRepository.findById(chatId).get();
            user2.setStage(Stage.EnterCostOfItem);
            reservationToPay.put(user2.getChatId(), res);
            userRepository.save(user2);
            sendMessage(u.getChatId(), "\uD83C\uDF89 Спасибо. Модерация пройдена и я забронировал тебе возмещение. \n" +
                    "\n" +
                    "\uD83D\uDCAC Переходи к написанию отзывов. \n" +
                    "\n" +
                    "\uD83D\uDE42 Я буду ждать скриншоты твоих опубликованных отзывов на том же сайте, " +
                    "где была осуществлена покупка \uD83D\uDCA1 Ты можешь их отправлять так, как тебе " +
                    "удобно – по факту публикации каждого или все вместе. Пожалуйста, помни, что возмещение " +
                    "по каждому продукту я смогу отправить после проверки отзыва на него.");
            return;
        }
        if (callbackData.startsWith("Disapproved_")) {
            String arr = callbackData.substring(12);
            long numberOfRes = Integer.parseInt(arr);
            Order or = orderRepository.findById(numberOfRes).get();
            User u = or.getUser();

            var prRes = or.getProductReservation().getReservation();
            var list = new ArrayList<ProductReservation>();
            ProductReservation pr = new ProductReservation();
            Reservation r = new Reservation();
            r.setUser(u);
            r.setStatus(prRes.getStatus());
            r.setCreatedAt(LocalDateTime.now());
            reservationRepository.save(r);

            pr.setReservation(r);
            pr.setProduct(or.getProductReservation().getProduct());
            pr.setQuantity(or.getProductReservation().getQuantity());
            productReservationRepository.save(pr);
            list.add(pr);
            currentProdResInOrder.put(u.getChatId(), list);

            setReasonForRejectingOrder(chatId, u.getChatId());
            return;
        }
        switch (callbackData) {
            case "Confirm":
                editMessageAfterAgreeRulesAndTerms(chatId, messageId);
                setUserCommands(chatId);
                tryToGetAcquainted(chatId);
                break;
            case "orders":
                showListOfUnseenOrders(chatId);
                break;
            case "reviews":
                showListOfUnseenReviews(chatId);
                break;
            case "questions":
                showListOfUnseenQuestions(chatId);
                break;
            case "answer":
                User user = userRepository.findById(chatId).get();
                user.setStage(Stage.AnsweringQuestion);
                userRepository.save(user);
                break;
            case "No":
                editMessageAfterChooseInvited(chatId, messageId);
                inviteFriend(chatId);
                startTalkingAboutProgram(chatId);
                break;
            case "Yes":
                User user1 = userRepository.findById(chatId).get();
                user1.setStage(Stage.EnterUserNameOfFriend);
                userRepository.save(user1);
                editMessageAfterChooseInvited(chatId, messageId);
                invitedByFriend(chatId);
                break;
            case "Next":
                int cur = currentInds.get(chatId);
                currentInds.put(chatId, cur + 1);
                getTextOfProgramPage(chatId, messageId);
                break;
            case "noThatImage":
                User user5 = userRepository.findById(chatId).get();
                user5.setStage(Stage.DoingNothing);
                userRepository.save(user5);
                Review order1 = reviewRepository.findFirstByStatusOrderByCreatedAtDesc(Status.Unseen).get();
                order1.setStatus(Status.Disapproved);
                reviewRepository.save(order1);
                sendMessage(order1.getUser().getChatId(), "Заказ отклонен из-за " +
                        "неправильно присланной фотографии.");
                showListOfUnseenReviews(chatId);
                break;
            case "enterManually":
                User user6 = userRepository.findById(chatId).get();
                user6.setStage(Stage.EnterReasonManuallyToReview);
                userRepository.save(user6);
                break;
            case "enterReason":
                User user4 = userRepository.findById(chatId).get();
                user4.setStage(Stage.DoingNothing);
                userRepository.save(user4);
                Order order = orderRepository.findFirstByStatusOrderByCreatedAtDesc(Status.Unseen).get();
                order.setStatus(Status.Disapproved);
                orderRepository.save(order);
                sendMessage(order.getUser().getChatId(), "Заказ отклонен из-за " +
                        "неправильно присланной фотографии.");
                showListOfUnseenOrders(chatId);
                break;
            case "enterReasonManually":
                User user2 = userRepository.findById(chatId).get();
                user2.setStage(Stage.EnterReasonManually);
                userRepository.save(user2);
                break;
            case "WantMore":
                sendTextAfterMore(chatId);
                break;
            case "End":
                sendTextAfterEnd(chatId);
                break;
            default:
                sendMessage(chatId, "Извините, команда не распознана.");
                break;
        }
    }
    private void getTextUpdateFromAdmin(Update update) {
        long chatId = update.getMessage().getChatId();
        String messageText = update.getMessage().getText();

        User user = new User();
        if (!messageText.equals("/start")) {
            user = userRepository.findById(chatId).get();
        }

        if (messageText.equals(MODERATION)) {
            moderation(chatId);
        } else if (messageText.equals(EMPLOYEE)) {
            employeeManagement(chatId);
        } else if (messageText.equals(CHANGE)) {
            ChangeListOfProducts(chatId);
        } else {
            switch (messageText) {
                case "/start":
                    registerUser(update.getMessage());
                    sendMessage(chatId, EmojiParser.parseToUnicode("Привет\uD83D\uDC4B Меня зовут Толик. " +
                            "Я – бот, и помогаю с участием в проекте «Эксперты Professor SkinGood». " +
                            "Здесь ты сможешь попробовать \uD83E\uDDF4уходовую косметику «Professor SkinGood», " +
                            "\uD83D\uDCAC поделиться своим мнением о ней и \uD83C\uDF89 выиграть призы! " +
                            "За твое мнение мы возместим стоимость покупки и каждый месяц будем разыгрывать " +
                            "20+ призов среди участников, в том числе сертификат на 10 000 рублей."));
                    sendRulesAndTermsOfUse(chatId);
                    break;
                case "/rules":
                    sendRules(chatId);
                    break;
                case "/policy":
                    sendTermsOfUse(chatId);
                    break;
                case "/info":
                    sendInfo(chatId);
                    break;
                case "/moderation":
                    moderation(chatId);
                    break;
                case "/employee":
                    employeeManagement(chatId);
                    break;
                case "/report":
                    getReport(chatId);
                    break;
                case "/change":
                    ChangeListOfProducts(chatId);
                    break;
                default:
                    if (user.getStage().equals(Stage.EnterFirstName) && !messageText.startsWith("/")) {
                        SetUserName(chatId, messageText);
                        greatings(chatId);
                        friendInviteYou(chatId);
                        break;
                    } else if (user.getStage().equals(Stage.EnterUserNameOfFriend) && !messageText.startsWith("/")) {
                        SetUserNameOfFriend(chatId, messageText);
                        send(chatId);
                        sendFirstPageOfProgram(chatId);
                        break;
                    } else if (user.getStage().equals(Stage.ChangingList) && !messageText.startsWith("/")) {
                        PrintListOfProducts(chatId, messageText);
                        break;
                    } else if (user.getStage().equals(Stage.EnterNameOfItemToAdd) && !messageText.startsWith("/")) {
                        newItem(chatId, messageText);
                        break;
                    } else if (user.getStage().equals(Stage.EnterShopOfItemToAdd) && !messageText.startsWith("/")) {
                        setShopToItem(chatId, messageText);
                        break;
                    } else if (user.getStage().equals(Stage.EnterCountOfItemToAdd) && !messageText.startsWith("/")) {
                        setCountToItem(chatId, messageText);
                        break;
                    } else if (user.getStage().equals(Stage.EnterReasonManually) && !messageText.startsWith("/")) {
                        User user2 = userRepository.findById(chatId).get();
                        user2.setStage(Stage.DoingNothing);
                        userRepository.save(user2);
                        Order order = orderRepository.findFirstByStatusOrderByCreatedAtDesc(Status.Unseen).get();
                        order.setStatus(Status.Disapproved);
                        orderRepository.save(order);
                        sendMessage(order.getUser().getChatId(), "Заказ отклонен по причине:\n" + messageText);
                        break;
                    } else if (user.getStage().equals(Stage.EnterReasonManuallyToReview) && !messageText.startsWith("/")) {
                        User user2 = userRepository.findById(chatId).get();
                        user2.setStage(Stage.DoingNothing);
                        userRepository.save(user2);
                        Review order = reviewRepository.findFirstByStatusOrderByCreatedAtDesc(Status.Unseen).get();
                        order.setStatus(Status.Disapproved);
                        reviewRepository.save(order);
                        sendMessage(order.getUser().getChatId(), "Отзыв отклонен по причине:\n" + messageText);
                        break;
                    } else if (user.getStage().equals(Stage.AnsweringQuestion) && !messageText.startsWith("/")) {
                        User user2 = userRepository.findById(chatId).get();
                        user2.setStage(Stage.DoingNothing);
                        userRepository.save(user2);
                        Question unseenQuestion = questionRepository
                                .findFirstByStatusOrderByCreatedAtDesc(Status.Unseen).get();
                        unseenQuestion.setStatus(Status.Approved);
                        User u = unseenQuestion.getUser();
                        Answer answer = new Answer();
                        answer.setAnswer(messageText);
                        answer.setQuestion(unseenQuestion);
                        answer.setUser(u);
                        answer.setUs(userRepository.findById(chatId).get());
                        answerRepository.save(answer);
                        questionRepository.save(unseenQuestion);
                        sendMessage(u.getChatId(), "Ответ на ваш вопрос:\n" + messageText);
                        showListOfUnseenQuestions(chatId);
                        break;
                    } else if (user.getStage().equals(Stage.EnterCostOfItem) && !messageText.startsWith("/")) {
                        User user2 = userRepository.findById(chatId).get();
                        user2.setStage(Stage.DoingNothing);
                        userRepository.save(user2);
                        Integer cost = Integer.parseInt(messageText);
                        var res = reservationToPay.get(user2.getChatId());
                        var list = productReservationRepository.findByReservation(res);
                        for (var i:
                                list) {
                            i.setCost(cost);
                            productReservationRepository.save(i);
                        }
                        showListOfUnseenOrders(chatId);
                        break;
                    } else if (user.getStage().equals(Stage.EnterNewAdminUser) && !messageText.startsWith("/")) {
                        newAdministrator(chatId, normalizeUsername(messageText));
                        break;
                    } else if (user.getStage().equals(Stage.EnterNewModeratorUser) && !messageText.startsWith("/")) {
                        newModerator(chatId, normalizeUsername(messageText));
                        break;
                    }
                    sendMessage(chatId, "Извините, команда не распознана.");
                    break;
            }
        }
    }
    private void getCallBackQueryUpdateFromAdmin(Update update) {
        String callbackData = update.getCallbackQuery().getData();
        var m = update.getCallbackQuery().getMessage();
        long messageId = update.getCallbackQuery().getMessage().getMessageId();
        long chatId = update.getCallbackQuery().getMessage().getChatId();

        if (callbackData.startsWith("product_")) {
            String number = callbackData.substring(8);
            long id = Integer.parseInt(number);
            var product = productRepository.findById(id);
            Product pr = product.get();
            var products = productRepository.findAllByTitle(pr.getTitle());
            var prList = curProdToR.get(chatId);
            prList.add(products);
            curProdToR.put(chatId, prList);
            return;
        }

        if (callbackData.startsWith("order_")) {
            String number = callbackData.substring(6);
            long indOfProductRes = Integer.parseInt(number);

            var ind = 0;
            for (var i:
                    currentProdResInOrder.get(chatId)) {
                if (i.getId().equals(indOfProductRes)) {
                    createOrder(chatId, ind);
                    break;
                }
                ind++;
            }
            //createOrder(chatId, ind);
            return;
        }
        if (callbackData.startsWith("review_")) {
            String number = callbackData.substring(7);
            long indOfProductRes = Integer.parseInt(number);

            var ind = 0;
            for (var i:
                    currentProdResInReview.get(chatId)) {
                if (i.getId().equals(indOfProductRes)) {
                    createReview(chatId, ind);
                    break;
                }
                ind++;
            }
            //createOrder(chatId, ind);
            return;
        }
        if (callbackData.startsWith("addShop_")) {
            String number = callbackData.substring(8);
            long indOfProduct = Integer.parseInt(number);
            Product pr = productRepository.findById(indOfProduct).get();
            Product p = new Product();
            p.setTitle(pr.getTitle());
            p.setNameOfProject(pr.getNameOfProject());
            curProd.put(chatId, p);

            sendMessage(chatId, "Введите название магазина, в котором продается продукт");

            User u = userRepository.findById(chatId).get();
            u.setStage(Stage.EnterShopOfItemToAdd);
            userRepository.save(u);
            return;
        }
        if (callbackData.startsWith("shop_")) {
            String number = callbackData.substring(5);
            long indOfProduct = Integer.parseInt(number);

            Reservation r = new Reservation();
            r.setUser(userRepository.findById(chatId).get());

            Product pr = productRepository.findById(indOfProduct).get();
            int count = pr.getCountAvailable() - 1;
            pr.setCountAvailable(count);
            productRepository.save(pr);
            r.setCreatedAt(LocalDateTime.now());
            r.setStatus(Status.Unseen);
            reservationRepository.save(r);
            ProductReservation productReservation = new ProductReservation();
            productReservation.setProduct(pr);
            productReservation.setReservation(r);
            productReservation.setQuantity(1);

            productReservationRepository.save(productReservation);

            var lpr =  currentProdResInOrder.get(chatId);
            lpr.add(productReservation);
            currentProdResInOrder.put(chatId, lpr);
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText("Забронировано " + pr.getTitle() + " на " + pr.getShop() + ".");

            try {
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
            return;
        }
        if (callbackData.startsWith("Approver_")) {
            String[] arr = (callbackData.substring(9)).split(" ");
            long numberOfRes = Integer.parseInt(arr[0]);
            long numberOfOrd = Integer.parseInt(arr[1]);
            Review or = reviewRepository.findById(numberOfOrd).get();
            Reservation res = reservationRepository.findById(numberOfRes).get();
            res.setStatus(Status.Approved);
            or.setStatus(Status.Approved);
            reviewRepository.save(or);
            reservationRepository.save(res);
            User u = or.getUser();

            User user2 = userRepository.findById(chatId).get();
            user2.setStage(Stage.DoingNothing);
            userRepository.save(user2);
            sendMessage(u.getChatId(), "\uD83C\uDF8A Модерация всех " +
                    "твоих отзывов пройдена! \uD83C\uDF8A\n");
            showListOfUnseenReviews(chatId);
            return;
        }
        if (callbackData.startsWith("Disapprover_")) {
            String arr = callbackData.substring(12);
            long numberOfRes = Integer.parseInt(arr);
            Review or = reviewRepository.findById(numberOfRes).get();
            User u = or.getUser();

            var prRes = or.getProductReservation().getReservation();
            var list = new ArrayList<ProductReservation>();
            ProductReservation pr = new ProductReservation();
            Reservation r = new Reservation();
            r.setUser(u);
            r.setStatus(prRes.getStatus());
            r.setCreatedAt(LocalDateTime.now());
            reservationRepository.save(r);

            pr.setReservation(r);
            pr.setProduct(or.getProductReservation().getProduct());
            pr.setQuantity(or.getProductReservation().getQuantity());
            productReservationRepository.save(pr);
            list.add(pr);
            currentProdResInReview.put(u.getChatId(), list);

            setReasonForRejectingReview(chatId, u.getChatId());
            return;
        }
        if (callbackData.startsWith("Approved_")) {
            String[] arr = (callbackData.substring(9)).split(" ");
            long numberOfRes = Integer.parseInt(arr[0]);
            long numberOfOrd = Integer.parseInt(arr[1]);
            Order or = orderRepository.findById(numberOfOrd).get();
            Reservation res = reservationRepository.findById(numberOfRes).get();
            res.setStatus(Status.Approved);
            or.setStatus(Status.Approved);
            orderRepository.save(or);
            reservationRepository.save(res);
            User u = or.getUser();

            sendMessage(chatId, "Введите стоимость товара из чека");
            User user2 = userRepository.findById(chatId).get();
            user2.setStage(Stage.EnterCostOfItem);
            reservationToPay.put(user2.getChatId(), res);
            userRepository.save(user2);
            sendMessage(u.getChatId(), "\uD83C\uDF89 Спасибо. Модерация пройдена и я забронировал тебе возмещение. \n" +
                    "\n" +
                    "\uD83D\uDCAC Переходи к написанию отзывов. \n" +
                    "\n" +
                    "\uD83D\uDE42 Я буду ждать скриншоты твоих опубликованных отзывов на том же сайте, " +
                    "где была осуществлена покупка \uD83D\uDCA1 Ты можешь их отправлять так, как тебе " +
                    "удобно – по факту публикации каждого или все вместе. Пожалуйста, помни, что возмещение " +
                    "по каждому продукту я смогу отправить после проверки отзыва на него.");
            return;
        }
        if (callbackData.startsWith("Disapproved_")) {
            String arr = callbackData.substring(12);
            long numberOfRes = Integer.parseInt(arr);
            Order or = orderRepository.findById(numberOfRes).get();
            User u = or.getUser();

            var prRes = or.getProductReservation().getReservation();
            var list = new ArrayList<ProductReservation>();
            ProductReservation pr = new ProductReservation();
            Reservation r = new Reservation();
            r.setUser(u);
            r.setStatus(prRes.getStatus());
            r.setCreatedAt(LocalDateTime.now());
            reservationRepository.save(r);

            pr.setReservation(r);
            pr.setProduct(or.getProductReservation().getProduct());
            pr.setQuantity(or.getProductReservation().getQuantity());
            productReservationRepository.save(pr);
            list.add(pr);
            currentProdResInOrder.put(u.getChatId(), list);

            setReasonForRejectingOrder(chatId, u.getChatId());
            return;
        }
        if (callbackData.startsWith("changeShop_")) {
            String arr = callbackData.substring(11);
            long numberOfRes = Integer.parseInt(arr);
            Product or = productRepository.findById(numberOfRes).get();
            or.setStat(Stat.Unseen);
            productRepository.save(or);
            return;
        }
        if (callbackData.startsWith("skipOrder_")) {
            int numberOfRes = currentIndInList.get(chatId);
            numberOfRes++;
            var l = currentProdResInOrder.get(chatId);
            if (numberOfRes > l.size() - 1) {
                numberOfRes = 0;
            }
            String text = "Отправьте скрин для товара: " + l.get(numberOfRes).getProduct().getTitle();

            curImageInOrder.put(chatId, new ArrayList<>());
            numberOfRes++;
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText(text);
            Button yesButton = new Button("Пропустить", "skipOrder_" + numberOfRes);
            List<Button> buttons = new ArrayList<>();
            buttons.add(yesButton);
            InlineKeyboardMarkup markup = KeyboardMarkupBuilder.setKeyboard(buttons);
            message.setReplyMarkup(markup);

            try {
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
            return;
        }
        switch (callbackData) {
            case "Confirm":
                editMessageAfterAgreeRulesAndTerms(chatId, messageId);
                setUserCommands(chatId);
                tryToGetAcquainted(chatId);
                break;
            case "orders":
                showListOfUnseenOrders(chatId);
                break;
            case "reviews":
                showListOfUnseenReviews(chatId);
                break;
            case "questions":
                showListOfUnseenQuestions(chatId);
                break;
            case "answer":
                User user = userRepository.findById(chatId).get();
                user.setStage(Stage.AnsweringQuestion);
                userRepository.save(user);
                break;
            case "No":
                editMessageAfterChooseInvited(chatId, messageId);
                inviteFriend(chatId);
                startTalkingAboutProgram(chatId);
                break;
            case "Yes":
                User user1 = userRepository.findById(chatId).get();
                user1.setStage(Stage.EnterUserNameOfFriend);
                userRepository.save(user1);
                editMessageAfterChooseInvited(chatId, messageId);
                invitedByFriend(chatId);
                break;
            case "Next":
                int cur = currentInds.get(chatId);
                currentInds.put(chatId, cur + 1);
                getTextOfProgramPage(chatId, messageId);
                break;
            case "noThatImage":
                User user5 = userRepository.findById(chatId).get();
                user5.setStage(Stage.DoingNothing);
                userRepository.save(user5);
                Review order1 = reviewRepository.findFirstByStatusOrderByCreatedAtDesc(Status.Unseen).get();
                order1.setStatus(Status.Disapproved);
                reviewRepository.save(order1);
                sendMessage(order1.getUser().getChatId(), "Заказ отклонен из-за " +
                        "неправильно присланной фотографии.");
                showListOfUnseenReviews(chatId);
                break;
            case "enterManually":
                User user6 = userRepository.findById(chatId).get();
                user6.setStage(Stage.EnterReasonManuallyToReview);
                userRepository.save(user6);
                break;
            case "enterReason":
                User user4 = userRepository.findById(chatId).get();
                user4.setStage(Stage.DoingNothing);
                userRepository.save(user4);
                Order order = orderRepository.findFirstByStatusOrderByCreatedAtDesc(Status.Unseen).get();
                order.setStatus(Status.Disapproved);
                orderRepository.save(order);
                sendMessage(order.getUser().getChatId(), "Заказ отклонен из-за " +
                        "неправильно присланной фотографии.");
                showListOfUnseenOrders(chatId);
                break;
            case "enterReasonManually":
                User user2 = userRepository.findById(chatId).get();
                user2.setStage(Stage.EnterReasonManually);
                userRepository.save(user2);
                break;
            case "endAddingItem":
                endAddingItem(chatId);
                PrintListOfProducts(chatId, "new");
                break;
            case "deleteItem":
                changeStatInItem(chatId);
            case "addItem":
                addNewItem(chatId);
                break;
            case "WantMore":
                sendTextAfterMore(chatId);
                break;
            case "End":
                sendTextAfterEnd(chatId);
                break;
            case "AskQuestion":
                AskQuestion(chatId);
                break;
            case "hireEmployee":
                onboard(chatId);
                break;
            case "addModerator":
                addNewModerator(chatId);
                break;
            case "addAdmin":
                addNewAdministrator(chatId);
                break;
            default:
                sendMessage(chatId, "Извините, команда не распознана.");
                break;
        }
    }
    private void getReport(long chatId) {
        try {
            byte[] excelData = createExcelReport();

            SendDocument sendDocument = new SendDocument();
            sendDocument.setChatId(String.valueOf(chatId));
            sendDocument.setDocument(new InputFile(new ByteArrayInputStream(excelData), "report.xlsx"));
            execute(sendDocument);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    public byte[] createExcelReport() {
        Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // Создание первого листа
        Sheet sheet1 = workbook.createSheet("Лист 1");
        createProductsHeader(sheet1);

        // Создание второго листа
        Sheet sheet2 = workbook.createSheet("Отчет по вопросам");
        createQuestionHeader(sheet2);
        List<Answer> answers = (List<Answer>) answerRepository.findAll();

        int j = 1;
        for (var ans:
             answers) {
            Row row = sheet2.createRow(j);
            j++;
            row.createCell(0).setCellValue(ans.getQuestion().getId());
            row.createCell(1).setCellValue(ans.getUs().getChatId());
            row.createCell(2).setCellValue(ans.getUs().getUserName());
            row.createCell(3).setCellValue(ans.getUser().getChatId());
            row.createCell(4).setCellValue(ans.getUser().getUserName());
            row.createCell(5).setCellValue(ans.getQuestion().getQue());
            row.createCell(5).setCellValue(ans.getAnswer());
        }
        // Записываем файл
        try {
            workbook.write(outputStream);
            System.out.println("Отчет успешно создан: ");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                workbook.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return outputStream.toByteArray();
    }
    /*public byte[] createExcelReport() throws IOException {
        Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // Создаем первый лист
        Sheet sheet1 = workbook.createSheet("Лист 1");
        Row row1 = sheet1.createRow(0);
        Cell cell1 = row1.createCell(0);
        cell1.setCellValue("Данные для листа 1");

        // Создаем второй лист
        Sheet sheet2 = workbook.createSheet("Лист 2");
        Row row2 = sheet2.createRow(0);
        Cell cell2 = row2.createCell(0);
        cell2.setCellValue("Данные для листа 2");

        // Создаем третий лист
        Sheet sheet3 = workbook.createSheet("Лист 3");
        Row row3 = sheet3.createRow(0);
        Cell cell3 = row3.createCell(0);
        cell3.setCellValue("Данные для листа 3");

        // Записываем в ByteArrayOutputStream
        workbook.write(outputStream);
        workbook.close();

        return outputStream.toByteArray(); // Возвращаем массив байтов
    }*/

    private static void createQuestionHeader(Sheet sheet) {
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Id вопроса");
        headerRow.createCell(1).setCellValue("Id модератора в телеграм");
        headerRow.createCell(2).setCellValue("UserName модератора в телеграм");
        headerRow.createCell(3).setCellValue("Id задающего вопрос в телеграм");
        headerRow.createCell(4).setCellValue("UserName задающего вопрос в телеграм");
        headerRow.createCell(5).setCellValue("Вопрос от пользователя");
        headerRow.createCell(5).setCellValue("Ответ модератора");
    }
    private static void createProductsHeader(Sheet sheet) {
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Имя");
        headerRow.createCell(1).setCellValue("Возраст");
    }
    private void endToDoReservations(long chatId, Message mess) {
        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(String.valueOf(chatId));
        editMessageText.setText(mess.getText());
        editMessageText.setMessageId((int) mess.getMessageId());

        try {
            execute(editMessageText);
        } catch (TelegramApiException e) {
        }
    }
    private void employeeManagement(long chatId) {
        //List<User> moderators = userRepository.getAllByRole(Role.Moderator);

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText("Что вы хотите сделать с сотрудником?");
        Button questionButton = new Button("Нанять", "hireEmployee");
        Button qButton = new Button("Уволить", "fireEmployee");
        List<Button> buttons = new ArrayList<>();
        buttons.add(questionButton);
        buttons.add(qButton);
        InlineKeyboardMarkup markup = KeyboardMarkupBuilder.setKeyboard(buttons);
        sendMessage.setReplyMarkup(markup);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    private void onboard(long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText("Кого вы хотите нанять?");
        Button questionButton = new Button("Модератора", "addModerator");
        Button qButton = new Button("Администратора", "addAdmin");
        List<Button> buttons = new ArrayList<>();
        buttons.add(questionButton);
        buttons.add(qButton);
        InlineKeyboardMarkup markup = KeyboardMarkupBuilder.setKeyboard(buttons);
        sendMessage.setReplyMarkup(markup);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    private void addNewAdministrator(long chatId) {
        sendMessage(chatId, "Введите имя пользователя в telegram, " +
                "которому вы хотите дать роль администратора.");
        User u = userRepository.findById(chatId).get();
        u.setStage(Stage.EnterNewAdminUser);
        userRepository.save(u);
    }
    private void newAdministrator(long chatId, String message) {
        if (userRepository.findByUserName(message).isEmpty()) {
            sendMessage(chatId, "Вы не можете назначить нового администратора, " +
                    "потому что пользователь с этим именем " + message + " не воспользовался ботом или " +
                    "вы ввели неверный имя пользователя.");
            User u = userRepository.findById(chatId).get();
            u.setStage(Stage.DoingNothing);
            userRepository.save(u);

            return;
        }
        User u = userRepository.findByUserName(message).get();
        long chatIdOfNewAdmin = u.getChatId();
        u.setRole(Role.Admin);
        userRepository.save(u);

        sendMessage(chatId, message + " назначен новым администратором.");
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatIdOfNewAdmin));
        sendMessage.setText("Вы назначены новым администратором.");

        List<List<Button>> buttons = new ArrayList<>();
        List<Button> row1 = new ArrayList<>();
        row1.add(new Button(MODERATION, ""));
        row1.add(new Button(CHANGE, ""));
        buttons.add(row1);
        List<Button> row2 = new ArrayList<>();
        row2.add(new Button(EMPLOYEE, ""));
        buttons.add(row2);
        sendMessage.setReplyMarkup(KeyboardMarkupBuilder.setReplyKeyboardWithRaw(buttons));
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

        User us = userRepository.findById(chatId).get();
        us.setStage(Stage.DoingNothing);
        userRepository.save(us);
        setUserCommands(chatIdOfNewAdmin);
    }
    private void addNewModerator(long chatId) {
        sendMessage(chatId, "Введите имя пользователя в telegram, " +
                "которому вы хотите дать роль модератора.");
        User u = userRepository.findById(chatId).get();
        u.setStage(Stage.EnterNewModeratorUser);
        userRepository.save(u);
    }
    private void newModerator(long chatId, String message) {
        if (userRepository.findByUserName(message).isEmpty()) {
            sendMessage(chatId, "Вы не можете назначить нового модератора, " +
                    "потому что пользователь с этим именем " + message + " не воспользовался ботом или " +
                    "вы ввели неверный имя пользователя.");
            User u = userRepository.findById(chatId).get();
            u.setStage(Stage.DoingNothing);
            userRepository.save(u);
            return;
        }
        User u = userRepository.findByUserName(message).get();
        long chatIdOfNewAdmin = u.getChatId();
        u.setRole(Role.Admin);
        userRepository.save(u);

        sendMessage(chatId, message + " назначен новым модератором.");

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatIdOfNewAdmin));
        sendMessage.setText("Вы назначены новым модератором.");

        List<List<Button>> buttons = new ArrayList<>();
        List<Button> row1 = new ArrayList<>();
        row1.add(new Button(MODERATION, ""));
        buttons.add(row1);
        sendMessage.setReplyMarkup(KeyboardMarkupBuilder.setReplyKeyboardWithRaw(buttons));
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        User us = userRepository.findById(chatId).get();
        us.setStage(Stage.DoingNothing);
        userRepository.save(us);
        setUserCommands(chatIdOfNewAdmin);
    }
    private void changeStatInItem(long chatId) {
        User user = userRepository.findById(chatId).get();
        user.setStage(Stage.ChangingStatInItem);
        userRepository.save(user);
        StringBuilder text = new StringBuilder("Какой продукт вы хотите скрыть");

        List<List<Button>> buttons = new ArrayList<>();
        List<Button> currentRow = new ArrayList<>();
        var products = productRepository
                .findAllByNameOfProject("new");
        if (products != null) {
            Set<String> strings = new HashSet<>();

            int ind = 1;
            for (var j:
                    products) {
                if (strings.contains(j.getTitle())) {
                    continue;
                }
                for (var o:
                        products) {
                    if (o.getTitle().equals(j.getTitle()) && !strings.contains(o.getTitle())) {
                        text.append(ind).append(" ").append(o.getTitle());
                        for (var k:
                                products) {
                            if (o.getTitle().equals(k.getTitle())) {
                                if (k.getCountAvailable() == 0) {
                                    continue;
                                }
                                Button prButton = new Button(String.valueOf(ind) + " "
                                        + k.getShop(), "changeShop_" + k.getId());
                                currentRow.add(prButton);
                                if (currentRow.size() == 3) {
                                    buttons.add(new ArrayList<>(currentRow));
                                    currentRow.clear();
                                }
                            }
                        }
                        strings.add(o.getTitle());
                    }
                }
                ind++;
                text.append("\n");
            }
            if (!currentRow.isEmpty()) {
                buttons.add(new ArrayList<>(currentRow));
                currentRow.clear();
            }
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(String.valueOf(chatId));
            sendMessage.setText(text.toString());
            InlineKeyboardMarkup markup = KeyboardMarkupBuilder.setKeyboardWithRaw(buttons);
            sendMessage.setReplyMarkup(markup);
            try {
                execute(sendMessage);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }
    private void sendReview(long chatId) {
        User user = userRepository.findById(chatId).get();
        user.setStage(Stage.EnterImageReview);
        userRepository.save(user);
        String text = "Отправляй скорее.                                                                                                                      ❗❗Когда ты отправишь все скрины - нажми кнопку ‘я всё отправила!’ \n" +
                "⚠Если ты её не видишь, нажми на иконку клавиатуры в правом нижнем углу экрана.";

        curImageInReview.put(chatId, new ArrayList<>());
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    private void setCountToItem(long chatId, String message) {
        Product p = curProd.get(chatId);
        p.setCountAvailable(Integer.parseInt(message));
        p.setStat(Stat.Seen);
        productRepository.save(p);
        User u = userRepository.findById(chatId).get();
        u.setStage(Stage.DoingNothing);
        userRepository.save(u);
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText("Товар был добавлен.");
        Button questionButton = new Button("Добавить магазин", "addShop_" + p.getId());
        Button qButton = new Button("Закончить товар", "endAddingItem");
        List<Button> buttons = new ArrayList<>();
        buttons.add(questionButton);
        buttons.add(qButton);
        InlineKeyboardMarkup markup = KeyboardMarkupBuilder.setKeyboard(buttons);
        sendMessage.setReplyMarkup(markup);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    private void endAddingItem(long chatId) {
        sendMessage(chatId, "Добавление товара окончено.");
    }
    private void setShopToItem(long chatId, String message) {
        Product p = curProd.get(chatId);
        p.setShop(message);
        User u = userRepository.findById(chatId).get();
        u.setStage(Stage.EnterCountOfItemToAdd);
        userRepository.save(u);
        sendMessage(chatId, "Введите количества товара для этого магазина");
    }
    private void newItem(long chatId, String message) {
        Product product = new Product();
        product.setNameOfProject("new");
        product.setTitle(message);
        curProd.put(chatId, product);
        sendMessage(chatId, "Введите название магазина, в котором продается продукт");
        User u = userRepository.findById(chatId).get();
        u.setStage(Stage.EnterShopOfItemToAdd);
        userRepository.save(u);
    }
    private void addNewItem(long chatId) {
        sendMessage(chatId, "Введите название нового товара:");
        User u = userRepository.findById(chatId).get();
        u.setStage(Stage.EnterNameOfItemToAdd);
        userRepository.save(u);
    }
    private void howToDelete(long chatId) {
        String text = "Введите название товара, который хотите удалить.";
        SendMessage message = new SendMessage();
        message.setText(text.toString());
        message.setChatId(String.valueOf(chatId));
        /*List<Button> buttons = new ArrayList<>();
        Button orderButton = new Button("Добавить новый товар", "addItem");
        Button questionButton = new Button("Удалить товар", "deleteItem");
        Button questButton = new Button("Изменить товар", "changeItem");
        buttons.add(orderButton);
        buttons.add(questionButton);
        buttons.add(questButton);
        InlineKeyboardMarkup markup = KeyboardMarkupBuilder.setKeyboard(buttons);
        message.setReplyMarkup(markup);*/
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    private void createReview(long chatId, Integer ind) {
        Review or = new Review();
        or.setCreatedAt(LocalDateTime.now());
        or.setStatus(Status.Unseen);
        or.setUser(userRepository.findById(chatId).get());

        or.setProductReservation(currentProdResInReview.get(chatId).get(ind));
        int b = ind;
        var li = currentProdResInReview.get(chatId);
        li.remove(b);
        currentProdResInReview.put(chatId, li);

        reviewRepository.save(or);
        for (var im:
                curImageInReview.get(chatId)) {
            ReviewImage orderImage = new ReviewImage();
            orderImage.setReview(or);
            orderImage.setImage(im);
            reviewImageRepository.save(orderImage);
        }
    }
    private void createOrder(long chatId, Integer ind) {
        Order or = new Order();
        or.setCreatedAt(LocalDateTime.now());
        or.setStatus(Status.Unseen);
        or.setUser(userRepository.findById(chatId).get());

        or.setProductReservation(currentProdResInOrder.get(chatId).get(ind));
        var list = currentProdResInOrder.get(chatId);
        int b = ind;
        var li = currentProdResInReview.get(chatId);
        li.add(list.get(b));
        currentProdResInReview.put(chatId, li);
        list.remove(b);
        currentProdResInOrder.put(chatId, list);

        orderRepository.save(or);
        for (var im:
                curImageInOrder.get(chatId)) {
            OrderImage orderImage = new OrderImage();
            orderImage.setOrder(or);
            orderImage.setImage(im);
            orderImageRepository.save(orderImage);
        }
        //curImageInOrder.put(chatId, new ArrayList<>());
    }
    private void PrintListOfProducts(long chatId, String nameOfProject) {
        var products1 = productRepository
                .findAllByNameOfProject(nameOfProject);
        StringBuilder text = new StringBuilder();
        if (products1.size() == 0) {
            text.append("В проекте ").append(nameOfProject).append(" нет товаров");
        } else {
            List<Product> listOfProd = new ArrayList<>();
            for (var pr:
                    products1) {
                if (pr.getStat().equals(Stat.Unseen)) {
                    continue;
                }
                listOfProd.add(pr);
            }
            Set<String> strings = new HashSet<>();

            int ind = 1;
            for (var i:
                    listOfProd) {
                if (strings.contains(i.getTitle())) {
                    continue;
                }
                for (var j:
                        listOfProd) {
                    if (i.getTitle().equals(j.getTitle()) && !strings.contains(j.getTitle())) {
                        text.append(ind).append(" ").append(i.getTitle());
                        text.append("\n");
                        text.append("\nТовар размещен в магазинах:\n");
                        for (var k:
                                listOfProd) {
                            if (j.getTitle().equals(k.getTitle())) {
                                text.append(k.getShop()).append(" ").append(k.getCountAvailable()).append("\n");
                            }
                        }
                        strings.add(i.getTitle());
                    }
                }
                ind++;
                text.append("\n");
            }
        }
        SendMessage message = new SendMessage();
        message.setText(text.toString());
        message.setChatId(String.valueOf(chatId));
        List<Button> buttons = new ArrayList<>();
        Button orderButton = new Button("Добавить новый товар", "addItem");
        Button questionButton = new Button("Скрыть товар", "deleteItem");
        Button questButton = new Button("Изменить товар", "changeItem");
        buttons.add(orderButton);
        buttons.add(questionButton);
        buttons.add(questButton);
        InlineKeyboardMarkup markup = KeyboardMarkupBuilder.setKeyboard(buttons);
        message.setReplyMarkup(markup);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

    }
    private void ChangeListOfProducts(long chatId) {
        sendMessage(chatId, "Выберите проект, в котором хотите изменить список товаров:");
        User u = userRepository.findById(chatId).get();
        u.setStage(Stage.ChangingList);
        userRepository.save(u);
    }
    private void setReasonForRejectingReview(long moderatorChatId, long userChatId) {
        SendMessage message = new SendMessage();
        String text = "Выберите причину отказа или нажмите на кнопку для ввода вручную";
        List<Button> buttons = new ArrayList<>();
        Button orderButton = new Button("Не та картинка", "noThatImage");
        Button questionButton = new Button("Ввести вручную", "enterManually");
        buttons.add(orderButton);
        buttons.add(questionButton);
        InlineKeyboardMarkup markup = KeyboardMarkupBuilder.setKeyboard(buttons);
        message.setReplyMarkup(markup);
        message.setChatId(String.valueOf(moderatorChatId));
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    private void setReasonForRejectingOrder(long moderatorChatId, long userChatId) {
        SendMessage message = new SendMessage();
        String text = "Выберите причину отказа или нажмите на кнопку для ввода вручную";
        List<Button> buttons = new ArrayList<>();
        Button orderButton = new Button("Не та картинка", "enterReason");
        Button questionButton = new Button("Ввести вручную", "enterReasonManually");
        buttons.add(orderButton);
        buttons.add(questionButton);
        InlineKeyboardMarkup markup = KeyboardMarkupBuilder.setKeyboard(buttons);
        message.setReplyMarkup(markup);
        message.setChatId(String.valueOf(moderatorChatId));
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void moderation(long chatId) {
        String text = "За ваше отсутствие накопилось:\n";
        List<Order> orders = orderRepository.findAllByStatus(Status.Unseen);
        List<Review> reviews = reviewRepository.findAllByStatus(Status.Unseen);
        List<Question> questions = questionRepository.findAllByStatus(Status.Unseen);
        List<Button> buttons = new ArrayList<>();
        text += orders.size() + " заказов\n";
        text += reviews.size() + " отзывов\n";
        text += questions.size() + " вопросов";

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        Button orderButton = new Button("К заказам", "orders");
        Button reviewButton = new Button("К отзывам", "reviews");
        Button questionButton = new Button("К вопросам", "questions");
        buttons.add(orderButton);
        buttons.add(reviewButton);
        buttons.add(questionButton);
        InlineKeyboardMarkup markup = KeyboardMarkupBuilder.setKeyboard(buttons);
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void showListOfUnseenOrders(long chatId) {
        Optional<Order> unseenOrder = orderRepository.findFirstByStatusOrderByCreatedAtDesc(Status.Unseen);
        String text = "";
        if (unseenOrder.isEmpty()) {
            List<Review> reviews = reviewRepository.findAllByStatus(Status.Unseen);
            List<Question> questions = questionRepository.findAllByStatus(Status.Unseen);
            text = "Вы обработали все заказы\n У вас осталось:\n" + reviews.size() + " " +
                    "отзывов\n" + questions.size() + "вопросов";

            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText(text);
            Button reviewButton = new Button("К отзывам", "reviews");
            Button questionButton = new Button("К вопросам", "questions");
            List<Button> buttons = new ArrayList<>();
            buttons.add(reviewButton);
            buttons.add(questionButton);
            InlineKeyboardMarkup markup = KeyboardMarkupBuilder.setKeyboard(buttons);
            message.setReplyMarkup(markup);

            try {
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } else {
            getOrderDetailsWithProductsAndImages(unseenOrder.get(), chatId);
        }
    }
    public void getOrderDetailsWithProductsAndImages(Order order, long chatId) {
        User u = order.getUser();
        ProductReservation pr = order.getProductReservation();

        String text = pr.getProduct().getTitle();
        List<Button> buttons = new ArrayList<>();
        Button aptButton = new Button("Одобрить", "Approved_" + pr.getReservation().getId() + " " + order.getId());
        buttons.add(aptButton);
        Button nextButton = new Button("Отклонить", "Disapproved_" + order.getId());
        buttons.add(nextButton);
        Button reviewButton = new Button("К отзывам", "reviews");
        Button questionButton = new Button("К вопросам", "questions");
        buttons.add(reviewButton);
        buttons.add(questionButton);
        InlineKeyboardMarkup markup = KeyboardMarkupBuilder.setKeyboard(buttons);

        List<OrderImage> orderImages = orderImageRepository.findByOrder(order);
        System.out.println(orderImages.size());
        List<InputMedia> media = new ArrayList<>();

        if (orderImages.size() == 1) {
            for (var image : orderImages) {
                byte[] imageData = image.getImage().getImg();
                InputStream imageStream = new ByteArrayInputStream(imageData);

                SendPhoto sendPhoto = new SendPhoto();
                sendPhoto.setChatId(String.valueOf(chatId));
                sendPhoto.setPhoto(new InputFile(imageStream, "image.jpg"));
                sendPhoto.setCaption(String.valueOf(text));
                sendPhoto.setReplyMarkup(markup);

                try {
                    execute(sendPhoto);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }
        } else {
            for (var image : orderImages) {
                Image im = image.getImage();
                System.out.println(im.getId());
                byte[] imageData = im.getImg();

                InputStream imageStream = new ByteArrayInputStream(imageData);

                SendPhoto sendPhoto = new SendPhoto();
                sendPhoto.setChatId(String.valueOf(chatId));
                sendPhoto.setPhoto(new InputFile(imageStream, "image.jpg"));

                try {
                    execute(sendPhoto);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(String.valueOf(chatId));
            sendMessage.setText(String.valueOf(text));
            sendMessage.setReplyMarkup(markup);
            try {
                execute(sendMessage);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }
    private void showListOfUnseenReviews(long chatId) {
        Optional<Review> unseenOrder = reviewRepository.findFirstByStatusOrderByCreatedAtDesc(Status.Unseen);
        String text = "";
        if (unseenOrder.isEmpty()) {
            List<Order> reviews = orderRepository.findAllByStatus(Status.Unseen);
            List<Question> questions = questionRepository.findAllByStatus(Status.Unseen);
            text = "Вы обработали все отзывы\n У вас осталось:\n" + reviews.size() + " " +
                    "заказов\n" + questions.size() + "вопросов";
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText(text);
            Button reviewButton = new Button("К заказам", "orders");
            Button questionButton = new Button("К вопросам", "questions");
            List<Button> buttons = new ArrayList<>();
            buttons.add(reviewButton);
            buttons.add(questionButton);
            InlineKeyboardMarkup markup = KeyboardMarkupBuilder.setKeyboard(buttons);
            message.setReplyMarkup(markup);

            try {
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } else {
            getReviewDetailsWithProductsAndImages(unseenOrder.get(), chatId);
        }
    }
    public void getReviewDetailsWithProductsAndImages(Review order, long chatId) {
        User u = order.getUser();
        ProductReservation pr = order.getProductReservation();

        String text = pr.getProduct().getTitle();
        List<Button> buttons = new ArrayList<>();
        Button aptButton = new Button("Одобрить", "Approver_" + pr.getReservation().getId() + " " + order.getId());
        buttons.add(aptButton);
        Button nextButton = new Button("Отклонить", "Disapprover_" + order.getId());
        buttons.add(nextButton);
        Button reviewButton = new Button("К заказам", "orders");
        Button questionButton = new Button("К вопросам", "questions");
        buttons.add(reviewButton);
        buttons.add(questionButton);
        InlineKeyboardMarkup markup = KeyboardMarkupBuilder.setKeyboard(buttons);

        List<ReviewImage> orderImages = reviewImageRepository.findByReview(order);
        System.out.println(orderImages.size());
        List<InputMedia> media = new ArrayList<>();

        if (orderImages.size() == 1) {
            for (var image : orderImages) {
                byte[] imageData = image.getImage().getImg();
                InputStream imageStream = new ByteArrayInputStream(imageData);

                SendPhoto sendPhoto = new SendPhoto();
                sendPhoto.setChatId(String.valueOf(chatId));
                sendPhoto.setPhoto(new InputFile(imageStream, "image.jpg"));
                sendPhoto.setCaption(String.valueOf(text));
                sendPhoto.setReplyMarkup(markup);

                try {
                    execute(sendPhoto);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }
        } else {
            for (var image : orderImages) {
                Image im = image.getImage();
                System.out.println(im.getId());
                byte[] imageData = im.getImg();

                InputStream imageStream = new ByteArrayInputStream(imageData);

                SendPhoto sendPhoto = new SendPhoto();
                sendPhoto.setChatId(String.valueOf(chatId));
                sendPhoto.setPhoto(new InputFile(imageStream, "image.jpg"));

                try {
                    execute(sendPhoto);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(String.valueOf(chatId));
            sendMessage.setText(String.valueOf(text));
            sendMessage.setReplyMarkup(markup);
            try {
                execute(sendMessage);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }
    private void showListOfUnseenQuestions(long chatId) {
        Optional<Question> unseenQuestion = questionRepository.findFirstByStatusOrderByCreatedAtDesc(Status.Unseen);
        String text = "";
        if (unseenQuestion.isEmpty()) {
            text = "Вы обработали все вопросы";
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText(text);
            Button reviewButton = new Button("К заказам", "orders");
            Button questionButton = new Button("К отзывам", "reviews");
            List<Button> buttons = new ArrayList<>();
            buttons.add(reviewButton);
            buttons.add(questionButton);
            InlineKeyboardMarkup markup = KeyboardMarkupBuilder.setKeyboard(buttons);
            message.setReplyMarkup(markup);

            try {
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } else {
            getQuestion(unseenQuestion.get(), chatId);
        }
    }

    public void getQuestion(Question question, long chatId) {
        SendMessage message = new SendMessage();
        message.setText(question.getQue());
        message.setChatId(String.valueOf(chatId));
        List<Button> buttons = new ArrayList<>();
        Button answerButton = new Button("Ответить", "answer");
        Button reviewButton = new Button("К заказам", "orders");
        Button questionButton = new Button("К отзывам", "reviews");
        buttons.add(answerButton);
        buttons.add(reviewButton);
        buttons.add(questionButton);
        InlineKeyboardMarkup markup = KeyboardMarkupBuilder.setKeyboard(buttons);
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void showProductList(long chatId) {
        StringBuilder text = new StringBuilder("Создавай космически прекрасные образы с Influence Beauty! Выбирай, " +
                "с каким новинками ты готова покорять Вселенную!\n" +
                "\n" +
                "\uD83D\uDCA1Заказывать нужно только по приведенным ссылкам\n" +
                "\n");
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        List<List<Button>> buttons = new ArrayList<>();
        List<Button> currentRow = new ArrayList<>();
        var products1 = productRepository
                .findAllByNameOfProject("new");
        List<Product> products = new ArrayList<>();
        for (var pr:
             products1) {
            if (pr.getStat().equals(Stat.Unseen)) {
                continue;
            }
            products.add(pr);
        }
        String list = "";
        if (products != null) {
            int i = 1;
            Set<String> strings = new HashSet<>();

            int ind = 1;
            for (var j:
                 products) {
                if (strings.contains(j.getTitle())) {
                    continue;
                }
                for (var o:
                     products) {
                    if (o.getTitle().equals(j.getTitle()) && !strings.contains(o.getTitle())) {
                        text.append(ind).append(" ").append(o.getTitle());
                        for (var k:
                                products) {
                            if (o.getTitle().equals(k.getTitle())) {
                                if (k.getCountAvailable() == 0) {
                                    continue;
                                }
                                Button prButton = new Button(String.valueOf(ind) + " "
                                        + k.getShop(), "shop_" + k.getId());
                                currentRow.add(prButton);
                                if (currentRow.size() == 3) {
                                    buttons.add(new ArrayList<>(currentRow));
                                    currentRow.clear();
                                }
                            }
                        }
                        strings.add(o.getTitle());
                    }
                }
                ind++;
                text.append("\n");
            }
            if (!currentRow.isEmpty()) {
                buttons.add(new ArrayList<>(currentRow));
                currentRow.clear();
                currentRow.add(new Button("Я закончил делать бронирования", "endToDoReservations"));
                buttons.add(new ArrayList<>(currentRow));
                currentRow.clear();
            }
        }
        String text1 = "❗️Количество мест ограничено. Ничего не заказывай, пока не получишь " +
                "от меня подтверждения бронирования.\n" +
                "\n" +
                " Ты можешь выбрать несколько продуктов для тестирования. Бронировать мы будем с " +
                "тобой по одному продукту, последовательно. Пожалуйста, выбери первый продукт, " +
                "который ты хочешь забронировать.";
        message.setText(text + list + text1);
        InlineKeyboardMarkup markup = KeyboardMarkupBuilder.setKeyboardWithRaw(buttons);
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    private void sendScreen(long chatId) {
        User user = userRepository.findById(chatId).get();
        user.setStage(Stage.EnterImageOrder);
        userRepository.save(user);
        String text = "Отправляй скорее.                                                                                                                      ❗❗Когда ты отправишь все скрины - нажми кнопку ‘я всё отправила!’ \n" +
                "⚠Если ты её не видишь, нажми на иконку клавиатуры в правом нижнем углу экрана.";

        curImageInOrder.put(chatId, new ArrayList<>());
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    private byte[] downloadFileAsBytes(String fileId) throws TelegramApiException {
        GetFile getFile = new GetFile(fileId);
        File file = execute(getFile);
        String fileUrl = "https://api.telegram.org/file/bot" + getBotToken() + "/" + file.getFilePath();

        try (InputStream inputStream = new URL(fileUrl).openStream()) {
            return inputStream.readAllBytes();
        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0];
        }
    }
    private void ThanksForAnswer(long chatId) {
        String text = "Спасибо за ответ!";
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        Button yesButton = new Button("Продолжить", "N");
        List<Button> buttons = new ArrayList<>();
        buttons.add(yesButton);
        InlineKeyboardMarkup markup = KeyboardMarkupBuilder.setKeyboard(buttons);
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    private void ProductFamiliarization(long chatId) {
        String text1 = "\uD83D\uDCA5 А теперь давай познакомимся с продуктами Influence Beauty, " +
                "участвующими в программе прямо сейчас.";
        String text2 = "";
    }
    private void AskTolic(long chatId) {
        String text = "Связь с Толиком, который ответит тебе на любые вопрос. Задай свой вопрос.";
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        Button askButton = new Button("Задать вопрос", "AskQuestion");
        List<Button> buttons = new ArrayList<>();
        buttons.add(askButton);
        InlineKeyboardMarkup markup = KeyboardMarkupBuilder.setKeyboard(buttons);
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    private void AskQuestion(long chatId) {
        String Text = "Отправляй скорее.";
        User user = userRepository.findById(chatId).get();
        user.setStage(Stage.AskingQuestion);
        userRepository.save(user);
        sendMessage(chatId, Text);
    }
    private void sendFirstPageOfProgram(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(program.get(0));
        currentInds.put(chatId, 0);
        Button yesButton = new Button("Давай", "Next");
        List<Button> buttons = new ArrayList<>();
        buttons.add(yesButton);
        InlineKeyboardMarkup markup = KeyboardMarkupBuilder.setKeyboard(buttons);
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    private void getTextOfProgramPage(long chatId, long messageId) {
        EditMessageText confirmText = new EditMessageText();
        confirmText.setChatId(String.valueOf(chatId));
        confirmText.setText(program.get(currentInds.get(chatId)));
        if (currentInds.get(chatId) < program.size() - 1) {
            Button yesButton = new Button("Далее", "Next");
            List<Button> buttons = new ArrayList<>();
            buttons.add(yesButton);
            InlineKeyboardMarkup markup = KeyboardMarkupBuilder.setKeyboard(buttons);
            confirmText.setReplyMarkup(markup);
        }
        confirmText.setMessageId((int) messageId);
        try {
            execute(confirmText);
        } catch (TelegramApiException e) {
        }
        if (currentInds.get(chatId) == program.size() - 1) {
            SendPoll sendPoll = new SendPoll();
            sendPoll.setChatId(String.valueOf(chatId));
            sendPoll.setQuestion("Теперь ты знаешь всё о программе. И я хочу спросить, " +
                    "что тебя привлекает в ней большего всего:");
            sendPoll.setOptions(Arrays.asList("Возможность попробовать новые продукты",
                    "Возможность выиграть классные призы", "Возможность поделиться с брендом своим мнением "));
            sendPoll.setIsAnonymous(true);

            try {
                execute(sendPoll);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }
    private void saveFileToDatabase(long chatId, byte[] fileData, Long userId, Category category) {
        User us;
        if(!userRepository.findById(userId).isEmpty()) {
            Optional<User> u = userRepository.findById(userId);
            us = u.get();
            Image photo = new Image();
            photo.setCategory(category);
            photo.setCreatedAt(LocalDateTime.now());
            photo.setUser(us);
            photo.setImg(fileData);
            photo.setTitle("new");
            Image saved = imageRepository.save(photo);
            if (category.equals(Category.Order)) {
                var c = curImageInOrder.get(chatId);
                c.add(saved);
                curImageInOrder.put(chatId, c);
            } else {
                var c = curImageInReview.get(chatId);
                c.add(saved);
                curImageInReview.put(chatId, c);
            }
        }
    }
    private void sendTextsAfterModerationReview(long chatId) {
        sendMessage(chatId, "Напомню, что мы ежемесячно разыгрываем призы среди активных " +
                "участников программы. Если ты станешь победителем – мы обязательно тебя проинформируем. " +
                "Список победителей мы будем размещать здесь.");
        String text = "\uD83C\uDF80 Спасибо за участие в программе! Ты просто супер " +
                "\uD83E\uDD29\uD83E\uDD29\uD83E\uDD29 ! Кстати, ты можешь продолжить и попробовать другие продукты, " +
                "если хочешь.                                                            " +
                "Ежемесячно ты можешь пробовать 1-2 продукта бренда.";
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        Button continueButton = new Button("Хочу ещё", "WantMore");
        Button endButton = new Button("Спасибо я всё", "End");
        List<Button> buttons = new ArrayList<>();
        buttons.add(continueButton);
        buttons.add(endButton);
        InlineKeyboardMarkup markup = KeyboardMarkupBuilder.setKeyboard(buttons);
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    public void registerUser(Message msg) {
        if(userRepository.findById(msg.getChatId()).isEmpty()) {

            var chatId = msg.getChatId();
            var chat = msg.getChat();

            User user = new User();

            user.setChatId(chatId);
            user.setName(chat.getFirstName());
            user.setUserName(chat.getUserName());
            user.setNumberOfInvitedUsers(0);
            user.setStage(Stage.EnterFirstName);
            if (chatId == 959316826L) {
                user.setRole(Role.Admin);
            } else {
                user.setRole(Role.Customer);
            }

            userRepository.save(user);
            currentInds.put(chatId, 0);
            currentProdResInReview.put(chatId, new ArrayList<>());
            currentProdResInOrder.put(chatId, new ArrayList<>());
            curProdToR.put(chatId, new ArrayList<>());
            hasInvited.put(chatId, true);
            if (chatId == 959316826L) {
                usersRole.put(chatId, Role.Admin);
            } else {
                usersRole.put(chatId, Role.Customer);
            }
            if (userRepository.findById(959316826L).isPresent()) {
                sendMessage(959316826L, "Пользователь " + user.getUserName() + " присоединился");
            }
        } else {
            sendMessage(msg.getChatId(), "Рады видеть вас в нашем новом проекте");
            if (userRepository.findById(959316826L).isPresent()) {
                sendMessage(959316826L, "Пользователь " +
                        userRepository.findById(msg.getChatId()).get().getUserName() + " вошел в новый проект.");
            }
        }

    }
    private void sendRulesAndTermsOfUse(long chatId) {
        String str ="Более подробно с Правилами участия ты можешь ознакомиться по ссылкам ниже. " +
                "Чтобы продолжить участвовать необходимо подтвердить, что <a href='" + URL_To_Rules + "'>Правила</a> и " +
                "<a href='" + URL_TO_TERMS_OF_USE + "'>Условия Использования Сервиса</a> " +
                "прочитаны и принимаются тобой. Для этого нажми кнопку «Соглашаюсь»";
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(str);
        Button yesButton = new Button("Соглашаюсь", "Confirm");
        List<Button> buttons = new ArrayList<>();
        buttons.add(yesButton);
        InlineKeyboardMarkup markup = KeyboardMarkupBuilder.setKeyboard(buttons);
        message.setReplyMarkup(markup);
        message.setParseMode("HTML");
        message.disableWebPagePreview();

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    private void editMessageAfterAgreeRulesAndTerms(long chatId, long messageId) {
        EditMessageText confirmText = new EditMessageText();
        confirmText.setChatId(String.valueOf(chatId));
        String str2 = "Более подробно с Правилами участия ты можешь ознакомиться по ссылкам ниже.\n" +
                "Чтобы продолжить участвовать необходимо подтвердить, что " +
                "<a href='" + URL_To_Rules + "'>Правила</a> и " +
                "<a href='" + URL_TO_TERMS_OF_USE + "'>Условия Использования Сервиса</a> " +
                "прочитаны и принимаются тобой.";
        confirmText.setText(str2);
        confirmText.setParseMode("HTML");
        confirmText.disableWebPagePreview();
        confirmText.setMessageId((int) messageId);
        try {
            execute(confirmText);
        } catch (TelegramApiException e) {
        }
    }
    private void editMessageAfterChooseInvited(long chatId, long messageId) {
        hasInvited.put(chatId, false);
        EditMessageText confirmText = new EditMessageText();
        confirmText.setChatId(String.valueOf(chatId));
        String str2 = "Скажи, пожалуйста, в программу тебя пригласила подруга \uD83D\uDC6D?";
        confirmText.setText(str2);
        confirmText.setMessageId((int) messageId);
        try {
            execute(confirmText);
        } catch (TelegramApiException e) {
        }
    }
    private void sendTextAfterEnd(long chatId) {
        sendMessage(chatId, "Ок. Еще раз спасибо за участие. Я был рад познакомиться. " +
                "До новых встреч в наших программах!");
        sendMessage(chatId, "Приглашаю тебя присоединиться к группе ТОЛК по ссылке https://t.me/TolkRus - " +
                "ты будешь в курсе всех новостей по действующим проектам, в числе первых узнавать о новых проектах и " +
                "сможешь обсуждать проекты с другими участницами");
    }
    private void sendTextAfterMore(long chatId) {
        sendMessage(chatId, "\uD83D\uDE0A Круто!                                                           " +
                "                                               Напоминаю тебе полный список продуктов, которые " +
                "ты можешь попробовать \uD83E\uDDF4.");
    }
    private void setUserCommands(long chatId) {
        List<BotCommand> listOfCommands = new ArrayList<>();
        User user = userRepository.findById(chatId).get();
        if (user.getRole().equals(Role.Customer)) {
            listOfCommands.add(new BotCommand("/start", "Перезагрузка бота"));
            listOfCommands.add(new BotCommand("/rules", "Правила"));
            listOfCommands.add(new BotCommand("/policy", "Политика"));
            listOfCommands.add(new BotCommand("/info", "Как работает программа"));
            listOfCommands.add(new BotCommand("/product", "Продукты"));
            listOfCommands.add(new BotCommand("/screen", "Отправить скрин заказа"));
            listOfCommands.add(new BotCommand("/review", "Отправить скрин отзыва"));
            listOfCommands.add(new BotCommand("/help", "Задать вопрос Толику"));
        } else if (user.getRole().equals(Role.Moderator)) {
            listOfCommands.add(new BotCommand("/moderation", "Модерация"));
        } else {
            listOfCommands.add(new BotCommand("/moderation", "Модерация"));
            listOfCommands.add(new BotCommand("/change", "Изменение списка товаров"));
            listOfCommands.add(new BotCommand("/employee", "Управление сотрудниками"));
        }
        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            System.out.println(e.getMessage());
        }
    }
    private void sendImages(long chatId) {
        String text = "\uD83E\uDD73 Отлично. Отправляй скрин отзыва. \n" +
                "\uD83D\uDCA1 Ты можешь отправить одно или несколько изображений последовательно. \n" +
                "⚠ Если не все твои отзывы ещё загружены – отправляй те, что уже есть.";
        String text1 = "❗❗Для отправки следующих скринов отзывов, в любой момент зайди в меню и " +
                "выбери там команду «Отправляю скрин отзыва» . \uD83D\uDCDD После нажатия ты сможешь приложить и " +
                "отправить мне новые скрины опубликованных отзывов.";
        sendMessage(chatId, text);
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text1);
        Button yesButton = new Button("Я все отправила", "sentItAll");
        List<Button> buttons = new ArrayList<>();
        buttons.add(yesButton);
        InlineKeyboardMarkup markup = KeyboardMarkupBuilder.setKeyboard(buttons);
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    private void tryToGetAcquainted(long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText("Супер! \uD83C\uDF89 Теперь давай познакомимся. " +
                "Скажи мне как тебя зовут, пожалуйста.");

        User user = userRepository.findById(chatId).get();
        List<List<Button>> buttons = new ArrayList<>();
        if (user.getRole().equals(Role.Customer)) {
            List<Button> row1 = new ArrayList<>();
            row1.add(new Button(PRODUCT, ""));
            buttons.add(row1);
            List<Button> row2 = new ArrayList<>();
            row2.add(new Button(SENDORDERIMAGE, ""));
            row2.add(new Button(SENDREVIEWIMAGE, ""));
            buttons.add(row2);
            List<Button> row3 = new ArrayList<>();
            row3.add(new Button(ASKTOLIC, ""));
            buttons.add(row3);
        } else if (user.getRole().equals(Role.Moderator)) {
            List<Button> row1 = new ArrayList<>();
            row1.add(new Button(MODERATION, ""));
            buttons.add(row1);
        } else if (user.getRole().equals(Role.Admin)) {
            List<Button> row1 = new ArrayList<>();
            row1.add(new Button(MODERATION, ""));
            row1.add(new Button(CHANGE, ""));
            buttons.add(row1);
            List<Button> row2 = new ArrayList<>();
            row2.add(new Button(EMPLOYEE, ""));
            buttons.add(row2);
        }
        sendMessage.setReplyMarkup(KeyboardMarkupBuilder.setReplyKeyboardWithRaw(buttons));
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        User us = userRepository.findById(chatId).get();
        us.setStage(Stage.EnterFirstName);
        userRepository.save(us);
    }
    private void greatings(long chatId) {
        String text = usersName.get(chatId) + ", приятно познакомиться! Подсказка – внизу есть кнопка «Меню». " +
                "Там есть все нужные тебе команды.";
        sendMessage(chatId, text);
    }
    private void SetUserName(long chatId, String messageText) {
        usersName.put(chatId, messageText);
        User user = userRepository.findById(chatId).get();
        user.setStage(Stage.CheckingInviting);
        userRepository.save(user);
    }
    private void SetUserNameOfFriend(long chatId, String messageText) {
        if(userRepository.findByUserName(messageText).isPresent()){
            Optional<User> u = userRepository.findByUserName(messageText);
            User us = u.get();
            int count = us.getNumberOfInvitedUsers();
            us.setNumberOfInvitedUsers(count + 1);
            userRepository.save(us);
        }
        hasInvited.put(chatId, true);
    }
    private void send(long chatId) {
        String text = "Твоя подруга теперь ближе к призам за приглашения в программу! " +
                "Кстати, ты тоже можешь приглашать друзей и выиграть специальный приз сертификат в OZON на 4000 рублей. " +
                "В любой момент напиши мне /friends, и я расскажу тебе подробности.";
        sendMessage(chatId, text);

    }
    private void inviteFriend(long chatId) {
        String text = "Если ты захочешь пригласить кого-то в программу и выиграть за это специальный приз - " +
                "сертификат в OZON на 4000 рублей, то ты можешь в любой момент написать мне /friends или " +
                "выбрать этот пункт в меню. Я расскажу тебе подробности.";
        sendMessage(chatId, text);
    }
    private void invitedByFriend(long chatId) {
        String text = "Cообщи, пожалуйста, её имя пользователя в telegram (пример @username), " +
                "чтобы она смогла принять участие в конкурсе \uD83D\uDCA5 за приглашенных друзей!";
        sendMessage(chatId, text);
    }
    private void startTalkingAboutProgram(long chatId) {
        currentInds.put(chatId, 0);
        String text = "А пока давай я расскажу тебе про программу!";
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        Button nextButton = new Button("Давай", "Next");
        List<Button> buttons = new ArrayList<>();
        buttons.add(nextButton);
        InlineKeyboardMarkup markup = KeyboardMarkupBuilder.setKeyboard(buttons);
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
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
    private void sendRules(Long chatId) {
        String str = "Правила программы ты может прочитать <a href='" + URL_To_Rules + "'>тут</a>.";
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(str);

        message.setParseMode("HTML");
        message.disableWebPagePreview();
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    private void sendTermsOfUse(Long chatId) {
        String str = "Правила пользования сервисом ты может прочитать <a href='" + URL_TO_TERMS_OF_USE + "'>тут</a>.";
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(str);

        message.setParseMode("HTML");
        message.disableWebPagePreview();
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    private void sendInfo(Long chatId) {
        String str = "Пробуй, делись мнением, выигрывай призы!\n" +
                "Мы вернем тебе стоимость продукта в обмен на твоё честное мнение. Каждый месяц мы разыгрываем более " +
                "20 призов среди участников, в том числе сертификат на 10.000 рублей.\n" +
                "В проекте участвуют 10 продуктов Professor SkinGood, и ты можешь все их попробовать.\n" +
                "Покупать продукт не обязательно, если ты им уже пользовалась - можешь сразу поделиться своим мнением.";
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(str);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    private void friendInviteYou(Long chatId) {
        User user = userRepository.findById(chatId).get();
        user.setStage(Stage.CheckingInviting);
        userRepository.save(user);
        String str = "Скажи, пожалуйста, в программу тебя пригласила подруга \uD83D\uDC6D?";
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(str);
        Button yesButton = new Button("Да", "Yes");
        Button noButton = new Button("Нет", "No");
        List<Button> buttons = new ArrayList<>();
        buttons.add(yesButton);
        buttons.add(noButton);
        InlineKeyboardMarkup markup = KeyboardMarkupBuilder.setKeyboard(buttons);
        message.setReplyMarkup(markup);

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
