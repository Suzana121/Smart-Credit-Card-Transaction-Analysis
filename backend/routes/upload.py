from flask import Blueprint, request, jsonify
from flask_jwt_extended import jwt_required, get_jwt_identity
from firebase_config import db
from services.file_service import FileService
from services.ml_service import MLService
from services.data_validator import DataValidator
from services.user_profile_schema import validate_profile, merge_profile
from google.cloud import firestore
from google.cloud.firestore_v1.base_query import FieldFilter
from collections import defaultdict
import hashlib
import logging
import uuid
import sys

logger = logging.getLogger(__name__)
upload_bp = Blueprint('upload', __name__)

CATEGORY_TRANSLATION = {
    'מזון וצריכה': 'Food & Grocery', 'מסעדות, קפה וברים': 'Restaurants & Cafes',
    'מסעדות': 'Restaurants', 'אופנה': 'Fashion', 'בריאות': 'Health',
    'תחבורה': 'Transport', 'חינוך': 'Education', 'שירותי תקשורת': 'Telecommunications',
    'עירייה וממשלה': 'Government', 'שונות': 'Other', 'כללי': 'General',
    'בידור': 'Entertainment', 'ביטוח': 'Insurance', 'בנקאות ופיננסים': 'Finance',
    'רכב': 'Automotive', 'מחשבים ואלקטרוניקה': 'Electronics', 'ספורט': 'Sports',
    'תיירות ונסיעות': 'Travel', 'קניות': 'Shopping',
}

STATS_CATEGORY_TRANSLATION = {
    'Food & Grocery': 'Food', 'Restaurants & Cafes': 'Food', 'Restaurants': 'Food',
    'Fashion': 'Shopping', 'Shopping': 'Shopping',
    'Health': 'Health', 'Transport': 'Transport', 'Education': 'Education',
}

def translate_category(cat):
    if not cat: return 'General'
    return CATEGORY_TRANSLATION.get(cat,
                                    cat if not any(ord(c) > 127 for c in str(cat)) else 'General')

def normalize_stats_category(cat):
    """קטגוריה מנורמלת לסטטיסטיקה."""
    if not cat: return 'Other'
    return STATS_CATEGORY_TRANSLATION.get(cat, 'Other')

def user_files_ref(user_id):
    return db.collection('users').document(user_id).collection('files')

def txn_col(user_id, file_id):
    return user_files_ref(user_id).document(file_id).collection('transactions')

def parse_date(date_str):
    parts = date_str.replace('/', '-').split('-')
    if len(parts) != 3: return None, None
    if len(parts[0]) == 4: return parts[1], parts[0]
    return parts[1], parts[2]

def _serialize_share(doc, direction):
    data = doc.to_dict()
    ts = data.get('date')
    date_str = ts.isoformat() if hasattr(ts, 'isoformat') else str(ts) if ts else ''
    friend_phone = data.get('sharedWith') if direction == 'outgoing' else data.get('sharedBy')
    share = {
        'id': doc.id, 'sharedBy': data.get('sharedBy', ''),
        'sharedWith': data.get('sharedWith', ''), 'friendName': friend_phone,
        'transactionId': data.get('transactionId', ''), 'date': date_str,
        'direction': direction, 'transaction': None
    }
    txn_id      = data.get('transactionId')
    txn_user_id = data.get('transactionUserId')
    txn_file_id = data.get('transactionFileId')
    if txn_id and txn_user_id and txn_file_id:
        t_doc = txn_col(txn_user_id, txn_file_id).document(txn_id).get()
        if t_doc.exists:
            t = t_doc.to_dict()
            share['transaction'] = {
                'businessName': t.get('businessName', 'Unknown'),
                'amount': t.get('amount', 0), 'date': t.get('date', ''),
                'status': t.get('status', 'REGULAR'), 'category': t.get('category', '')
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
        status  = request.args.get('status', None)  # 'REGULAR' / 'IRREGULAR' / None

        if not file_id:
            profile_doc = db.collection('user_profiles').document(user_id).get()
            if profile_doc.exists:
                file_id = profile_doc.to_dict().get('latest_file_id')

        if not file_id:
            return jsonify({"transactions": [], "nextCursor": None, "hasMore": False}), 200

        col = txn_col(user_id, file_id)

        if status:
            # סינון לפי סטטוס — שליפת כל הרלוונטיות ומיון בצד השרת
            from google.cloud.firestore_v1.base_query import FieldFilter
            docs = col.where(filter=FieldFilter('status', '==', status)).stream()
            all_transactions = []
            for doc in docs:
                t = doc.to_dict()
                t['id'] = doc.id
                all_transactions.append(t)
            all_transactions.sort(key=lambda x: x.get('date', ''), reverse=True)

            if cursor:
                ids = [t['id'] for t in all_transactions]
                if cursor in ids:
                    all_transactions = all_transactions[ids.index(cursor) + 1:]

            transactions = all_transactions[:limit]
            last_id = transactions[-1]['id'] if transactions else None
            return jsonify({
                "transactions": transactions,
                "nextCursor":   last_id if len(transactions) == limit else None,
                "hasMore":      len(all_transactions) > limit
            }), 200
        else:
            # ללא סינון — order_by עם pagination תקין
            query = col.order_by('date', direction=firestore.Query.DESCENDING)
            if cursor:
                last_doc = col.document(cursor).get()
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
# GET /api/uploads
# ─────────────────────────────────────────────
@upload_bp.route('/uploads', methods=['GET'])
@jwt_required()
def get_uploads():
    try:
        user_id = get_jwt_identity()
        docs    = user_files_ref(user_id).stream()

        uploads = []
        for doc in docs:
            d = doc.to_dict()
            ts = d.get('uploaded_at')
            date_str = ts.isoformat() if hasattr(ts, 'isoformat') else str(ts) if ts else ''
            uploads.append({
                'id':               doc.id,
                'fileName':         d.get('file_name', 'Unknown'),
                'uploadedAt':       date_str,
                'transactionCount': d.get('transaction_count', 0),
                'irregularCount':   d.get('irregular_count', 0),
            })

        uploads.sort(key=lambda x: x.get('uploadedAt', ''), reverse=True)
        return jsonify(uploads[:6]), 200
    except Exception as e:
        return jsonify({"error": str(e)}), 500


# ─────────────────────────────────────────────
# POST /api/profile/update
# ─────────────────────────────────────────────
@upload_bp.route('/profile/update', methods=['POST'])
@jwt_required()
def update_profile():
    try:
        user_id   = get_jwt_identity()
        data      = request.get_json()
        overrides = data.get('overrides', {})

        if not overrides:
            return jsonify({"message": "No overrides provided"}), 200

        profile_ref    = db.collection('user_profiles').document(user_id)
        profile_doc    = profile_ref.get()
        user_profile   = profile_doc.to_dict() if profile_doc.exists else {}
        latest_file_id = user_profile.get('latest_file_id')

        known_merchants = set(user_profile.get('known_merchants', []))
        cat_counts      = dict(user_profile.get('category_counts', {}))

        for txn_id, new_status in overrides.items():
            if not latest_file_id: continue
            t_doc = txn_col(user_id, latest_file_id).document(txn_id).get()
            if t_doc.exists:
                t = t_doc.to_dict()
                if new_status == 'REGULAR':
                    merchant = t.get('businessName', '')
                    category = t.get('category', 'General')
                    if merchant: known_merchants.add(merchant)
                    if category: cat_counts[category] = cat_counts.get(category, 0) + 1

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
        user_id    = get_jwt_identity()
        data       = request.get_json()
        new_status = data.get('status')

        if not new_status or new_status not in ['REGULAR', 'IRREGULAR']:
            return jsonify({"error": "Invalid status"}), 400

        profile_doc    = db.collection('user_profiles').document(user_id).get()
        latest_file_id = profile_doc.to_dict().get('latest_file_id') if profile_doc.exists else None

        if not latest_file_id:
            return jsonify({"error": "No file found"}), 404

        doc_ref = txn_col(user_id, latest_file_id).document(transaction_id)
        doc     = doc_ref.get()

        if not doc.exists:
            return jsonify({"error": "Transaction not found"}), 404

        doc_ref.update({"status": new_status})
        updated      = doc_ref.get().to_dict()
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
        user_id  = get_jwt_identity()
        user_doc = db.collection('users').document(user_id).get()
        if not user_doc.exists:
            return jsonify({"error": "User not found"}), 404
        my_phone = user_doc.to_dict().get('phone', '')
        shares   = []
        for doc in db.collection('shares').where('sharedBy', '==', my_phone).stream():
            shares.append(_serialize_share(doc, 'outgoing'))
        for doc in db.collection('shares').where('sharedWith', '==', my_phone).stream():
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
        current_user_id   = get_jwt_identity()
        data              = request.get_json()
        shared_with_phone = data.get('sharedWith')
        transaction_id    = data.get('transactionId')

        if not shared_with_phone or not transaction_id:
            return jsonify({"error": "Missing sharedWith or transactionId"}), 400

        user_doc     = db.collection('users').document(current_user_id).get()
        sender_phone = user_doc.to_dict().get('phone', '')

        profile_doc    = db.collection('user_profiles').document(current_user_id).get()
        latest_file_id = profile_doc.to_dict().get('latest_file_id') if profile_doc.exists else None

        doc_ref = db.collection('shares').document()
        doc_ref.set({
            "sharedBy":          sender_phone,
            "sharedWith":        shared_with_phone,
            "transactionId":     transaction_id,
            "transactionUserId": current_user_id,
            "transactionFileId": latest_file_id,
            "date":              firestore.SERVER_TIMESTAMP
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

    file    = request.files['file']
    user_id = get_jwt_identity()

    try:
        original_filename = file.filename or 'unknown'

        # ── בדיקת כפילות לפי hash של תוכן הקובץ ──
        file_content = file.read()
        file_hash    = hashlib.md5(file_content).hexdigest()
        file.seek(0)

        existing = list(
            user_files_ref(user_id)
            .where(filter=FieldFilter('file_hash', '==', file_hash))
            .limit(1)
            .stream()
        )
        if existing:
            return jsonify({
                "error":   "duplicate_file",
                "message": "You have already uploaded this file before"
            }), 409

        df = FileService.validate_and_process_file(file)

        df, report = DataValidator.validate(df)
        if not report.is_valid():
            return jsonify({"error": report.errors[0]}), 400

        profile_ref  = db.collection('user_profiles').document(user_id)
        profile_doc  = profile_ref.get()
        raw_profile  = profile_doc.to_dict() if profile_doc.exists else {}
        user_profile = validate_profile(raw_profile)

        ml = MLService.get_instance()
        print('Starting ML classification...', flush=True)
        classified = ml.classify_transactions(df, user_profile)
        print('ML done, saving to Firestore...', flush=True)

        file_id = str(uuid.uuid4())
        col     = txn_col(user_id, file_id)

        # ── שמירת טרנזקציות + חישוב monthly_summary בו-זמנית ──
        batch           = db.batch()
        count           = 0
        irregular_count = 0
        monthly_summary = defaultdict(lambda: {
            'total': 0.0, 'regular': 0, 'irregular': 0,
            'categories': defaultdict(float)
        })

        for txn in classified:
            doc_ref      = col.document()
            is_irregular = txn['status'] == 'IRREGULAR'
            if is_irregular: irregular_count += 1

            translated_cat = translate_category(txn['category'])

            batch.set(doc_ref, {
                "businessName":      txn['businessName'],
                "amount":            txn['amount'],
                "date":              txn['date'],
                "category":          translated_cat,
                "txn_type":          txn.get('txn_type', ''),
                "currency":          txn['currency'],
                "original_amount":   txn.get('original_amount', txn['amount']),
                "original_currency": txn.get('original_currency', txn['currency']),
                "status":            txn['status'],
                "anomaly_score":     txn.get('anomaly_score', 0.0),
                "explanation":       txn.get('explanation', ''),
                "uploaded_at":       firestore.SERVER_TIMESTAMP,
            })

            # חישוב monthly_summary
            date_str = txn.get('date', '')
            parts    = date_str.replace('/', '-').split('-')
            if len(parts) == 3:
                if len(parts[0]) == 4:
                    month_key = f"{parts[0]}-{parts[1]}"  # YYYY-MM
                else:
                    month_key = f"{parts[2]}-{parts[1]}"  # YYYY-MM
                stats_cat = normalize_stats_category(translated_cat)
                monthly_summary[month_key]['total']               += txn['amount']
                monthly_summary[month_key]['categories'][stats_cat] += txn['amount']
                if is_irregular:
                    monthly_summary[month_key]['irregular'] += 1
                else:
                    monthly_summary[month_key]['regular'] += 1

            count += 1
            if count % 500 == 0:
                batch.commit()
                batch = db.batch()
        batch.commit()

        # המרת monthly_summary ל-dict סריאלי
        summary_serializable = {}
        for month_key, data in monthly_summary.items():
            summary_serializable[month_key] = {
                'total':      round(data['total'], 2),
                'regular':    data['regular'],
                'irregular':  data['irregular'],
                'categories': {k: round(v, 2) for k, v in data['categories'].items()}
            }

        # שמירת מטאדטה הקובץ עם monthly_summary
        user_files_ref(user_id).document(file_id).set({
            "file_name":         original_filename,
            "transaction_count": count,
            "irregular_count":   irregular_count,
            "uploaded_at":       firestore.SERVER_TIMESTAMP,
            "monthly_summary":   summary_serializable,  # ← pre-aggregated
            "file_hash":         file_hash,
        })

        # עדכון פרופיל + latest_file_id
        new_profile_data = {
            "new_amounts":    df['amount'].tolist(),
            "new_merchants":  df['businessName'].tolist(),
            "new_categories": df['category'].tolist(),
            "new_currencies": df['currency'].tolist(),
            "count":          len(df),
        }
        updated_profile = merge_profile(user_profile, new_profile_data)
        updated_profile['latest_file_id'] = file_id
        profile_ref.set(updated_profile)

        return jsonify({
            "message":      f"Successfully saved {count} transactions.",
            "total":        count,
            "irregular":    irregular_count,
            "regular":      count - irregular_count,
            "removed_rows": report.removed_rows,
            "warnings":     report.warnings,
            "file_id":      file_id,
        }), 200

    except ValueError as e:
        print("UPLOAD ERROR:", str(e))
        return jsonify({"error": str(e)}), 400
    except Exception as e:
        logger.error(f"Upload error: {e}")
        return jsonify({"error": str(e)}), 500