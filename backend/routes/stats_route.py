from flask import Blueprint, request, jsonify
from flask_jwt_extended import jwt_required, get_jwt_identity
from firebase_config import db
from collections import defaultdict

stats_bp = Blueprint('stats', __name__)

MONTH_ABBRS = {'01':'Jan','02':'Feb','03':'Mar','04':'Apr','05':'May','06':'Jun',
               '07':'Jul','08':'Aug','09':'Sep','10':'Oct','11':'Nov','12':'Dec'}

def month_abbr_to_num(month):
    return {'Jan':'01','Feb':'02','Mar':'03','Apr':'04','May':'05','Jun':'06',
            'Jul':'07','Aug':'08','Sep':'09','Oct':'10','Nov':'11','Dec':'12'}.get(month,'01')

@stats_bp.route('/stats', methods=['GET'])
@jwt_required()
def get_stats():
    try:
        user_id   = get_jwt_identity()
        month     = request.args.get('month', 'Jan')
        year      = request.args.get('year', '2026')
        month_num = month_abbr_to_num(month)
        month_key = f"{year}-{month_num}"  # e.g. "2026-01"

        files_ref = db.collection('users').document(user_id).collection('files')
        file_docs = list(files_ref.stream())

        if not file_docs:
            return jsonify({
                'totalSpend': 0, 'regularTransactionsCount': 0,
                'irregularTransactionsCount': 0, 'transactions': [],
                'expensesByCategory': [], 'monthlyExpenses': []
            }), 200

        # ── 1. קריאת monthly_summary מכל הקבצים (קריאות קלות — רק metadata) ──
        monthly_totals  = defaultdict(float)
        selected_total  = 0.0
        selected_regular   = 0
        selected_irregular = 0
        selected_categories = defaultdict(float)

        for file_doc in file_docs:
            summary = file_doc.to_dict().get('monthly_summary', {})

            for mk, data in summary.items():
                # mk = "YYYY-MM"
                mk_year  = mk[:4]
                mk_month = mk[5:7]
                mk_abbr  = MONTH_ABBRS.get(mk_month, '')
                if not mk_abbr: continue

                # בר-צ'ארט — כל הקבצים לשנה המבוקשת
                if mk_year == year:
                    monthly_totals[mk_abbr] += data.get('total', 0)

                # חודש נבחר — מכל הקבצים
                if mk == month_key:
                    selected_total      += data.get('total', 0)
                    selected_regular    += data.get('regular', 0)
                    selected_irregular  += data.get('irregular', 0)
                    for cat, amt in data.get('categories', {}).items():
                        selected_categories[cat] += amt

        # ── 2. שליפת טרנזקציות לחודש הנבחר — רק מהקובץ האחרון ──
        profile_doc    = db.collection('user_profiles').document(user_id).get()
        latest_file_id = profile_doc.to_dict().get('latest_file_id') if profile_doc.exists else None

        transactions = []
        if latest_file_id:
            # בדיקה אם הקובץ האחרון מכיל נתונים לחודש הנבחר
            latest_file_doc = files_ref.document(latest_file_id).get()
            if latest_file_doc.exists:
                summary = latest_file_doc.to_dict().get('monthly_summary', {})
                if month_key in summary:
                    docs = (files_ref.document(latest_file_id)
                            .collection('transactions')
                            .stream())
                    for doc in docs:
                        t        = doc.to_dict()
                        date_str = t.get('date', '')
                        parts    = date_str.replace('/', '-').split('-')
                        if len(parts) != 3: continue
                        if len(parts[0]) == 4:
                            t_year, t_month = parts[0], parts[1]
                        else:
                            t_month, t_year = parts[1], parts[2]
                        if t_month == month_num and t_year == year:
                            transactions.append({
                                'id':          doc.id,
                                'title':       t.get('businessName', 'Unknown'),
                                'date':        date_str,
                                'amount':      float(t.get('amount', 0)),
                                'category':    t.get('category', 'Other'),
                                'isIrregular': t.get('status') == 'IRREGULAR',
                            })

        # ── 3. בניית התשובה ──
        categories = [
            {'category': cat, 'amount': round(amt, 2)}
            for cat, amt in sorted(selected_categories.items(), key=lambda x: -x[1])
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
            'totalSpend':                 round(selected_total, 2),
            'regularTransactionsCount':   selected_regular,
            'irregularTransactionsCount': selected_irregular,
            'transactions':               transactions,
            'expensesByCategory':         categories,
            'monthlyExpenses':            monthly_expenses,
        }), 200

    except Exception as e:
        import traceback
        print("STATS ERROR:", str(e))
        print(traceback.format_exc())
        return jsonify({'error': str(e)}), 500