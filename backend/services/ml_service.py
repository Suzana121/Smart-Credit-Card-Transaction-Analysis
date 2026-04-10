import joblib
import json
import pandas as pd
import numpy as np
import os
from services.data_cleaner import clean_dataframe, format_report
from services.amount_parser import extract_amount_and_currency

_BASE = os.path.dirname(os.path.abspath(__file__))
_MODELS = os.path.join(_BASE, "..", "models")

def _get_model_path(name):
    path = os.path.join(_MODELS, name)
    if not os.path.exists(path):
        raise FileNotFoundError(f"Model file {name} missing")
    return path

# טעינה
model = joblib.load(_get_model_path("model.pkl"))
scaler = joblib.load(_get_model_path("scaler.pkl"))
with open(_get_model_path("feature_columns.json"), "r", encoding="utf-8") as f:
    FEATURE_COLS = json.load(f)

def predict_transactions(df: pd.DataFrame) -> pd.DataFrame:
    df, report = clean_dataframe(df)
    df = extract_amount_and_currency(df)

    # חישוב המרה וחישוב חציון (הבקשה של המרצה)
    rates = {"ILS": 1.0, "USD": 3.7, "EUR": 4.0, "GBP": 4.7}
    df["amount_billing"] = df.apply(lambda r: round(r["amount"] * rates.get(r["currency"], 1.0), 2), axis=1)
    user_median = df["amount_billing"].median()

    # פיצ'רים
    df["merchant_frequency"] = df.groupby("merchant")["merchant"].transform("count")
    df["is_new_merchant"] = (df["merchant_frequency"] == 1).astype(int)
    df["date"] = pd.to_datetime(df["date"])
    df["day_of_week"] = df["date"].dt.dayofweek

    # One-Hot Encoding
    cat_dummies = pd.get_dummies(df, columns=['category', 'transaction_type', 'currency'], prefix=['cat', 'type', 'curr'])
    final_df = pd.concat([df, cat_dummies.reindex(columns=cat_dummies.columns.difference(df.columns))], axis=1)
    
    for col in FEATURE_COLS:
        if col not in final_df.columns: final_df[col] = 0

    # חיזוי
    X = final_df[FEATURE_COLS].fillna(0)
    X_scaled = scaler.transform(X)
    final_df["is_anomaly"] = (model.predict(X_scaled) == -1)

    # הסברים וסיכון (English + Median)
    explanations, risk_levels = [], []
    for _, row in final_df.iterrows():
        if row["is_anomaly"]:
            if row["amount_billing"] > user_median * 3:
                explanations.append(f"Significantly higher than your median ({user_median} ILS)")
                risk_levels.append("high")
            elif row["is_new_merchant"]:
                explanations.append("First time at this merchant")
                risk_levels.append("medium")
            else:
                explanations.append("Unusual transaction pattern")
                risk_levels.append("medium")
        else:
            explanations.append("Normal transaction")
            risk_levels.append("low")
    
    final_df["explanation"], final_df["risk_level"] = explanations, risk_levels
    return final_df