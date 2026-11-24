package com.masrouf.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public final class VoiceParserTest {
    @Test public void recognizesDerjaPurchaseAndProduct(){
        VoiceParser.Result result=VoiceParser.parse("chrit khobz b alfin");
        assertEquals("purchase",result.type);assertEquals("bread",result.productKey);assertEquals(2000L,result.amountMilli);assertFalse(result.ambiguous);
    }
    @Test public void parsesDinarAndMillimeWords(){
        assertEquals(5000L,VoiceParser.parse("khamsa dinars").amountMilli);
        assertEquals(10000L,VoiceParser.parse("achra dinars").amountMilli);
        assertEquals(1100L,VoiceParser.parse("alf meyya").amountMilli);
        assertEquals(2100L,VoiceParser.parse("alfin w mya").amountMilli);
    }
    @Test public void recognizesDonationAndChildAllowance(){
        assertEquals("donation",VoiceParser.parse("sadaqa 2 DT").type);
        VoiceParser.Result child=VoiceParser.parse("a3tit flous l Sara 5 dt");
        assertEquals("child_allowance",child.type);assertEquals("sara",child.childName);assertEquals(5000L,child.amountMilli);
    }
    @Test public void arabiziConversionDoesNotChangeAmounts(){
        assertEquals("dfaعt 10.500 DT",VoiceParser.toArabicScript("dfa3t 10.500 DT"));
    }
    @Test public void flagsMissingAmountsAndMultipleNumericCandidates(){
        assertTrue(VoiceParser.parse("chrit khobz").ambiguous);
        assertTrue(VoiceParser.parse("chrit khobz 2 kg b 3 DT").ambiguous);
    }
}
