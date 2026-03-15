from flask import Blueprint, request, jsonify
from flask_jwt_extended import jwt_required, get_jwt_identity
from firebase_config import db
from services.file_service import FileService
import pandas as pd

upload_bp = Blueprint('upload', __name__)

@upload_bp.route('/transactions', methods=['GET'])
@jwt_required()
def get_transactions():
    try:
        # חילוץ ה-ID הייחודי מהטוקן (ה-Identity שקבענו ב-auth.py)
        user_id = get_jwt_identity()

        # שליפת עסקאות ששייכות למשתמש (שימוש ב-user_id במקום userId לאחידות)
        docs = db.collection('transactions').where('user_id', '==', user_id).stream()

        transactions = []
        for doc in docs:
            t = doc.to_dict()
            t['id'] = doc.id
            transactions.append(t)

        return jsonify(transactions), 200
    except Exception as e:
        print(f"Error fetching transactions: {e}")
        return jsonify({"error": str(e)}), 500

@upload_bp.route('/upload', methods=['POST'])
@jwt_required()
def upload_file():
    if 'file' not in request.files:
        return jsonify({"error": "No file part"}), 400

    file = request.files['file']

    try:
        # שימוש ב-FileService שכבר כולל את הוספת עמודת ה-category
        df = FileService.validate_and_process_file(file)

        user_id = get_jwt_identity()
        batch = db.batch()
        count = 0

        for index, row in df.iterrows():
            try:
                # זיהוי שם בית העסק
                business_name = row.get('שם בית העסק') or row.get('BusinessName') or row.get('שם עסק')

                # --- 1. התעלמות משורות ריקות או שורות סיכום ---
                if pd.isna(business_name) or not str(business_name).strip():
                    continue

                business_name_str = str(business_name).strip()
                if "סה\"כ" in business_name_str or "סך הכל" in business_name_str:
                    continue

                # שליפת נתונים בסיסיים
                amount_val = row.get('סכום חיוב') or row.get('Amount') or row.get('סכום')
                date_val = row.get('תאריך עסקה') or row.get('Date') or row.get('תאריך')
                category_val = row.get('category', 'כללי')

                # ניקוי סכום (טיפול בסימני ₪ ופסיקים)
                clean_amount = 0.0
                if amount_val is not None and not pd.isna(amount_val):
                    try:
                        if isinstance(amount_val, str):
                            amount_str = amount_val.replace('₪', '').replace(',', '').strip()
                            clean_amount = float(amount_str) if amount_str else 0.0
                        else:
                            clean_amount = float(amount_val)
                    except:
                        clean_amount = 0.0

                # יצירת רשומה חדשה מקושרת ל-user_id
                doc_ref = db.collection('transactions').document()
                record = {
                    "user_id": user_id, # מזהה המשתמשת
                    "businessName": business_name_str,
                    "amount": clean_amount,
                    "date": str(date_val).strip() if not pd.isna(date_val) else "",
                    "category": str(category_val).strip(),
                    "status": "REGULAR",
                    "uploaded_at": pd.Timestamp.now().isoformat() # הוספת זמן העלאה
                }

                batch.set(doc_ref, record)
                count += 1

                # מניעת חריגה ממגבלות Batch של Firestore (עד 500 פעולות)
                if count % 500 == 0:
                    batch.commit()
                    batch = db.batch()

            except Exception as row_error:
                print(f"Error processing row {index}: {row_error}")
                continue

        if count > 0:
            batch.commit()
            return jsonify({"message": f"עיבוד הקובץ הושלם! נשמרו {count} עסקאות."}), 200
        else:
            return jsonify({"error": "לא נמצאו נתונים תקינים בקובץ."}), 400

    except Exception as e:
        print(f"FATAL Server Error: {e}")
        return jsonify({"error": f"Server error: {str(e)}"}), 500