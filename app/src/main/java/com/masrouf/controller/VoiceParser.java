package com.masrouf.controller;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.masrouf1.db.ChildSpendingSummary;
import com.example.masrouf1.db.MasroufEntities.CategoryEntity;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic multilingual voice parser for French, English, Tunisian/Standard Arabic & mixed speech.
 * Preserves original wording, Arabic characters, French accents, and numbers without translation.
 */
public final class VoiceParser {
    public static final class Result {
        public final String text, label, type, childName, productKey;
        public final long amountMilli;
        public final boolean ambiguous;

        Result(String text, String label, String type, String childName, String productKey, long amountMilli, boolean ambiguous) {
            this.text = text;
            this.label = label;
            this.type = type;
            this.childName = childName;
            this.productKey = productKey;
            this.amountMilli = amountMilli;
            this.ambiguous = ambiguous;
        }
    }

    public static final class RichResult {
        public final String rawText;
        public final String detectedLanguage; // "fr", "ar", "en", "mixed"
        public final String operationType;    // "purchase", "expense", "child_allowance"
        public final long amountMilli;
        public final String label;
        public final long matchedCategoryId;
        public final String matchedCategoryName;
        public final int matchedAllocationPercent;
        public final String store;
        public final String quantity;
        public final String unit;
        public final long matchedChildId;
        public final String matchedChildName;
        public final boolean ambiguous;

        RichResult(String rawText, String detectedLanguage, String operationType, long amountMilli,
                   String label, long matchedCategoryId, String matchedCategoryName,
                   int matchedAllocationPercent, String store, String quantity, String unit,
                   long matchedChildId, String matchedChildName, boolean ambiguous) {
            this.rawText = rawText;
            this.detectedLanguage = detectedLanguage;
            this.operationType = operationType;
            this.amountMilli = amountMilli;
            this.label = label;
            this.matchedCategoryId = matchedCategoryId;
            this.matchedCategoryName = matchedCategoryName;
            this.matchedAllocationPercent = matchedAllocationPercent;
            this.store = store;
            this.quantity = quantity;
            this.unit = unit;
            this.matchedChildId = matchedChildId;
            this.matchedChildName = matchedChildName;
            this.ambiguous = ambiguous;
        }
    }

    private static final Pattern NUMBER = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*(millimes?|mil|dt|tnd|dinars?|dinar|د\\.?ت|دينار|دنانير)?", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern QTY_UNIT = Pattern.compile("\\b(\\d+(?:[.,]\\d+)?)\\s*(kg|g|l|ml|litre|litres|pièce|pièces|piece|pieces|paquet|paquets|pack|botte|bottes|bouteille|bouteilles|كغ|لتر|علبة|قطعة)\\b", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern STORE_PATTERN = Pattern.compile("(?:\\b(?:chez|au|magasin|supermarche|at|from)\\s+|\\b(?:من|في|عند)\\s+)([\\p{L}0-9'\\-]{2,24}(?:\\s+[\\p{L}0-9'\\-]{2,20})?)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern CHILD = Pattern.compile("(?:child|enfant|a3tit\\s+flous\\s+l|flous\\s+l|pour\\s+(?:mon\\s+)?enfant|à\\s+(?:mon\\s+)?enfant|a\\s+(?:mon\\s+)?enfant|ل|لطفلي|لابني|لبنتي)\\s+([\\p{L}]{2,24})", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern ARABIC_CHARS = Pattern.compile("[\\u0600-\\u06FF]");
    private static final Pattern LATIN_LETTERS = Pattern.compile("[A-Za-zÀ-ÖØ-öø-ÿ]");

    private static final String[][] PRODUCTS = {
            {"bread", "khobz|pain|bread|خبز|خبزة|خُبز|baguette"},
            {"milk", "halib|hlib|lait|milk|حليب|لبن"},
            {"meat", "lham|viande|meat|لحم|escalope|poulet|chicken|djaj|دجاج"},
            {"oil", "zit|huile|oil|زيت"},
            {"sugar", "sokkor|sucre|sugar|سكر"},
            {"coffee", "kahwa|qahwa|cafe|coffee|قهوة"},
            {"vegetables", "khodhra|legumes|vegetables|خضرة|خضر|tomate|pomme de terre|oignon"},
            {"fruit", "ghalla|fruits|fruit|غلة|فواكه|فاكهة"},
            {"clothing", "baskets|chaussures|vetements|pull|pantalon|robe|shoes|clothes|sneakers|shirt|حذاء|ملابس"}
    };

    private static final String[] ENGLISH_TOKENS = {
            "bought", "buy", "spent", "spend", "paid", "pay", "gave", "give",
            "dinars", "dinar", "for", "and", "the", "my", "child", "internet",
            "bill", "electricity", "water", "charity", "donation", "from", "store",
            "milk", "bread", "meat", "clothes", "shoes", "groceries", "taxi", "bus"
    };

    private static final String[] FRENCH_TOKENS = {
            "acheté", "acheter", "payé", "payer", "dépensé", "donné", "enfant",
            "pour", "avec", "chez", "du", "de", "des", "les", "une", "dans",
            "dinars", "lait", "pain", "courses", "facture", "électricité", "eau",
            "vêtements", "baskets", "transport", "santé", "médecin", "pharmacie"
    };

    private VoiceParser() { }

    public static String toArabicScript(String input) {
        String source = input == null ? "" : input.trim();
        if (source.isEmpty()) return "";
        return source
                .replaceAll("(?<=[A-Za-z])3|3(?=[A-Za-z])", "ع")
                .replaceAll("(?<=[A-Za-z])7|7(?=[A-Za-z])", "ح")
                .replaceAll("(?<=[A-Za-z])5|5(?=[A-Za-z])", "خ")
                .replaceAll("(?<=[A-Za-z])9|9(?=[A-Za-z])", "ق");
    }

    /** Detects primary language ("fr", "ar", "en", or "mixed") without translating text. */
    @NonNull
    public static String detectLanguage(@Nullable String input) {
        if (input == null || input.trim().isEmpty()) return "fr";
        String trimmed = input.trim();
        boolean hasArabic = ARABIC_CHARS.matcher(trimmed).find();
        boolean hasLatin = LATIN_LETTERS.matcher(trimmed).find();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        boolean hasArabizi = lower.matches(".*\\b(chrit|khsart|dfa3t|dfaat|a3tit|khobz|hlib|flous|sadaqa|alfin|khamsa|achra)\\b.*");

        if (hasArabic && hasLatin) {
            return "mixed";
        }
        if (hasArabic) {
            return "ar";
        }
        if (hasArabizi) {
            return "mixed";
        }
        int enHits = 0;
        int frHits = 0;
        for (String tok : ENGLISH_TOKENS) {
            if (containsWord(lower, tok)) enHits++;
        }
        for (String tok : FRENCH_TOKENS) {
            if (containsWord(lower, tok)) frHits++;
        }
        if (trimmed.matches(".*[éèêàçùôîûëïüœ].*")) {
            frHits += 2;
        }
        if (enHits > frHits) return "en";
        return "fr";
    }

    public static Result parse(String input) {
        String original = input == null ? "" : input.trim();
        String clean = normalize(original);
        long amount = extractAmountMilli(original, clean);
        String childName = extractChildName(original);
        String productKey = detectProductKey(clean, original);
        String type = detectType(clean, original, productKey, !childName.isEmpty());
        String label = !productKey.isEmpty() ? capitalize(productKey)
                : ("donation".equals(type) ? "Don"
                : ("child_allowance".equals(type) ? "Don enfant" : "Dépense"));
        boolean ambiguous = amount <= 0 || hasCompetingAmountTokens(clean, original);
        return new Result(original, label, type, childName, productKey, amount, ambiguous);
    }

    @NonNull
    public static RichResult parseRich(
            @Nullable String rawInput,
            @Nullable List<CategoryEntity> userCategories,
            @Nullable List<ChildSpendingSummary> children,
            boolean parentAccount) {
        String raw = rawInput == null ? "" : rawInput.trim();
        String detectedLang = detectLanguage(raw);
        String clean = normalize(raw);
        long amount = extractAmountMilli(raw, clean);

        String productKey = detectProductKey(clean, raw);
        String extractedChildName = extractChildName(raw);
        ChildSpendingSummary matchedChild = findMatchingChild(raw, clean, extractedChildName, children);
        boolean childGiftCue = matchedChild != null || !extractedChildName.isEmpty() || containsChildGiftKeyword(clean, raw);

        String operationType;
        if (parentAccount && childGiftCue && !containsPurePurchaseVerb(clean, raw)) {
            operationType = "child_allowance";
        } else if (!productKey.isEmpty() || containsPurePurchaseVerb(clean, raw)) {
            operationType = "purchase";
        } else {
            operationType = "expense";
        }

        // Preserve original spoken words cleanly without translating language
        String cleanLabel = extractCleanLabel(raw);
        if (cleanLabel.isEmpty()) {
            cleanLabel = raw;
        }

        String quantity = "1";
        String unit = "pièce";
        Matcher qtyMatcher = QTY_UNIT.matcher(raw);
        if (qtyMatcher.find()) {
            quantity = qtyMatcher.group(1) == null ? "1" : qtyMatcher.group(1).replace(',', '.');
            unit = normalizeUnit(qtyMatcher.group(2));
        }

        String store = "Commerce";
        Matcher storeMatcher = STORE_PATTERN.matcher(raw);
        if (storeMatcher.find() && storeMatcher.group(1) != null) {
            String candidateStore = storeMatcher.group(1).trim();
            if (!candidateStore.isEmpty() && !candidateStore.matches(".*\\d.*")) {
                store = candidateStore;
            }
        }

        CategoryEntity bestCat = matchBestCategory(raw, clean, productKey, operationType, userCategories);
        long catId = bestCat == null ? 0L : bestCat.id;
        String catName = bestCat == null ? "" : bestCat.name;
        int allocPct = bestCat == null ? 0 : Math.max(0, bestCat.allocationPercent);

        boolean ambiguous = amount <= 0L;
        return new RichResult(
                raw,
                detectedLang,
                operationType,
                amount,
                cleanLabel,
                catId,
                catName,
                allocPct,
                store,
                quantity,
                unit,
                matchedChild == null ? 0L : matchedChild.childProfileId,
                matchedChild == null ? extractedChildName : matchedChild.nickname,
                ambiguous);
    }

    @NonNull
    public static String extractCleanLabel(@Nullable String rawInput) {
        if (rawInput == null) return "";
        String s = rawInput.trim();
        if (s.isEmpty()) return "";
        // Strip trailing/inline amount expression while preserving original spoken script & words
        s = s.replaceAll("(?iu)\\s+(?:à|pour|for|at|avec|b|بـ?|ب)\\s*\\d+(?:[.,]\\d+)?\\s*(?:dt|tnd|dinars?|dinar|millimes?|mil|د\\.?ت|دينار|دنانير)?\\b", " ");
        s = s.replaceAll("(?iu)\\b\\d+(?:[.,]\\d+)?\\s*(?:dt|tnd|dinars?|dinar|millimes?|mil|د\\.?ت|دينار|دنانير)\\b", " ");
        s = s.replaceAll("\\s{2,}", " ").trim();
        return s.isEmpty() ? rawInput.trim() : s;
    }

    private static String normalizeUnit(@Nullable String rawUnit) {
        if (rawUnit == null) return "pièce";
        String u = rawUnit.toLowerCase(Locale.ROOT).trim();
        if (u.equals("kg") || u.equals("كغ")) return "kg";
        if (u.equals("g")) return "g";
        if (u.startsWith("l") || u.equals("لتر")) return "L";
        if (u.startsWith("paq") || u.equals("pack") || u.equals("علبة")) return "paquet";
        if (u.startsWith("botte")) return "botte";
        return "pièce";
    }

    @Nullable
    private static ChildSpendingSummary findMatchingChild(
            String raw,
            String clean,
            String extractedChildName,
            @Nullable List<ChildSpendingSummary> children) {
        if (children == null || children.isEmpty()) return null;
        String target = normalizeName(extractedChildName);
        if (!target.isEmpty()) {
            for (ChildSpendingSummary c : children) {
                if (c.nickname != null && normalizeName(c.nickname).equals(target)) {
                    return c;
                }
            }
        }
        String rawLower = raw.toLowerCase(Locale.ROOT);
        for (ChildSpendingSummary c : children) {
            if (c.nickname == null || c.nickname.trim().isEmpty()) continue;
            String nickNorm = normalizeName(c.nickname);
            if (!nickNorm.isEmpty() && (containsWord(clean, nickNorm) || rawLower.contains(c.nickname.toLowerCase(Locale.ROOT)))) {
                return c;
            }
        }
        return null;
    }

    @Nullable
    private static CategoryEntity matchBestCategory(
            String raw,
            String clean,
            String productKey,
            String operationType,
            @Nullable List<CategoryEntity> categories) {
        if (categories == null || categories.isEmpty()) return null;
        String rawLower = raw.toLowerCase(Locale.ROOT);
        String targetType = "purchase".equals(operationType) ? "product" : "expense";

        // 1. Direct name match (in case user speaks category name directly)
        for (CategoryEntity cat : categories) {
            if (cat.name != null && rawLower.contains(cat.name.toLowerCase(Locale.ROOT))) {
                return cat;
            }
        }

        // 2. Multilingual semantic mapping
        String[][] categoryHints = {
                {"clothing", "Vêtements", "baskets|chaussures|vetements|pull|pantalon|robe|shoes|clothes|sneakers|shirt|حذاء|ملابس|قميص"},
                {"food", "Alimentation", "pain|lait|khobz|hlib|halib|courses|viande|poulet|fruits|legumes|milk|bread|meat|groceries|حليب|خبز|لحم|دجاج|خضر|فواكه|أكل|طعام"},
                {"hygiene", "Hygiène", "savon|shampooing|hygiene|dentifrice|parfum|soap|shampoo|صابون|شامبو|نظافة"},
                {"computer", "Matériel", "ordinateur|laptop|pc|telephone|clavier|ecran|computer|phone|حاسوب|هاتف"},
                {"home_equip", "Maison", "meuble|table|chaise|cuisine|frigo|furniture|home|أثاث|مطبخ"},
                {"health", "Santé", "medecin|docteur|pharmacie|medicament|hopital|doctor|pharmacy|medicine|health|طبيب|دواء|صيدلية|صحة"},
                {"transport", "Transport", "taxi|bus|essence|carburant|train|metro|uber|fuel|gas|transport|طاكسي|حافلة|بنزين|نقل"},
                {"bills", "Factures", "facture|electricite|eau|internet|wifi|gaz|loyer|bill|electricity|water|rent|فاتورة|الكهرباء|الماء|انترنت|كراء"},
                {"education", "Études", "ecole|livre|cahier|etudes|cours|universite|school|book|study|tuition|مدرسة|كتاب|دفتر|دراسة"},
                {"leisure", "Loisirs", "cinema|cafe|restaurant|sortie|sport|voyage|cinema|coffee|trip|movie|سينما|قهوة|مطعم|سفر|ترفيه"},
                {"donations", "Dons", "don|sadaqa|charite|association|charity|donation|تبرع|تبرعت|صدقة|للجمعية"},
                {"children", "Enfants", "enfant|pocket|allowance|argent de poche|child|children|مصروف|طفل|طفلي|ابني|بنتي"}
        };

        for (String[] hint : categoryHints) {
            Pattern p = Pattern.compile("(?:" + hint[2] + ")", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            if (p.matcher(clean).find() || p.matcher(rawLower).find() || (!productKey.isEmpty() && hint[0].equals(productKey))) {
                for (CategoryEntity cat : categories) {
                    if (cat.name != null && normalize(cat.name).contains(normalize(hint[1]))) {
                        return cat;
                    }
                }
            }
        }

        // 3. Fallback to first category matching target operationType
        for (CategoryEntity cat : categories) {
            if (targetType.equalsIgnoreCase(cat.type)) {
                return cat;
            }
        }
        return categories.get(0);
    }

    private static boolean containsChildGiftKeyword(String clean, String raw) {
        return raw.matches(".*(أعطيت|مصروف|طفلي|لابني|لبنتي).*")
                || clean.matches(".*\\b(a3tit|flous|donne|donner|enfant|child|gave|allowance|pocket)\\b.*");
    }

    private static boolean containsPurePurchaseVerb(String clean, String raw) {
        return raw.matches(".*(شريت|اشتريت).*")
                || clean.matches(".*\\b(chrit|achete|acheter|bought|buy|groceries|courses)\\b.*");
    }

    private static boolean containsWord(String text, String token) {
        return Pattern.compile("(?<![\\p{L}])" + Pattern.quote(token) + "(?![\\p{L}])",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(text).find();
    }

    private static boolean hasCompetingAmountTokens(String clean, String original) {
        int matches = 0;
        Matcher m = NUMBER.matcher(original);
        while (m.find()) matches++;
        if (matches > 1) return true;
        int lexical = 0;
        String[] cues = {"alfin", "alf", "khamsa", "achra", "meyya", "mya", "dinars", "dinar", "دينار"};
        for (String cue : cues) if (clean.contains(cue) || original.contains(cue)) lexical++;
        return matches > 0 && lexical > 1;
    }

    private static long extractAmountMilli(String original, String clean) {
        String normalizedOriginal = convertArabicDigits(original);
        Matcher numeric = NUMBER.matcher(normalizedOriginal);
        long candidate = 0L;
        while (numeric.find()) {
            String num = numeric.group(1);
            String unit = numeric.group(2) == null ? "" : numeric.group(2).toLowerCase(Locale.ROOT);
            // Ignore unit quantity numbers if another explicit currency amount exists
            try {
                double value = Double.parseDouble(num.replace(',', '.'));
                long parsed = Math.round(value * (unit.startsWith("mil") ? 1L : 1000L));
                if (!unit.isEmpty()) return parsed;
                candidate = parsed;
            } catch (NumberFormatException ignored) { }
        }
        if (candidate > 0L) return candidate;

        long words = 0L;
        if (clean.contains("alfin") || original.contains("ألفين") || original.contains("الفين")) words += 2000L;
        else if (clean.contains("alf") || original.contains("ألف") || original.contains("الف")) words += 1000L;
        if (clean.contains("meyya") || clean.contains("mya") || original.contains("مئة") || original.contains("مية")) words += 100L;
        if (clean.contains("achra") || original.contains("عشرة")) words += 10_000L;
        if (clean.contains("khamsa") || original.contains("خمسة")) words += 5_000L;
        if (clean.contains("cinq") || clean.contains("five")) words += 5_000L;
        if (clean.contains("dix") || clean.contains("ten")) words += 10_000L;
        if (clean.contains("vingt") || clean.contains("twenty")) words += 20_000L;
        return words;
    }

    private static String convertArabicDigits(String input) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c >= '\u0660' && c <= '\u0669') {
                sb.append((char) ('0' + (c - '\u0660')));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String extractChildName(String original) {
        Matcher matcher = CHILD.matcher(original);
        if (!matcher.find()) return "";
        String name = matcher.group(1) == null ? "" : matcher.group(1).trim();
        if (name.matches("(?i)(enfant|child|dinar|dinars|dt|tnd|poulet|pain|lait|mon|pour)")) return "";
        return name;
    }

    private static String detectType(String clean, String raw, String productKey, boolean hasChildName) {
        if (hasChildName || clean.contains("a3tit") || clean.contains("child") || clean.contains("enfant")
                || clean.contains("argent de poche") || raw.contains("أعطيت") || raw.contains("مصروف")) {
            return "child_allowance";
        }
        if (clean.contains("sadaqa") || clean.contains("don") || clean.contains("charite")
                || clean.contains("charity") || raw.contains("تبرع") || raw.contains("صدقة")) {
            return "donation";
        }
        if (!productKey.isEmpty() || containsPurePurchaseVerb(clean, raw)) {
            return "purchase";
        }
        return "expense";
    }

    private static String detectProductKey(String clean, String raw) {
        String lowerRaw = raw == null ? "" : raw.toLowerCase(Locale.ROOT);
        for (String[] entry : PRODUCTS) {
            if (Pattern.compile("(?:" + entry[1] + ")", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(clean).find()
                    || Pattern.compile("(?:" + entry[1] + ")", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(lowerRaw).find()) {
                return entry[0];
            }
        }
        return "";
    }

    private static String capitalize(String value) {
        return value.isEmpty() ? "" : Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    public static String normalizeName(String input) {
        return normalize(input).replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
    }

    private static String normalize(String input) {
        if (input == null) return "";
        return Normalizer.normalize(input, Normalizer.Form.NFD).replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
    }
}
