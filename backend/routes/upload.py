from flask import Blueprint, request, jsonify
from flask_jwt_extended import jwt_required, get_jwt_identity
from firebase_config import db
from services.file_service import FileService
from services.ml_service import MLService
from services.data_validator import DataValidator
from services.user_profile_schema import validate_profile, merge_profile
from google.cloud import firestore
import logging

logger = logging.getLogger(__name__)
upload_bp = Blueprint('upload', __name__)


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


@upload_bp.route('/transactions', methods=['GET'])
@jwt_required()
def get_transactions():
    try:
        user_id = get_jwt_identity()
        docs = db.collection('transactions').where('user_id', '==', user_id).stream()
        transactions = []
        for doc in docs:
            t = doc.to_dict()
            t['id'] = doc.id
            transactions.append(t)
        return jsonify(transactions), 200
    except Exception as e:
        return jsonify({"error": str(e)}), 500


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


@upload_bp.route('/upload', methods=['POST'])
@jwt_required()
def upload_file():
    if 'file' not in request.files:
        return jsonify({"error": "No file part"}), 400

    file = request.files['file']
    user_id = get_jwt_identity()

    try:
        # 1. קריאה וניקוי
        df = FileService.validate_and_process_file(file)

        # 2. ולידציה של הנתונים
        df, report = DataValidator.validate(df)
        if not report.is_valid():
            return jsonify({"error": report.errors[0]}), 400
        if report.warnings:
            logger.warning(f"Validation warnings for user {user_id}: {report.warnings}")

        # 3. פרופיל משתמש מ-Firestore
        profile_ref = db.collection('user_profiles').document(user_id)
        profile_doc = profile_ref.get()
        raw_profile  = profile_doc.to_dict() if profile_doc.exists else {}
        user_profile = validate_profile(raw_profile)  # ודא שכל השדות קיימים

        # 4. הרצת המודל
        ml = MLService.get_instance()
        classified = ml.classify_transactions(df, user_profile)

        # 5. שמירה ל-Firestore
        batch = db.batch()
        count = 0
        for txn in classified:
            doc_ref = db.collection('transactions').document()
            batch.set(doc_ref, {
                "user_id":           user_id,
                "businessName":      txn['businessName'],
                "amount":            txn['amount'],
                "date":              txn['date'],
                "category":          txn['category'],
                "txn_type":          txn.get('txn_type', ''),
                "currency":          txn['currency'],
                "original_amount":   txn.get('original_amount', txn['amount']),
                "original_currency": txn.get('original_currency', txn['currency']),
                "status":            txn['status'],
                "anomaly_score":     txn.get('anomaly_score', 0.0),
                "explanation":       txn.get('explanation', ''),
                "uploaded_at":       firestore.SERVER_TIMESTAMP,
            })
            count += 1
            if count % 500 == 0:
                batch.commit()
                batch = db.batch()
        batch.commit()

        # 6. עדכון פרופיל עם merge_profile
        new_profile_data = {
            "new_amounts":    df['amount'].tolist(),
            "new_merchants":  df['businessName'].tolist(),
            "new_categories": df['category'].tolist(),
            "new_currencies": df['currency'].tolist(),
            "count":          len(df),
        }
        updated_profile = merge_profile(user_profile, new_profile_data)
        profile_ref.set(updated_profile)

        irregular_count = sum(1 for t in classified if t['status'] == 'IRREGULAR')
        return jsonify({
            "message":        f"Successfully saved {count} transactions.",
            "total":          count,
            "irregular":      irregular_count,
            "regular":        count - irregular_count,
            "removed_rows":   report.removed_rows,
            "warnings":       report.warnings,
        }), 200

    except ValueError as e:
        return jsonify({"error": str(e)}), 400
    except Exception as e:
        logger.error(f"Upload error: {e}")
        return jsonify({"error": str(e)}), 500