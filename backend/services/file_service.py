import pandas as pd
from io import BytesIO
import re

ALLOWED_EXTENSIONS = {'csv', 'xlsx', 'xls'}

# מיפוי סמלי/שמות מטבע לקודים סטנדרטיים
CURRENCY_SYMBOL_MAP = {
    '₪': 'ILS', 'ils': 'ILS', 'nis': 'ILS', 'il': 'ILS',
    'שח': 'ILS', 'ש"ח': 'ILS', 'shekel': 'ILS',
    '$': 'USD', 'usd': 'USD', 'dollar': 'USD',
    '€': 'EUR', 'eur': 'EUR', 'euro': 'EUR',
    '£': 'GBP', 'gbp': 'GBP', 'pound': 'GBP',
}

# כינויים לכל שדה - כולל כל הפורמטים
COLUMN_ALIASES = {
    'businessName':      ['שם בית העסק', 'שם בית עסק', 'שם עסק', 'BusinessName', 'merchant', 'Merchant', 'בית עסק'],
    'amount':            ['סכום חיוב', 'סכום', 'Amount', 'charge', 'החיוב', 'חיוב', 'סכום חיוב'],
    'original_amount':   ['סכום עסקה', 'סכום עסקה מקורי', 'original_amount', 'Original Amount', 'סכום עסקה'],
    'date':              ['תאריך עסקה', 'תאריך', 'Date', 'transaction_date', 'תאריך רכישה'],
    'category':          ['קטגוריה', 'Category', 'ענף'],   # <-- פורמט 2 משתמש ב"ענף"
    'txn_type':          ['סוג עסקה', 'סוג', 'Type', 'transaction_type'],
    'currency':          ['מטבע חיוב', 'מטבע', 'Currency', 'מטבע חיוב העסקה', 'מטבע לחיוב'],
    'original_currency': ['מטבע עסקה מקורי', 'מטבע העסקה', 'original_currency', 'מטבע מקור'],
}

SKIP_ROWS_RANGE = range(0, 7)  # ננסה עד 6 שורות דילוג


def _normalize_currency(raw) -> str:
    """ממיר סמל/שם מטבע לקוד סטנדרטי."""
    if pd.isna(raw):
        return 'ILS'
    key = str(raw).strip()
    return CURRENCY_SYMBOL_MAP.get(key.lower(),
           CURRENCY_SYMBOL_MAP.get(key, 'ILS'))


def _extract_amount_and_currency(raw):
    """
    מחלץ סכום ומטבע מערך גולמי.
    מטפל ב: '₪150', '$99.5', '150.00 ILS', '1,234.56', 199.0
    מחזיר (float, currency_code)
    """
    if pd.isna(raw):
        return 0.0, 'ILS'

    raw_str = str(raw).strip()
    detected_currency = 'ILS'

    # חיפוש סמל מטבע
    for symbol, code in CURRENCY_SYMBOL_MAP.items():
        if symbol in raw_str:
            detected_currency = code
            raw_str = raw_str.replace(symbol, '')
            break

    # הסרת פסיקים ורווחים
    clean = raw_str.replace(',', '').strip()

    try:
        return float(clean), detected_currency
    except (ValueError, TypeError):
        return 0.0, detected_currency


def _parse_date(raw) -> str:
    """
    מנרמל תאריך לפורמט DD-MM-YYYY.
    מטפל ב: DD/MM/YYYY, DD/MM/YY, DD-MM-YYYY, YYYY-MM-DD
    """
    if pd.isna(raw):
        return ''
    raw_str = str(raw).strip()
    try:
        # אם זה פורמט ISO: YYYY-MM-DD - dayfirst=False
        if len(raw_str) >= 10 and raw_str[4] in ('-', '/'):
            d = pd.to_datetime(raw_str, dayfirst=False)
        else:
            d = pd.to_datetime(raw_str, dayfirst=True)
        return d.strftime('%d-%m-%Y')
    except Exception:
        return raw_str


def _find_col(df, aliases):
    for alias in aliases:
        if alias in df.columns:
            return alias
    return None


def _has_known_columns(df):
    """בודק שיש לפחות עמודת שם עסק ועמודת סכום."""
    has_name = _find_col(df, COLUMN_ALIASES['businessName']) is not None
    has_amount = (_find_col(df, COLUMN_ALIASES['amount']) is not None or
                  _find_col(df, COLUMN_ALIASES['original_amount']) is not None)
    return has_name and has_amount


class FileService:

    @staticmethod
    def allowed_file(filename):
        return '.' in filename and \
               filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

    @staticmethod
    def validate_and_process_file(file_storage):
        filename   = file_storage.filename
        file_bytes = file_storage.read()
        is_excel   = file_bytes[:4] in (b'PK\x03\x04', b'\xd0\xcf\x11\xe0')

        df = FileService._read_file(file_bytes, is_excel)
        col = {field: _find_col(df, aliases)
               for field, aliases in COLUMN_ALIASES.items()}

        if not col['businessName']:
            raise ValueError("לא נמצאה עמודת שם בית עסק בקובץ")

        result_rows = []

        for _, row in df.iterrows():
            biz = row.get(col['businessName'], '')
            if pd.isna(biz) or not str(biz).strip():
                continue
            if any(s in str(biz) for s in ['סה"כ', 'סהכ', 'total', 'Total', '---']):
                continue

            # --- סכום ומטבע חיוב ---
            amount_raw   = row.get(col['amount']) if col['amount'] else None
            currency_raw = row.get(col['currency']) if col['currency'] else None

            if currency_raw is not None and not pd.isna(currency_raw):
                # עמודת מטבע נפרדת (פורמטים 1 ו-3)
                currency = _normalize_currency(currency_raw)
                try:
                    amount = float(str(amount_raw).replace(',', '')
                                   .replace('₪','').replace('$','')
                                   .replace('€','').replace('£','').strip())
                except (ValueError, TypeError):
                    amount = 0.0
            else:
                # מטבע מוטמע בסכום (פורמט 2: '₪199')
                amount, currency = _extract_amount_and_currency(amount_raw)

            # --- סכום עסקה מקורי ---
            orig_raw     = row.get(col['original_amount']) if col['original_amount'] else None
            orig_cur_raw = row.get(col['original_currency']) if col['original_currency'] else None

            if orig_raw is not None and not pd.isna(orig_raw):
                orig_amount, orig_currency_from_val = _extract_amount_and_currency(orig_raw)
                orig_currency = (_normalize_currency(orig_cur_raw)
                                 if orig_cur_raw and not pd.isna(orig_cur_raw)
                                 else orig_currency_from_val)
            else:
                orig_amount   = amount
                orig_currency = currency

            # --- תאריך ---
            date_raw = row.get(col['date']) if col['date'] else None
            date_str = _parse_date(date_raw)

            # --- קטגוריה (כולל "ענף" של פורמט 2) ---
            category = 'General'
            if col['category']:
                cat_raw = row.get(col['category'])
                if cat_raw and not pd.isna(cat_raw):
                    category = str(cat_raw).strip()

            # --- סוג עסקה ---
            txn_type = ''
            if col['txn_type']:
                t_raw = row.get(col['txn_type'])
                if t_raw and not pd.isna(t_raw):
                    txn_type = str(t_raw).strip()

            result_rows.append({
                'businessName':      str(biz).strip(),
                'amount':            amount,
                'date':              date_str,
                'category':          category,
                'txn_type':          txn_type,
                'currency':          currency,
                'original_amount':   orig_amount,
                'original_currency': orig_currency,
            })

        if not result_rows:
            raise ValueError("לא נמצאו עסקאות תקינות בקובץ")

        df = pd.DataFrame(result_rows)

        # --- ניקוי נתונים ---
        # 1. הסרת שורות כפולות (אותו עסק + סכום + תאריך)
        df = df.drop_duplicates(subset=["businessName", "amount", "date"])

        # 2. ערכים חסרים - מילוי ברירות מחדל
        df["businessName"]      = df["businessName"].fillna("Unknown")
        df["category"]          = df["category"].fillna("General")
        df["date"]              = df["date"].fillna("")
        df["txn_type"]          = df["txn_type"].fillna("")
        df["currency"]          = df["currency"].fillna("ILS")
        df["original_currency"] = df["original_currency"].fillna("ILS")

        # 3. סכומים לא תקינים - הסרה
        df["amount"] = pd.to_numeric(df["amount"], errors="coerce").fillna(0)
        df = df[df["amount"] > 0]

        # 4. שמות עסקים קצרים מדי (בד"כ שגיאות קריאה)
        df = df[df["businessName"].str.len() > 1]

        return df.reset_index(drop=True)

    @staticmethod
    def _read_file(file_bytes, is_excel):
        """מנסה לקרוא קובץ עם skiprows שונים עד שמוצא עמודות מוכרות."""
        if is_excel:
            for skip in SKIP_ROWS_RANGE:
                try:
                    df = pd.read_excel(BytesIO(file_bytes), skiprows=skip)
                    df.columns = df.columns.astype(str).str.strip()
                    if _has_known_columns(df):
                        return df
                except Exception:
                    continue
            # ניסיון ללא skiprows
            df = pd.read_excel(BytesIO(file_bytes))
            df.columns = df.columns.astype(str).str.strip()
            return df
        else:
            for enc in ['utf-8', 'utf-8-sig', 'cp1255', 'iso-8859-8']:
                for skip in SKIP_ROWS_RANGE:
                    try:
                        df = pd.read_csv(BytesIO(file_bytes),
                                         encoding=enc, skiprows=skip)
                        df.columns = df.columns.astype(str).str.strip()
                        if _has_known_columns(df):
                            return df
                    except Exception:
                        continue
            raise ValueError("לא ניתן לקרוא את הקובץ - פורמט לא מוכר")