from flask import Blueprint, request, jsonify
from flask_jwt_extended import jwt_required, get_jwt_identity
from services.file_service import validate_and_process_file # השם החדש

upload_bp = Blueprint('upload', __name__)

@upload_bp.route('/upload', methods=['POST'])
@jwt_required()
def upload_file():
    # בדיקה אם הקובץ קיים בבקשה
    if 'file' not in request.files:
        return jsonify({"error": "No file part"}), 400
    
    file = request.files['file']
    
    if file.filename == '':
        return jsonify({"error": "No selected file"}), 400

    # קריאה לפונקציה המשודרגת (תומכת ב-CSV ו-Excel)
    success, result = validate_and_process_file(file)

    if not success:
        return jsonify({"error": result}), 400

    df = result # אם הצליח, ה-result הוא ה-DataFrame
    user_id = get_jwt_identity()

    # כאן מגיע השלב של השמירה ל-Firestore (ה-Batch שעשינו)
    # ... (הקוד של ה-batch commit) ...

    return jsonify({
        "message": f"Successfully processed {len(df)} transactions",
        "format": "Excel" if file.filename.lower().endswith(('.xlsx', '.xls')) else "CSV"
    }), 200