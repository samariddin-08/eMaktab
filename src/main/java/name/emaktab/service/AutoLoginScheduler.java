// src/main/java/name/emaktab/scheduler/AutoLoginScheduler.java
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
     * Har kuni soat 08:00 (Asia/Samarkand) da ishlaydi.
     * Zone: sizning system timezone bo'yicha "Asia/Samarkand" ishlatildi.
     */
    @Scheduled(cron = "0 24 19 * * *", zone = "Asia/Samarkand")
    @Transactional
    public void runAutoLoginForAllUsers() {
        List<User> users = userRepository.findAll();
        for (User user : users) {
            try {
                String chatId = user.getTelegramId();

                telegramBot.sendMessage(chatId, "🔁 Avtomatik kirish urinish boshlanmoqda..."+user.getUsername());

                // Call login service (metod nomi mos bo‘lsa o‘zgartiring)
                LoginResult result = loginService.loginAndGetCookies(user.getUsername(), user.getPassword());

                // Update entity
                user.setLastLogin(LocalDateTime.now(ZoneId.of("Asia/Samarkand")));
                user.setLastLoginMessage(result.message);

                if (result.success) {
                    // save cookies as JSON
                    String cookiesJson = gson.toJson(result.cookies);
                    user.setCookie(cookiesJson);

                    // set approximate expiry (if API returns expiry parse it, else fallback)
                    user.setCookieExpiry(LocalDateTime.now(ZoneId.of("Asia/Samarkand")).plusDays(3));

                    userRepository.save(user);

                    telegramBot.sendMessage(chatId.toString(), "✅ Avtomatik login muvaffaqiyatli bo'ldi. Cookie yangilandi.");
                } else {
                    // failure: if unknown due to captcha, ask user to solve
                    userRepository.save(user);

                    String message = "❌ Avtomatik kirish amalga oshmadi: " + result.message;
                    telegramBot.sendMessage(chatId.toString(), message);
                }

            } catch (Exception ex) {
                ex.printStackTrace();
                // log + notify user/admin
                String err = "⚠️ Avtomatik kirishda xato yuz berdi: " + ex.getMessage();
                // notify user if possible
                try {
                    telegramBot.sendMessage(user.getTelegramId().toString(), err);
                } catch (Exception e) { /* ignore */ }
            }
        }
    }
}
