package name.emaktab.telegram;

import name.emaktab.entity.User;
import name.emaktab.payload.LoginResult;
import name.emaktab.repository.UserRepository;
import name.emaktab.service.LoginService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Component
public class EmaktabBot extends TelegramLongPollingBot {

    private static final Logger logger = LoggerFactory.getLogger(EmaktabBot.class);

    private final LoginService loginService;
    private final UserRepository userRepository;
    private final String botUsername;
    private final String botToken;

    // Lombok o'rniga qo'lda to'g'ri konstruktor yozdik.
    // Bu bot tokenini null bo'lib qolishidan va dastur o'chib qolishidan himoya qiladi.
    public EmaktabBot(
            LoginService loginService,
            UserRepository userRepository,
            @Value("${telegram.bot.username}") String botUsername,
            @Value("${telegram.bot.token}") String botToken) {

        // Telegram kutubxonasining o'ziga tokenni uzatamiz
        super(botToken);

        this.loginService = loginService;
        this.userRepository = userRepository;
        this.botUsername = botUsername;
        this.botToken = botToken;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String chatId = update.getMessage().getChatId().toString();
            String text = update.getMessage().getText().trim();

            if (text.equals("/start")) {
                sendMessage(chatId, "👋 Assalomu alaykum!\nIltimos login va parolni yuboring.\nMasalan: `login123 parol123`");
                return;
            }

            String[] parts = text.split("\\s+", 2);
            if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
                sendMessage(chatId, "⚠️ Iltimos, login va parolni to‘g‘ri yozing: `login parol`");
                return;
            }

            String login = parts[0];
            String password = parts[1];

            logger.info("Login attempt: {} for chatId: {}", login, chatId);


            LoginService.LoginResult result = loginService.loginAndGetCookies(login, password);
            if (result == null || !result.success) {
                sendMessage(chatId, "❌ Login amalga oshmadi.\nUsername yoki parolni tekshiring! " );
                return;
            }


            // Telegram ID bo'yicha barcha foydalanuvchilarni qidirish
            List<User> usersByTelegramId = userRepository.findAllByTelegramId(chatId);
            for (User existingUser : usersByTelegramId) {
                if (existingUser.getUsername().equals(login)) {
                    sendMessage(chatId, "🚫 Ushbu foydalanuvchi allaqachon ro‘yxatdan o‘tgan!");
                    return;
                }
            }

            // Yangi foydalanuvchi qo'shish
            User user = new User();
            user.setUsername(login);
            user.setPassword(password);
            user.setTelegramId(chatId);
            userRepository.save(user);

            sendMessage(chatId, "✅ Muvaffaqiyatli tizimga kirdingiz!\n" + result.message); // getMessage() metodiga moslashtirildi
        }
    }

    public void sendMessage(String chatId, String text) {
        try {
            execute(SendMessage.builder()
                    .chatId(chatId)
                    .text(text)
                    .parseMode("Markdown")
                    .build());
        } catch (Exception e) {
            logger.error("Failed to send message to chatId: {}, error: {}", chatId, e.getMessage());
            try {
                execute(SendMessage.builder()
                        .chatId(chatId)
                        .text("⚠️ Xatolik yuz berdi, iltimos keyinroq urinib ko'ring.")
                        .build());
            } catch (Exception ex) {
                logger.error("Failed to send error message: {}", ex.getMessage());
            }
        }
    }
}
