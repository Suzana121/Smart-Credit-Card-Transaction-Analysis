package com.cardify.app // ודאי שזה תואם לשם החבילה שלך

object NetworkConfig {
    // כל אחת יכולה לשנות כאן את ה-IP לצרכים שלה לפני ההרצה
    // 10.0.2.2 מתאים לאמולטור
    const val BASE_URL = "http://192.168.1.191:5001"

    // כתובות מלאות לנוחות (אופציונלי)
    const val REGISTER_URL = "$BASE_URL/auth/register"
}