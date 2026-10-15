package com.qwibbler.salarm.salah;

import android.content.Context;

import com.batoulapps.adhan.CalculationMethod;
import com.batoulapps.adhan.Madhab;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Encodes and decodes Masjid definitions and prayer events to/from a compact QR code format.
 */
public class MasjidQrCodec {

    public static final String SCHEME_PREFIX = "salarm:v1?";

    private static final Map<String, String> CALC_TO_CODE = new HashMap<>();
    private static final Map<String, String> CODE_TO_CALC = new HashMap<>();

    static {
        addCalcMapping("MUSLIM_WORLD_LEAGUE", "mwl");
        addCalcMapping("EGYPTIAN", "egy");
        addCalcMapping("KARACHI", "kr");
        addCalcMapping("UMM_AL_QURA", "uaq");
        addCalcMapping("DUBAI", "dub");
        addCalcMapping("MOON_SIGHTING_COMMITTEE", "msc");
        addCalcMapping("NORTH_AMERICA", "na");
        addCalcMapping("KUWAIT", "kwt");
        addCalcMapping("QATAR", "qtr");
        addCalcMapping("SINGAPORE", "sg");
        addCalcMapping("TEHRAN", "thr");
        addCalcMapping("TURKEY", "tr");
        addCalcMapping("OTHER", "oth");
    }

    private static void addCalcMapping(String enumName, String code) {
        CALC_TO_CODE.put(enumName, code);
        CODE_TO_CALC.put(code, enumName);
    }

    public static class ParsedMasjidResult {
        public final Masjid masjid;
        public final List<MasjidEvent> events;

        public ParsedMasjidResult(Masjid masjid, List<MasjidEvent> events) {
            this.masjid = masjid;
            this.events = events;
        }
    }

    /**
     * Encodes a Masjid and its events into a compact QR code string.
     */
    public static String encode(Masjid masjid, List<MasjidEvent> events) {
        return encode(null, masjid, events);
    }

    /**
     * Encodes a Masjid and its events into a compact QR code string, skipping 'tz' if it matches autoderived timezone.
     */
    public static String encode(Context context, Masjid masjid, List<MasjidEvent> events) {
        if (masjid == null) return "Salarm";

        StringBuilder sb = new StringBuilder(SCHEME_PREFIX);
        Map<String, String> params = new LinkedHashMap<>();

        // Name
        if (masjid.name != null && !masjid.name.trim().isEmpty()) {
            params.put("n", encodeUriComponent(masjid.name.trim()));
        }

        // Lat / Long
        params.put("loc", String.format(Locale.US, "%.5f,%.5f", masjid.latitude, masjid.longitude));

        // Timezone (only if non-null and differs from location-derived timezone)
        if (masjid.timezoneId != null && !masjid.timezoneId.trim().isEmpty()) {
            String autoTz = null;
            if (context != null) {
                SalahUtils.OfflineCity autoCity = SalahUtils.findNearestOfflineCity(context, masjid.latitude, masjid.longitude);
                if (autoCity != null) {
                    autoTz = autoCity.timezoneId;
                }
            }
            if (autoTz == null || !masjid.timezoneId.trim().equals(autoTz)) {
                params.put("tz", encodeUriComponent(masjid.timezoneId.trim()));
            }
        }

        // Calculation Method
        if (masjid.calcMethod != null) {
            String code = CALC_TO_CODE.get(masjid.calcMethod.trim());
            if (code != null) {
                params.put("c", code);
            }
        }

        // Madhab
        if (masjid.madhab != null) {
            String mCode = "HANAFI".equalsIgnoreCase(masjid.madhab.trim()) ? "h" : "s";
            params.put("m", mCode);
        }

        // Intervals
        if (masjid.intervalsCount == 3) {
            params.put("i", masjid.boundary1 + "," + masjid.boundary2);
        } else {
            params.put("i", String.valueOf(masjid.boundary1 > 0 ? masjid.boundary1 : 16));
        }

        // Events
        if (events != null && !events.isEmpty()) {
            List<String> encodedEvents = new ArrayList<>();
            for (MasjidEvent event : events) {
                String eventCode = encodeMasjidEvent(event);
                if (eventCode != null && !eventCode.isEmpty()) {
                    encodedEvents.add(eventCode);
                }
            }
            if (!encodedEvents.isEmpty()) {
                params.put("e", String.join(";", encodedEvents));
            }
        }

        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!first) sb.append("&");
            sb.append(entry.getKey()).append("=").append(entry.getValue());
            first = false;
        }

        return sb.toString();
    }

    /**
     * Decodes a QR code string back into a Masjid object and its events.
     */
    public static ParsedMasjidResult decode(String qrText) {
        return decode(null, qrText);
    }

    /**
     * Decodes a QR code string back into a Masjid object and its events using context for timezone fallback.
     */
    public static ParsedMasjidResult decode(Context context, String qrText) {
        if (qrText == null || qrText.trim().isEmpty()) {
            return null;
        }

        String content = qrText.trim();
        Map<String, String> params = parseQueryParams(content);
        if (params.isEmpty() && !content.startsWith("salarm:")) {
            // Not a Salarm QR URI, return null
            return null;
        }

        Masjid masjid = new Masjid();
        List<MasjidEvent> events = new ArrayList<>();

        // Name
        if (params.get("n") != null) {
            masjid.name = decodeUriComponent(params.get("n"));
        }

        // Location
        if (params.get("loc") != null) {
            String loc = params.get("loc");
            if (loc != null) {
                String[] parts = loc.split(",");
                if (parts.length >= 2) {
                    try {
                        masjid.latitude = Double.parseDouble(parts[0].trim());
                        masjid.longitude = Double.parseDouble(parts[1].trim());
                    } catch (NumberFormatException ignored) {}
                }
            }
        }

        // Timezone
        if (params.get("tz") != null) {
            String tzVal = params.get("tz");
            if (tzVal != null) {
                masjid.timezoneId = decodeUriComponent(tzVal);
            }
        } else if (context != null) {
            SalahUtils.OfflineCity autoCity = SalahUtils.findNearestOfflineCity(context, masjid.latitude, masjid.longitude);
            if (autoCity != null) {
                masjid.timezoneId = autoCity.timezoneId;
            } else {
                masjid.timezoneId = SalahTimezoneUtils.getPhoneTimeZoneId();
            }
        } else {
            masjid.timezoneId = SalahTimezoneUtils.getPhoneTimeZoneId();
        }

        // Calculation Method
        if (params.containsKey("c")) {
            String code = params.get("c");
            String calcEnum = CODE_TO_CALC.get(code);
            if (calcEnum != null) {
                masjid.calcMethod = calcEnum;
            } else {
                masjid.calcMethod = CalculationMethod.MUSLIM_WORLD_LEAGUE.name();
            }
        } else {
            masjid.calcMethod = CalculationMethod.MUSLIM_WORLD_LEAGUE.name();
        }

        // Madhab
        if (params.containsKey("m")) {
            String m = params.get("m");
            masjid.madhab = "h".equalsIgnoreCase(m) ? Madhab.HANAFI.name() : Madhab.SHAFI.name();
        } else {
            masjid.madhab = Madhab.SHAFI.name();
        }

        // Intervals
        if (params.get("i") != null) {
            String iStr = params.get("i");
            if (iStr != null) {
                if (iStr.contains(",")) {
                    String[] parts = iStr.split(",");
                    masjid.intervalsCount = 3;
                    try {
                        masjid.boundary1 = Integer.parseInt(parts[0].trim());
                        masjid.boundary2 = Integer.parseInt(parts[1].trim());
                    } catch (NumberFormatException e) {
                        masjid.boundary1 = 10;
                        masjid.boundary2 = 20;
                    }
                } else {
                    masjid.intervalsCount = 2;
                    try {
                        masjid.boundary1 = Integer.parseInt(iStr.trim());
                        masjid.boundary2 = 31;
                    } catch (NumberFormatException e) {
                        masjid.boundary1 = 16;
                        masjid.boundary2 = 31;
                    }
                }
            }
        } else {
            masjid.intervalsCount = 2;
            masjid.boundary1 = 16;
            masjid.boundary2 = 31;
        }

        // Events
        if (params.get("e") != null) {
            String eventsStr = params.get("e");
            if (eventsStr != null) {
                String[] rawEvents = eventsStr.split(";");
                for (String rawEvent : rawEvents) {
                    MasjidEvent event = decodeMasjidEvent(rawEvent.trim());
                    if (event != null) {
                        events.add(event);
                    }
                }
            }
        }

        return new ParsedMasjidResult(masjid, events);
    }

    private static String encodeMasjidEvent(MasjidEvent event) {
        if (event == null || event.salahType == null || event.definition == null) return null;

        String typeCode = getSalahTypeCode(event.salahType);
        if (typeCode == null) return null;

        RelationalDefinition def = event.definition;
        if (def.isFixedTime) {
            return String.format(Locale.US, "%s:%02d:%02d", typeCode, def.fixedHour, def.fixedMinute);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(typeCode).append(":");

        // Offset
        int offset = (def.calculation != null) ? def.calculation.offsetMinutes : 0;
        sb.append(String.format(Locale.US, "%+d", offset));

        // Extreme (l/e) - omit if Maghrib default or false
        boolean isMaghrib = "MAGHRIB".equalsIgnoreCase(event.salahType);
        if (def.anchor != null) {
            if (!isMaghrib || def.anchor.isMax) {
                sb.append(def.anchor.isMax ? "l" : "e");
            }
        }

        // Anchor
        String anchorCode = getAnchorCode(def.anchor != null ? def.anchor.basicEvent : null);
        if (anchorCode != null) {
            sb.append(anchorCode);
        } else {
            sb.append(typeCode);
        }

        // Rounding
        if (def.calculation != null && def.calculation.roundingIntervalMinutes > 0 && def.calculation.roundingDirection != null) {
            sb.append("r");
            String dirCode = switch (def.calculation.roundingDirection) {
                case NEAREST -> "n";
                case UP -> "u";
                case DOWN -> "d";
            };
            sb.append(dirCode).append(def.calculation.roundingIntervalMinutes);

            // Adjustment (only allowed/encoded when rounding is present)
            if (def.calculation.adjustmentMinutes != 0) {
                sb.append("a").append(String.format(Locale.US, "%+d", def.calculation.adjustmentMinutes));
            }
        }

        return sb.toString();
    }

    private static MasjidEvent decodeMasjidEvent(String rawEvent) {
        if (rawEvent == null || !rawEvent.contains(":")) return null;

        String[] parts = rawEvent.split(":", 2);
        String typeCode = parts[0].trim();
        String defStr = parts[1].trim();

        String salahType = parseSalahTypeCode(typeCode);
        if (salahType == null) return null;

        MasjidEvent event = new MasjidEvent();
        event.salahType = salahType;
        event.isSelected = true;
        event.isManuallyDeselected = false;
        event.definition = new RelationalDefinition();

        // Check if fixed time e.g. 13:30
        if (defStr.contains(":")) {
            String[] timeParts = defStr.split(":");
            if (timeParts.length >= 2) {
                try {
                    event.definition.isFixedTime = true;
                    event.definition.fixedHour = Integer.parseInt(timeParts[0].trim());
                    event.definition.fixedMinute = Integer.parseInt(timeParts[1].trim());
                    return event;
                } catch (NumberFormatException ignored) {}
            }
        }

        // Relational definition parsing using regex
        // Pattern: ([+-]\d+)([le])?([a-z]+)(?:r([nud])(\d+)(?:a([+-]\d+))?)?
        Pattern pattern = Pattern.compile("^([+-]\\d+)([le])?([a-z]+)(?:r([nud])(\\d+)(?:a([+-]\\d+))?)?$");
        Matcher matcher = pattern.matcher(defStr);

        if (!matcher.matches()) {
            return null;
        }

        int offset = Integer.parseInt(matcher.group(1));
        String extreme = matcher.group(2);
        String anchorCode = matcher.group(3);
        String roundDir = matcher.group(4);
        String roundIntervalStr = matcher.group(5);
        String adjStr = matcher.group(6);

        event.definition.isFixedTime = false;
        if (event.definition.calculation == null) {
            event.definition.calculation = new RelationalCalculation();
        }
        event.definition.calculation.offsetMinutes = offset;

        if (event.definition.anchor == null) {
            event.definition.anchor = new RelationalAnchor();
        }

        if (extreme != null) {
            event.definition.anchor.isMax = "l".equalsIgnoreCase(extreme);
        } else {
            event.definition.anchor.isMax = false;
        }

        event.definition.anchor.basicEvent = parseAnchorCode(anchorCode);

        // Rounding
        if (roundDir != null && roundIntervalStr != null) {
            event.definition.calculation.roundingIntervalMinutes = Integer.parseInt(roundIntervalStr);
            event.definition.calculation.roundingDirection = switch (roundDir) {
                case "u" -> RelationalRule.RoundingDirection.UP;
                case "d" -> RelationalRule.RoundingDirection.DOWN;
                default -> RelationalRule.RoundingDirection.NEAREST;
            };

            // Adjustment
            if (adjStr != null) {
                event.definition.calculation.adjustmentMinutes = Integer.parseInt(adjStr);
            }
        }

        return event;
    }

    private static String getSalahTypeCode(String salahType) {
        if (salahType == null) return null;
        return switch (salahType.toUpperCase(Locale.US)) {
            case "FAJR" -> "f";
            case "SUNRISE" -> "s";
            case "DHUHR" -> "d";
            case "ASR" -> "a";
            case "MAGHRIB" -> "m";
            case "ISHA" -> "i";
            case "JUMA" -> "j";
            default -> null;
        };
    }

    private static String parseSalahTypeCode(String code) {
        if (code == null) return null;
        return switch (code.toLowerCase(Locale.US)) {
            case "f" -> "FAJR";
            case "s" -> "SUNRISE";
            case "d" -> "DHUHR";
            case "a" -> "ASR";
            case "m" -> "MAGHRIB";
            case "i" -> "ISHA";
            case "j" -> "JUMA";
            default -> null;
        };
    }

    private static String getAnchorCode(RelationalRule.AnchorEvent event) {
        if (event == null) return null;
        return switch (event) {
            case FAJR -> "f";
            case SUNRISE -> "s";
            case DHUHR -> "d";
            case ASR_SHAFI -> "as";
            case ASR_HANAFI -> "ah";
            case MAGHRIB -> "m";
            case ISHA -> "i";
        };
    }

    private static RelationalRule.AnchorEvent parseAnchorCode(String code) {
        if (code == null) return RelationalRule.AnchorEvent.FAJR;
        return switch (code.toLowerCase(Locale.US)) {
            case "f" -> RelationalRule.AnchorEvent.FAJR;
            case "s" -> RelationalRule.AnchorEvent.SUNRISE;
            case "d" -> RelationalRule.AnchorEvent.DHUHR;
            case "as" -> RelationalRule.AnchorEvent.ASR_SHAFI;
            case "ah", "a" -> RelationalRule.AnchorEvent.ASR_HANAFI;
            case "m" -> RelationalRule.AnchorEvent.MAGHRIB;
            case "i" -> RelationalRule.AnchorEvent.ISHA;
            default -> RelationalRule.AnchorEvent.FAJR;
        };
    }

    private static Map<String, String> parseQueryParams(String url) {
        Map<String, String> params = new LinkedHashMap<>();
        int questionMarkIdx = url.indexOf('?');
        String queryStr = (questionMarkIdx >= 0) ? url.substring(questionMarkIdx + 1) : url;

        String[] pairs = queryStr.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf('=');
            if (idx > 0) {
                String key = pair.substring(0, idx).trim();
                String value = pair.substring(idx + 1).trim();
                params.put(key, value);
            }
        }
        return params;
    }

    private static String encodeUriComponent(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }

    private static String decodeUriComponent(String value) {
        if (value == null) return null;
        try {
            return URLDecoder.decode(value.replace("%20", "+"), StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }
}
