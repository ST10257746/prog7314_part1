const admin = require('firebase-admin');
const fs = require('fs');
const dotenv = require('dotenv');

if (fs.existsSync('.env')) {
  dotenv.config();
} else if (fs.existsSync('.env.local')) {
  dotenv.config({ path: '.env.local' });
} else {
  dotenv.config();
}

// Initialize Firebase Admin SDK
// Option 1: Using service account key file (recommended for local dev)
// Option 2: Using environment variables (recommended for deployment)

let firebaseApp;

try {
  // Check if service account key file exists
  const serviceAccount = require('../firebase-admin-key.json');
  
  firebaseApp = admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
  });
  
  console.log('✅ Firebase Admin initialized with service account key');
} catch (error) {
  // Fallback to environment variables
  if (process.env.FIREBASE_PROJECT_ID && 
      process.env.FIREBASE_PRIVATE_KEY && 
      process.env.FIREBASE_CLIENT_EMAIL) {
    
    firebaseApp = admin.initializeApp({
      credential: admin.credential.cert({
        projectId: process.env.FIREBASE_PROJECT_ID,
        privateKey: process.env.FIREBASE_PRIVATE_KEY.replace(/\\n/g, '\n'),
        clientEmail: process.env.FIREBASE_CLIENT_EMAIL
      })
    });
    
    console.log('✅ Firebase Admin initialized with environment variables');
  } else {
    console.error('❌ Firebase Admin initialization failed: No credentials found');
    throw new Error('Firebase credentials not configured');
  }
}

const db = admin.firestore();
const auth = admin.auth();

module.exports = { admin, db, auth };

