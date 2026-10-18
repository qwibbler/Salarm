package com.qwibbler.salarm.salah;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.batoulapps.adhan.CalculationMethod;
import com.batoulapps.adhan.Madhab;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class MasjidQrCodecTest {

    @Test
    public void testEncodeAndDecode_FullMasjid() {
        Masjid masjid = new Masjid();
        masjid.name = "Islamic Center of North America";
        masjid.latitude = 37.7749;
        masjid.longitude = -122.4194;
        masjid.timezoneId = "America/Los_Angeles";
        masjid.calcMethod = CalculationMethod.KARACHI.name();
        masjid.madhab = Madhab.HANAFI.name();
        masjid.intervalsCount = 2;
        masjid.boundary1 = 15;
        masjid.boundary2 = 31;

        List<MasjidEvent> events = new ArrayList<>();

        // 1. Fajr: -5esrn10a+2
        MasjidEvent fajr = new MasjidEvent();
        fajr.salahType = "FAJR";
        fajr.definition.calculation.offsetMinutes = -5;
        fajr.definition.anchor.isMax = false;
        fajr.definition.anchor.basicEvent = RelationalRule.AnchorEvent.SUNRISE;
        fajr.definition.calculation.roundingDirection = RelationalRule.RoundingDirection.NEAREST;
        fajr.definition.calculation.roundingIntervalMinutes = 10;
        fajr.definition.calculation.adjustmentMinutes = 2;
        events.add(fajr);

        // 2. Dhuhr: Fixed time 13:30
        MasjidEvent dhuhr = new MasjidEvent();
        dhuhr.salahType = "DHUHR";
        dhuhr.definition.isFixedTime = true;
        dhuhr.definition.fixedHour = 13;
        dhuhr.definition.fixedMinute = 30;
        events.add(dhuhr);

        // 3. Asr: +15ls
        MasjidEvent asr = new MasjidEvent();
        asr.salahType = "ASR";
        asr.definition.calculation.offsetMinutes = 15;
        asr.definition.anchor.isMax = true;
        asr.definition.anchor.basicEvent = RelationalRule.AnchorEvent.SUNRISE;
        events.add(asr);

        // 4. Maghrib: +0m
        MasjidEvent maghrib = new MasjidEvent();
        maghrib.salahType = "MAGHRIB";
        maghrib.definition.calculation.offsetMinutes = 0;
        maghrib.definition.anchor.basicEvent = RelationalRule.AnchorEvent.MAGHRIB;
        events.add(maghrib);

        // 5. Isha: +20lmaru5a+1
        MasjidEvent isha = new MasjidEvent();
        isha.salahType = "ISHA";
        isha.definition.calculation.offsetMinutes = 20;
        isha.definition.anchor.isMax = true;
        isha.definition.anchor.basicEvent = RelationalRule.AnchorEvent.MAGHRIB;
        isha.definition.calculation.roundingDirection = RelationalRule.RoundingDirection.UP;
        isha.definition.calculation.roundingIntervalMinutes = 5;
        isha.definition.calculation.adjustmentMinutes = 1;
        events.add(isha);

        String encoded = MasjidQrCodec.encode(null, masjid, events);
        assertNotNull(encoded);
        assertTrue(encoded.startsWith("https://salarm.meafar.com/mc?"));
        assertTrue(encoded.contains("n=Islamic%20Center%20of%20North%20America"));
        assertTrue(encoded.contains("c=kr"));
        assertTrue(encoded.contains("m=h"));
        assertTrue(encoded.contains("i=15"));
        assertTrue(encoded.contains("tz=America%2FLos_Angeles"));

        MasjidQrCodec.ParsedMasjidResult result = MasjidQrCodec.decode(null, encoded);
        assertNotNull(result);
        assertNotNull(result.masjid);
        assertEquals("Islamic Center of North America", result.masjid.name);
        assertEquals(37.7749, result.masjid.latitude, 0.0001);
        assertEquals(-122.4194, result.masjid.longitude, 0.0001);
        assertEquals("America/Los_Angeles", result.masjid.timezoneId);
        assertEquals(CalculationMethod.KARACHI.name(), result.masjid.calcMethod);
        assertEquals(Madhab.HANAFI.name(), result.masjid.madhab);
        assertEquals(2, result.masjid.intervalsCount);
        assertEquals(15, result.masjid.boundary1);

        assertEquals(5, result.events.size());

        // Verify Fajr
        MasjidEvent decodedFajr = result.events.stream().filter(e -> "FAJR".equalsIgnoreCase(e.salahType)).findFirst().orElse(null);
        assertNotNull(decodedFajr);
        assertEquals(-5, decodedFajr.definition.calculation.offsetMinutes);
        assertFalse(decodedFajr.definition.anchor.isMax);
        assertEquals(RelationalRule.AnchorEvent.SUNRISE, decodedFajr.definition.anchor.basicEvent);
        assertEquals(RelationalRule.RoundingDirection.NEAREST, decodedFajr.definition.calculation.roundingDirection);
        assertEquals(10, decodedFajr.definition.calculation.roundingIntervalMinutes);
        assertEquals(2, decodedFajr.definition.calculation.adjustmentMinutes);

        // Verify Dhuhr fixed time
        MasjidEvent decodedDhuhr = result.events.stream().filter(e -> "DHUHR".equalsIgnoreCase(e.salahType)).findFirst().orElse(null);
        assertNotNull(decodedDhuhr);
        assertTrue(decodedDhuhr.definition.isFixedTime);
        assertEquals(13, decodedDhuhr.definition.fixedHour);
        assertEquals(30, decodedDhuhr.definition.fixedMinute);

        // Verify Maghrib
        MasjidEvent decodedMaghrib = result.events.stream().filter(e -> "MAGHRIB".equalsIgnoreCase(e.salahType)).findFirst().orElse(null);
        assertNotNull(decodedMaghrib);
        assertEquals(0, decodedMaghrib.definition.calculation.offsetMinutes);
        assertEquals(RelationalRule.AnchorEvent.MAGHRIB, decodedMaghrib.definition.anchor.basicEvent);

        // Verify Isha
        MasjidEvent decodedIsha = result.events.stream().filter(e -> "ISHA".equalsIgnoreCase(e.salahType)).findFirst().orElse(null);
        assertNotNull(decodedIsha);
        assertEquals(20, decodedIsha.definition.calculation.offsetMinutes);
        assertEquals(RelationalRule.AnchorEvent.MAGHRIB, decodedIsha.definition.anchor.basicEvent);
        assertEquals(RelationalRule.RoundingDirection.UP, decodedIsha.definition.calculation.roundingDirection);
        assertEquals(5, decodedIsha.definition.calculation.roundingIntervalMinutes);
        assertEquals(1, decodedIsha.definition.calculation.adjustmentMinutes);
    }

    @Test
    public void testThreeIntervalsEncoding() {
        Masjid masjid = new Masjid();
        masjid.name = "Three Intervals Masjid";
        masjid.latitude = 24.8607;
        masjid.longitude = 67.0011;
        masjid.intervalsCount = 3;
        masjid.boundary1 = 11;
        masjid.boundary2 = 21;

        String encoded = MasjidQrCodec.encode(null, masjid, null);
        assertTrue(encoded.contains("i=11%2C21") || encoded.contains("i=11,21"));

        MasjidQrCodec.ParsedMasjidResult result = MasjidQrCodec.decode(null, encoded);
        assertNotNull(result);
        assertEquals(3, result.masjid.intervalsCount);
        assertEquals(11, result.masjid.boundary1);
        assertEquals(21, result.masjid.boundary2);
    }

    @Test
    public void testRelationalEventShortCodes() {
        // Test +0esrn5
        MasjidEvent event1 = new MasjidEvent();
        event1.salahType = "FAJR";
        event1.definition.calculation.offsetMinutes = 0;
        event1.definition.anchor.isMax = false;
        event1.definition.anchor.basicEvent = RelationalRule.AnchorEvent.SUNRISE;
        event1.definition.calculation.roundingDirection = RelationalRule.RoundingDirection.NEAREST;
        event1.definition.calculation.roundingIntervalMinutes = 5;

        Masjid masjid = new Masjid();
        masjid.name = "Test";
        List<MasjidEvent> events = List.of(event1);

        String encoded = MasjidQrCodec.encode(null, masjid, events);
        assertTrue(encoded.contains("e=f%3A%2B0esrn5") || encoded.contains("e=f:+0esrn5"));

        MasjidQrCodec.ParsedMasjidResult decoded = MasjidQrCodec.decode(null, encoded);
        assertNotNull(decoded);
        assertEquals(1, decoded.events.size());
        MasjidEvent e = decoded.events.get(0);
        assertEquals(0, e.definition.calculation.offsetMinutes);
        assertFalse(e.definition.anchor.isMax);
        assertEquals(RelationalRule.AnchorEvent.SUNRISE, e.definition.anchor.basicEvent);
        assertEquals(RelationalRule.RoundingDirection.NEAREST, e.definition.calculation.roundingDirection);
        assertEquals(5, e.definition.calculation.roundingIntervalMinutes);
        assertEquals(0, e.definition.calculation.adjustmentMinutes);
    }

    @Test
    public void testInvalidOrNullInput() {
        assertNull(MasjidQrCodec.decode(null, null));
        assertNull(MasjidQrCodec.decode(null, ""));
        assertNull(MasjidQrCodec.decode(null, "invalid text without scheme"));
        assertNull(MasjidQrCodec.decode(null, "http://example.com?name=test&loc=1,2"));
        assertNull(MasjidQrCodec.decode(null, "https://google.com/maps?q=123,456"));
    }

    @Test
    public void testEventsWithUnencodedPlusAndSpaces() {
        String qr = "https://salarm.meafar.com/mc?n=Test%20Masjid&loc=37.7749,-122.4194&e=f:+0s;d:13:30;a:15ls";
        MasjidQrCodec.ParsedMasjidResult result = MasjidQrCodec.decode(null, qr);
        assertNotNull(result);
        assertNotNull(result.masjid);
        assertEquals("Test Masjid", result.masjid.name);
        assertEquals(3, result.events.size());

        MasjidEvent fajr = result.events.get(0);
        assertEquals("FAJR", fajr.salahType);
        assertEquals(0, fajr.definition.calculation.offsetMinutes);
        assertEquals(RelationalRule.AnchorEvent.SUNRISE, fajr.definition.anchor.basicEvent);

        MasjidEvent dhuhr = result.events.get(1);
        assertEquals("DHUHR", dhuhr.salahType);
        assertTrue(dhuhr.definition.isFixedTime);
        assertEquals(13, dhuhr.definition.fixedHour);
        assertEquals(30, dhuhr.definition.fixedMinute);

        MasjidEvent asr = result.events.get(2);
        assertEquals("ASR", asr.salahType);
        assertEquals(15, asr.definition.calculation.offsetMinutes);
        assertTrue(asr.definition.anchor.isMax);
    }

    @Test
    public void testUserProvidedQrString() {
        String qr = "https://salarm.meafar.com/mc?n=Northern%20Sui%20Gas%20Society%20Jamia%20Masjid&loc=31.45472,74.39781&c=kr&m=h&i=10,20&e=f:-20es;j:13:30;d:13:30;a:+0lah;m:+0m;i:+0li";
        MasjidQrCodec.ParsedMasjidResult result = MasjidQrCodec.decode(null, qr);
        assertNotNull(result);
        assertNotNull(result.masjid);
        assertEquals("Northern Sui Gas Society Jamia Masjid", result.masjid.name);
        assertEquals(31.45472, result.masjid.latitude, 0.00001);
        assertEquals(74.39781, result.masjid.longitude, 0.00001);
        assertEquals(CalculationMethod.KARACHI.name(), result.masjid.calcMethod);
        assertEquals(Madhab.HANAFI.name(), result.masjid.madhab);
        assertEquals(3, result.masjid.intervalsCount);
        assertEquals(10, result.masjid.boundary1);
        assertEquals(20, result.masjid.boundary2);

        assertNotNull(result.events);
        assertEquals(6, result.events.size());
    }

    @Test
    public void testBaselinePayloadFromPrompt() {
        String qr = "https://salarm.meafar.com/mc?n=Test%20Masjid&loc=24.8607%2C67.0011&tz=Asia%2FKarachi&c=mwl&m=h&i=16&e=f%3A-20f%3Br%3A30";
        MasjidQrCodec.ParsedMasjidResult result = MasjidQrCodec.decode(null, qr);
        assertNotNull(result);
        assertNotNull(result.masjid);
        assertEquals("Test Masjid", result.masjid.name);
        assertEquals(24.8607, result.masjid.latitude, 0.0001);
        assertEquals(67.0011, result.masjid.longitude, 0.0001);
        assertEquals("Asia/Karachi", result.masjid.timezoneId);
        assertEquals(CalculationMethod.MUSLIM_WORLD_LEAGUE.name(), result.masjid.calcMethod);
        assertEquals(Madhab.HANAFI.name(), result.masjid.madhab);
        assertEquals(1, result.events.size());
        assertEquals("FAJR", result.events.get(0).salahType);
        assertEquals(-20, result.events.get(0).definition.calculation.offsetMinutes);
    }

    @Test
    public void testMalformedPercentEscapes() {
        String qr = "https://salarm.meafar.com/mc?n=Test%2&loc=24.8607%ZZ67.0011&e=f:%2B10s";
        MasjidQrCodec.ParsedMasjidResult result = MasjidQrCodec.decode(null, qr);
        assertNotNull(result);
        assertNotNull(result.masjid);
        assertEquals("Test%2", result.masjid.name);
    }

    @Test
    public void testOversizedIntegersAndMalformedEvents() {
        String qr = "https://salarm.meafar.com/mc?n=Test&e=f:%2B99999999999999999999s;d:13:30;invalid_event";
        MasjidQrCodec.ParsedMasjidResult result = MasjidQrCodec.decode(null, qr);
        assertNotNull(result);
        assertEquals(1, result.events.size());
        assertEquals("DHUHR", result.events.get(0).salahType);
    }

    @Test
    public void testPayloadWithNoEvents() {
        String qr = "https://salarm.meafar.com/mc?n=No%20Events%20Masjid&loc=30.0,70.0";
        MasjidQrCodec.ParsedMasjidResult result = MasjidQrCodec.decode(null, qr);
        assertNotNull(result);
        assertNotNull(result.masjid);
        assertEquals("No Events Masjid", result.masjid.name);
        assertTrue(result.events.isEmpty());
    }
}
