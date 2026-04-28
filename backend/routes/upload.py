from flask import Blueprint, request, jsonify
from flask_jwt_extended import jwt_required, get_jwt_identity
from firebase_config import db
from services.file_service import FileService
from services.ml_service import MLService
from services.data_validator import DataValidator
from services.user_profile_schema import validate_profile, merge_profile
from google.cloud import firestore
import logging
import uuid

logger = logging.getLogger(__name__)
upload_bp = Blueprint('upload', __name__)

CATEGORY_TRANSLATION = {
    'מזון וצריכה': 'Food & Grocery',
    'מסעדות, קפה וברים': 'Restaurants & Cafes',
    'מסעדות': 'Restaurants',
    'אופנה': 'Fashion',
    'בריאות': 'Health',
    'תחבורה': 'Transport',
    'חינוך': 'Education',
    'שירותי תקשורת': 'Telecommunications',
    'עירייה וממשלה': 'Government',
    'שונות': 'Other',
    'כללי': 'General',
    'בידור': 'Entertainment',
    'ביטוח': 'Insurance',
    'בנקאות ופיננסים': 'Finance',
    'רכב': 'Automotive',
    'מחשבים ואלקטרוניקה': 'Electronics',
    'ספורט': 'Sports',
    'תיירות ונסיעות': 'Travel',
    'שירותים מקצועיים': 'Professional Services',
    'קניות': 'Shopping',
}

def translate_category(cat):
    if not cat:
        return 'General'
    return CATEGORY_TRANSLATION.get(cat, cat if not any(ord(c) > 127 for c in str(cat)) else 'General')


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


# ─────────────────────────────────────────────
# GET /api/transactions
# ─────────────────────────────────────────────
@upload_bp.route('/transactions', methods=['GET'])
@jwt_required()
def get_transactions():
    try:
        user_id = get_jwt_identity()
        limit   = int(request.args.get('limit', 20))
        cursor  = request.args.get('cursor', None)
        file_id = request.args.get('file_id', None)

        from google.cloud.firestore_v1.base_query import FieldFilter

        # אם לא נבחר קובץ ספציפי — משתמשים ב-latest_file_id מהפרופיל
        if not file_id:
            profile_doc = db.collection('user_profiles').document(user_id).get()
            if profile_doc.exists:
                file_id = profile_doc.to_dict().get('latest_file_id')

        query = (db.collection('transactions')
                 .where(filter=FieldFilter('user_id', '==', user_id)))

        if file_id:
            query = query.where(filter=FieldFilter('file_id', '==', file_id))

        query = query.order_by('date', direction=firestore.Query.DESCENDING)

        if cursor:
            last_doc = db.collection('transactions').document(cursor).get()
            if last_doc.exists:
                query = query.start_after(last_doc)

        query = query.limit(limit)

        transactions = []
        last_id = None
        for doc in query.stream():
            t = doc.to_dict()
            t['id'] = doc.id
            transactions.append(t)
            last_id = doc.id

        return jsonify({
            "transactions": transactions,
            "nextCursor":   last_id if len(transactions) == limit else None,
            "hasMore":      len(transactions) == limit
        }), 200
    except Exception as e:
        import traceback
        print("GET_TRANSACTIONS ERROR:", str(e))
        print(traceback.format_exc())
        return jsonify({"error": str(e)}), 500



# ─────────────────────────────────────────────
# GET /api/uploads  — רשימת קבצים שהמשתמש העלה
# ─────────────────────────────────────────────
@upload_bp.route('/uploads', methods=['GET'])
@jwt_required()
def get_uploads():
    try:
        user_id = get_jwt_identity()

        from google.cloud.firestore_v1.base_query import FieldFilter

        docs = (db.collection('uploads')
                .where(filter=FieldFilter('user_id', '==', user_id))
                .stream())

        uploads = []
        for doc in docs:
            d = doc.to_dict()
            ts = d.get('uploaded_at')
            date_str = ts.isoformat() if hasattr(ts, 'isoformat') else str(ts) if ts else ''
            uploads.append({
                'id':                doc.id,
                'fileName':          d.get('file_name', 'Unknown'),
                'uploadedAt':        date_str,
                'transactionCount':  d.get('transaction_count', 0),
                'irregularCount':    d.get('irregular_count', 0),
                '_ts':               ts,  # לצורך מיון
            })

        # מיון לפי תאריך העלאה — החדש ביותר ראשון
        uploads.sort(key=lambda x: x.get('uploadedAt', ''), reverse=True)
        uploads = uploads[:6]

        # הסרת שדה העזר לפני החזרה
        for u in uploads:
            u.pop('_ts', None)

        return jsonify(uploads), 200
    except Exception as e:
        import traceback
        print("GET_UPLOADS ERROR:", str(e))
        print(traceback.format_exc())
        return jsonify({"error": str(e)}), 500


# ─────────────────────────────────────────────
# POST /api/profile/update
# ─────────────────────────────────────────────
@upload_bp.route('/profile/update', methods=['POST'])
@jwt_required()
def update_profile():
    try:
        user_id = get_jwt_identity()
        data    = request.get_json()
        overrides = data.get('overrides', {})

        if not overrides:
            return jsonify({"message": "No overrides provided"}), 200

        profile_ref = db.collection('user_profiles').document(user_id)
        profile_doc = profile_ref.get()
        user_profile = profile_doc.to_dict() if profile_doc.exists else {}

        known_merchants = set(user_profile.get('known_merchants', []))
        cat_counts      = dict(user_profile.get('category_counts', {}))

        for txn_id, new_status in overrides.items():
            txn_doc = db.collection('transactions').document(txn_id).get()
            if txn_doc.exists:
                t = txn_doc.to_dict()
                if new_status == 'REGULAR':
                    merchant = t.get('businessName', '')
                    category = t.get('category', 'General')
                    if merchant:
                        known_merchants.add(merchant)
                    if category:
                        cat_counts[category] = cat_counts.get(category, 0) + 1

        user_profile['known_merchants'] = list(known_merchants)
        user_profile['category_counts'] = cat_counts
        profile_ref.set(user_profile, merge=True)

        return jsonify({"message": f"Profile updated with {len(overrides)} correction(s)"}), 200

    except Exception as e:
        logger.error(f"Profile update error: {e}")
        return jsonify({"error": str(e)}), 500


# ─────────────────────────────────────────────
# PUT /api/transactions/<id>
# ─────────────────────────────────────────────
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


# ─────────────────────────────────────────────
# GET /api/shares
# ─────────────────────────────────────────────
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


# ─────────────────────────────────────────────
# POST /api/shares
# ─────────────────────────────────────────────
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


# ─────────────────────────────────────────────
# POST /api/upload
# ─────────────────────────────────────────────
@upload_bp.route('/upload', methods=['POST'])
@jwt_required()
def upload_file():
    if 'file' not in request.files:
        return jsonify({"error": "No file part"}), 400

    file = request.files['file']
    user_id = get_jwt_identity()

    try:
        # 1. קריאה וניקוי
        original_filename = file.filename or 'unknown'
        df = FileService.validate_and_process_file(file)

        # 2. ולידציה
        df, report = DataValidator.validate(df)
        if not report.is_valid():
            return jsonify({"error": report.errors[0]}), 400
        if report.warnings:
            logger.warning(f"Validation warnings for user {user_id}: {report.warnings}")

        # 3. פרופיל משתמש
        profile_ref = db.collection('user_profiles').document(user_id)
        profile_doc = profile_ref.get()
        raw_profile  = profile_doc.to_dict() if profile_doc.exists else {}
        user_profile = validate_profile(raw_profile)

        # 4. הרצת המודל
        ml = MLService.get_instance()
        classified = ml.classify_transactions(df, user_profile)

        # 5. יצירת מזהה קובץ ייחודי
        file_id = str(uuid.uuid4())

        # 6. שמירת הטרנזקציות עם file_id
        batch = db.batch()
        count = 0
        irregular_count = 0
        for txn in classified:
            doc_ref = db.collection('transactions').document()
            is_irregular = txn['status'] == 'IRREGULAR'
            if is_irregular:
                irregular_count += 1
            batch.set(doc_ref, {
                "user_id":           user_id,
                "file_id":           file_id,          # ← חדש
                "businessName":      txn['businessName'],
                "amount":            txn['amount'],
                "date":              txn['date'],
                "category":          translate_category(txn['category']),
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

        # 7. שמירת מטאדטה של הקובץ ב-uploads collection
        upload_ref = db.collection('uploads').document(file_id)
        upload_ref.set({
            "user_id":           user_id,
            "file_name":         original_filename,
            "transaction_count": count,
            "irregular_count":   irregular_count,
            "uploaded_at":       firestore.SERVER_TIMESTAMP,
        })

        # 8. עדכון פרופיל + שמירת latest_file_id ביחד
        new_profile_data = {
            "new_amounts":    df['amount'].tolist(),
            "new_merchants":  df['businessName'].tolist(),
            "new_categories": df['category'].tolist(),
            "new_currencies": df['currency'].tolist(),
            "count":          len(df),
        }
        updated_profile = merge_profile(user_profile, new_profile_data)
        updated_profile['latest_file_id'] = file_id  # ← נשמר יחד עם הפרופיל
        profile_ref.set(updated_profile)

        return jsonify({
            "message":        f"Successfully saved {count} transactions.",
            "total":          count,
            "irregular":      irregular_count,
            "regular":        count - irregular_count,
            "removed_rows":   report.removed_rows,
            "warnings":       report.warnings,
            "file_id":        file_id,
        }), 200

    except ValueError as e:
        return jsonify({"error": str(e)}), 400
    except Exception as e:
        logger.error(f"Upload error: {e}")
        return jsonify({"error": str(e)}), 500