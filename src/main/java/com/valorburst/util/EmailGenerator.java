package com.valorburst.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import net.sourceforge.pinyin4j.PinyinHelper;

import java.io.InputStream;
import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class EmailGenerator {

    private static final Pattern NON_ASCII = Pattern.compile("[^a-zA-Z0-9]");
    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    private static final Map<String, Map<String, Double>> DOMAIN_WEIGHTS = loadDomainWeights();
    private static final Map<String, String> REGION_TO_LOCALE = new HashMap<>();
    private static final Map<String, Map<String, Double>> FALLBACK_LOCALE_WEIGHTS = new HashMap<>();
    private static final Map<String, Faker> FAKER_CACHE = new ConcurrentHashMap<>();
    private static final Random RANDOM = new Random();

    private static final double[] DIGITS_WEIGHTS = {0.5, 0.1, 0.15, 0.2, 0.15};
    private static final List<UsernameStrategy> STRATEGIES = new ArrayList<>();

    static {
        REGION_TO_LOCALE.put("en_AU", "en_AU");
        REGION_TO_LOCALE.put("zh_TW", "zh_TW");
        REGION_TO_LOCALE.put("en_US", "en_US");
        REGION_TO_LOCALE.put("th_TH", "th_TH");
        REGION_TO_LOCALE.put("vi_VN", "vi_VN");
        REGION_TO_LOCALE.put("id_ID", "id_ID");
        REGION_TO_LOCALE.put("en_CA", "en_CA");
        REGION_TO_LOCALE.put("en_GB", "en_GB");

        REGION_TO_LOCALE.put("en_SG", "fallback");
        REGION_TO_LOCALE.put("ms_MY", "fallback");

        FALLBACK_LOCALE_WEIGHTS.put("en_SG", Map.of(
                "en_US", 0.5, "en_GB", 0.2, "en_AU", 0.15, "en_CA", 0.1, "en_IN", 0.05
        ));

        FALLBACK_LOCALE_WEIGHTS.put("ms_MY", Map.of(
                "en_US", 0.6, "zh_CN", 0.25, "hi_IN", 0.10, "en_GB", 0.05
        ));

        // 用户名策略（结构模仿 Python）
        STRATEGIES.add((fn, ln) -> ln.charAt(0) + fn);
        STRATEGIES.add((fn, ln) -> ln + fn.charAt(0));
        STRATEGIES.add((fn, ln) -> fn + ln);
        STRATEGIES.add((fn, ln) -> randomChar(fn) + ln);
        STRATEGIES.add((fn, ln) -> ln + randomChar(fn));
        STRATEGIES.add((fn, ln) -> shuffle(fn) + ln);
        STRATEGIES.add((fn, ln) -> ln + randomLetters(3));
        STRATEGIES.add((fn, ln) -> ln + randomMix(1, 3) + randomChar(fn));
        STRATEGIES.add((fn, ln) -> ln + randomChar(fn) + randomMix(1, 3));
    }

    public static String generateEmail(String regionCode) {
        String region = resolveRegion(regionCode);
        String locale = REGION_TO_LOCALE.getOrDefault(region, "en_US");
        Map<String, Double> domainWeights = DOMAIN_WEIGHTS.getOrDefault(region, DOMAIN_WEIGHTS.get("en_US"));

        String actualLocale = locale.equals("fallback")
                ? pickByWeight(FALLBACK_LOCALE_WEIGHTS.get(region))
                : locale;

        Faker faker = getFaker(actualLocale);
        String firstName = faker.name().firstName();
        String lastName = faker.name().lastName();

        String fn = processName(firstName, region);
        String ln = processName(lastName, region);

        String username = generateUsername(fn, ln);
        String domain = pickByWeight(domainWeights);

        return username + domain;
    }

    private static Map<String, Map<String, Double>> loadDomainWeights() {
    try {
        ObjectMapper mapper = new ObjectMapper();
        InputStream is = EmailGenerator.class.getResourceAsStream("/static/email_domain_weights.json");
        TypeReference<Map<String, Map<String, Double>>> typeRef = new TypeReference<>() {};
        return mapper.readValue(is, typeRef);
    } catch (Exception e) {
        e.printStackTrace();
        return Collections.emptyMap();
    }
}

    private static String processName(String name, String region) {
        String result;
        if ("zh_TW".equals(region)) {
            result = toPinyin(name);
        } else {
            result = sanitize(name);
        }
        return result.isEmpty() ? randomLetters(3) : result;
    }

    private static String generateUsername(String fn, String ln) {
        String username = "";

        while (username.length() < 6) {
            UsernameStrategy strategy = pickRandom(STRATEGIES);
            username = appendDigits(strategy.build(fn, ln));
        }

        // Padding to 8 if needed
        if (username.length() < 8) {
            int need = 8 - username.length();
            username += randomDigits(need);
        }

        // Must start with letter
        if (!Character.isLetter(username.charAt(0))) {
            username = (char) ('a' + RANDOM.nextInt(26)) + username.substring(1);
        }

        return username.toLowerCase(Locale.ROOT);
    }

    private static String toPinyin(String name) {
        StringBuilder sb = new StringBuilder();
        for (char c : name.toCharArray()) {
            String[] pinyin = PinyinHelper.toHanyuPinyinStringArray(c);
            if (pinyin != null && pinyin.length > 0) {
                sb.append(pinyin[0].charAt(0)); // first letter of first pinyin
            }
        }
        return sb.toString().toLowerCase();
    }

    private static String appendDigits(String base) {
        String[] options = {
                "",
                String.valueOf(RANDOM.nextInt(10)),
                randomDigits(2),
                randomDigits(3),
                randomDigits(4)
        };
        return base + weightedPick(options, DIGITS_WEIGHTS);
    }

    private static String weightedPick(String[] options, double[] weights) {
        double r = RANDOM.nextDouble();
        double acc = 0.0;
        for (int i = 0; i < options.length; i++) {
            acc += weights[i];
            if (r <= acc) return options[i];
        }
        return options[0];
    }

    private static String randomDigits(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) sb.append(RANDOM.nextInt(10));
        return sb.toString();
    }

    private static char randomChar(String s) {
        return s.charAt(RANDOM.nextInt(s.length()));
    }

    private static String randomLetters(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++)
            sb.append((char) ('a' + RANDOM.nextInt(26)));
        return sb.toString();
    }

    private static String randomMix(int min, int max) {
        int len = RANDOM.nextInt(max - min + 1) + min;
        String base = "abcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) sb.append(base.charAt(RANDOM.nextInt(base.length())));
        return sb.toString();
    }

    private static String shuffle(String s) {
        List<Character> chars = new ArrayList<>();
        for (char c : s.toCharArray()) chars.add(c);
        Collections.shuffle(chars);
        StringBuilder sb = new StringBuilder();
        for (char c : chars) sb.append(c);
        return sb.toString();
    }

    private static <T> T pickRandom(List<T> list) {
        return list.get(RANDOM.nextInt(list.size()));
    }

    private static String resolveRegion(String shortCode) {
        return switch (shortCode) {
            case "au" -> "en_AU";
            case "zh" -> "zh_TW";
            case "th" -> "th_TH";
            case "vi" -> "vi_VN";
            case "id" -> "id_ID";
            case "sg" -> "en_SG";
            case "my" -> "ms_MY";
            case "ca" -> "en_CA";
            case "gb" -> "en_GB";
            case "en" -> "en_US";
            default -> "en_US";
        };
    }

    private static <T> T pickByWeight(Map<T, Double> weightedMap) {
        double total = weightedMap.values().stream().mapToDouble(d -> d).sum();
        double r = RANDOM.nextDouble() * total;
        double acc = 0.0;
        for (Map.Entry<T, Double> entry : weightedMap.entrySet()) {
            acc += entry.getValue();
            if (r <= acc) return entry.getKey();
        }
        return weightedMap.keySet().iterator().next();
    }

    private static Faker getFaker(String localeStr) {
        return FAKER_CACHE.computeIfAbsent(localeStr, l -> new Faker(new Locale(l.split("_")[0], l.split("_")[1])));
    }

    private static String sanitize(String input) {
        if (input == null) return "";

        // 1. Unicode 正规化：分解带重音字母
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);

        // 2. 去掉重音符号
        String noDiacritics = DIACRITICS.matcher(normalized).replaceAll("");

        // 3. 去除非字母数字
        String asciiOnly = NON_ASCII.matcher(noDiacritics).replaceAll("");

        return asciiOnly.toLowerCase();
    }

    // ==== Interface ====

    private interface UsernameStrategy {
        String build(String firstName, String lastName);
    }

    // ==== Test ====
    public static void main(String[] args) {
        for (int i = 0; i < 100; i++) {
            System.out.println(generateEmail("au"));
            // System.out.println(generateEmail("zh"));
            // System.out.println(generateEmail("th"));
            // System.out.println(generateEmail("vi"));
            // System.out.println(generateEmail("en"));
            // System.out.println(generateEmail("sg"));
            // System.out.println(generateEmail("my"));
            // System.out.println(generateEmail("id"));
            // System.out.println(generateEmail("ca"));
            // System.out.println(generateEmail("gb"));
        }
    }
}
