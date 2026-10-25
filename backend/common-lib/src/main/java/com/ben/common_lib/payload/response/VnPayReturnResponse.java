package com.ben.common_lib.payload.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * What the FE shows after VNPay redirects the browser back (Return URL).
 * Display only: the booking/payment status is changed by the IPN, never by this.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VnPayReturnResponse {

    // Signature matched: the params really come from VNPay
    private boolean validSignature;

    // VNPay reports the payment as successful (vnp_ResponseCode = 00)
    private boolean success;

    private String transactionId;
    private String responseCode;
    private String message;
}
