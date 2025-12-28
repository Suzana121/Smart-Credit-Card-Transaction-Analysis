
// קובץ: backend/db/firebase-service.js

const admin = require('firebase-admin');

// ********** ודאי שהנתיב לקובץ המפתח נכון **********
// אם קובץ המפתח שלך (serviceAccountKey.json) יושב ישירות ב-backend, הנתיב הוא:
const serviceAccount = require('../serviceAccountKey.json');
// ***************************************************


// הגדרות ואיפוס (Initialize) של Firebase
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
  // הערה: אין צורך ב-databaseURL עבור Cloud Firestore
});

// ייצוא המופעים של ה-DB שבהם נשתמש
const db = admin.firestore();

// ייצוא המודולים כדי שנוכל להשתמש בהם במודולים אחרים (Users, Transactions)
module.exports = {
  db,
  admin // אם תצטרכי גישה למודול ה-Auth של Firebase
};