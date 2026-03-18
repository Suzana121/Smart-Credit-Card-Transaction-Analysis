from flask import Blueprint, request, jsonify
from flask_jwt_extended import jwt_required, get_jwt_identity
from firebase_config import db
from services.file_service import FileService
from google.cloud import firestore
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

def _serialize_share(doc, direction):
    data = doc.to_dict()
    # Convert Firestore timestamp to ISO string
    ts = data.get('date')
    date_str = ts.isoformat() if hasattr(ts, 'isoformat') else str(ts) if ts else ''

    share = {
        'id': doc.id,
        'sharedBy': data.get('sharedBy', ''),
        'sharedWith': data.get('sharedWith', ''),
        'transactionId': data.get('transactionId', ''),
        'date': date_str,
        'direction': direction,
        'transaction': None
    }

    txn_id = data.get('transactionId')
    if txn_id:
        txn_doc = db.collection('transactions').document(txn_id).get()
        if txn_doc.exists:
            t = txn_doc.to_dict()
            share['transaction'] = {
                'businessName': t.get('businessName', 'Unknown'),
                'amount': t.get('amount', 0),
                'date': t.get('date', ''),
                'status': t.get('status', 'REGULAR'),
                'category': t.get('category', '')
            }
    return share


@upload_bp.route('/shares', methods=['GET'])
@jwt_required()
def get_shares():
    try:
        user_id = get_jwt_identity()

        # Look up the current user's username for incoming query
        user_doc = db.collection('users').document(user_id).get()
        username = user_doc.to_dict().get('username', '') if user_doc.exists else ''

        shares = []

        # Outgoing: shares the current user sent
        for doc in db.collection('shares').where('sharedBy', '==', user_id).stream():
            shares.append(_serialize_share(doc, 'outgoing'))

        # Incoming: shares where someone used this user's username as sharedWith
        if username:
            for doc in db.collection('shares').where('sharedWith', '==', username).stream():
                shares.append(_serialize_share(doc, 'incoming'))

        print(f"Returning {len(shares)} shares for user {user_id}")
        return jsonify(shares), 200

    except Exception as e:
        print(f"Error fetching shares: {e}")
        return jsonify({"error": str(e)}), 500


@upload_bp.route('/shares', methods=['POST'])
@jwt_required()
def create_share():
    try:
        shared_by = get_jwt_identity()
        data = request.get_json()

        shared_with = data.get('sharedWith')
        transaction_id = data.get('transactionId')

        if not shared_with or not transaction_id:
            return jsonify({"error": "Missing sharedWith or transactionId"}), 400

        doc_ref = db.collection('shares').document()
        doc_ref.set({
            "sharedBy": shared_by,
            "sharedWith": shared_with,
            "transactionId": transaction_id,
            "date": firestore.SERVER_TIMESTAMP
        })

        print(f"Share saved: {shared_by} -> {shared_with}, transaction={transaction_id}")
        return jsonify({"id": doc_ref.id}), 201

    except Exception as e:
        print(f"Error saving share: {e}")
        return jsonify({"error": str(e)}), 500


@upload_bp.route('/transactions/<transaction_id>', methods=['PUT'])
@jwt_required()
def update_transaction_status(transaction_id):
    try:
        data = request.get_json()
        new_status = data.get('status')
        if not new_status:
            return jsonify({"error": "Missing 'status' field"}), 400

        doc_ref = db.collection('transactions').document(transaction_id)
        doc = doc_ref.get()
        if not doc.exists:
            print(f"Transaction not found: {transaction_id}")
            return jsonify({"error": "Transaction not found"}), 404

        doc_ref.update({"status": new_status})
        print(f"Updated transaction {transaction_id} -> status: {new_status}")
        return jsonify({"id": transaction_id, "status": new_status}), 200

    except Exception as e:
        print(f"Error updating transaction status: {e}")
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