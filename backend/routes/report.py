"""
report.py - PDF report with full Hebrew support
Requirements:
    pip install python-bidi arabic-reshaper
    fonts/DavidLibre-Regular.ttf (or any Hebrew TTF font)
"""

from flask import Blueprint, make_response
from flask_jwt_extended import jwt_required, get_jwt_identity
from firebase_config import db
from reportlab.lib.pagesizes import A4
from reportlab.lib import colors
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.units import cm
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, HRFlowable
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from io import BytesIO
from datetime import datetime
import os
import logging

logger = logging.getLogger(__name__)
report_bp = Blueprint('report', __name__)

# ── Colors ──────────────────────────────────────
TEAL       = colors.HexColor('#006769')
RED        = colors.HexColor('#E23125')
GREEN      = colors.HexColor('#38D325')
LIGHT_GRAY = colors.HexColor('#F5F5F5')
DARK_GRAY  = colors.HexColor('#333333')
LIGHT_RED  = colors.HexColor('#FDECEA')

# ── Hebrew font setup ────────────────────────────
HEBREW_FONT = 'Helvetica'          # fallback
HEBREW_FONT_BOLD = 'Helvetica-Bold'

def _setup_hebrew_font():
    """טוען פונט עברי אם קיים."""
    global HEBREW_FONT, HEBREW_FONT_BOLD
    font_path      = os.path.join(os.path.dirname(__file__), '..', 'fonts', 'DavidLibre-Regular.ttf')
    font_bold_path = os.path.join(os.path.dirname(__file__), '..', 'fonts', 'DavidLibre-Bold.ttf')
    try:
        if os.path.exists(font_path):
            pdfmetrics.registerFont(TTFont('Hebrew', font_path))
            HEBREW_FONT = 'Hebrew'
            logger.info("Hebrew font loaded successfully")
        if os.path.exists(font_bold_path):
            pdfmetrics.registerFont(TTFont('HebrewBold', font_bold_path))
            HEBREW_FONT_BOLD = 'HebrewBold'
    except Exception as e:
        logger.warning(f"Hebrew font not loaded, using fallback: {e}")

_setup_hebrew_font()

# ── Hebrew text helper ───────────────────────────
def has_hebrew(text):
    return any('\u0590' <= c <= '\u05FF' for c in str(text))

def smart_text(text):
    """מחזיר טקסט נכון - עברית עם bidi+reshape, אנגלית כמו שהיא."""
    if not text:
        return 'Unknown'
    s = str(text).strip()
    if not has_hebrew(s):
        return s
    try:
        import arabic_reshaper
        from bidi.algorithm import get_display
        reshaped = arabic_reshaper.reshape(s)
        return get_display(reshaped)
    except Exception:
        # fallback - הצג רק חלק אנגלי אם קיים
        english = ' '.join(w for w in s.split() if all(ord(c) < 128 for c in w) and len(w) > 1)
        return english if english else f'Business #{abs(hash(s)) % 9999}'

# ── Category translation ─────────────────────────
CATEGORY_TRANSLATION = {
    'מזון וצריכה': 'Food & Grocery', 'מסעדות, קפה וברים': 'Restaurants & Cafes',
    'מסעדות': 'Restaurants', 'אופנה': 'Fashion', 'בריאות': 'Health',
    'תחבורה': 'Transport', 'חינוך': 'Education', 'שירותי תקשורת': 'Telecommunications',
    'עירייה וממשלה': 'Government', 'שונות': 'Other', 'כללי': 'General',
    'בידור': 'Entertainment', 'ביטוח': 'Insurance', 'בנקאות ופיננסים': 'Finance',
    'רכב': 'Automotive', 'מחשבים ואלקטרוניקה': 'Electronics', 'ספורט': 'Sports',
    'תיירות ונסיעות': 'Travel', 'קניות': 'Shopping',
}

def translate_cat(cat):
    if not cat: return 'General'
    return CATEGORY_TRANSLATION.get(cat,
                                    cat if not any(ord(c) > 127 for c in str(cat)) else 'General')


# ── Report endpoint ──────────────────────────────
@report_bp.route('/report', methods=['GET'])
@jwt_required()
def generate_report():
    user_id = get_jwt_identity()
    try:
        txn_docs     = db.collection('transactions').where('user_id', '==', user_id).stream()
        transactions = [doc.to_dict() for doc in txn_docs]
        user_doc     = db.collection('users').document(user_id).get()
        username     = user_doc.to_dict().get('username', 'User') if user_doc.exists else 'User'

        total     = len(transactions)
        irregular = [t for t in transactions if t.get('status') == 'IRREGULAR']
        regular   = total - len(irregular)

        buffer = BytesIO()
        doc    = SimpleDocTemplate(buffer, pagesize=A4,
                                   rightMargin=2*cm, leftMargin=2*cm,
                                   topMargin=2*cm, bottomMargin=2*cm)
        styles = getSampleStyleSheet()
        story  = []

        # ── Styles ──
        h1_style = ParagraphStyle('h1', parent=styles['Title'],
                                  textColor=TEAL, fontSize=20, fontName=HEBREW_FONT_BOLD, spaceAfter=4)
        h2_style = ParagraphStyle('h2', parent=styles['Normal'],
                                  textColor=TEAL, fontSize=13, fontName=HEBREW_FONT_BOLD, spaceAfter=6, spaceBefore=8)
        h2_red   = ParagraphStyle('h2r', parent=h2_style, textColor=RED)
        body_style = ParagraphStyle('body', parent=styles['Normal'],
                                    fontSize=9, textColor=DARK_GRAY, fontName=HEBREW_FONT, spaceAfter=3)
        small_style = ParagraphStyle('small', parent=styles['Normal'],
                                     fontSize=8, textColor=colors.gray, fontName=HEBREW_FONT)

        def hpara(text, style):
            """Paragraph עם טיפול נכון בעברית."""
            return Paragraph(smart_text(text) if has_hebrew(text) else text, style)

        # ── Header ──
        story += [
            Paragraph("Cardify - Transaction Report", h1_style),
            Paragraph(f"Account: {username}  |  Date: {datetime.now().strftime('%d/%m/%Y %H:%M')}",
                      small_style),
            HRFlowable(width="100%", thickness=2, color=TEAL, spaceAfter=12),
        ]

        # ── Summary ──
        summary = Table(
            [['Total', 'Regular', 'Irregular', 'Total Spend'],
             [str(total), str(regular), str(len(irregular)),
              f"ILS {sum(float(t.get('amount',0)) for t in transactions):,.2f}"]],
            colWidths=[4.2*cm]*4
        )
        summary.setStyle(TableStyle([
            ('BACKGROUND',(0,0),(-1,0),TEAL),('TEXTCOLOR',(0,0),(-1,0),colors.white),
            ('FONTNAME',(0,0),(-1,0),HEBREW_FONT_BOLD),('FONTSIZE',(0,0),(-1,-1),10),
            ('ALIGN',(0,0),(-1,-1),'CENTER'),('ROWHEIGHT',(0,0),(-1,-1),26),
            ('BACKGROUND',(0,1),(-1,1),LIGHT_GRAY),
            ('BACKGROUND',(2,1),(2,1),LIGHT_RED),('TEXTCOLOR',(2,1),(2,1),RED),
            ('FONTNAME',(2,1),(2,1),HEBREW_FONT_BOLD),
            ('GRID',(0,0),(-1,-1),0.5,colors.lightgrey),
        ]))
        story += [summary, Spacer(1, 16)]

        # ── All transactions ──
        story += [
            HRFlowable(width="100%", thickness=1, color=colors.lightgrey, spaceAfter=6),
            Paragraph("All Transactions", h2_style),
        ]

        rows = [['Business', 'Amount', 'Date', 'Category', 'Status']]
        for t in sorted(transactions, key=lambda x: x.get('date',''), reverse=True):
            biz_raw = t.get('businessName','Unknown')
            rows.append([
                smart_text(biz_raw)[:28],
                f"{float(t.get('amount',0)):,.2f}",
                t.get('date',''),
                translate_cat(t.get('category','General'))[:16],
                t.get('status','REGULAR'),
            ])

        irregular_indices = [
            i+1 for i, t in enumerate(
                sorted(transactions, key=lambda x: x.get('date',''), reverse=True))
            if t.get('status') == 'IRREGULAR'
        ]
        row_colors = []
        for idx in irregular_indices:
            row_colors += [
                ('TEXTCOLOR',(4,idx),(4,idx),RED),
                ('FONTNAME',(4,idx),(4,idx),HEBREW_FONT_BOLD),
                ('BACKGROUND',(0,idx),(-1,idx),LIGHT_RED),
            ]

        all_t = Table(rows, colWidths=[5.2*cm, 2.5*cm, 2.4*cm, 3.2*cm, 2.2*cm])
        all_t.setStyle(TableStyle([
                                      ('BACKGROUND',(0,0),(-1,0),TEAL),('TEXTCOLOR',(0,0),(-1,0),colors.white),
                                      ('FONTNAME',(0,0),(-1,0),HEBREW_FONT_BOLD),('FONTSIZE',(0,0),(-1,-1),7.5),
                                      ('ALIGN',(0,0),(-1,-1),'LEFT'),('ALIGN',(1,0),(1,-1),'RIGHT'),
                                      ('ALIGN',(4,0),(4,-1),'CENTER'),('ROWHEIGHT',(0,0),(-1,-1),16),
                                      ('ROWBACKGROUNDS',(0,1),(-1,-1),[colors.white, LIGHT_GRAY]),
                                      ('GRID',(0,0),(-1,-1),0.3,colors.lightgrey),
                                      ('LEFTPADDING',(0,0),(-1,-1),4),
                                      ('FONTNAME',(0,1),(-1,-1),HEBREW_FONT),
                                  ] + row_colors))
        story.append(all_t)

        # ── Footer ──
        story += [
            Spacer(1, 20),
            HRFlowable(width="100%", thickness=1, color=TEAL),
            Paragraph("Generated by Cardify | Smart Credit Card Transaction Analysis",
                      ParagraphStyle('footer', parent=styles['Normal'],
                                     textColor=colors.gray, fontSize=8, alignment=1))
        ]

        doc.build(story)
        pdf  = buffer.getvalue()
        buffer.close()

        resp = make_response(pdf)
        resp.headers['Content-Type']        = 'application/pdf'
        resp.headers['Content-Disposition'] = \
            f'attachment; filename="cardify_report_{datetime.now().strftime("%Y%m%d")}.pdf"'
        return resp

    except Exception as e:
        logger.error(f"Report error: {e}")
        return {"error": str(e)}, 500