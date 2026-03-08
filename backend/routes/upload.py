from flask import Blueprint, request, jsonify
from flask_jwt_extended import jwt_required, get_jwt_identity
from firebase_config import db
from services.file_service import FileService
import pandas as pd

upload_bp = Blueprint('upload', __name__)

@upload_bp.route('/transactions', methods=['GET'])
@jwt_required() # <--- זה מה שיחסום גישה למי שלא מחובר
def get_transactions():
    try:
        user_id = get_jwt_identity() # מחלץ את ה-ID מהטוקן

        # שליפת עסקאות ששייכות אך ורק למשתמש הזה
        docs = db.collection('transactions').where('userId', '==', user_id).stream()

        transactions = []
        for doc in docs:
            t = doc.to_dict()
            t['id'] = doc.id # מוסיפים את ה-ID של המסמך
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
        df = FileService.validate_and_process_file(file)

        # הדפסה לטרמינל כדי לראות מה pandas באמת רואה
        print("Columns found in Excel:", df.columns.tolist())

        user_id = get_jwt_identity()
        batch = db.batch()
        count = 0

        for index, row in df.iterrows():
            try:
                # שימוש ב-.get() עם ערך ברירת מחדל None
                amount_val = row.get('סכום חיוב') or row.get('Amount') or row.get('סכום')
                business_name = row.get('שם בית העסק') or row.get('BusinessName') or row.get('שם עסק')
                date_val = row.get('תאריך עסקה') or row.get('Date') or row.get('תאריך')

                # אם השורה ריקה לגמרי, פשוט נמשיך הלאה בלי לקרוס
                if pd.isna(business_name) or (isinstance(business_name, str) and not business_name.strip()):
                    continue

                # ניקוי סכום בטוח
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

                doc_ref = db.collection('transactions').document()
                record = {
                    "userId": user_id,
                    "businessName": str(business_name).strip(),
                    "amount": clean_amount,
                    "date": str(date_val).strip() if not pd.isna(date_val) else "",
                    "status": "REGULAR"
                }

                batch.set(doc_ref, record)
                count += 1

                if count % 500 == 0:
                    batch.commit()
                    batch = db.batch()
            except Exception as row_error:
                print(f"Error processing row {index}: {row_error}")
                continue

        if count > 0:
            batch.commit()
            return jsonify({"message": f"Successfully processed {count} transactions"}), 200
        else:
            return jsonify({"error": "No valid data found in file. Check column names."}), 400

    except Exception as e:
        print(f"FATAL Server Error: {e}")
        return jsonify({"error": f"Server error: {str(e)}"}), 500