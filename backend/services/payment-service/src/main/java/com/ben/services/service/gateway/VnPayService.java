package com.ben.services.service.gateway;

import com.ben.services.model.Payment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * VNPay (API v2.1.0) integration: builds the signed payment URL and verifies the
 * signature VNPay puts on the IPN call and on the browser redirect (Return URL).
 *
 * Signing rule: sort all vnp_* params by name, join as key=urlEncodedValue with '&',
 * then HMAC-SHA512 with the merchant's hash secret.
 */
@Service
public class VnPayService {

    private static final String VERSION = "2.1.0";
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter VNPAY_DATE = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Value("${vnpay.tmn-code}")
    private String tmnCode;

    @Value("${vnpay.hash-secret}")
    private String hashSecret;

    @Value("${vnpay.pay-url}")
    private String payUrl;

    // FE page VNPay redirects the browser to; the bookingId is appended
    @Value("${vnpay.return-url}")
    private String returnUrl;

    @Value("${vnpay.expire-minutes:10}")
    private int expireMinutes;

    public String createPaymentUrl(Payment payment, String clientIp) {
        ZonedDateTime now = ZonedDateTime.now(VN_ZONE);

        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Version", VERSION);
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", tmnCode);
        params.put("vnp_Amount", String.valueOf(toVnpAmount(payment.getAmount())));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", payment.getTransactionId());
        params.put("vnp_OrderInfo", "Thanh toan booking " + payment.getBookingId());
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", returnUrl + "/" + payment.getBookingId());
        params.put("vnp_IpAddr", clientIp);
        params.put("vnp_CreateDate", now.format(VNPAY_DATE));
        params.put("vnp_ExpireDate", now.plusMinutes(expireMinutes).format(VNPAY_DATE));

        String query = buildHashData(params);
        return payUrl + "?" + query + "&vnp_SecureHash=" + hmacSha512(hashSecret, query);
    }

    /** True if the vnp_* params (IPN or Return URL) were really signed by VNPay with our secret. */
    public boolean isValidSignature(Map<String, String> params) {
        String received = params.get("vnp_SecureHash");
        if (received == null) {
            return false;
        }
        Map<String, String> signed = new TreeMap<>(params);
        signed.remove("vnp_SecureHash");
        signed.remove("vnp_SecureHashType");
        String expected = hmacSha512(hashSecret, buildHashData(signed));
        // Constant-time compare so the check does not leak how many characters matched
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.US_ASCII),
                received.toLowerCase().getBytes(StandardCharsets.US_ASCII));
    }

    /**
     * When the link stops accepting payments: created + expire-minutes (same window as vnp_ExpireDate).
     * Booking stores it so a duplicate request knows whether the old link can still be handed out.
     */
    public Instant expiresAt(Payment payment) {
        Instant created = payment.getCreatedAt() != null
                ? payment.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant()
                : Instant.now();
        return created.plus(Duration.ofMinutes(expireMinutes));
    }

    /** VNPay amounts are in VND x 100 with no decimals. */
    public long toVnpAmount(Double amount) {
        return Math.round(amount) * 100;
    }

    private String buildHashData(Map<String, String> sortedParams) {
        return sortedParams.entrySet().stream()
                .filter(e -> e.getKey().startsWith("vnp_") && e.getValue() != null && !e.getValue().isEmpty())
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.US_ASCII))
                .collect(Collectors.joining("&"));
    }

    private String hmacSha512(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Cannot compute VNPay signature", e);
        }
    }
}
