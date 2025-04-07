package com.valorburst.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class PhoneGenerator {
    private static final Map<String, RegionPhoneConfig> REGION_CONFIG = new HashMap<>();
    private static final Map<String, List<String>> PREFIX_DATA = loadPhonePrefixes();
    private static final Map<String, String> SHORT_TO_REGION = new HashMap<>();

    static {
        REGION_CONFIG.put("en_AU", new RegionPhoneConfig(9, "+61"));
        REGION_CONFIG.put("zh_TW", new RegionPhoneConfig(10, "+886"));
        REGION_CONFIG.put("en_US", new RegionPhoneConfig(10, "+1"));
        REGION_CONFIG.put("th_TH", new RegionPhoneConfig(9, "+66"));
        REGION_CONFIG.put("vi_VN", new RegionPhoneConfig(10, "+84"));
        REGION_CONFIG.put("en_SG", new RegionPhoneConfig(8, "+65"));
        REGION_CONFIG.put("ms_MY", new RegionPhoneConfig(9, "+60"));
        REGION_CONFIG.put("id_ID", new RegionPhoneConfig(11, "+62"));
        REGION_CONFIG.put("en_CA", new RegionPhoneConfig(10, "+1"));
        REGION_CONFIG.put("en_GB", new RegionPhoneConfig(10, "+44"));

        SHORT_TO_REGION.put("au", "en_AU");
        SHORT_TO_REGION.put("zh", "zh_TW");
        SHORT_TO_REGION.put("en", "en_US");
        SHORT_TO_REGION.put("th", "th_TH");
        SHORT_TO_REGION.put("vi", "vi_VN");
        SHORT_TO_REGION.put("sg", "en_SG");
        SHORT_TO_REGION.put("my", "ms_MY");
        SHORT_TO_REGION.put("id", "id_ID");
        SHORT_TO_REGION.put("ca", "en_CA");
        SHORT_TO_REGION.put("gb", "en_GB");
    }

    public static String generatePhone(String shortCode) {
        String region = SHORT_TO_REGION.getOrDefault(shortCode, "en_US");

        RegionPhoneConfig config = REGION_CONFIG.getOrDefault(region, REGION_CONFIG.get("en_US"));
        List<String> prefixes = PREFIX_DATA.getOrDefault(region, PREFIX_DATA.get("en_US"));

        String prefix = getRandomElement(prefixes);
        int remainingLength = config.length - prefix.length();

        StringBuilder sb = new StringBuilder();
        sb.append(config.countryCode).append(prefix);

        for (int i = 0; i < remainingLength; i++) {
            sb.append((int) (Math.random() * 10));
        }

        return sb.toString();
    }

    private static String getRandomElement(List<String> list) {
        return list.get(new Random().nextInt(list.size()));
    }

    private static Map<String, List<String>> loadPhonePrefixes() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream is = PhoneGenerator.class.getResourceAsStream("/static/phone_prefixes.json");
            return mapper.readValue(is, new TypeReference<Map<String, List<String>>>() {});
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyMap();
        }
    }

    private static class RegionPhoneConfig {
        int length;
        String countryCode;

        RegionPhoneConfig(int length, String countryCode) {
            this.length = length;
            this.countryCode = countryCode;
        }
    }

    public static void main(String[] args) {
        for (int i = 0; i < 10; i++) {
            System.out.println(generatePhone("au"));
            // System.out.println(generatePhone("zh"));
            // System.out.println(generatePhone("en"));
            // System.out.println(generatePhone("th"));
            // System.out.println(generatePhone("vi"));
            // System.out.println(generatePhone("sg"));
            // System.out.println(generatePhone("my"));
            // System.out.println(generatePhone("id"));
            // System.out.println(generatePhone("ca"));
            // System.out.println(generatePhone("gb"));
        }
    }
}

