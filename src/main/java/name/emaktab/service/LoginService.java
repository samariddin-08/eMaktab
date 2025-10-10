package name.emaktab.service;

import name.emaktab.entity.User;
import name.emaktab.payload.LoginResult;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;


@Service
public class LoginService {

        private static final String LOGIN_PAGE = "https://login.emaktab.uz/login";

        public LoginResult loginAndGetCookies(String username, String password) {
            try {
                // 1️⃣ GET sahifa — CSRF token va cookie olish
                Connection.Response getRes = Jsoup.connect(LOGIN_PAGE)
                        .method(Connection.Method.GET)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                        .timeout(10000)
                        .execute();

                Map<String, String> cookies = getRes.cookies();
                String csrf = getRes.parse()
                        .select("input[name=_token]")
                        .attr("value");

                // 2️⃣ POST — login, parol, va token bilan
                Connection.Response postRes = Jsoup.connect(LOGIN_PAGE)
                        .cookies(cookies)
                        .data("_token", csrf)
                        .data("login", username)
                        .data("password", password)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                        .referrer(LOGIN_PAGE)
                        .method(Connection.Method.POST)
                        .followRedirects(true)
                        .execute();

                String body = postRes.body().toLowerCase(Locale.ROOT);

                if (body.contains("logout") || body.contains("профиль")) {
                    return LoginResult.success(postRes.cookies(), "Login successful!");

                } else if (body.contains("неверный") || body.contains("ошибка")) {
                    return LoginResult.failed("Wrong login or password.", postRes.cookies());
                } else {
                    return LoginResult.unknown(postRes.cookies(), "Could not determine login result.");
                }

            } catch (Exception e) {
                return LoginResult.failed("Error: " + e.getMessage(), Collections.emptyMap());
            }
        }

        // 🔹 Helper DTO ichki sinf
        public static class LoginResult {
            public final boolean success;
            public final Map<String,String> cookies;
            public final String message;

            private LoginResult(boolean success, Map<String,String> cookies, String message) {
                this.success = success;
                this.cookies = cookies;
                this.message = message;
            }

            public static LoginResult success(Map<String,String> cookies, String message) {
                return new LoginResult(true, cookies, message);
            }

            public static LoginResult failed(String message, Map<String,String> cookies) {
                return new LoginResult(false, cookies, message);
            }

            public static LoginResult unknown(Map<String,String> cookies, String message) {
                return new LoginResult(false, cookies, message);
            }
        }


}
