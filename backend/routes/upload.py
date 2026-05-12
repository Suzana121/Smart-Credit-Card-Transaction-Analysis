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

logger = logging.getLogger(__name__)
upload_bp = Blueprint('upload', __name__)

MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024  # 5MB
ALLOWED_EXTENSIONS  = {'csv', 'xlsx', 'xls'}

def allowed_file(filename):
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

# ─── Dictionaries ───
CATEGORY_TRANSLATION = {
    'מזון וצריכה': 'Food & Grocery', 'מסעדות, קפה וברים': 'Restaurants & Cafes',
    'מסעדות': 'Restaurants', 'אופנה': 'Fashion', 'בריאות': 'Health',
    'תחבורה': 'Transport', 'חינוך': 'Education', 'שירותי תקשורת': 'Telecommunications',
    'עירייה וממשלה': 'Government', 'שונות': 'Other', 'כללי': 'General',
    'בידור': 'Entertainment', 'ביטוח': 'Insurance', 'בנקאות ופיננסים': 'Finance',
    'רכב': 'Automotive', 'מחשבים ואלקטרוניקה': 'Electronics', 'ספורט': 'Sports',
    'תיירות ונסיעות': 'Travel', 'קניות': 'Shopping',
}

BUSINESS_CATEGORY_KEYWORDS = {
    'Food': [
        'מסעדה', 'מקדונלד', 'mcdonalds', "mcdonald's", 'קפה', 'cafe', 'coffee',
        'פיצה', 'pizza', 'סופר', 'super', 'ארומה', 'aroma', 'וולט', 'wolt',
        'תן ביס', 'tenbishvil', '10bis', 'שופרסל', 'shufersal', 'יוחננוף',
        'רמי לוי', 'rami levy', 'קונדיטוריה', 'המבורגר', 'burger', 'בורגר',
        'שווארמה', 'פלאפל', 'סושי', 'sushi', 'סטייק', 'steak', 'מאפייה',
        'bakery', 'אסם', 'תנובה', 'osher ad', 'אושר עד', 'victory', 'ויקטורי',
        'mega', 'מגא', 'co-op', 'cofix', 'קופיקס', 'kfc', 'burger king',
        'subway', 'dominos', 'דומינוס', 'pasta', 'פסטה',
    ],
    'Health': [
        'פארם', 'pharm', 'מרפאה', 'clinic', 'כללית', 'מכבי', 'doctor',
        'בי פארם', 'be pharm', 'bepharm', 'בית מרקחת', 'גוד פארם', 'goodpharm',
        'pharmacy', 'רופא', 'שיניים', 'dental', 'אופטיקה', 'optica',
        'לאומית', 'הדסה', 'איכילוב', 'רמב"ם', 'סורוקה', 'בית חולים',
        'hospital', 'super-pharm', 'superpharm',
    ],
    'Shopping': [
        'זארה', 'zara', 'h&m', 'hm', 'אמזון', 'amazon', 'ksp',
        'אייבורי', 'ivory', 'עזריאלי', 'azrieli', 'shein', 'שיין',
        'הלבשה', 'ביגוד', 'נעליים', 'shoes', 'adidas', 'nike', 'reebok',
        'castro', 'קסטרו', 'renuar', 'רנואר', 'golf', 'גולף', 'terminal x',
        'terminalx', 'fox', 'פוקס', 'pull&bear', 'mango', 'מנגו',
        'bershka', 'next', 'קינג סטור', 'king store', 'toys', 'צעצועים',
        'ikea', 'איקאה', 'ace', 'home center', 'homecenter',
    ],
    'Transport': [
        'דלק', 'fuel', 'פז', 'paz', 'סונול', 'sonol', 'דור אלון', 'doralon',
        'רכבת', 'train', 'אוטובוס', 'bus', 'מונית', 'taxi', 'gett', 'gettaxi',
        'פנגו', 'pango', 'רב-קו', 'ravkav', 'אל על', 'elal', 'el al',
        'ישראייר', 'israir', 'arkia', 'ארקיע', 'parking', 'חניה',
        'electric', 'אופניים', 'bird', 'lime', 'לאנטו', 'transit',
    ],
    'Education': [
        'אוניברסיטה', 'university', 'college', 'טכניון', 'technion',
        'לימודים', 'המכללה', 'קורס', 'course', 'udemy', 'coursera',
        'מכון', 'בית ספר', 'school', 'גן ילדים', 'kindergarten',
        'תלמוד תורה', 'ישיבה', 'seminar', 'סמינר',
    ],
    'Entertainment': [
        'סינמה', 'cinema', 'yes', 'hot', 'netflix', 'spotify', 'apple',
        'google play', 'steam', 'playstation', 'xbox', 'בילוי', 'פנאי',
        'bowling', 'escape', 'קולנוע', 'תיאטרון', 'theater', 'concert',
        'הופעה', 'מוזיאון', 'museum', 'zoo', 'גן חיות',
    ],
    'Finance': [
        'ביטוח', 'insurance', 'בנק', 'bank', 'פנסיה', 'pension',
        'קרן השתלמות', 'הלוואה', 'loan', 'ריבית', 'interest',
        'ויזה', 'visa', 'mastercard', 'paypal', 'bit', 'ביט', 'pepper',
    ],
    'Telecommunications': [
        'סלקום', 'cellcom', 'פרטנר', 'partner', 'הוט מובייל', 'hot mobile',
        'רמי', '012', '013', 'bezeq', 'בזק', 'יס', 'internet',
        'אינטרנט', 'סלולר', 'cellular', 'mobile',
    ],
}

STATS_CATEGORY_TRANSLATION = {
    'Food & Grocery': 'Food', 'Restaurants & Cafes': 'Food', 'Restaurants': 'Food',
    'Fashion': 'Shopping', 'Shopping': 'Shopping',
    'Health': 'Health', 'Transport': 'Transport', 'Education': 'Education',
    'Entertainment': 'Entertainment', 'Finance': 'Finance',
    'Telecommunications': 'Telecommunications', 'Automotive': 'Transport',
    'Travel': 'Transport', 'Electronics': 'Shopping', 'Sports': 'Shopping',
    'Insurance': 'Finance', 'Government': 'Other', 'General': 'Other',
}


# ─── Helper functions ───
def translate_category(cat):
    if not cat: return 'General'
    return CATEGORY_TRANSLATION.get(cat,
                                    cat if not any(ord(c) > 127 for c in str(cat)) else 'General')


def classify_by_business_name(business_name: str):
    if not business_name:
        return None
    name_lower = business_name.lower()
    for category, keywords in BUSINESS_CATEGORY_KEYWORDS.items():
        for kw in keywords:
            if kw.lower() in name_lower:
                return category
    return None


def normalize_stats_category(cat: str, business_name: str = '') -> str:
    if not cat:
        return classify_by_business_name(business_name) or 'Other'
    if cat in ('Other', 'General', 'other', 'general'):
        return classify_by_business_name(business_name) or 'Other'
    return STATS_CATEGORY_TRANSLATION.get(cat, cat)


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
        status  = request.args.get('status', None)

        if not file_id:
            profile_doc = db.collection('user_profiles').document(user_id).get()
            if profile_doc.exists:
                file_id = profile_doc.to_dict().get('latest_file_id')

        if not file_id:
            return jsonify({"transactions": [], "nextCursor": None, "hasMore": False}), 200

        col = txn_col(user_id, file_id)

        if status:
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
        updated       = doc_ref.get().to_dict()
        updated['id'] = transaction_id
        return jsonify(updated), 200
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@upload_bp.route('/transactions/filter-options', methods=['GET'])
@jwt_required()
def get_filter_options():
    try:
        user_id = get_jwt_identity()
        file_id = request.args.get('file_id', None)

        if not file_id:
            profile_doc = db.collection('user_profiles').document(user_id).get()
            if profile_doc.exists:
                file_id = profile_doc.to_dict().get('latest_file_id')

        if not file_id:
            return jsonify({"categories": [], "minAmount": 0, "maxAmount": 5000}), 200

        col  = txn_col(user_id, file_id)
        docs = col.stream()

        categories = set()
        amounts    = []

        for doc in docs:
            t = doc.to_dict()
            cat = t.get('category', '')
            if cat:
                categories.add(cat)
            amt = t.get('amount')
            if isinstance(amt, (int, float)):
                amounts.append(amt)

        return jsonify({
            "categories": sorted(list(categories)),
            "minAmount":  round(min(amounts), 2) if amounts else 0,
            "maxAmount":  round(max(amounts), 2) if amounts else 5000,
        }), 200
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
# POST /api/sync-contacts
# ─────────────────────────────────────────────
@upload_bp.route('/sync-contacts', methods=['POST'])
@jwt_required()
def sync_contacts():
    data       = request.get_json()
    raw_phones = data.get('phones', [])

    normalized_phones = set()
    for p in raw_phones:
        clean_p = re.sub(r'\D', '', p)
        if clean_p.startswith('972'):
            clean_p = '0' + clean_p[3:]
        if clean_p:
            normalized_phones.add(clean_p)

    matches = []
    try:
        for user_doc in db.collection('users').stream():
            user_data      = user_doc.to_dict()
            user_phone     = str(user_data.get('phone', ''))
            clean_db_phone = re.sub(r'\D', '', user_phone)
            if clean_db_phone in normalized_phones:
                matches.append({
                    "id":               user_doc.id,
                    "name":             user_data.get('username', 'Unknown'),
                    "phone":            user_phone,
                    "photo_url":        user_data.get('photo_url'),
                    "is_from_contacts": True
                })
    except Exception as e:
        return jsonify({"error": str(e)}), 500

    return jsonify(matches), 200


# ─────────────────────────────────────────────
# POST /api/upload
# ─────────────────────────────────────────────
@upload_bp.route('/upload', methods=['POST'])
@jwt_required()
def upload_file():
    if 'file' not in request.files:
        return jsonify({"error": "no_file", "message": "No file was uploaded."}), 400

    file = request.files['file']
    if file.filename == '':
        return jsonify({"error": "empty_filename", "message": "Selected file has no name."}), 400

    if not allowed_file(file.filename):
        return jsonify({
            "error":   "invalid_file_type",
            "message": "Unsupported file type. Please upload Excel or CSV files only."
        }), 400

    user_id = get_jwt_identity()

    try:
        original_filename = file.filename or 'unknown'

        # ── בדיקת גודל וכפילות ──
        file_content = file.read()
        if len(file_content) > MAX_FILE_SIZE_BYTES:
            return jsonify({"error": "file_too_large", "message": "File is too large (Max 5MB)."}), 413

        file_hash = hashlib.md5(file_content).hexdigest()
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
        if df is None or df.empty:
            return jsonify({"error": "empty_data", "message": "The file is empty or contains no valid data."}), 400

        df.columns = [col.strip() for col in df.columns]

        required_columns = ['businessName', 'amount', 'date', 'category']
        missing = [col for col in required_columns if col not in df.columns.tolist()]
        if missing:
            return jsonify({
                "error":   "invalid_structure",
                "message": f"Missing required columns: {', '.join(missing)}"
            }), 400

        for col in required_columns:
            if df[col].isnull().all() or (df[col].astype(str).str.strip() == '').all():
                return jsonify({
                    "error":   "empty_column",
                    "message": f"The column '{col}' is missing data."
                }), 400

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

            date_str = txn.get('date', '')
            parts    = date_str.replace('/', '-').split('-')
            if len(parts) == 3:
                if len(parts[0]) == 4:
                    month_key = f"{parts[0]}-{parts[1]}"
                else:
                    month_key = f"{parts[2]}-{parts[1]}"
                stats_cat = normalize_stats_category(translated_cat, txn.get('businessName', ''))
                monthly_summary[month_key]['total']                += txn['amount']
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

        summary_serializable = {}
        for month_key, data in monthly_summary.items():
            summary_serializable[month_key] = {
                'total':      round(data['total'], 2),
                'regular':    data['regular'],
                'irregular':  data['irregular'],
                'categories': {k: round(v, 2) for k, v in data['categories'].items()}
            }

        user_files_ref(user_id).document(file_id).set({
            "file_name":         original_filename,
            "transaction_count": count,
            "irregular_count":   irregular_count,
            "uploaded_at":       firestore.SERVER_TIMESTAMP,
            "monthly_summary":   summary_serializable,
            "file_hash":         file_hash,
        })

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