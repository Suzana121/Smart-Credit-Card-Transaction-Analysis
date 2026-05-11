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
import re
import pandas as pd

logger = logging.getLogger(__name__)
upload_bp = Blueprint('upload', __name__)

CATEGORY_TRANSLATION = {
    # מילים מהרשימה המקורית שלך
    'שופרסל': 'Food & Grocery', 'יוחננוף': 'Food & Grocery', 'סופר': 'Food & Grocery',
    'ארומה': 'Restaurants & Cafes', 'מקדונלד': 'Restaurants', 'פיצה': 'Restaurants',
    'וולט': 'Restaurants', 'wolt': 'Restaurants', 'תן ביס': 'Restaurants',
    'זארה': 'Fashion', 'zara': 'Fashion', 'h&m': 'Fashion', 'shein': 'Fashion',
    'פארם': 'Health', 'pharm': 'Health', 'כללית': 'Health', 'מכבי': 'Health',
    'פנגו': 'Transport', 'pango': 'Transport', 'דלק': 'Transport',
}

STATS_CATEGORY_TRANSLATION = {
    'Food & Grocery': 'Food',
    'Restaurants & Cafes': 'Food',
    'Restaurants': 'Food',
    'Fashion': 'Shopping',
    'Shopping': 'Shopping',
    'Health': 'Health',
    'Transport': 'Transport',
    'Education': 'Education',
    'Travel': 'Travel',
    'Electronics': 'Shopping'
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

def _extract_month_key(date_str: str):
    """מחזיר מפתח חודשי בפורמט YYYY-MM מתוך תאריך dd-mm-yyyy או yyyy-mm-dd."""
    if not date_str:
        return None
    parts = date_str.replace('/', '-').split('-')
    if len(parts) != 3:
        return None
    if len(parts[0]) == 4:  # yyyy-mm-dd
        return f"{parts[0]}-{parts[1]}"
    else:  # dd-mm-yyyy
        return f"{parts[2]}-{parts[1]}"

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



@upload_bp.route('/sync-contacts', methods=['POST'])
@jwt_required()
def sync_contacts():
    data = request.get_json()
    raw_phones = data.get('phones', [])

    # נרמול מספרי הטלפון שמגיעים מהמכשיר
    normalized_phones = set() # שימוש ב-set לחיפוש מהיר יותר
    for p in raw_phones:
        clean_p = re.sub(r'\D', '', p)
        if clean_p.startswith('972'):
            clean_p = '0' + clean_p[3:]
        if clean_p:
            normalized_phones.add(clean_p)

    matches = []

    try:
        users_ref = db.collection('users')
        all_users = users_ref.stream()

        for user_doc in all_users:
            user_data = user_doc.to_dict()
            user_phone = str(user_data.get('phone', ''))
            clean_db_phone = re.sub(r'\D', '', user_phone)

            # בדיקה אם המשתמש קיים באנשי הקשר של השולח
            if clean_db_phone in normalized_phones:
                matches.append({
                    "id": user_doc.id,
                    "name": user_data.get('username', 'Unknown'),
                    "phone": user_phone,
                    "photo_url": user_data.get('photo_url'),
                    "is_from_contacts": True
                })

    except Exception as e:
        return jsonify({"error": str(e)}), 500

    # החזרת המשתמשים שנמצאו בלבד
    return jsonify(matches), 200
# ─────────────────────────────────────────────
# POST /api/upload
# ─────────────────────────────────────────────
MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024  # 5MB
MAX_FILES_PER_USER  = 20
ALLOWED_EXTENSIONS = {'csv', 'xlsx', 'xls'}

def allowed_file(filename):
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

def allowed_file(filename):
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

def _extract_month_key(date_str: str):
    if not date_str: return None
    parts = date_str.replace('/', '-').split('-')
    if len(parts) != 3: return None
    return f"{parts[0]}-{parts[1]}" if len(parts[0]) == 4 else f"{parts[2]}-{parts[1]}"

@upload_bp.route('/upload', methods=['POST'])
@jwt_required()
def upload_file():
    # 1. בדיקות ראשוניות של הקובץ
    if 'file' not in request.files:
        return jsonify({"error": "no_file", "message": "No file was uploaded."}), 400

    file = request.files['file']
    if file.filename == '':
        return jsonify({"error": "empty_filename", "message": "Selected file has no name."}), 400

    if not allowed_file(file.filename):
        return jsonify({
            "error": "invalid_file_type",
            "message": "Unsupported file type. Please upload Excel or CSV files only."
        }), 400

    user_id = get_jwt_identity()

    try:
        # 2. בדיקת גודל וכפילות (Hash)
        file_content = file.read()
        if len(file_content) > MAX_FILE_SIZE_BYTES:
            return jsonify({"error": "file_too_large", "message": "File is too large (Max 5MB)."}), 413

        file.seek(0)
        file_hash = hashlib.md5(file_content).hexdigest()

        existing = list(db.collection('users').document(user_id).collection('files')
                        .where(filter=FieldFilter('file_hash', '==', file_hash)).limit(1).get())
        if existing:
            return jsonify({"error": "duplicate_file", "message": "You have already uploaded this file before."}), 409

        # 3. המרה ל-DataFrame וניקוי רווחים בשמות עמודות
        df = FileService.validate_and_process_file(file)
        if df is None or df.empty:
            return jsonify({"error": "empty_data", "message": "The file is empty or contains no valid data."}), 400

        # ניקוי שמות עמודות מרווחים מיותרים
        df.columns = [col.strip() for col in df.columns]

        # 4. בדיקת "וגם" (AND) - האם כל עמודות החובה קיימות?
        required_columns = ['businessName', 'amount', 'date', 'category']
        actual_columns = df.columns.tolist()
        missing = [col for col in required_columns if col not in actual_columns]

        if missing:
            return jsonify({
                "error": "invalid_structure",
                "message": f"Invalid file structure. Missing required columns: {', '.join(missing)}"
            }), 400

        # 5. בדיקת תוכן - האם אחת מעמודות החובה קיימת אך ריקה לחלוטין?
        for col in required_columns:
            if df[col].isnull().all() or (df[col].astype(str).str.strip() == '').all():
                return jsonify({
                    "error": "empty_column",
                    "message": f"The column '{col}' is missing data. Please make sure it's not empty."
                }), 400

        # 6. וולידציה נוספת ועיבוד ML
        df, report = DataValidator.validate(df)
        if not report.is_valid():
            return jsonify({"error": "validation_failed", "message": report.errors[0]}), 400

        profile_ref = db.collection('user_profiles').document(user_id)
        profile_doc = profile_ref.get()
        user_profile = validate_profile(profile_doc.to_dict() if profile_doc.exists else {})

        ml = MLService.get_instance()
        classified_df = ml.classify_transactions(df, user_profile)
        if isinstance(classified_df, list):
            classified_df = pd.DataFrame(classified_df)

        # 7. שמירה ב-Batch (עסקאות + מטא-דאטה + עדכון פרופיל)
        batch = db.batch()
        file_id = str(uuid.uuid4())

        # נתיב לשמירת הטרנזקציות
        tx_collection_ref = db.collection('users').document(user_id).collection('files').document(file_id).collection('transactions')

        count = 0
        irregular_count = 0
        for _, row in classified_df.iterrows():
            is_irr = (row.get('status') == 'IRREGULAR')
            batch.set(tx_collection_ref.document(), {
                "businessName": str(row['businessName']),
                "amount": float(row['amount']),
                "date": str(row['date']),
                "category": str(row['category']),
                "status": row.get('status', 'REGULAR'),
                "is_irregular": is_irr,
                "created_at": firestore.SERVER_TIMESTAMP
            })
            count += 1
            if is_irr: irregular_count += 1

        # שמירת פרטי הקובץ
        file_meta_ref = db.collection('users').document(user_id).collection('files').document(file_id)
        batch.set(file_meta_ref, {
            "file_name": file.filename,
            "transaction_count": count,
            "irregular_count": irregular_count,
            "uploaded_at": firestore.SERVER_TIMESTAMP,
            "file_hash": file_hash
        })

        # עדכון הפרופיל כדי שהאפליקציה תדע למשוך את הקובץ האחרון (פותר את בעיית ה-200 ללא נתונים)
        batch.set(profile_ref, {"latest_file_id": file_id}, merge=True)

        # ביצוע סופי של כל השמירות
        batch.commit()

        return jsonify({
            "message": f"Successfully processed {count} transactions.",
            "file_id": file_id,
            "total": count,
            "irregular": irregular_count
        }), 200

    except Exception as e:
        logger.error(f"Upload error: {str(e)}")
        return jsonify({"error": "server_error", "message": "An error occurred during processing."}), 500