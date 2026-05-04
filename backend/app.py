from dotenv import load_dotenv
load_dotenv()  # חייב להיות לפני שאר ה-imports!

from flask import Flask, jsonify
from flask_jwt_extended import JWTManager
from routes.upload import upload_bp
from routes.report import report_bp
from routes.stats_route import stats_bp
from routes.chat_route import chat_bp
from auth import auth_bp
from services.ml_service import MLService
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = Flask(__name__)
app.config["JWT_SECRET_KEY"] = "YOUR_SUPER_SECRET_KEY"
jwt = JWTManager(app)

try:
    MLService.get_instance()
    logger.info("ML model loaded successfully at startup")
except Exception as e:
    logger.warning(f"ML model not loaded: {e}")

app.register_blueprint(auth_bp,   url_prefix="/auth")
app.register_blueprint(upload_bp, url_prefix="/api")
app.register_blueprint(report_bp, url_prefix="/api")
app.register_blueprint(stats_bp,  url_prefix="/api")
app.register_blueprint(chat_bp,   url_prefix="/api")


@app.route("/")
def home():
    return jsonify({"message": "Welcome to Cardify API"})

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5001, debug=True)