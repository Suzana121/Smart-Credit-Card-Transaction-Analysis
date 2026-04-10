from firebase_config import db

def get_user_upload_history(user_id):
    """שולף את רשימת 6 הקבצים האחרונים ללא העסקאות שבתוכם (כדי לחסוך בנתונים)"""
    uploads_ref = db.collection("users").document(user_id).collection("uploads")
    docs = uploads_ref.order_by("timestamp", direction=db.DESCENDING).limit(6).get()
    
    history = []
    for doc in docs:
        data = doc.to_dict()
        # אנחנו מחזירים רק מטא-דאטה לרשימה הראשית
        history.append({
            "uploadId": data.get("uploadId"),
            "fileName": data.get("fileName"),
            "timestamp": data.get("timestamp"),
            "hasAnomaly": data.get("hasAnomaly")
        })
    return history