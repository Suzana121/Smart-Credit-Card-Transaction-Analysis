from flask import Blueprint, jsonify, request
from flask_jwt_extended import jwt_required, get_jwt_identity
from firebase_config import db

transactions_bp = Blueprint('transactions', __name__)

@transactions_bp.route('/transactions', methods=['GET'])
# @jwt_required()  # ניתן להחזיר אם את רוצה הגנה של טוקן
def get_transactions():
    try:
        # --- לוגיקת Pagination (דרישה 1) ---
        # קבלת כמות העסקאות לטעינה (ברירת מחדל 20)
        limit_val = int(request.args.get('limit', 20))
        # קבלת מזהה העסקה האחרונה שנטענה כדי לדעת מאיפה להמשיך
        last_doc_id = request.args.get('last_doc_id')

        # יצירת השאילתה עם מיון (חשוב בשביל Pagination)
        query = db.collection('transactions').order_by('date', direction='DESCENDING')

        # מימוש ה-Pagination: אם קיבלנו מזהה, נתחיל את השאילתה אחריו
        if last_doc_id:
            last_doc_ref = db.collection('transactions').document(last_doc_id).get()
            if last_doc_ref.exists:
                query = query.start_after(last_doc_ref)

        # הפעלת ה-Limit (חוסך טעינת נתונים מיותרת מהשרת)
        docs = query.limit(limit_val).stream()

        transaction_list = []
        for doc in docs:
            data = doc.to_dict()

            # שליפת הנתונים (שמות השדות כפי שהופיעו אצלך)
            merchant = (data.get("BusinessName") or
                        data.get("businessName") or
                        data.get("Description") or
                        data.get("merchant") or
                        "Unknown")

            amount = (data.get("Amount") or
                      data.get("amount") or
                      0)

            date = (data.get("Date") or
                    data.get("date") or
                    "")

            # קביעת סטטוס (חיבור לנתונים המשתנים ב-Firebase - דרישה 2)
            status_from_file = data.get("Status") or data.get("status")
            if status_from_file:
                final_status = status_from_file
            else:
                final_status = "IRREGULAR" if data.get("Irregular") else "REGULAR"

            # --- הסלקטור (המסנן) שכתבת ---
            if float(amount) == 0 and merchant == "Unknown":
                continue

            transaction_list.append({
                "id": doc.id,
                "date": date,
                "merchant": merchant,
                "businessName": merchant,
                "amount": amount,
                "currency": "₪",
                "status": final_status,
                "isRecognized": True
            })

        # הדפסה לטרמינל כדי שתוכלי להראות למרצה שזה עובד
        print(f"Pagination active: Sent {len(transaction_list)} transactions (Limit: {limit_val})")

        return jsonify(transaction_list), 200

    except Exception as e:
        print(f"Error fetching transactions: {e}")
        return jsonify({"error": str(e)}), 500