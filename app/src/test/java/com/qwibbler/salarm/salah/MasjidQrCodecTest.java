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
        assertTrue(encoded.startsWith("salarm:v1?"));
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
    }
}
