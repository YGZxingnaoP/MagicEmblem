package com.magicemblem.school;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.magicemblem.MagicEmblem;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 学校密码管理器
 * 
 * 从 school_passwords.json 读取各学校的硬编码密码。
 * 认证流程：
 * 1. 玩家输入学号 + 密码
 * 2. 系统将输入的密码与 school_passwords.json 中的硬编码密码比对
 * 3. 若匹配则认证成功
 * 
 * 密码文件不应提交到版本控制（已在.gitignore中排除）。
 */
public class SchoolPasswordManager {

    /** 学校ID -> 密码 的映射 */
    private static Map<String, String> passwords = new HashMap<>();

    /**
     * 加载密码配置文件
     * 从 classpath 中的 school_passwords.json 读取
     */
    public static void loadPasswords() {
        try (InputStream is = SchoolPasswordManager.class.getClassLoader()
                .getResourceAsStream("school_passwords.json")) {
            if (is == null) {
                MagicEmblem.LOGGER.warn("school_passwords.json not found, using empty password map");
                return;
            }
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, String>>() {}.getType();
            passwords = gson.fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), type);
            MagicEmblem.LOGGER.info("Loaded {} school passwords", passwords.size());
        } catch (Exception e) {
            MagicEmblem.LOGGER.error("Failed to load school_passwords.json", e);
        }
    }

    /**
     * 获取指定学校的密码
     * @param schoolId 学校标识（如 "USST"）
     * @return 密码，若不存在返回null
     */
    public static String getPassword(String schoolId) {
        return passwords.get(schoolId);
    }

    /**
     * 本地验证：比对用户输入的密码与硬编码密码
     * 
     * @param schoolId 学校标识（如 "USST"）
     * @param inputPassword 用户输入的密码
     * @return true 密码匹配，false 不匹配
     */
    public static boolean authenticate(String schoolId, String inputPassword) {
        String storedPassword = getPassword(schoolId);
        if (storedPassword == null) {
            MagicEmblem.LOGGER.error("No password configured for school: {}", schoolId);
            return false;
        }
        return storedPassword.equals(inputPassword);
    }
}
