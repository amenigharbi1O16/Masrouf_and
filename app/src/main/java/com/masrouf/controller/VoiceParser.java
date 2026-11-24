package com.masrouf.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Deterministic on-device parser for short French, Arabic, Derja and Arabizi transaction phrases. */
public final class VoiceParser {
    public static final class Result {
        public final String text, label, type, childName, productKey;
        public final long amountMilli;
        public final boolean ambiguous;
        Result(String text,String label,String type,String childName,String productKey,long amountMilli,boolean ambiguous){
            this.text=text;this.label=label;this.type=type;this.childName=childName;this.productKey=productKey;this.amountMilli=amountMilli;this.ambiguous=ambiguous;
        }
    }
    private static final String NUMBER_TOKEN="(?<![\\p{L}\\p{N}])[0-9]+(?:[.,][0-9]{1,3})?(?=\\s*(?:(?:dt|dinars?|دينار|دنانير)\\b)?(?:\\b|$))";
    private static final Pattern NUMBER=Pattern.compile("("+NUMBER_TOKEN+")");
    private static final Pattern NUMERIC_TOKEN=Pattern.compile(NUMBER_TOKEN);
    private static final Map<String,Integer> WORDS=new HashMap<>();
    private static final Map<String,String> PRODUCTS=new HashMap<>();
    static {
        for(String key:new String[]{"wahed","wahda","wehed","wa7ed"})WORDS.put(key,1);
        for(String key:new String[]{"ithnin","thnin","zouze"})WORDS.put(key,2);
        for(String key:new String[]{"thletha","thlatha"})WORDS.put(key,3);
        for(String key:new String[]{"arba","arbaa"})WORDS.put(key,4);
        for(String key:new String[]{"khamsa","khams"})WORDS.put(key,5);
        for(String key:new String[]{"setta","sitta"})WORDS.put(key,6);
        for(String key:new String[]{"seb3a","sab3a"})WORDS.put(key,7);
        for(String key:new String[]{"thmenya","thmanya"})WORDS.put(key,8);
        for(String key:new String[]{"tes3a","tis3a"})WORDS.put(key,9);
        for(String key:new String[]{"achra","3achra","3asra"})WORDS.put(key,10);
        PRODUCTS.put("khobz","bread");PRODUCTS.put("خبز","bread");
        PRODUCTS.put("7lib","milk");PRODUCTS.put("hlib","milk");PRODUCTS.put("حليب","milk");
        PRODUCTS.put("l7am","meat");PRODUCTS.put("لحم","meat");
        PRODUCTS.put("djej","chicken");PRODUCTS.put("دجاج","chicken");
        PRODUCTS.put("zit","oil");PRODUCTS.put("زيت","oil");
        PRODUCTS.put("skar","sugar");PRODUCTS.put("سكر","sugar");
        PRODUCTS.put("9ahwa","coffee");PRODUCTS.put("قهوة","coffee");
        PRODUCTS.put("khodhra","vegetables");PRODUCTS.put("خضرة","vegetables");
        PRODUCTS.put("ghalla","fruit");PRODUCTS.put("غلة","fruit");
    }
    private VoiceParser(){}

    /** Converts Arabizi letters while preserving numeric amount tokens exactly. */
    public static String toArabicScript(String input){
        if(input==null)return "";boolean[] amountDigits=new boolean[input.length()];Matcher matcher=NUMERIC_TOKEN.matcher(input);
        while(matcher.find())for(int i=matcher.start();i<matcher.end();i++)amountDigits[i]=true;
        StringBuilder out=new StringBuilder();for(int i=0;i<input.length();i++){
            char original=input.charAt(i);if(amountDigits[i]){out.append(original);continue;}char c=Character.toLowerCase(original);
            switch(c){case '3':out.append('ع');break;case '7':out.append('ح');break;case '9':out.append('ق');break;case '2':out.append('ء');break;case '5':out.append('خ');break;case '6':out.append('ط');break;case '8':out.append('غ');break;default:out.append(original);}
        }return out.toString();
    }

    public static Result parse(String input){
        String original=input==null?"":input.trim(), normalized=normalize(original);
        long amount=amountMilli(normalized);
        boolean child=hasAny(normalized,"aatiit flous","a3tit flous","3tit flous","عطيت فلوس","أعطيت نقود","اعطيت فلوس","donne argent","donner argent");
        boolean donation=hasAny(normalized,"sadaqa","sada9a","don","تبرع","صدقة","charite","charité");
        boolean purchase=hasAny(normalized,"chrit","chrina","chريت","اشريت","اشتريت","achete","acheté","achat","خديت","شريت");
        String type=child?"child_allowance":donation?"donation":purchase?"purchase":"expense";
        String productKey="";for(String token:normalized.split(" "))if(PRODUCTS.containsKey(token)){productKey=PRODUCTS.get(token);break;}
        String label=child?"Argent de poche":donation?"Don caritatif":purchase&&!productKey.isEmpty()?productKey:purchase?"Achat":"Dépense";
        String childName="";
        if(child){int marker=normalized.lastIndexOf(" l ");if(marker>=0&&marker+3<normalized.length())childName=normalized.substring(marker+3).trim();
            if(childName.isEmpty()){marker=normalized.lastIndexOf(" ل ");if(marker>=0&&marker+3<normalized.length())childName=normalized.substring(marker+3).trim();}
            childName=childName.replaceFirst("\\b(?:[0-9]+(?:[.,][0-9]{1,3})?|alf|alfin|elfin|lef|dt|dinar|dinars|meyya|miyye|myat|achra|khamsa|arba)\\b.*$","").trim();}
        Matcher candidates=NUMBER.matcher(normalized);int count=0;while(candidates.find())count++;
        boolean ambiguous=amount<=0||count>1||(child&&childName.isEmpty())||(purchase&&productKey.isEmpty());
        return new Result(original,label,type,childName,productKey,amount,ambiguous);
    }

    private static long amountMilli(String text){
        if(hasAny(text,"alf meyya","alf miyya","alf miyye","alf mya","الف مية","ألف مية"))return 1100L;
        Matcher matcher=NUMBER.matcher(text);String numeric=null,after="";int numericCount=0;
        while(matcher.find()){numericCount++;String tail=text.substring(matcher.end());if(numeric==null){numeric=matcher.group(1);after=tail;}if(tail.matches("(?s)^\\s*(dt|dinars?|دينار|دنانير)\\b.*")){numeric=matcher.group(1);after=tail;break;}}
        if(numeric!=null){
            BigDecimal value=new BigDecimal(numeric.replace(',','.'));
            try{if(after.matches("(?s).*\\b(lef|alf|dinars?|dinar|dt|دينار|دنانير)\\b.*"))return value.multiply(BigDecimal.valueOf(1000)).setScale(0,RoundingMode.HALF_UP).longValueExact();
                return value.setScale(3,RoundingMode.HALF_UP).movePointRight(3).longValueExact();}catch(RuntimeException ignored){return 0;}
        }
        long total=0;String[] tokens=text.split("[^\\p{L}\\p{N}]+" );
        for(int i=0;i<tokens.length;i++){
            String word=tokens[i];Integer number=WORDS.get(word);
            if(number!=null){String next=i+1<tokens.length?tokens[i+1]:"";
                if(isDinar(next)||next.equals("lef")||next.equals("alf")){total+=number*1000L;i++;}
                else if(isHundred(next)){total+=number*100L;i++;}
                else total+=number*1000L;
            }else if(word.equals("alfin")||word.equals("elfin")||word.equals("zouz")||word.equals("زوز"))total+=2000;
            else if(word.equals("alf")||word.equals("lef")||word.equals("ألف")||word.equals("الف"))total+=1000;
            else if(word.equals("meyya")||word.equals("miyye")||word.equals("meya")||word.equals("mya")||word.equals("myat")||word.equals("مائة")||word.equals("مية"))total+=100;
        }
        return total;
    }
    private static boolean isDinar(String value){return value.startsWith("dinar")||value.equals("dt")||value.equals("دينار")||value.equals("دنانير");}
    private static boolean isHundred(String value){return value.equals("meyya")||value.equals("miyye")||value.equals("meya")||value.equals("mya")||value.equals("myat")||value.equals("مية")||value.equals("مائة");}
    private static boolean hasAny(String text,String... values){for(String value:values)if(text.contains(normalize(value)))return true;return false;}
    public static String normalizeName(String input){return normalize(input).replaceAll("[^\\p{L}\\p{N}]+"," ").trim();}
    private static String normalize(String input){
        String value=Normalizer.normalize(input.toLowerCase(Locale.ROOT),Normalizer.Form.NFD).replaceAll("\\p{M}+","").replace('’',' ').replace('ı','i');
        StringBuilder ascii=new StringBuilder();for(char c:value.toCharArray()){if(c>='٠'&&c<='٩')ascii.append((char)('0'+c-'٠'));else ascii.append(c);}
        return ascii.toString().replaceAll("[^\\p{L}\\p{N}.,]+"," ").replaceAll("\\s+"," ").trim();
    }
}
