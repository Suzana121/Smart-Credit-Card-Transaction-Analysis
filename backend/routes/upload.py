from flask import Blueprint, request, jsonify
from services.file_service import validate_and_process_csv
from firebase_config import db # ייבוא ה-Client של פיירבייס שלך
import datetime

upload_bp = Blueprint('upload', __name__)

@upload_bp.route('/upload', methods=['POST'])
def upload_file():
    if 'file' not in request.files:
        return jsonify({"error": "No file part"}), 400
    
    file = request.files['file']
    is_valid, result = validate_and_process_csv(file)

    if not is_valid:
        return jsonify({"error": result}), 400

    # 1. ניקוי שמות העמודות (הורדת ירידות שורה \n)
    result.columns = [col.replace('\n', ' ').strip() for col in result.columns]

    # 2. המרה לפורמט ש-Firestore אוהב (רשימה של דיקשנריז)
    transactions = result.to_dict(orient='records')

    try:
        # 3. שמירה ל-Firebase
        # אנחנו יוצרים "Batch" כדי לשמור הרבה שורות בבת אחת (יותר מהיר)
        batch = db.batch()
        for txn in transactions:
            # יצירת מסמך חדש בתוך אוסף שנקרא transactions
            doc_ref = db.collection('transactions').document()
            # מוסיפים לנתונים גם חותמת זמן של ההעלאה
            txn['upload_date'] = datetime.datetime.now()
            batch.set(doc_ref, txn)
        
        batch.commit()

        return jsonify({
            "message": "הנתונים נשמרו בהצלחה ב-Firebase!",
            "count": len(transactions)
        }), 200

    except Exception as e:
        return jsonify({"error": f"שגיאה בשמירה ל-DB: {str(e)}"}), 500