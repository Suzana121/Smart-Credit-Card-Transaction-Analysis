import pandas as pd
import numpy as np
import joblib
import json
import os
from sklearn.ensemble import IsolationForest
from sklearn.preprocessing import StandardScaler

print("🚀 Starting Advanced Training Script...")

try:
    # 1. טעינת הנתונים
    if not os.path.exists("../data/transactions.csv"):
        raise FileNotFoundError("הקובץ transactions.csv לא נמצא בתיקיית data")
    
    df = pd.read_csv("../data/transactions.csv")
    print(f"📂 Data loaded: {len(df)} rows")

    # 2. הנדסת פיצ'רים (Feature Engineering)
    print("🔧 Engineering features...")

    # א. זמן (מבוסס תאריך בלבד)
    df['date'] = pd.to_datetime(df['date'])
    df['day_of_week'] = df['date'].dt.dayofweek
    df['is_weekend'] = (df['day_of_week'] >= 5).astype(int)

    # ב. סכומים ויחסים
    df['ratio_billing_to_transaction'] = df['amount_billing'] / (df['amount_transaction'] + 0.01)

    # ג. בית עסק ותדירות
    df['merchant_frequency'] = df.groupby('merchant')['merchant'].transform('count')
    df['is_new_merchant'] = (df['merchant_frequency'] == 1).astype(int)

    # ד. טיפול במידע חסר (Flags)
    df['category_missing'] = df['category'].isna().astype(int)
    df['currency_missing'] = df['currency'].isna().astype(int)

    # ה. מילוי ערכים חסרים לפני קידוד
    df['category'] = df['category'].fillna('Unknown')
    df['transaction_type'] = df['transaction_type'].fillna('Unknown')
    df['currency'] = df['currency'].fillna('Unknown')

    # ו. קידוד קטגוריות (One-Hot Encoding)
    # הערה: זה יוצר עמודות נפרדות לכל קטגוריה (cat_מסעדות, cat_אופנה וכו')
    df = pd.get_dummies(df, columns=['category', 'transaction_type', 'currency'], prefix=['cat', 'type', 'curr'])

    # 3. בחירת עמודות הפיצ'רים הסופיות
    # אנחנו לוקחים את כל העמודות שיצרנו + עמודות ה-One-Hot
    FEATURE_COLS = [
        'amount_transaction', 'amount_billing', 'ratio_billing_to_transaction',
        'merchant_frequency', 'is_new_merchant', 'category_missing', 'currency_missing',
        'day_of_week', 'is_weekend'
    ] + [c for c in df.columns if c.startswith(('cat_', 'type_', 'curr_'))]

    X = df[FEATURE_COLS].fillna(0)
    print(f"📊 Features selected: {len(FEATURE_COLS)} columns")

    # 4. נרמול (StandardScaler) - חובה!
    print("📏 Normalizing data...")
    scaler = StandardScaler()
    X_scaled = scaler.fit_transform(X)

    # 5. אימון המודל (Isolation Forest)
    print("🧠 Training Isolation Forest...")
    model = IsolationForest(n_estimators=200, contamination=0.05, random_state=42)
    model.fit(X_scaled)

    # 6. יצירת Baseline עבור ה-Explainer (הסברים לחריגות)
    # אנחנו שומרים את הממוצעים של עסקאות "תקינות" כדי להשוות מולן
    normal_indices = model.predict(X_scaled) == 1
    baseline_stats = X[normal_indices].mean().to_dict()

    # 7. שמירת כל הקבצים לתיקיית ה-Backend
    output_dir = "../backend/models"
    os.makedirs(output_dir, exist_ok=True)

    joblib.dump(model, os.path.join(output_dir, "model.pkl"))
    joblib.dump(scaler, os.path.join(output_dir, "scaler.pkl"))
    joblib.dump(baseline_stats, os.path.join(output_dir, "explainer.pkl"))
    
    with open(os.path.join(output_dir, "feature_columns.json"), "w", encoding="utf-8") as f:
        json.dump(FEATURE_COLS, f, ensure_ascii=False, indent=2)

    print(f"✅ All 4 files saved successfully in: {output_dir}")
    print(f"📈 Model ready for prediction with {len(FEATURE_COLS)} features.")

except Exception as e:
    print(f"❌ Error during training: {e}")