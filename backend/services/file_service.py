import pandas as pd
import io

def validate_and_process_csv(file):
    try:
        # קריאת כל הקובץ לתוך רשימת שורות כדי למצוא איפה הטבלה מתחילה
        content = file.stream.read().decode("UTF-8")
        lines = content.splitlines()
        
        # חיפוש השורה שבה מופיעה מילת מפתח כמו "תאריך" או "סכום"
        header_line_index = 0
        for i, line in enumerate(lines):
            if "תאריך" in line or "סכום" in line:
                header_line_index = i
                break
        
        # קריאה מחדש של ה-CSV החל מהשורה שמצאנו
        file.stream.seek(0) # מחזיר את הסמן לתחילת הקובץ
        df = pd.read_csv(io.StringIO(content), skiprows=header_line_index)

        if df.empty:
            return False, "הקובץ ריק מנתונים"

        # ניקוי עמודות ריקות (Unnamed) שנוצרות לפעמים בסוף
        df = df.loc[:, ~df.columns.str.contains('^Unnamed')]

        return True, df

    except Exception as e:
        return False, f"שגיאה בעיבוד הקובץ: {str(e)}"