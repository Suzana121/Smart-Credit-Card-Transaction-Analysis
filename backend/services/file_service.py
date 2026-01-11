import pandas as pd
import io
import os

def validate_and_process_file(file):
    try:
        filename = file.filename.lower()
        # קריאת התוכן הגולמי של הקובץ
        file_bytes = file.read()
        
        if filename.endswith('.csv'):
            # טיפול ב-CSV
            content = file_bytes.decode("UTF-8")
            lines = content.splitlines()
            
            # חיפוש שורת הכותרת
            header_line_index = 0
            for i, line in enumerate(lines):
                if "תאריך" in line or "סכום" in line:
                    header_line_index = i
                    break
            
            # קריאת ה-CSV החל מהשורה שמצאנו
            df = pd.read_csv(io.StringIO(content), skiprows=header_line_index)

        elif filename.endswith(('.xlsx', '.xls')):
            # טיפול ב-Excel
            # אנחנו קוראים את האקסל ומשתמשים ב-header=None כדי למצוא את השורה בעצמנו
            df_temp = pd.read_excel(io.BytesIO(file_bytes), header=None)
            
            # חיפוש השורה שבה מופיעה מילת מפתח
            header_line_index = 0
            for i, row in df_temp.iterrows():
                # בודקים אם המילים מופיעות בתוך אחד התאים בשורה
                if row.astype(str).str.contains('תאריך|סכום').any():
                    header_line_index = i
                    break
            
            # קריאה מחדש עם ה-Header הנכון
            df = pd.read_excel(io.BytesIO(file_bytes), skiprows=header_line_index)

        else:
            return False, "פורמט קובץ לא נתמך (יש להעלות CSV או Excel)"

        if df.empty:
            return False, "הקובץ ריק מנתונים"

        # ניקוי עמודות ריקות (Unnamed) שנוצרות לפעמים בסוף
        df = df.loc[:, ~df.columns.str.contains('^Unnamed')]
        
        # ניקוי רווחים משמות העמודות (חשוב מאוד לאקסל)
        df.columns = [col.strip() for col in df.columns]

        return True, df

    except Exception as e:
        return False, f"שגיאה בעיבוד הקובץ: {str(e)}"