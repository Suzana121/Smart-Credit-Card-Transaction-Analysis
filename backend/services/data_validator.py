"""
data_validator.py
-----------------
אחראי לוודא שהנתונים שמגיעים מהמשתמש תקינים לפני שמגיעים למודל.
בודק ערכים חסרים, פורמטים שגויים, וערכים חריגים.

שימוש:
    from services.data_validator import DataValidator
    df_clean, report = DataValidator.validate(df)
"""

import pandas as pd
import numpy as np
import re
from dataclasses import dataclass, field
from typing import List


@dataclass
class ValidationReport:
    """דוח ולידציה - מה תוקן ומה הוסר."""
    original_rows:    int = 0
    final_rows:       int = 0
    removed_rows:     int = 0
    fixed_fields:     List[str] = field(default_factory=list)
    warnings:         List[str] = field(default_factory=list)
    errors:           List[str] = field(default_factory=list)

    def is_valid(self) -> bool:
        return self.final_rows > 0 and not self.errors

    def summary(self) -> str:
        return (
            f"Original: {self.original_rows} rows | "
            f"Final: {self.final_rows} rows | "
            f"Removed: {self.removed_rows} rows | "
            f"Warnings: {len(self.warnings)}"
        )


class DataValidator:

    # גבולות סבירים לסכומים
    MIN_AMOUNT = 0.01
    MAX_AMOUNT = 1_000_000.0

    # פורמטי תאריך מקובלים
    DATE_PATTERNS = [
        r'^\d{2}-\d{2}-\d{4}$',   # DD-MM-YYYY
        r'^\d{2}/\d{2}/\d{4}$',   # DD/MM/YYYY
        r'^\d{2}/\d{2}/\d{2}$',   # DD/MM/YY
        r'^\d{4}-\d{2}-\d{2}$',   # YYYY-MM-DD
    ]

    # מטבעות מותרים
    VALID_CURRENCIES = {'ILS', 'USD', 'EUR', 'GBP'}

    @staticmethod
    def validate(df: pd.DataFrame) -> tuple:
        """
        מקבל DataFrame מ-FileService, מוודא ומנקה.
        מחזיר (df_clean, ValidationReport).
        """
        report = ValidationReport(original_rows=len(df))
        df = df.copy()

        # --- 1. בדיקת עמודות חובה ---
        required = ['businessName', 'amount', 'date']
        for col in required:
            if col not in df.columns:
                report.errors.append(f"Missing required column: {col}")

        if report.errors:
            report.final_rows = 0
            return df, report

        # --- 2. ניקוי שמות עסקים ---
        before = len(df)
        df['businessName'] = df['businessName'].astype(str).str.strip()
        # הסרת שורות עם שם ריק או קצר מדי
        df = df[df['businessName'].str.len() > 1]
        # הסרת שורות שהן סיכום
        skip_keywords = ['סה"כ', 'סהכ', 'total', 'Total', '---', 'סך הכל']
        mask = df['businessName'].apply(
            lambda x: not any(k in str(x) for k in skip_keywords)
        )
        df = df[mask]
        removed = before - len(df)
        if removed > 0:
            report.warnings.append(f"Removed {removed} summary/invalid business name rows")

        # --- 3. ולידציה וניקוי סכומים ---
        before = len(df)
        df['amount'] = pd.to_numeric(df['amount'], errors='coerce')

        null_amounts = df['amount'].isna().sum()
        if null_amounts > 0:
            report.fixed_fields.append(f"amount: {null_amounts} non-numeric values set to 0")
            df['amount'] = df['amount'].fillna(0)

        # הסרת סכומים מחוץ לטווח סביר
        df = df[(df['amount'] >= DataValidator.MIN_AMOUNT) &
                (df['amount'] <= DataValidator.MAX_AMOUNT)]
        removed = before - len(df)
        if removed > 0:
            report.warnings.append(
                f"Removed {removed} rows with amounts outside valid range "
                f"({DataValidator.MIN_AMOUNT}-{DataValidator.MAX_AMOUNT})"
            )

        # --- 4. ולידציה תאריכים ---
        def is_valid_date(date_str):
            if not date_str or pd.isna(date_str):
                return False
            s = str(date_str).strip()
            return any(re.match(p, s) for p in DataValidator.DATE_PATTERNS)

        invalid_dates = (~df['date'].apply(is_valid_date)).sum()
        if invalid_dates > 0:
            report.warnings.append(
                f"{invalid_dates} rows have unrecognized date format - "
                f"will use empty string"
            )
            df.loc[~df['date'].apply(is_valid_date), 'date'] = ''

        # --- 5. ולידציה מטבע ---
        if 'currency' in df.columns:
            invalid_currencies = ~df['currency'].isin(DataValidator.VALID_CURRENCIES)
            n_invalid = invalid_currencies.sum()
            if n_invalid > 0:
                report.fixed_fields.append(
                    f"currency: {n_invalid} unknown currencies replaced with ILS"
                )
                df.loc[invalid_currencies, 'currency'] = 'ILS'

        # --- 6. ערכים חסרים - מילוי ברירות מחדל ---
        defaults = {
            'category':          'General',
            'txn_type':          '',
            'currency':          'ILS',
            'original_amount':   df['amount'],
            'original_currency': 'ILS',
        }
        for col, default in defaults.items():
            if col in df.columns:
                null_count = df[col].isna().sum()
                if null_count > 0:
                    df[col] = df[col].fillna(default)
                    report.fixed_fields.append(
                        f"{col}: {null_count} missing values filled with default"
                    )

        # --- 7. הסרת כפילויות ---
        before = len(df)
        df = df.drop_duplicates(subset=['businessName', 'amount', 'date'])
        removed = before - len(df)
        if removed > 0:
            report.warnings.append(f"Removed {removed} duplicate transactions")

        # --- 8. בדיקת מינימום שורות ---
        if len(df) < 3:
            report.errors.append(
                f"Too few valid transactions after cleaning: {len(df)}. "
                f"Minimum required: 3"
            )

        df = df.reset_index(drop=True)
        report.final_rows  = len(df)
        report.removed_rows = report.original_rows - report.final_rows

        return df, report