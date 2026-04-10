import pandas as pd
import numpy as np
import os

np.random.seed(42)

# הגדרות כמויות
NUM_USERS = 50
NUM_NORMAL = 5000
NUM_ANOMALY = 250

# קטגוריות וסוגי עסקאות
CATEGORIES = ["מסעדות", "סופרמרקט", "אופנה", "דלק", "פנאי", "חשמל", "פארם", "חינוך", "תחבורה", "אחר"]
TRANSACTION_TYPES = ["רגילה", "תשלומים", "עסקת חו\"ל", "הוראת קבע"]

MERCHANTS = {
    "מסעדות": ["מקדונלד'ס", "קפה גרג", "גולדה", "דומינוס"],
    "סופרמרקט": ["שופרסל", "רמי לוי", "ויקטורי", "טיב טעם"],
    "אופנה": ["זארה", "H&M", "קסטרו", "פוקס"],
    "דלק": ["פז", "סונול", "דלק"],
    "פנאי": ["יס פלאנט", "ספוטיפיי", "נטפליקס"],
    "חשמל": ["KSP", "מחסני חשמל", "אייבורי"],
    "פארם": ["סופר-פארם", "Be"],
    "חינוך": ["אוניברסיטה", "Udemy"],
    "תחבורה": ["רב-קו", "Gett", "רכבת ישראל"],
    "אחר": ["עירייה", "חברת חשמל", "ביטוח"]
}

CATEGORY_PROFILE = {
    "מסעדות": (120, 50), "סופרמרקט": (350, 120), "אופנה": (250, 150),
    "דלק": (200, 40), "פנאי": (60, 20), "חשמל": (800, 500),
    "פארם": (150, 70), "חינוך": (200, 100), "תחבורה": (50, 20), "אחר": (200, 100)
}

def make_row(uid, cat, amt_ils, is_anomaly):
    merch = np.random.choice(MERCHANTS[cat])
    t_type = np.random.choice(TRANSACTION_TYPES, p=[0.7, 0.15, 0.1, 0.05])
    currency = "ILS"
    rate = 1.0
    
    if t_type == "עסקת חו\"ל":
        currency = np.random.choice(["USD", "EUR", "GBP"])
        rate = {"USD": 3.7, "EUR": 4.0, "GBP": 4.7}[currency]
    
    # חישוב הסכומים לפי שמות העמודות ב-Backend
    amount_transaction = round(amt_ils / rate, 2)
    amount_billing = round(amt_ils, 2)
    
    # תאריך ושעה
    day, month = np.random.randint(1, 29), np.random.randint(1, 13)
    hour = np.random.randint(8, 23) if not is_anomaly else np.random.randint(0, 6)
    date_str = f"2024-{month:02d}-{day:02d} {hour:02d}:{np.random.randint(0,60):02d}"

    return {
        "user_id": uid,
        "date": date_str,      # שונה מ-timestamp
        "merchant": merch,
        "category": cat,
        "amount": amount_transaction, # שונה מ-amount
        "currency": currency,
        "amount_billing": amount_billing, # שונה מ-amount_ils
        "transaction_type": t_type,
        "is_anomaly": is_anomaly
    }

rows = []
users = [f"user_{i:03d}" for i in range(1, NUM_USERS + 1)]

# יצירת עסקאות רגילות
for _ in range(NUM_NORMAL):
    cat = np.random.choice(CATEGORIES)
    mean, std = CATEGORY_PROFILE[cat]
    amt_ils = max(10, np.random.normal(mean, std))
    rows.append(make_row(np.random.choice(users), cat, amt_ils, 0))

# יצירת אנומליות (סכומים גבוהים פי 5-15)
for _ in range(NUM_ANOMALY):
    cat = np.random.choice(CATEGORIES)
    mean, std = CATEGORY_PROFILE[cat]
    amt_ils = mean * np.random.uniform(5, 15)
    rows.append(make_row(np.random.choice(users), cat, amt_ils, 1))

# ערבוב ושמירה
df = pd.DataFrame(rows).sample(frac=1).reset_index(drop=True)

# יצירת תיקיית דאטה אם לא קיימת
os.makedirs("data", exist_ok=True)
df.to_csv("data/transactions.csv", index=False, encoding="utf-8-sig")

print(f"✅ Dataset created successfully: data/transactions.csv")
print(f"📊 Total rows: {len(df)} (Normal: {NUM_NORMAL}, Anomalies: {NUM_ANOMALY})")