from flask import Flask, jsonify
from flask_jwt_extended import JWTManager
from routes.upload import upload_bp
from routes.report import report_bp
from auth import auth_bp
from services.ml_service import MLService
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = Flask(__name__)

# Secret key for JWT
app.config["JWT_SECRET_KEY"] = "YOUR_SUPER_SECRET_KEY"

# אתחול JWT
jwt = JWTManager(app)

# טעינת מודל ה-ML פעם אחת ב-startup
try:
    MLService.get_instance()
    logger.info("ML model loaded successfully at startup")
except Exception as e:
    logger.warning(f"ML model not loaded (run train_model.py first): {e}")

# רישום blueprints
app.register_blueprint(auth_bp,    url_prefix="/auth")
app.register_blueprint(upload_bp,  url_prefix="/api")
app.register_blueprint(report_bp,  url_prefix="/api")

@app.route("/")
def home():
    return jsonify({"message": "Welcome to Smart Credit Card Transaction Analysis API"})

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5001, debug=True)