import pandas as pd
from flask import Blueprint, request, jsonify
from flask_jwt_extended import jwt_required, get_jwt_identity
from firebase_config import db

upload_bp = Blueprint('upload', __name__)

@upload_bp.route('/upload', methods=['POST'])
@jwt_required()
def upload_file():
    if 'file' not in request.files:
        return jsonify({"error": "No file part"}), 400

    file = request.files['file']

    if file.filename == '':
        return jsonify({"error": "No selected file"}), 400

    try:
        # 1. קריאת הקובץ (תומך גם ב-CSV וגם ב-Excel)
        if file.filename.endswith('.csv'):
            df = pd.read_csv(file)
        else:
            df = pd.read_excel(file)

        # נקיון רווחים בשמות העמודות (למשל " Amount " יהפוך ל-"Amount")
        df.columns = df.columns.str.strip()

        user_id = get_jwt_identity()
        batch = db.batch()

        # 2. לולאה שעוברת שורה-שורה ושומרת לפיירבייס
        count = 0
        for index, row in df.iterrows():
            doc_ref = db.collection('transactions').document()

            # כאן הקסם: אנחנו מחפשים את השמות בכל הוריאציות האפשריות
            # שימי לב שאנחנו ממירים את הסכום למספר (float) כדי שלא יהיה 0
            try:
                amount_val = float(row.get('Amount') or row.get('amount') or row.get('סכום') or 0)
            except:
                amount_val = 0.0

            record = {
                "userId": user_id,
                "BusinessName": str(row.get('BusinessName') or row.get('business_name') or row.get('שם בית עסק') or 'Unknown'),
                "Amount": amount_val,
                "Date": str(row.get('Date') or row.get('date') or row.get('תאריך') or ''),
                "Status": str(row.get('Status') or 'REGULAR'),
                "Description": str(row.get('BusinessName') or row.get('business_name') or 'Unknown') # תוספת ליתר ביטחון
            }

            batch.set(doc_ref, record)
            count += 1

        batch.commit()

        return jsonify({
            "message": f"Successfully processed {count} transactions",
            "format": "CSV/Excel"
        }), 200

    except Exception as e:
        print(f"Upload Error: {e}")
        return jsonify({"error": str(e)}), 500

''''
from flask import Blueprint, request, jsonify
from flask_jwt_extended import jwt_required, get_jwt_identity
from services.file_service import validate_and_process_file # השם החדש

upload_bp = Blueprint('upload', __name__)

@upload_bp.route('/upload', methods=['POST'])
@jwt_required()
def upload_file():
    # בדיקה אם הקובץ קיים בבקשה
    if 'file' not in request.files:
        return jsonify({"error": "No file part"}), 400
    
    file = request.files['file']
    
    if file.filename == '':
        return jsonify({"error": "No selected file"}), 400

    # קריאה לפונקציה המשודרגת (תומכת ב-CSV ו-Excel)
    success, result = validate_and_process_file(file)

    if not success:
        return jsonify({"error": result}), 400

    df = result # אם הצליח, ה-result הוא ה-DataFrame
    user_id = get_jwt_identity()

    # כאן מגיע השלב של השמירה ל-Firestore (ה-Batch שעשינו)
    # ... (הקוד של ה-batch commit) ...

    return jsonify({
        "message": f"Successfully processed {len(df)} transactions",
        "format": "Excel" if file.filename.lower().endswith(('.xlsx', '.xls')) else "CSV"
    }), 200
    '''
