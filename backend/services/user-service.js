// ייבוא מופע ה-DB המאותחל והספרייה bcrypt להצפנת סיסמאות
const { db } = require('../db/firebase-service');
const bcrypt = require('bcrypt'); // משמש להצפנה ואימות סיסמאות

// הגדרת האוסף שבו נעבוד
const usersCollection = db.collection('users');

/**
 * מנסה למצוא משתמש לפי כתובת המייל שלו.
 * נדרש לשאילתה ב-Firestore מכיוון שהמייל אינו ה-ID של המסמך.
 *
 * @param {string} email - כתובת המייל של המשתמש
 * @returns {Promise<Object | null>} - אובייקט המשתמש (כולל id) או null אם לא נמצא
 */
const findUserByEmail = async (email) => {
  try {
    // יצירת שאילתה: חפש במסמכים שבהם שדה 'email' שווה ל-email המבוקש.
    const snapshot = await usersCollection.where('email', '==', email).limit(1).get();

    if (snapshot.empty) {
      return null; // לא נמצא משתמש
    }

    // מחזיר את המסמך הראשון שנמצא
    const doc = snapshot.docs[0];
    return {
      id: doc.id,
      ...doc.data()
    };
  } catch (error) {
    console.error("Error finding user by email:", error);
    throw new Error("Database error during user search.");
  }
};

/**
 * יוצר משתמש חדש במסד הנתונים.
 *
 * @param {string} email - כתובת המייל של המשתמש
 * @param {string} hashedPassword - הסיסמה המוצפנת (כבר עברה גיבוב ב-route)
 * @returns {Promise<string>} - ה-ID של המשתמש החדש שנוצר
 */
const createUser = async (email, hashedPassword) => {
  try {
    // שימוש בפונקציית .add() כדי לתת ל-Firestore ליצור ID אוטומטי
    const newUserRef = await usersCollection.add({
      email: email,
      password: hashedPassword, 
      createdAt: new Date().toISOString()
    });

    return newUserRef.id;
  } catch (error) {
    console.error("Error creating user:", error);
    throw new Error("Database error during user creation.");
  }
};

/**
 * מנסה לאמת משתמש עבור כניסה.
 * משווה את הסיסמה שסופקה מול הסיסמה המוצפנת ב-DB.
 *
 * @param {string} email - כתובת המייל
 * @param {string} password - הסיסמה הגולמית שהוזנה על ידי המשתמש
 * @returns {Promise<Object | null>} - אובייקט המשתמש ללא סיסמה או null אם האימות נכשל
 */
const authenticateUser = async (email, password) => {
  const user = await findUserByEmail(email);

  if (!user) {
    return null; // משתמש לא קיים
  }

  // השוואת הסיסמה המוצפנת ב-DB (user.password) מול הסיסמה הגולמית שהוזנה
  const isMatch = await bcrypt.compare(password, user.password); 

  if (isMatch) {
    // החזרת האובייקט של המשתמש (מוציא את הסיסמה המוצפנת למען אבטחה)
    const { password, ...userWithoutPassword } = user;
    return userWithoutPassword;
  }

  return null; // אימות נכשל
};


module.exports = {
  findUserByEmail,
  createUser,
  authenticateUser
};