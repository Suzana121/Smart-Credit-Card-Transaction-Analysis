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
