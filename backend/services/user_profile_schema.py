"""
user_profile_schema.py
-----------------------
מגדיר את מבנה פרופיל המשתמש ב-Firestore.
משמש כ-single source of truth לכל השדות והברירות מחדל.

Collection: user_profiles
Document ID: user_id (זהה ל-Firebase Auth UID)
"""

from google.cloud import firestore


def default_profile() -> dict:
    """
    מחזיר פרופיל ריק עם כל השדות והברירות מחדל.
    משמש למשתמש חדש שעוד לא העלה קובץ (Cold Start).
    """
    return {
        # --- סטטיסטיקות הוצאות ---
        "median_spend":       0.0,   # חציון הוצאות ב-ILS
        "mean_spend":         0.0,   # ממוצע הוצאות ב-ILS
        "all_amounts":        [],    # רשימת כל הסכומים (עד 1000 אחרונים)

        # --- היסטוריית עסקים ---
        "known_merchants":    [],    # רשימת עסקים מוכרים
        "top_merchants":      {},    # {שם_עסק: מספר_עסקאות}

        # --- קטגוריות ---
        "category_counts":    {},    # {קטגוריה: מספר_עסקאות}

        # --- מטבע ---
        "primary_currency":   "ILS", # המטבע הנפוץ ביותר
        "currency_history":   [],    # רשימת מטבעות (עד 1000 אחרונים)

        # --- מטא ---
        "total_transactions": 0,     # סך כל העסקאות שנותחו
        "upload_count":       0,     # מספר פעמים שהעלה קובץ
        "last_upload_at":     None,  # תאריך העלאה אחרונה
        "created_at":         firestore.SERVER_TIMESTAMP,
    }


def validate_profile(profile: dict) -> dict:
    """
    מוודא שכל השדות קיימים בפרופיל.
    שימושי למשתמשים ישנים שנוצרו לפני שהוספנו שדות חדשים.
    ממלא שדות חסרים בברירות המחדל.
    """
    defaults = default_profile()
    validated = {}
    for key, default_val in defaults.items():
        val = profile.get(key, default_val)
        # בדיקת סוג - אם הסוג שגוי, נחזור לברירת מחדל
        if not isinstance(val, type(default_val)) and default_val is not None:
            val = default_val
        validated[key] = val
    return validated


def merge_profile(existing: dict, new_data: dict) -> dict:
    """
    ממזג פרופיל קיים עם נתונים חדשים מהעלאה.
    מחזיר פרופיל מעודכן לשמירה ב-Firestore.
    """
    import numpy as np
    from collections import Counter

    # ודא שהפרופיל הקיים שלם
    base = validate_profile(existing)

    # --- עדכון סכומים ---
    all_amounts = base["all_amounts"] + new_data.get("new_amounts", [])
    all_amounts = all_amounts[-1000:]  # שמור רק 1000 אחרונים

    base["all_amounts"]  = all_amounts
    base["median_spend"] = float(np.median(all_amounts)) if all_amounts else 0.0
    base["mean_spend"]   = float(np.mean(all_amounts))   if all_amounts else 0.0

    # --- עדכון עסקים ---
    new_merchants = new_data.get("new_merchants", [])
    known = set(base["known_merchants"]) | set(new_merchants)
    base["known_merchants"] = list(known)

    top = Counter(base["top_merchants"])
    for m in new_merchants:
        top[m] += 1
    base["top_merchants"] = dict(top.most_common(100))  # שמור top 100

    # --- עדכון קטגוריות ---
    cat_counts = Counter(base["category_counts"])
    for cat in new_data.get("new_categories", []):
        cat_counts[cat] += 1
    base["category_counts"] = dict(cat_counts)

    # --- עדכון מטבע ---
    currency_history = base["currency_history"] + new_data.get("new_currencies", [])
    currency_history = currency_history[-1000:]
    base["currency_history"]  = currency_history
    base["primary_currency"]  = Counter(currency_history).most_common(1)[0][0] \
                                 if currency_history else "ILS"

    # --- עדכון מטא ---
    base["total_transactions"] += new_data.get("count", 0)
    base["upload_count"]       += 1
    base["last_upload_at"]      = firestore.SERVER_TIMESTAMP

    return base