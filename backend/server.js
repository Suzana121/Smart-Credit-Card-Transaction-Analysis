// קובץ: backend/server.js (הקובץ הראשי)

const express = require('express');
const bodyParser = require('body-parser');
const authRoutes = require('./routes/auth-routes'); // ייבוא הנתיבים

// ודא שה-Firebase Admin SDK מאותחל (ע"י ייבוא פעם אחת)
require('./db/firebase-service');

const app = express();
const PORT = process.env.PORT || 3000;

app.use(bodyParser.json());

// הגדרת הנתיבים (Routes)
app.use('/api/auth', authRoutes);


app.listen(PORT, () => {
  console.log(`Server is running on port ${PORT}`);
});