from flask import Blueprint, request, jsonify
from flask_jwt_extended import jwt_required, get_jwt_identity
from firebase_config import db
from services.file_service import FileService
from google.cloud import firestore
import pandas as pd

upload_bp = Blueprint('upload', __name__)

# --- פונקציית עזר לסריאליזציה של שיתוף ---
def _serialize_share(doc, direction):
    data = doc.to_dict()
    ts = data.get('date')
    date_str = ts.isoformat() if hasattr(ts, 'isoformat') else str(ts) if ts else ''

    friend_phone = data.get('sharedWith') if direction == 'outgoing' else data.get('sharedBy')

    share = {
        'id': doc.id,
        'sharedBy': data.get('sharedBy', ''),
        'sharedWith': data.get('sharedWith', ''),
        'friendName': friend_phone,
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

# --- שליפת עסקאות של המשתמש עם Pagination (מתוקן) ---
@upload_bp.route('/transactions', methods=['GET'])
@jwt_required()
def get_transactions():
    try:
        user_id = get_jwt_identity()

        # קבלת פרמטרים מהבקשה (לדרישת ה-Pagination)
        limit_val = int(request.args.get('limit', 20))
        last_doc_id = request.args.get('last_doc_id')

        # בניית השאילתה עם סינון לפי המשתמש ומיון לפי תאריך
        query = db.collection('transactions') \
            .where('user_id', '==', user_id) \
            .order_by('date', direction='DESCENDING')

        # אם יש לנו מזהה של הדף הקודם, נמשיך ממנו
        if last_doc_id:
            last_doc_ref = db.collection('transactions').document(last_doc_id).get()
            if last_doc_ref.exists:
                query = query.start_after(last_doc_ref)

        # הפעלת ה-Limit (כאן מתבצע ה-Pagination בפועל)
        docs = query.limit(limit_val).stream()

        transactions = []
        for doc in docs:
            t = doc.to_dict()
            t['id'] = doc.id
            transactions.append(t)

        # הדפסה לטרמינל בשביל הבדיקה שלך
        print(f"Pagination active (User: {user_id}): Sent {len(transactions)} transactions")

        return jsonify(transactions), 200
    except Exception as e:
        print(f"DEBUG: Error in get_transactions: {e}")
        return jsonify({"error": str(e)}), 500
# --- עדכון סטטוס עסקה ---
@upload_bp.route('/transactions/<transaction_id>', methods=['PUT'])
@jwt_required()
def update_transaction_status(transaction_id):
    try:
        user_id = get_jwt_identity()
        data = request.get_json()
        new_status = data.get('status')

        if not new_status or new_status not in ['REGULAR', 'IRREGULAR']:
            return jsonify({"error": "Invalid status"}), 400

        doc_ref = db.collection('transactions').document(transaction_id)
        doc = doc_ref.get()

        if not doc.exists:
            return jsonify({"error": "Transaction not found"}), 404

        if doc.to_dict().get('user_id') != user_id:
            return jsonify({"error": "Unauthorized"}), 403

        doc_ref.update({"status": new_status})
        updated = doc_ref.get().to_dict()
        updated['id'] = transaction_id

        return jsonify(updated), 200
    except Exception as e:
        return jsonify({"error": str(e)}), 500

# --- שליפת היסטוריית שיתופים (Wallet) ---
@upload_bp.route('/shares', methods=['GET'])
@jwt_required()
def get_shares():
    try:
        user_id = get_jwt_identity()
        user_doc = db.collection('users').document(user_id).get()
        if not user_doc.exists:
            return jsonify({"error": "User not found"}), 404

        my_phone = user_doc.to_dict().get('phone', '')
        shares = []

        outgoing = db.collection('shares').where('sharedBy', '==', my_phone).stream()
        for doc in outgoing:
            shares.append(_serialize_share(doc, 'outgoing'))

        incoming = db.collection('shares').where('sharedWith', '==', my_phone).stream()
        for doc in incoming:
            shares.append(_serialize_share(doc, 'incoming'))

        return jsonify(shares), 200
    except Exception as e:
        return jsonify({"error": str(e)}), 500

# --- יצירת שיתוף חדש ---
@upload_bp.route('/shares', methods=['POST'])
@jwt_required()
def post_share():
    try:
        current_user_id = get_jwt_identity()
        data = request.get_json()
        shared_with_phone = data.get('sharedWith')
        transaction_id = data.get('transactionId')

        if not shared_with_phone or not transaction_id:
            return jsonify({"error": "Missing sharedWith or transactionId"}), 400

        user_doc = db.collection('users').document(current_user_id).get()
        sender_phone = user_doc.to_dict().get('phone', '')

        doc_ref = db.collection('shares').document()
        doc_ref.set({
            "sharedBy": sender_phone,
            "sharedWith": shared_with_phone,
            "transactionId": transaction_id,
            "date": firestore.SERVER_TIMESTAMP
        })

        return jsonify({"success": True, "id": doc_ref.id}), 201
    except Exception as e:
        return jsonify({"error": str(e)}), 500

# --- העלאת קובץ אקסל ועיבודו (גרסה ללא ML זמנית) ---
@upload_bp.route('/upload', methods=['POST'])
@jwt_required()
def upload_file():
    if 'file' not in request.files:
        return jsonify({"error": "No file part"}), 400

    file = request.files['file']
    try:
        df = FileService.validate_and_process_file(file)
        user_id = get_jwt_identity()
        batch = db.batch()
        count = 0

        for index, row in df.iterrows():
            business_name = row.get('שם בית העסק') or row.get('BusinessName') or row.get('שם עסק')

            if pd.isna(business_name) or not str(business_name).strip() or "סה\"כ" in str(business_name):
                continue

            amount_val = row.get('סכום חיוב') or row.get('Amount') or row.get('סכום')
            date_val = row.get('תאריך עסקה') or row.get('Date') or row.get('תאריך')
            category_val = row.get('category', 'General')

            doc_ref = db.collection('transactions').document()
            record = {
                "user_id": user_id,
                "businessName": str(business_name).strip(),
                "amount": float(str(amount_val).replace('₪','').replace(',','')) if amount_val else 0.0,
                "date": str(date_val).strip() if not pd.isna(date_val) else "",
                "category": str(category_val).strip(),
                "status": "REGULAR",
                "uploaded_at": firestore.SERVER_TIMESTAMP
            }
            batch.set(doc_ref, record)
            count += 1
            if count % 500 == 0:
                batch.commit()
                batch = db.batch()

        batch.commit()
        return jsonify({"message": f"Successfully saved {count} transactions."}), 200
    except Exception as e:
        print(f"DEBUG: Upload error: {e}")
        return jsonify({"error": str(e)}), 500