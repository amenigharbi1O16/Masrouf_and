package com.masrouf.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Offline parser for common Tunisian Derja money phrases and Arabizi input. */
public final class VoiceParser {
    public static final class Result {
        public final String text,label,type,childName;
        public final long amountMilli;
        Result(String text,String label,String type,String childName,long amountMilli){this.text=text;this.label=label;this.type=type;this.childName=childName;this.amountMilli=amountMilli;}
    }
    private static final Pattern NUMBER=Pattern.compile("(?<![\\p{L}])([0-9]+(?:[.,][0-9]{1,3})?)(?![\\p{L}])");
    private static final Map<String,Integer> WORDS=new HashMap<>();
    static {
        WORDS.put("wahed",1);WORDS.put("wahda",1);WORDS.put("wehed",1);WORDS.put("ithnin",2);WORDS.put("thnin",2);WORDS.put("zouze",2);WORDS.put("thletha",3);WORDS.put("thlatha",3);WORDS.put("arba",4);WORDS.put("arbaa",4);WORDS.put("khamsa",5);WORDS.put("khams",5);WORDS.put("setta",6);WORDS.put("sitta",6);WORDS.put("seb3a",7);WORDS.put("sab3a",7);WORDS.put("thmenya",8);WORDS.put("thmanya",8);WORDS.put("tes3a",9);WORDS.put("tis3a",9);WORDS.put("achra",10);WORDS.put("3achra",10);WORDS.put("3asra",10);
    }
    private VoiceParser(){}
    public static String toArabicScript(String input){
        if(input==null)return "";StringBuilder out=new StringBuilder();for(char c:input.toLowerCase(Locale.ROOT).toCharArray()){
            switch(c){case '3':out.append('ع');break;case '7':out.append('ح');break;case '9':out.append('ق');break;case '2':out.append('ء');break;case '5':out.append('خ');break;case '6':out.append('ط');break;case '8':out.append('غ');break;case '0':out.append('٠');break;case '1':out.append('١');break;case '4':out.append('٤');break;default:out.append(c);}
        }return out.toString();
    }
    public static Result parse(String input){
        String original=input==null?"":input.trim();String normalized=normalize(original);long amount=amountMilli(normalized);
        boolean child=normalized.contains("aatiit flous")||normalized.contains("3tit flous")||normalized.contains("3tيت فلوس")||normalized.contains("عطيت فلوس");
        boolean donation=normalized.contains("sadaqa")||normalized.contains("sada9a")||normalized.contains("don")||normalized.contains("صدقة");
        String type=child?"child_allowance":donation?"donation":"expense";String label=child?"Argent de poche":donation?"Don caritatif":"Dépense vocale";
        String childName="";int marker=normalized.lastIndexOf(" l");if(child&&marker>=0&&marker+2<normalized.length())childName=normalized.substring(marker+2).trim();
        if(childName.isEmpty()&&child){int arabic=original.lastIndexOf('ل');if(arabic>=0&&arabic+1<original.length())childName=original.substring(arabic+1).trim();}
        if(!donation&&!child){String cleaned=original.replaceAll("[0-9]+([.,][0-9]{1,3})?","").trim();if(!cleaned.isEmpty())label=cleaned;}
        return new Result(original,label,type,childName,amount);
    }
    private static long amountMilli(String text){
        if(text.contains("alf meyya")||text.contains("alf miyya")||text.contains("الف مية"))return 1100L;
        Matcher number=NUMBER.matcher(text);if(number.find()){
            BigDecimal value=new BigDecimal(number.group(1).replace(',','.'));String after=text.substring(number.end());
            try{if(after.matches(".*\\b(lef|alf)\\b.*"))return value.multiply(BigDecimal.valueOf(1000)).setScale(0,RoundingMode.HALF_UP).longValueExact();return value.setScale(3,RoundingMode.HALF_UP).movePointRight(3).longValueExact();}catch(RuntimeException ignored){return 0;}
        }
        long value=0;String[] tokens=text.split("[^\\p{L}0-9]+");
        for(int i=0;i<tokens.length;i++){
            String token=tokens[i];Integer numberWord=WORDS.get(token);
            if(numberWord!=null){long n=numberWord;String next=i+1<tokens.length?tokens[i+1]:"";
                if(next.equals("lef")||next.equals("alf")||next.equals("دينار")){value+=n*1000L;i++;}
                else if(next.equals("meyya")||next.equals("miyye")||next.equals("meya")){value+=n*100L;i++;}
                else if(next.startsWith("dinar")||next.equals("dt")||next.equals("dinars")){value+=n*1000L;i++;}
                else value+=n*1000L;
            } else if(token.equals("alf")||token.equals("lef"))value+=1000;
            else if(token.equals("meyya")||token.equals("miyye")||token.equals("meya"))value+=100;
        }
        return value;
    }
    private static String normalize(String input){String value=Normalizer.normalize(input.toLowerCase(Locale.ROOT),Normalizer.Form.NFD).replaceAll("\\p{M}+","").replace('’',' ').replace('ı','i');StringBuilder ascii=new StringBuilder();for(char c:value.toCharArray()){if(c>='٠'&&c<='٩')ascii.append((char)('0'+c-'٠'));else ascii.append(c);}return ascii.toString().replaceAll("[^\\p{L}\\p{N}.,]+"," ").replaceAll("\\s+"," ").trim();}
}
