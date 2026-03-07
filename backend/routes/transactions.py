from flask import Blueprint, jsonify
from flask_jwt_extended import jwt_required, get_jwt_identity
from firebase_config import db

transactions_bp = Blueprint('transactions', __name__)

@transactions_bp.route('/transactions', methods=['GET'])
# @jwt_required()
def get_transactions():
    try:
        docs = db.collection('transactions').stream()

        transaction_list = []
        for doc in docs:
            data = doc.to_dict()

            # שליפת הנתונים
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

            # קביעת סטטוס
            status_from_file = data.get("Status") or data.get("status")
            if status_from_file:
                final_status = status_from_file
            else:
                final_status = "IRREGULAR" if data.get("Irregular") else "REGULAR"

            # --- הסלקטור (המסנן) ---
            # אם הסכום הוא 0 והשם הוא "Unknown" - דלג על השורה הזאת!
            if float(amount) == 0 and merchant == "Unknown":
                continue
            # ----------------------

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

        print(f"Sent {len(transaction_list)} valid transactions to app (filtered out empty ones)")
        return jsonify(transaction_list), 200

    except Exception as e:
        print(f"Error: {e}")
        return jsonify({"error": str(e)}), 500

'''
from flask import Blueprint, jsonify
from flask_jwt_extended import jwt_required, get_jwt_identity
from firebase_config import db

# יצירת "בלופרינט" - ככה פלאסק יודע שיש פה חלק חדש במערכת
transactions_bp = Blueprint('transactions', __name__)

@transactions_bp.route('/transactions', methods=['GET'])
@jwt_required()
def get_transactions():
    try:
        # 1. בודקים מי המשתמש שמבקש את המידע
        current_user = get_jwt_identity()
        print(f"Fetching transactions for user: {current_user}")

        # 2. הולכים לפיירבייס ומבקשים את כל המסמכים ששייכים למשתמש הזה
        # (אנחנו מניחים שבכל מסמך שמרת שדה בשם 'userId')
        docs = db.collection('transactions').where('userId', '==', current_user).stream()

        transaction_list = []
        for doc in docs:
            data = doc.to_dict()
            # 3. מסדרים את המידע יפה כדי לשלוח לאפליקציה
            transaction_list.append({
                "id": doc.id,
                "date": data.get("Date", ""),
                "merchant": data.get("Description", "Unknown"),
                "amount": data.get("Amount", 0),
                "currency": "₪",
                "status": "IRREGULAR" if data.get("Irregular") else "REGULAR",
                "isRecognized": data.get("isRecognized", True)
            })

        return jsonify(transaction_list), 200

    except Exception as e:
        print(f"Error fetching transactions: {e}")
        return jsonify({"error": str(e)}), 500
        '''
