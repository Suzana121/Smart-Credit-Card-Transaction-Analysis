import pandas as pd
import os
from io import BytesIO

# הגדרות
ALLOWED_EXTENSIONS = {'csv', 'xlsx', 'xls'}
MAX_FILE_SIZE = 1 * 1024 * 1024  # 1MB בדיוק

class FileService:
    @staticmethod
    def allowed_file(filename):
        return '.' in filename and \
            filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

    @staticmethod
    def validate_and_process_file(file_storage):
        filename = file_storage.filename
        file_bytes = file_storage.read()

        # זיהוי לפי חתימת קובץ (Magic Bytes) - הכי בטוח
        is_excel = file_bytes.startswith(b'PK\x03\x04')

        try:
            if is_excel:
                # בקבצי MAX, הנתונים מתחילים בשורה 4 (אינדקס 3 ב-python)
                # אנחנו קוראים את הקובץ ומחפשים איפה נמצאת הכותרת "תאריך עסקה"
                df = pd.read_excel(BytesIO(file_bytes))

                # אם נמצאה הכותרת בשורה 4, נחתוך את מה שמעליה
                if 'תאריך עסקה' not in df.columns:
                    df = pd.read_excel(BytesIO(file_bytes), skiprows=3)
            else:
                # טיפול ב-CSV רגיל
                try:
                    df = pd.read_csv(BytesIO(file_bytes), encoding='utf-8')
                except:
                    df = pd.read_csv(BytesIO(file_bytes), encoding='cp1255')

            # ניקוי רווחים בשמות העמודות
            df.columns = df.columns.str.strip()
            return df

        except Exception as e:
            raise ValueError(f"שגיאה בעיבוד הקובץ: {str(e)}")