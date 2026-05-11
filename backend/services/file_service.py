import pandas as pd
from io import BytesIO
import re

ALLOWED_EXTENSIONS = {'csv', 'xlsx', 'xls'}

CURRENCY_SYMBOL_MAP = {
    '₪': 'ILS', 'ils': 'ILS', 'nis': 'ILS', 'il': 'ILS',
    'שח': 'ILS', 'ש"ח': 'ILS', 'shekel': 'ILS',
    '$': 'USD', 'usd': 'USD', 'dollar': 'USD',
    '€': 'EUR', 'eur': 'EUR', 'euro': 'EUR',
    '£': 'GBP', 'gbp': 'GBP', 'pound': 'GBP',
}

COLUMN_ALIASES = {
    'businessName':      ['שם בית העסק', 'שם בית עסק', 'בית עסק', 'תיאור', 'שם מוטב', 'פרטים', 'שם העסק'],
    'amount':            ['סכום חיוב', 'סכום', 'חיוב', 'סכום בש"ח'],
    'original_amount':   ['סכום עסקה', 'סכום המקור', 'סכום מקורי'],
    'date':              ['תאריך עסקה', 'תאריך רכישה', 'תאריך'],
    'category':          ['קטגוריה', 'ענף', 'Category'],
    'txn_type':          ['סוג עסקה', 'סוג'],
    'currency':          ['מטבע חיוב', 'מטבע'],
}

def _normalize_currency(raw) -> str:
    if pd.isna(raw): return 'ILS'
    key = str(raw).strip().replace('₪', '').strip()
    return CURRENCY_SYMBOL_MAP.get(key.lower(), CURRENCY_SYMBOL_MAP.get(key, 'ILS'))

def _extract_amount_and_currency(raw):
    if pd.isna(raw): return 0.0, 'ILS'
    raw_str = str(raw).strip()
    detected_currency = 'ILS'
    for symbol, code in CURRENCY_SYMBOL_MAP.items():
        if symbol in raw_str:
            detected_currency = code
            raw_str = raw_str.replace(symbol, '')
            break
    clean = raw_str.replace(',', '').strip()
    try:
        return float(clean), detected_currency
    except:
        return 0.0, detected_currency

def _parse_date(raw) -> str:
    if pd.isna(raw): return ''
    raw_str = str(raw).strip()
    try:
        if len(raw_str) >= 10 and raw_str[4] in ('-', '/'):
            d = pd.to_datetime(raw_str, dayfirst=False)
        else:
            d = pd.to_datetime(raw_str, dayfirst=True)
        return d.strftime('%d-%m-%Y')
    except:
        return raw_str

def _find_col(df, aliases):
    for alias in aliases:
        if alias in df.columns:
            return alias
    return None

class FileService:
    @staticmethod
    def allowed_file(filename):
        return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

    @staticmethod
    def validate_and_process_file(file_storage):
        filename = file_storage.filename
        file_bytes = file_storage.read()

        # זיהוי אם אקסל לפי Header
        is_excel = file_bytes[:4] in (b'PK\x03\x04', b'\xd0\xcf\x11\xe0')

        # קריאת הקובץ דרך המנגנון החסין
        df = FileService._safe_read_file(file_bytes, is_excel)
        if df is None:
            return pd.DataFrame() # מחזיר DataFrame ריק במקום None או List
        col = {field: _find_col(df, aliases) for field, aliases in COLUMN_ALIASES.items()}

        if not col['businessName']:
            raise ValueError("לא נמצאה עמודת שם בית עסק או תיאור בקובץ")

        result_rows = []
        for _, row in df.iterrows():
            biz = row.get(col['businessName'], '')
            if pd.isna(biz) or not str(biz).strip(): continue
            if any(s in str(biz) for s in ['סה"כ', 'סהכ', 'total', 'Total', '---']): continue

            amount_raw = row.get(col['amount'])
            currency_raw = row.get(col['currency'])

            if currency_raw and not pd.isna(currency_raw):
                currency = _normalize_currency(currency_raw)
                try:
                    amount = float(str(amount_raw).replace(',', '').replace('₪','').replace('$','').strip())
                except: amount = 0.0
            else:
                amount, currency = _extract_amount_and_currency(amount_raw)

            date_raw = row.get(col['date'])
            date_str = _parse_date(date_raw)

            result_rows.append({
                'businessName': str(biz).strip(),
                'amount': amount,
                'date': date_str,
                'category': str(row.get(col['category'], 'General')).strip() if col['category'] else 'General',
                'txn_type': str(row.get(col['txn_type'], '')).strip() if col['txn_type'] else '',
                'currency': currency,
                'original_amount': amount, # פישוט לצורך התיקון
                'original_currency': currency
            })

        if not result_rows:
            raise ValueError("לא נמצאו עסקאות תקינות")

        final_df = pd.DataFrame(result_rows)
        final_df = final_df[final_df["amount"] > 0]
        return final_df.reset_index(drop=True)

    @staticmethod
    def _safe_read_file(file_bytes, is_excel):
        file_buffer = BytesIO(file_bytes)
        if is_excel:
            for engine in [None, 'openpyxl', 'calamine']:
                try:
                    file_buffer.seek(0)
                    df = pd.read_excel(file_buffer, header=None, engine=engine)
                    if isinstance(df, dict): df = next(iter(df.values()))
                    res = FileService._extract_logic(df)
                    if res is not None: return res
                except: continue

        for enc in ['cp1255', 'utf-8', 'utf-8-sig']:
            try:
                file_buffer.seek(0)
                df = pd.read_csv(file_buffer, encoding=enc, header=None, on_bad_lines='skip')
                res = FileService._extract_logic(df)
                if res is not None: return res
            except: continue

        raise ValueError("פורמט קובץ לא נתמך או כותרות חסרות")

    @staticmethod
    def _extract_logic(df):
        if df is None or df.empty: return None
        data = df.values.tolist()
        keywords = {'שם בית עסק', 'בית עסק', 'תאריך עסקה', 'סכום חיוב', 'ענף', 'שם מוטב'}

        for i, row in enumerate(data):
            is_header_row = False
            for cell in row:
                if pd.notna(cell):
                    cell_str = str(cell).replace('\n', ' ').replace('\r', ' ').lower().strip()
                    cell_str = ' '.join(cell_str.split())
                    if any(key in cell_str for key in keywords):
                        is_header_row = True
                        break

            if is_header_row:
                new_df = df.iloc[i + 1:].copy()
                headers = []
                for idx, h in enumerate(data[i]):
                    if pd.notna(h):
                        clean_h = str(h).replace('\n', ' ').strip()
                        headers.append(' '.join(clean_h.split()))
                    else:
                        headers.append(f"col_{idx}")

                new_df.columns = headers
                valid_cols = [c for c in new_df.columns if not re.match(r'^Unnamed|^nan|^col_', str(c), re.IGNORECASE)]
                new_df = new_df[valid_cols]

                # כאן החלק החשוב: מחזירים DataFrame נקי
                final_df = new_df.dropna(how='all').reset_index(drop=True)
                return final_df
        return None