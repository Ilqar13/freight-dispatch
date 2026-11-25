package az.freight.util;

public final class PhoneUtil {
    private PhoneUtil() {}

    /** Boşluq, tire və mötərizələri atır: "+994 50 123-45-67" → "+994501234567". */
    public static String normalize(String phone) {
        return phone == null ? "" : phone.replaceAll("[\\s\\-()]", "").trim();
    }
}
