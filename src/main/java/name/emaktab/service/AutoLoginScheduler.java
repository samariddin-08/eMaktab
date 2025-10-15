package name.emaktab.service;

import com.google.gson.Gson;
import name.emaktab.entity.User;
import name.emaktab.repository.UserRepository;
import name.emaktab.service.LoginService.LoginResult;
import name.emaktab.telegram.EmaktabBot;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class AutoLoginScheduler {

    private final UserRepository userRepository;
    private final LoginService loginService;
    private final EmaktabBot telegramBot;
    private final Gson gson = new Gson();

    public AutoLoginScheduler(UserRepository userRepository,
                              LoginService loginService,
                              EmaktabBot telegramBot) {
        this.userRepository = userRepository;
        this.loginService = loginService;
        this.telegramBot = telegramBot;
    }

    /**
     * Har kuni soat 23:27 da (Asia/Samarkand) ishlaydi.
     */
    @Scheduled(cron = "00 50 23 * * ?", zone = "Asia/Samarkand")
    @Transactional
    public void runAutoLoginForAllUsers() {
        // telegramId bo'yicha foydalanuvchilarni guruhlash
        Map<String, List<User>> usersByTelegramId = userRepository.findAll()
                .stream()
                .collect(Collectors.groupingBy(User::getTelegramId));

        for (Map.Entry<String, List<User>> entry : usersByTelegramId.entrySet()) {
            String chatId = entry.getKey();
            List<User> users = entry.getValue();

            try {
                // Faqat bir marta boshlang'ich xabar
                StringBuilder initialMessage = new StringBuilder("🔁 Avtomatik kirish urinish boshlanmoqda:\n");
                users.forEach(user -> initialMessage.append("- ").append(user.getUsername()).append("\n"));
                telegramBot.sendMessage(chatId, initialMessage.toString());

                StringBuilder resultMessage = new StringBuilder();
                boolean allSuccess = true;

                for (User user : users) {
                    LoginResult result = loginService.loginAndGetCookies(user.getUsername(), user.getPassword());
                    user.setLastLogin(LocalDateTime.now(ZoneId.of("Asia/Samarkand")));
                    user.setLastLoginMessage(result.message);

                    if (result.success) {
                        String cookiesJson = gson.toJson(result.cookies);
                        user.setCookie(cookiesJson);
                        user.setCookieExpiry(LocalDateTime.now(ZoneId.of("Asia/Samarkand")).plusDays(3));
                        resultMessage.append("✅ ").append(user.getUsername()).append(": Login muvaffaqiyatli\n");
                    } else {
                        allSuccess = false;
                        resultMessage.append("❌ ").append(user.getUsername()).append(": ").append(result.message).append("\n");
                    }
                    userRepository.save(user);
                }

                // Faqat bitta natija xabari
                if (resultMessage.length() > 0) {
                    telegramBot.sendMessage(chatId, resultMessage.toString());
                }

            } catch (Exception ex) {
                String err = "⚠️ Avtomatik kirishda umumiy xato yuz berdi: " + ex.getMessage();
                telegramBot.sendMessage(chatId, err);
            }
        }
    }
}