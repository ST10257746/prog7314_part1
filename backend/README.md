# FitTrackr REST API Backend

A Node.js/Express REST API backend for the FitTrackr fitness tracking Android application. This API connects to Firebase Firestore for data storage and Firebase Auth for authentication.

## 🚀 Features

- **User Management**: Registration, login, profile updates
- **Workout Tracking**: Create, read, update, delete workout sessions
- **Goals Management**: Set and track fitness goals
- **Progress Tracking**: Monitor weight, BMI, and body composition over time
- **Nutrition Logging**: Track meals and daily nutrition intake
- **Firebase Integration**: Uses Firebase Admin SDK for auth and Firestore

## 📋 Prerequisites

- Node.js (v18 or higher)
- Firebase project with Firestore enabled
- Firebase Admin SDK service account key

## 🛠️ Setup Instructions

### 1. Install Dependencies

```bash
cd backend
npm install
```

### 2. Firebase Configuration

#### Option A: Using Service Account Key (Recommended for Local Development)

1. Go to Firebase Console → Project Settings → Service Accounts
2. Click "Generate New Private Key"
3. Save the JSON file as `firebase-admin-key.json` in the `backend` folder
4. **Important**: This file is gitignored - never commit it!

#### Option B: Using Environment Variables (Recommended for Deployment)

1. Create a `.env` file in the `backend` folder:

```env
PORT=3000
FIREBASE_PROJECT_ID=your-project-id
FIREBASE_PRIVATE_KEY="-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----\n"
FIREBASE_CLIENT_EMAIL=firebase-adminsdk-xxxxx@your-project.iam.gserviceaccount.com
```

### 3. Run the Server

**Development mode (with auto-reload):**
```bash
npm run dev
```

**Production mode:**
```bash
npm start
```

The server will run on `http://localhost:3000` by default.

## 📡 API Endpoints

### Health Check
- `GET /` - API status
- `GET /api/health` - Health check with uptime

### User Routes (`/api/users`)
- `POST /api/users/register` - Register new user
- `POST /api/users/login` - Login user (requires auth token)
- `GET /api/users/:userId` - Get user profile (requires auth)
- `PUT /api/users/:userId` - Update user profile (requires auth)
- `DELETE /api/users/:userId` - Delete user account (requires auth)

### Workout Routes (`/api/workouts`)
- `GET /api/workouts` - Get all workouts for authenticated user
- `GET /api/workouts/:workoutId` - Get specific workout
- `POST /api/workouts` - Create new workout session
- `PUT /api/workouts/:workoutId` - Update workout
- `DELETE /api/workouts/:workoutId` - Delete workout
- `GET /api/workouts/stats/summary` - Get workout statistics

### Goals Routes (`/api/goals`)
- `GET /api/goals/:userId` - Get all goals for user
- `GET /api/goals/user/:userId/active` - Get active goals
- `POST /api/goals` - Create new goal
- `PUT /api/goals/:goalId` - Update goal
- `PUT /api/goals/:goalId/progress` - Update goal progress
- `DELETE /api/goals/:goalId` - Delete goal

### Progress Routes (`/api/progress`)
- `GET /api/progress/:userId` - Get progress entries
- `GET /api/progress/:userId/latest` - Get latest progress entry
- `GET /api/progress/:userId/summary` - Get progress summary statistics
- `POST /api/progress` - Create new progress entry
- `PUT /api/progress/:progressId` - Update progress entry
- `DELETE /api/progress/:progressId` - Delete progress entry

### Nutrition Routes (`/api/nutrition`)
- `GET /api/nutrition/:userId` - Get nutrition entries
- `GET /api/nutrition/:userId/daily/:date` - Get daily nutrition summary
- `POST /api/nutrition` - Create new nutrition entry
- `PUT /api/nutrition/:nutritionId` - Update nutrition entry
- `DELETE /api/nutrition/:nutritionId` - Delete nutrition entry

## 🔐 Authentication

Most endpoints require authentication using Firebase ID tokens:

```
Authorization: Bearer <firebase-id-token>
```

The Android app should:
1. Authenticate users with Firebase Auth
2. Get the ID token: `user.getIdToken()`
3. Include it in API requests

## 🌐 Deployment Options

### Option 1: Render.com (Recommended - Free Tier Available)

1. Create account on [render.com](https://render.com)
2. Create new "Web Service"
3. Connect your GitHub repository
4. Configure:
   - **Build Command**: `cd backend && npm install`
   - **Start Command**: `cd backend && npm start`
   - **Environment Variables**: Add Firebase credentials
5. Deploy!

### Option 2: Railway.app

1. Create account on [railway.app](https://railway.app)
2. Create new project from GitHub repo
3. Add environment variables
4. Railway auto-detects Node.js and deploys

### Option 3: Heroku

1. Install Heroku CLI
2. Create `Procfile` in backend folder:
   ```
   web: node server.js
   ```
3. Deploy:
   ```bash
   heroku create fittrackr-api
   heroku config:set FIREBASE_PROJECT_ID=your-id
   # ... add other env vars
   git push heroku main
   ```

## 📝 Environment Variables for Production

When deploying, set these environment variables:

```
PORT=3000
FIREBASE_PROJECT_ID=your-firebase-project-id
FIREBASE_PRIVATE_KEY=your-firebase-private-key
FIREBASE_CLIENT_EMAIL=your-firebase-client-email
NODE_ENV=production
```

## 🧪 Testing the API

### Using cURL:

**Register user:**
```bash
curl -X POST http://localhost:3000/api/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123",
    "displayName": "Test User",
    "age": 25,
    "weightKg": 70
  }'
```

**Create workout:**
```bash
curl -X POST http://localhost:3000/api/workouts \
  -H "Authorization: Bearer YOUR_FIREBASE_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "workoutName": "Morning Run",
    "workoutType": "CARDIO",
    "startTime": 1234567890000,
    "durationSeconds": 1800,
    "caloriesBurned": 300
  }'
```

### Using Postman:
1. Import the API endpoints
2. Set Authorization header for protected routes
3. Test all CRUD operations

## 📦 Project Structure

```
backend/
├── config/
│   └── firebase.js          # Firebase Admin SDK config
├── middleware/
│   └── auth.js              # Authentication middleware
├── routes/
│   ├── userRoutes.js        # User endpoints
│   ├── workoutRoutes.js     # Workout endpoints
│   ├── goalsRoutes.js       # Goals endpoints
│   ├── progressRoutes.js    # Progress endpoints
│   └── nutritionRoutes.js   # Nutrition endpoints
├── .env.example             # Environment variables template
├── .gitignore
├── package.json
├── server.js                # Main server file
└── README.md
```

## 🔧 Troubleshooting

**"Firebase credentials not configured"**
- Make sure `firebase-admin-key.json` exists OR environment variables are set correctly

**"Unauthorized" errors**
- Verify the Firebase ID token is valid and not expired
- Check Authorization header format: `Bearer <token>`

**CORS errors**
- The API has CORS enabled for all origins in development
- For production, update CORS settings in `server.js`

## 📄 License

This project is part of the PROG7314 assignment.

