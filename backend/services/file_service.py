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

    @staticmethod # עכשיו זה בתוך המחלקה (עם Tab/הזחה)
    def validate_and_process_file(file_storage):
        filename = file_storage.filename
        file_bytes = file_storage.read()

        # בדיקה אם זה קובץ אקסל לפי ה-Magic Numbers של הקובץ
        is_excel = file_bytes.startswith(b'PK\x03\x04')

        try:
            if is_excel:
                df = pd.read_excel(BytesIO(file_bytes))
                # אם העמודה לא נמצאה, כנראה שיש שורות כותרת ריקות (נפוץ בבנקים)
                if 'תאריך עסקה' not in df.columns:
                    df = pd.read_excel(BytesIO(file_bytes), skiprows=3)
            else:
                try:
                    df = pd.read_csv(BytesIO(file_bytes), encoding='utf-8')
                except:
                    df = pd.read_csv(BytesIO(file_bytes), encoding='cp1255')

            # ניקוי רווחים בשמות העמודות
            df.columns = df.columns.str.strip()

            # --- הוספת הטיפול בקטגוריות ---
            possible_names = ['קטגוריה', 'Category']
            category_col = next((col for col in possible_names if col in df.columns), None)

            if category_col:
                df['category'] = df[category_col].fillna('כללי')
            else:
                df['category'] = 'כללי'

            return df

        except Exception as e:
            raise ValueError(f"שגיאה בעיבוד הקובץ: {str(e)}")