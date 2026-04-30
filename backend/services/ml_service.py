"""
ml_service.py
-------------
טוען את המודל המאומן ומבצע inference + יצירת הסברי XAI
על כל עסקה שמועברת מ-FileService.

נטען פעם אחת ב-startup של Flask ומשומש לכל הבקשות.
"""

import numpy as np
import pandas as pd
import joblib
import os
import logging
from datetime import datetime

logger = logging.getLogger(__name__)

MODELS_DIR = 'models'

# שערי המרה קבועים — מספיקים לזיהוי חריגות
FALLBACK_RATES = {
    'ILS': 1.0,
    'USD': 3.7,
    'EUR': 4.0,
    'GBP': 4.6,
}

class MLService:
    _instance = None

    def __init__(self):
        self.model          = joblib.load(f'{MODELS_DIR}/isolation_forest.pkl')
        self.scaler         = joblib.load(f'{MODELS_DIR}/scaler.pkl')
        self.feature_cols   = joblib.load(f'{MODELS_DIR}/feature_columns.pkl')
        self.top_merchants  = joblib.load(f'{MODELS_DIR}/top_merchants.pkl')
        logger.info("MLService loaded successfully")

    @classmethod
    def get_instance(cls):
        """Singleton - נטען פעם אחת."""
        if cls._instance is None:
            cls._instance = cls()
        return cls._instance

    def _to_ils(self, amount, currency):
        """ממיר סכום למטבע ILS באמצעות שערים קבועים."""
        rate = FALLBACK_RATES.get(currency, 1.0)
        return round(amount * rate, 2)

    def _parse_date_features(self, date_str):
        try:
            d = pd.to_datetime(date_str, dayfirst=True)
            day_of_week = (d.dayofweek + 1) % 7
            return day_of_week, d.month
        except Exception:
            return 3, 6

    def _build_feature_row(self, row, user_profile):
        """בונה וקטור features לעסקה אחת כולל User Profile Features."""
        amount    = float(row.get('amount', 0))
        currency  = row.get('currency', 'ILS')
        merchant  = row.get('businessName', 'Other')
        category  = row.get('category', 'Other')
        date_str  = row.get('date', '')

        amount_ils = self._to_ils(amount, currency)

        day_of_week, month = self._parse_date_features(date_str)
        is_weekend          = int(day_of_week in (5, 6))
        currency_is_foreign = int(currency != 'ILS')
        amount_log          = np.log1p(amount_ils)

        median_spend    = user_profile.get('median_spend', 200)
        known_merchants = set(user_profile.get('known_merchants', []))
        category_counts = user_profile.get('category_counts', {})
        total_txns      = max(user_profile.get('total_transactions', 1), 1)

        amount_vs_median   = min((amount_ils / median_spend) if median_spend > 0 else 1.0, 50.0)
        is_new_merchant    = int(merchant not in known_merchants) if known_merchants else 0
        cat_count          = category_counts.get(category, 0)
        category_frequency = cat_count / total_txns

        feature_dict = {col: 0 for col in self.feature_cols}
        feature_dict['amount_log']           = amount_log
        feature_dict['day_of_week']          = day_of_week
        feature_dict['month']                = month
        feature_dict['is_weekend']           = is_weekend
        feature_dict['currency_is_foreign']  = currency_is_foreign
        feature_dict['amount_vs_median']     = amount_vs_median
        feature_dict['is_new_merchant']      = is_new_merchant
        feature_dict['category_frequency']   = category_frequency

        merchant_enc = merchant if merchant in self.top_merchants else 'Other'
        col_m = f'm_{merchant_enc}'
        if col_m in feature_dict:
            feature_dict[col_m] = 1

        col_c = f'cat_{category}'
        if col_c in feature_dict:
            feature_dict[col_c] = 1

        col_cur = f'cur_{currency}'
        if col_cur in feature_dict:
            feature_dict[col_cur] = 1

        return feature_dict, amount_ils

    def _generate_explanation(self, row, amount_ils, user_profile):
        """מחזיר הסבר טקסטואלי לעסקה חשודה."""
        reasons = []

        merchant        = row.get('businessName', '')
        currency        = row.get('currency', 'ILS')
        date_str        = row.get('date', '')
        category        = row.get('category', 'Other')
        median_spend    = user_profile.get('median_spend', 200)
        known_merchants = set(user_profile.get('known_merchants', []))
        category_counts = user_profile.get('category_counts', {})
        total_txns      = max(user_profile.get('total_transactions', 1), 1)

        if median_spend > 0 and amount_ils > median_spend * 5:
            ratio = round(amount_ils / median_spend, 1)
            reasons.append(('high_amount', f"Amount is {ratio}x higher than your typical median spend"))

        if merchant and merchant not in known_merchants:
            reasons.append(('new_merchant', "First time transaction at this merchant"))

        if currency != 'ILS':
            reasons.append(('foreign_currency', f"Transaction in foreign currency ({currency})"))

        try:
            d   = pd.to_datetime(date_str, dayfirst=True)
            dow = (d.dayofweek + 1) % 7
            if dow in (5, 6):
                day_name = 'Friday' if dow == 5 else 'Saturday'
                reasons.append(('weekend', f"Transaction on {day_name} - outside your usual pattern"))
        except Exception:
            pass

        if category_counts:
            cat_freq = category_counts.get(category, 0) / total_txns
            if cat_freq < 0.01:
                reasons.append(('rare_category', f"Rare spending category for your profile ({category})"))

        priority = ['high_amount', 'foreign_currency', 'new_merchant', 'weekend', 'rare_category']
        for p in priority:
            for reason_type, text in reasons:
                if reason_type == p:
                    return text

        return "Transaction pattern deviates from your usual behavior"

    def classify_transactions(self, df_transactions, user_profile):
        """
        מקבל DataFrame מ-FileService ופרופיל משתמש,
        מחזיר רשימת dict עם status ו-explanation לכל עסקה.
        """
        results      = []
        feature_rows = []
        amounts_ils  = []

        for _, row in df_transactions.iterrows():
            feat_dict, amount_ils = self._build_feature_row(row.to_dict(), user_profile)
            feature_rows.append(feat_dict)
            amounts_ils.append(amount_ils)

        if not feature_rows:
            return results

        X        = pd.DataFrame(feature_rows)[self.feature_cols].fillna(0)
        X_scaled = self.scaler.transform(X)

        predictions = self.model.predict(X_scaled)
        scores      = self.model.score_samples(X_scaled)

        for i, (_, row) in enumerate(df_transactions.iterrows()):
            is_irregular = (predictions[i] == -1)
            explanation  = ''
            if is_irregular:
                explanation = self._generate_explanation(
                    row.to_dict(), amounts_ils[i], user_profile
                )
            results.append({
                'businessName':      row.get('businessName', ''),
                'amount':            row.get('amount', 0),
                'date':              row.get('date', ''),
                'category':          row.get('category', 'Other'),
                'txn_type':          row.get('txn_type', ''),
                'currency':          row.get('currency', 'ILS'),
                'original_amount':   row.get('original_amount', row.get('amount', 0)),
                'original_currency': row.get('original_currency', row.get('currency', 'ILS')),
                'status':            'IRREGULAR' if is_irregular else 'REGULAR',
                'anomaly_score':     round(float(scores[i]), 4),
                'explanation':       explanation,
            })

        return results

    def update_user_profile(self, existing_profile, df_transactions):
        """
        מעדכן את פרופיל המשתמש לאחר העלאה.
        """
        amounts_ils = [
            self._to_ils(row['amount'], row.get('currency', 'ILS'))
            for _, row in df_transactions.iterrows()
        ]
        all_amounts = existing_profile.get('all_amounts', []) + amounts_ils
        merchants   = set(existing_profile.get('known_merchants', []))
        merchants.update(df_transactions['businessName'].tolist())

        cat_counts = existing_profile.get('category_counts', {})
        for cat in df_transactions['category']:
            cat_counts[cat] = cat_counts.get(cat, 0) + 1

        all_currencies   = existing_profile.get('currency_history', []) + \
                           df_transactions['currency'].tolist()
        from collections import Counter
        primary_currency = Counter(all_currencies).most_common(1)[0][0] if all_currencies else 'ILS'

        return {
            'median_spend':       float(np.median(all_amounts)) if all_amounts else 0.0,
            'mean_spend':         float(np.mean(all_amounts))   if all_amounts else 0.0,
            'known_merchants':    list(merchants),
            'category_counts':    cat_counts,
            'total_transactions': existing_profile.get('total_transactions', 0) + len(df_transactions),
            'primary_currency':   primary_currency,
            'currency_history':   all_currencies[-1000:],
            'all_amounts':        all_amounts[-1000:],
        }