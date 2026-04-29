from flask import Blueprint, request, jsonify
from flask_jwt_extended import jwt_required, get_jwt_identity
from firebase_config import db
from google.cloud.firestore_v1.base_query import FieldFilter
from collections import defaultdict

stats_bp = Blueprint('stats', __name__)

CATEGORY_TRANSLATION = {
    'מזון וצריכה': 'Food', 'מסעדות, קפה וברים': 'Food', 'מסעדות': 'Food',
    'Food & Grocery': 'Food', 'Restaurants & Cafes': 'Food', 'Restaurants': 'Food',
    'אופנה': 'Shopping', 'Fashion': 'Shopping', 'Shopping': 'Shopping', 'קניות': 'Shopping',
    'בריאות': 'Health', 'Health': 'Health',
    'תחבורה': 'Transport', 'Transport': 'Transport',
    'חינוך': 'Education', 'Education': 'Education',
}

def normalize_category(cat: str) -> str:
    if not cat: return 'Other'
    return CATEGORY_TRANSLATION.get(cat, 'Other')

def month_abbr_to_num(month: str) -> str:
    return {
        'Jan': '01', 'Feb': '02', 'Mar': '03', 'Apr': '04',
        'May': '05', 'Jun': '06', 'Jul': '07', 'Aug': '08',
        'Sep': '09', 'Oct': '10', 'Nov': '11', 'Dec': '12'
    }.get(month, '01')

MONTH_ABBRS = {
    '01': 'Jan', '02': 'Feb', '03': 'Mar', '04': 'Apr',
    '05': 'May', '06': 'Jun', '07': 'Jul', '08': 'Aug',
    '09': 'Sep', '10': 'Oct', '11': 'Nov', '12': 'Dec'
}

@stats_bp.route('/stats', methods=['GET'])
@jwt_required()
def get_stats():
    try:
        user_id   = get_jwt_identity()
        month     = request.args.get('month', 'Jan')
        year      = request.args.get('year', '2026')
        month_num = month_abbr_to_num(month)

        # 1. שליפת latest_file_id מהפרופיל — קריאה אחת בלבד
        profile_doc    = db.collection('user_profiles').document(user_id).get()
        latest_file_id = profile_doc.to_dict().get('latest_file_id') if profile_doc.exists else None

        # 2. שליפת טרנזקציות — מסוננות לפי file_id אם קיים
        query = db.collection('transactions').where(filter=FieldFilter('user_id', '==', user_id))
        if latest_file_id:
            query = query.where(filter=FieldFilter('file_id', '==', latest_file_id))

        transactions   = []
        monthly_totals = defaultdict(float)

        for doc in query.stream():
            t        = doc.to_dict()
            date_str = t.get('date', '')

            parts = date_str.replace('/', '-').split('-')
            if len(parts) != 3:
                continue
            if len(parts[0]) == 4:  # YYYY-MM-DD
                t_year, t_month_num = parts[0], parts[1]
            else:                   # DD-MM-YYYY
                t_month_num, t_year = parts[1], parts[2]

            t_month_abbr = MONTH_ABBRS.get(t_month_num, '')
            if not t_month_abbr:
                continue

            amount = float(t.get('amount', 0))

            # monthly_totals — רק לשנה המבוקשת
            if t_year == year:
                monthly_totals[t_month_abbr] += amount

            # טרנזקציות לחודש הנבחר בלבד
            if t_month_num == month_num and t_year == year:
                transactions.append({
                    'id':          doc.id,
                    'title':       t.get('businessName', 'Unknown'),
                    'date':        date_str,
                    'amount':      amount,
                    'category':    normalize_category(t.get('category', 'Other')),
                    'isIrregular': t.get('status') == 'IRREGULAR',
                })

        # 3. חישוב סטטיסטיקה
        total_spend     = sum(tx['amount'] for tx in transactions)
        regular_count   = sum(1 for tx in transactions if not tx['isIrregular'])
        irregular_count = sum(1 for tx in transactions if tx['isIrregular'])

        cat_totals = defaultdict(float)
        for tx in transactions:
            cat_totals[tx['category']] += tx['amount']

        categories = [
            {'category': cat, 'amount': round(amt, 2)}
            for cat, amt in sorted(cat_totals.items(), key=lambda x: -x[1])
        ]

        month_order = list(MONTH_ABBRS.values())
        monthly_expenses = [
            {'month': abbr, 'amount': round(total, 2)}
            for abbr, total in sorted(
                monthly_totals.items(),
                key=lambda x: month_order.index(x[0]) if x[0] in month_order else 99
            )
            if total > 0
        ]

        return jsonify({
            'totalSpend':                 round(total_spend, 2),
            'regularTransactionsCount':   regular_count,
            'irregularTransactionsCount': irregular_count,
            'transactions':               transactions,
            'expensesByCategory':         categories,
            'monthlyExpenses':            monthly_expenses,
        }), 200

    except Exception as e:
        import traceback
        print("STATS ERROR:", str(e))
        print(traceback.format_exc())
        return jsonify({'error': str(e)}), 500