package name.emaktab.telegram;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import name.emaktab.entity.User;
import name.emaktab.payload.LoginResult;
import name.emaktab.repository.UserRepository;
import name.emaktab.service.LoginService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class EmaktabBot extends TelegramLongPollingBot {

    private final LoginService emaktabBot; // bizning login servisini inject qilamiz
    private final UserRepository userRepository;

    @Value("${telegram.bot.username}")
    private String botUsername;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @SneakyThrows
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String chatId = update.getMessage().getChatId().toString();
            String text = update.getMessage().getText().trim();

            if (text.equals("/start")) {
                sendMessage(chatId, "👋 Assalomu alaykum!\nIltimos login va parolni yuboring.\nMasalan: `login123 parol123`");
                return;
            }

            String[] parts = text.split(" ");
            if (parts.length != 2) {
                sendMessage(chatId, "⚠️ Iltimos, login va parolni to‘g‘ri yozing: `login parol`");
                return;
            }

            String login = parts[0];
            String password = parts[1];

            System.out.println(login + " " + password);
            // 🔐 Loginni tekshiramiz

            LoginService.LoginResult result = emaktabBot.loginAndGetCookies(login, password);
            if (result.success) {
                Optional<User> byTelegramId = userRepository.findByTelegramId(chatId);
                if (byTelegramId.isPresent()) {
                    User user = byTelegramId.get();
                    user.setUsername(login);
                    user.setPassword(password);
                    userRepository.save(user);
                }else {
                    User user = new User();
                    user.setUsername(login);
                    user.setPassword(password);
                    user.setTelegramId(chatId);
                    userRepository.save(user);
                }

                sendMessage(chatId, "✅ Muvaffaqiyatli tizimga kirdingiz!\n" + result.message);
            } else {
                sendMessage(chatId, "❌ Login amalga oshmadi.\n" + result.message);
            }
        }
    }

    public void sendMessage(String chatId, String text) {
        try {
            execute(SendMessage.builder().chatId(chatId).text(text).build());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
