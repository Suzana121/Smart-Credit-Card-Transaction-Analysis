"""
train_model.py
--------------
מאמן את מודל ה-Isolation Forest על הנתונים הסינתטיים,
ושומר את המודל, ה-Scaler וה-Encoder כקבצי .pkl.

שימוש:
    python train_model.py
    --> שומר: models/isolation_forest.pkl
               models/scaler.pkl
               models/encoder.pkl
               models/top_merchants.pkl
"""

import pandas as pd
import numpy as np
import joblib
import os
from sklearn.ensemble import IsolationForest
from sklearn.preprocessing import StandardScaler
from sklearn.metrics import classification_report, confusion_matrix

DATA_PATH    = 'data/transactions_synthetic.csv'
MODELS_DIR   = 'models'
TOP_N_MERCHANTS = 50   # מגבילים ל-Top 50 עסקים

# ------------------------------------------------
# 1. טעינת נתונים
# ------------------------------------------------
print("Loading data...")
df = pd.read_csv(DATA_PATH)
print(f"  Total rows: {len(df)}")

# ------------------------------------------------
# 2. Feature Engineering
# ------------------------------------------------
print("Engineering features...")

def parse_date_features(date_str):
    """מחלץ day_of_week ו-month מתאריך בפורמט DD-MM-YYYY."""
    try:
        d = pd.to_datetime(date_str, dayfirst=True)
        return d.dayofweek, d.month
    except Exception:
        return 3, 6  # ברירת מחדל: רביעי, יוני

# day_of_week (0=Sunday בקוד שלנו - ישראל)
# Python: Monday=0 ... Sunday=6, אנחנו ממירים ל: Sunday=0 ... Saturday=6
date_features = df['date'].apply(parse_date_features)
df['day_of_week'] = date_features.apply(lambda x: (x[0] + 1) % 7)  # המרה לסדר ישראלי
df['month']       = date_features.apply(lambda x: x[1])
df['is_weekend']  = df['day_of_week'].isin([5, 6]).astype(int)  # שישי=5, שבת=6

# amount_log: log של הסכום (מפחית עיוות מסכומים גדולים)
df['amount_log'] = np.log1p(df['amount'])

# currency_is_foreign
df['currency_is_foreign'] = (df['currency'] != 'ILS').astype(int)

# --- User Profile Features (מחושבים per-user) ---
# amount_vs_median: כמה גדולה העסקה ביחס לחציון של אותו משתמש
user_medians = df.groupby('user_id')['amount'].transform('median')
df['amount_vs_median'] = (df['amount'] / user_medians.replace(0, 1)).clip(upper=50.0)

# is_new_merchant: בנתונים הסינתטיים - עסקים של IRREGULAR הם תמיד חדשים
df['is_new_merchant'] = (df['status'] == 'IRREGULAR').astype(int)

# category_frequency: תדירות הקטגוריה per-user
cat_freq = df.groupby(['user_id', 'category'])['amount'].transform('count')
total_per_user = df.groupby('user_id')['amount'].transform('count')
df['category_frequency'] = cat_freq / total_per_user

# ------------------------------------------------
# 3. One-Hot Encoding - Top 50 merchants + Other
# ------------------------------------------------
top_merchants = df['businessName'].value_counts().head(TOP_N_MERCHANTS).index.tolist()
df['merchant_encoded'] = df['businessName'].apply(
    lambda x: x if x in top_merchants else 'Other'
)
merchant_dummies = pd.get_dummies(df['merchant_encoded'], prefix='m')

# One-Hot לקטגוריות
category_dummies = pd.get_dummies(df['category'], prefix='cat')

# One-Hot למטבע
currency_dummies = pd.get_dummies(df['currency'], prefix='cur')

# ------------------------------------------------
# 4. הרכבת מטריצת Features
# ------------------------------------------------
feature_cols = ['amount_log', 'day_of_week', 'month', 'is_weekend', 'currency_is_foreign', 'amount_vs_median', 'is_new_merchant', 'category_frequency']
X_base = df[feature_cols].copy()
X = pd.concat([X_base, merchant_dummies, category_dummies, currency_dummies], axis=1)
X = X.fillna(0)

y_true = (df['status'] == 'IRREGULAR').astype(int)

print(f"  Feature matrix shape: {X.shape}")

# ------------------------------------------------
# 5. StandardScaler - fit על train בלבד
# ------------------------------------------------
print("Fitting StandardScaler...")
# Train/Test split - 80/20 לפי הסדר הכרונולוגי (אחרי ערבוב כבר נעשה ב-generate_data)
split_idx = int(len(X) * 0.8)
X_train, X_test = X.iloc[:split_idx], X.iloc[split_idx:]
y_train, y_test = y_true.iloc[:split_idx], y_true.iloc[split_idx:]

scaler = StandardScaler()
X_train_scaled = scaler.fit_transform(X_train)
X_test_scaled  = scaler.transform(X_test)   # transform בלבד - לא fit!

# ------------------------------------------------
# 6. אימון Isolation Forest
# ------------------------------------------------
print("Training Isolation Forest...")
model = IsolationForest(
    n_estimators=100,
    contamination=0.05,
    max_samples='auto',
    random_state=42,
    n_jobs=-1
)
model.fit(X_train_scaled)

# ------------------------------------------------
# 7. הערכה על Test Set
# ------------------------------------------------
print("\nEvaluating on test set...")
y_pred_raw = model.predict(X_test_scaled)
# Isolation Forest: -1 = anomaly, 1 = normal
y_pred = (y_pred_raw == -1).astype(int)

print("\nClassification Report:")
print(classification_report(y_test, y_pred, target_names=['REGULAR', 'IRREGULAR']))

print("Confusion Matrix:")
cm = confusion_matrix(y_test, y_pred)
print(f"  TN={cm[0][0]}  FP={cm[0][1]}")
print(f"  FN={cm[1][0]}  TP={cm[1][1]}")

# ------------------------------------------------
# 8. שמירת המודלים
# ------------------------------------------------
os.makedirs(MODELS_DIR, exist_ok=True)

joblib.dump(model,         f'{MODELS_DIR}/isolation_forest.pkl')
joblib.dump(scaler,        f'{MODELS_DIR}/scaler.pkl')
joblib.dump(list(X.columns), f'{MODELS_DIR}/feature_columns.pkl')  # שמות עמודות
joblib.dump(top_merchants, f'{MODELS_DIR}/top_merchants.pkl')

print(f"\nModels saved to {MODELS_DIR}/")
print("  isolation_forest.pkl")
print("  scaler.pkl")
print("  feature_columns.pkl")
print("  top_merchants.pkl")