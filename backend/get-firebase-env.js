#!/usr/bin/env node

/**
 * Helper script to extract Firebase credentials for Railway environment variables
 * Run: node get-firebase-env.js
 */

const fs = require('fs');
const path = require('path');

try {
  const keyPath = path.join(__dirname, 'firebase-admin-key.json');
  
  if (!fs.existsSync(keyPath)) {
    console.error('❌ firebase-admin-key.json not found!');
    console.log('Make sure the file exists in the backend directory.');
    process.exit(1);
  }

  const serviceAccount = require('./firebase-admin-key.json');

  console.log('\n🔥 Firebase Environment Variables for Railway\n');
  console.log('Copy these to Railway > Variables tab:\n');
  console.log('─'.repeat(60));
  
  console.log('\n📝 FIREBASE_PROJECT_ID:');
  console.log(serviceAccount.project_id);
  
  console.log('\n📝 FIREBASE_CLIENT_EMAIL:');
  console.log(serviceAccount.client_email);
  
  console.log('\n📝 FIREBASE_PRIVATE_KEY:');
  console.log('(Copy the entire value below, including quotes)');
  console.log(JSON.stringify(serviceAccount.private_key));
  
  console.log('\n─'.repeat(60));
  console.log('\n✅ Done! Copy these values to Railway.\n');
  
  // Also create a .env file for local testing
  const envContent = `PORT=3000

# Firebase Admin SDK Credentials
FIREBASE_PROJECT_ID=${serviceAccount.project_id}
FIREBASE_PRIVATE_KEY=${JSON.stringify(serviceAccount.private_key)}
FIREBASE_CLIENT_EMAIL=${serviceAccount.client_email}

# CORS Settings
ALLOWED_ORIGINS=*
`;

  fs.writeFileSync('.env.local', envContent);
  console.log('💾 Also saved to .env.local for reference\n');

} catch (error) {
  console.error('❌ Error:', error.message);
  process.exit(1);
}

