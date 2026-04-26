"""
generate_data.py
----------------
יוצר נתוני עסקאות סינתטיים שמשקפים את מבנה ה-Firestore של Cardify.
95% עסקאות רגילות + 5% חריגות.

שימוש:
    python generate_data.py
    --> שומר: data/transactions_synthetic.csv
"""

import numpy as np
import pandas as pd
import os
import random
from datetime import datetime, timedelta

# --- הגדרות ---
N_USERS          = 50
N_PER_USER       = 200      # עסקאות לכל משתמש
ANOMALY_RATE     = 0.05     # 5% חריגות
OUTPUT_PATH      = 'data/transactions_synthetic.csv'
RANDOM_SEED      = 42

np.random.seed(RANDOM_SEED)
random.seed(RANDOM_SEED)

# --- בתי עסק ישראליים (נפוצים - Top 50) ---
KNOWN_MERCHANTS = [
    'שופרסל', 'רמי לוי', 'יוחננוף', 'מגה', 'ויקטורי',
    'ארומה', 'קפה גרג', 'קפה ג\'ו', 'קפיטל', 'קפה לנדוור',
    'מקדונלד\'ס', 'בורגר קינג', 'שוקולד', 'פרש מרקט', 'קפה הפרה',
    'בנק הפועלים', 'בנק לאומי', 'בנק דיסקונט', 'מזרחי טפחות', 'בנק מרכנתיל',
    'HOT', 'HOT Mobile', 'Partner', 'Cellcom', 'YES',
    'כרמל מרקט', 'AM:PM', 'שוק הכרמל', 'רולדין', 'ABC',
    'H&M', 'ZARA', 'פוקס', 'רנואר', 'גולף',
    'סופר-פארם', 'ניו-פארם', 'Good Pharm', 'כללית', 'מכבי',
    'דלק', 'פז', 'סונול', 'דור אלון', 'Ten',
    'רכבת ישראל', 'אגד', 'גט', 'Wolt', 'יונייטד',
]

# בתי עסק לא מוכרים (לחריגות)
UNKNOWN_MERCHANTS = [
    'AMAZON INTL', 'ALIEXPRESS', 'STEAM GAMES', 'NETFLIX USD',
    'UBER EATS NYC', 'MARRIOTT HOTELS', 'DELTA AIRLINES',
    'PAYPAL *UNKNOWN', 'APPLE.COM/BILL', 'GOOGLE *SERVICES',
    'FOREIGN STORE XYZ', 'INTL MERCHANT 001', 'OVERSEAS SHOP',
    'CRYPTO EXCHANGE', 'ONLINE CASINO PRO',
]

# קטגוריות
CATEGORIES = {
    'Food':      ['שופרסל', 'רמי לוי', 'יוחננוף', 'ארומה', 'קפה גרג', 'מקדונלד\'ס', 'Wolt', 'שוק הכרמל'],
    'Health':    ['כללית', 'מכבי', 'סופר-פארם', 'ניו-פארם', 'Good Pharm'],
    'Shopping':  ['H&M', 'ZARA', 'פוקס', 'רנואר', 'גולף', 'AMAZON INTL', 'ALIEXPRESS'],
    'Transport': ['דלק', 'פז', 'סונול', 'רכבת ישראל', 'אגד', 'גט'],
    'Education': ['כרמל מרקט'],
    'Other':     [],  # כל השאר
}

def _merchant_to_category(merchant):
    for cat, merchants in CATEGORIES.items():
        if merchant in merchants:
            return cat
    return 'Other'

def _random_date(start_year=2024, end_year=2025, anomaly=False):
    """מחזיר תאריך אקראי. חריגות - בסוף שבוע או שעות לילה."""
    start = datetime(start_year, 1, 1)
    end   = datetime(end_year, 12, 31)
    delta = (end - start).days
    d = start + timedelta(days=random.randint(0, delta))

    if anomaly:
        # סוף שבוע (שישי=4, שבת=5 ב-Python weekday)
        while d.weekday() not in (4, 5):
            d = start + timedelta(days=random.randint(0, delta))
    return d.strftime('%d-%m-%Y')

def generate_normal_transaction(user_id, user_median):
    """עסקה רגילה - סכום קרוב לחציון המשתמש, עסק מוכר."""
    merchant  = random.choice(KNOWN_MERCHANTS)
    # log-normal: ימין-מוטה, כמו הוצאות אמיתיות
    amount    = float(np.random.lognormal(mean=np.log(max(user_median, 10)), sigma=0.6))
    amount    = round(max(amount, 1.0), 2)
    date      = _random_date(anomaly=False)
    category  = _merchant_to_category(merchant)
    currency  = 'ILS'
    return {
        'user_id':           user_id,
        'businessName':      merchant,
        'amount':            amount,
        'date':              date,
        'category':          category,
        'txn_type':          random.choice(['רגילה', 'תשלומים']),
        'currency':          currency,
        'original_amount':   amount,
        'original_currency': currency,
        'status':            'REGULAR',
    }

def generate_anomalous_transaction(user_id, user_median):
    """עסקה חריגה - אחת מהתבניות הבאות."""
    anomaly_type = random.choice(['high_amount', 'new_merchant', 'foreign', 'late_night'])

    if anomaly_type == 'high_amount':
        # סכום גבוה פי 5-20 מהחציון
        merchant  = random.choice(KNOWN_MERCHANTS)
        amount    = round(user_median * np.random.uniform(5, 20), 2)
        currency  = 'ILS'
        date      = _random_date(anomaly=False)

    elif anomaly_type == 'new_merchant':
        # עסק לא מוכר
        merchant  = random.choice(UNKNOWN_MERCHANTS)
        amount    = round(float(np.random.lognormal(mean=np.log(max(user_median, 10)), sigma=0.6)), 2)
        currency  = 'ILS'
        date      = _random_date(anomaly=False)

    elif anomaly_type == 'foreign':
        # עסקה במטבע זר
        merchant  = random.choice(UNKNOWN_MERCHANTS)
        amount    = round(np.random.uniform(50, 500), 2)
        currency  = random.choice(['USD', 'EUR', 'GBP'])
        date      = _random_date(anomaly=False)

    else:  # late_night / weekend
        merchant  = random.choice(KNOWN_MERCHANTS + UNKNOWN_MERCHANTS)
        amount    = round(float(np.random.lognormal(mean=np.log(max(user_median * 2, 10)), sigma=0.8)), 2)
        currency  = 'ILS'
        date      = _random_date(anomaly=True)  # סוף שבוע

    category = _merchant_to_category(merchant)
    return {
        'user_id':           user_id,
        'businessName':      merchant,
        'amount':            amount,
        'date':              date,
        'category':          category,
        'txn_type':          random.choice(['רגילה', 'תשלומים', 'חו"ל']),
        'currency':          currency,
        'original_amount':   amount,
        'original_currency': currency,
        'status':            'IRREGULAR',
    }

def generate_dataset():
    all_transactions = []

    for u in range(N_USERS):
        user_id     = f'synthetic_user_{u:03d}'
        user_median = np.random.uniform(20, 500)   # חציון אישי שונה לכל משתמש
        n_normal    = int(N_PER_USER * (1 - ANOMALY_RATE))
        n_anomaly   = N_PER_USER - n_normal

        for _ in range(n_normal):
            all_transactions.append(generate_normal_transaction(user_id, user_median))
        for _ in range(n_anomaly):
            all_transactions.append(generate_anomalous_transaction(user_id, user_median))

    df = pd.DataFrame(all_transactions)
    df = df.sample(frac=1, random_state=RANDOM_SEED).reset_index(drop=True)  # ערבוב

    os.makedirs('data', exist_ok=True)
    df.to_csv(OUTPUT_PATH, index=False, encoding='utf-8-sig')

    print(f"Generated {len(df)} transactions for {N_USERS} users")
    print(f"  REGULAR:   {(df['status']=='REGULAR').sum()} ({(df['status']=='REGULAR').mean()*100:.1f}%)")
    print(f"  IRREGULAR: {(df['status']=='IRREGULAR').sum()} ({(df['status']=='IRREGULAR').mean()*100:.1f}%)")
    print(f"Saved to: {OUTPUT_PATH}")
    return df

if __name__ == '__main__':
    generate_dataset()