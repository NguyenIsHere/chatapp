package com.example.chatapp.util;

public class PhoneNumberUtil {
  public static String normalizePhoneNumber(String phoneNumber) {
    String cleanedNumber = phoneNumber.replaceAll("[^0-9+]", "");
    if (!cleanedNumber.startsWith("+")) {
      if (cleanedNumber.startsWith("0")) {
        cleanedNumber = "+84" + cleanedNumber.substring(1);
      } else {
        cleanedNumber = "+84" + cleanedNumber;
      }
    }
    return cleanedNumber;
  }
}