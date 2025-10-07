# Firebase Data Storage & API Integration Summary

## 🔐 Security Model - ALL DATA IS USER-SPECIFIC

Every API endpoint uses **JWT token verification** to ensure users can ONLY access their own data:
- ✅ Token extracted from Authorization header
- ✅ `userId` from token used to filter ALL queries
- ✅ Forbidden (403) errors if user tries to access another user's data

---

## 📊 Data Collections in Firestore

### 1. **`users` Collection** ✅ USING API
**Document ID:** `userId` (from Firebase Auth)

**Stored Data:**
```javascript
{
  userId: "abc123...",
  email: "user@example.com",
  displayName: "John Doe",
  age: 25,
  weightKg: 75.5,
  heightCm: 180,
  profileImageUrl: "https://...",
  
  // Fitness Goals
  dailyStepGoal: 10000,
  dailyCalorieGoal: 2000,
  dailyWaterGoal: 8,
  weeklyWorkoutGoal: 5,
  
  // Nutrition Goals
  proteinGoalG: 150,
  carbsGoalG: 200,
  fatsGoalG: 65,
  
  createdAt: 1696723200000,
  updatedAt: 1696723200000
}
```

**API Endpoints:**
- `POST /api/users/register` - Create user profile (after Firebase Auth)
- `POST /api/users/login` - Get user data
- `GET /api/users/:userId` - Get user profile (owner only)
- `PUT /api/users/:userId` - Update user profile (owner only)
- `DELETE /api/users/:userId` - Delete user account (owner only)

**Security:** ✅ `req.user.uid === userId` verification on all requests

---

### 2. **`workoutSessions` Collection** ✅ API READY (Not yet integrated in app)
**Document ID:** Auto-generated

**Stored Data:**
```javascript
{
  userId: "abc123...",  // ← USER-SPECIFIC FILTER
  workoutName: "Morning Run",
  workoutType: "CARDIO",
  startTime: 1696723200000,
  endTime: 1696726800000,
  durationSeconds: 3600,
  caloriesBurned: 500,
  distanceKm: 8.5,
  notes: "Felt great!",
  status: "COMPLETED",
  createdAt: 1696723200000,
  isSynced: true
}
```

**API Endpoints:**
- `GET /api/workouts` - Get user's workouts (filtered by userId)
- `GET /api/workouts/:workoutId` - Get specific workout (owner only)
- `POST /api/workouts` - Create workout
- `PUT /api/workouts/:workoutId` - Update workout (owner only)
- `DELETE /api/workouts/:workoutId` - Delete workout (owner only)
- `GET /api/workouts/stats/summary` - Get workout statistics

**Security:** ✅ All queries filtered by `userId` from token

---

### 3. **`goals` Collection** ✅ API READY (Not yet integrated in app)
**Document ID:** Auto-generated

**Stored Data:**
```javascript
{
  userId: "abc123...",  // ← USER-SPECIFIC FILTER
  goalType: "WEIGHT_LOSS",
  targetValue: 70,
  currentValue: 75.5,
  startDate: 1696723200000,
  targetDate: 1704067200000,
  description: "Lose 5kg",
  isActive: true,
  isCompleted: false,
  createdAt: 1696723200000,
  updatedAt: 1696723200000,
  completedAt: null
}
```

**API Endpoints:**
- `GET /api/goals/:userId` - Get user's goals (owner only)
- `GET /api/goals/user/:userId/active` - Get active goals (owner only)
- `POST /api/goals` - Create goal
- `PUT /api/goals/:goalId` - Update goal (owner only)
- `PUT /api/goals/:goalId/progress` - Update goal progress (owner only)
- `DELETE /api/goals/:goalId` - Delete goal (owner only)

**Security:** ✅ All queries filtered by `userId` from token

---

### 4. **`nutritionEntries` Collection** ✅ API READY (Not yet integrated in app)
**Document ID:** Auto-generated

**Stored Data:**
```javascript
{
  userId: "abc123...",  // ← USER-SPECIFIC FILTER
  foodName: "Chicken Breast",
  mealType: "DINNER",
  servingSize: "200g",
  calories: 330,
  proteinG: 62,
  carbsG: 0,
  fatsG: 7,
  fiberG: 0,
  sugarG: 0,
  notes: "Grilled",
  timestamp: 1696723200000,
  createdAt: 1696723200000
}
```

**API Endpoints:**
- `GET /api/nutrition/:userId` - Get nutrition entries (owner only)
- `GET /api/nutrition/:userId/daily/:date` - Get daily nutrition summary
- `POST /api/nutrition` - Create nutrition entry
- `PUT /api/nutrition/:nutritionId` - Update entry (owner only)
- `DELETE /api/nutrition/:nutritionId` - Delete entry (owner only)

**Security:** ✅ All queries filtered by `userId` from token

---

### 5. **`progressTracking` Collection** ✅ API READY (Not yet integrated in app)
**Document ID:** Auto-generated

**Stored Data:**
```javascript
{
  userId: "abc123...",  // ← USER-SPECIFIC FILTER
  date: 1696723200000,
  weightKg: 75.5,
  bmi: 23.3,
  bodyFatPercentage: 18,
  muscleMassKg: 32,
  notes: "Feeling stronger",
  createdAt: 1696723200000
}
```

**API Endpoints:**
- `GET /api/progress/:userId` - Get progress entries (owner only)
- `GET /api/progress/:userId/latest` - Get latest entry (owner only)
- `GET /api/progress/:userId/summary` - Get progress summary (owner only)
- `POST /api/progress` - Create progress entry
- `PUT /api/progress/:progressId` - Update entry (owner only)
- `DELETE /api/progress/:progressId` - Delete entry (owner only)

**Security:** ✅ All queries filtered by `userId` from token

---

## 🔌 Current API Integration Status

### ✅ FULLY INTEGRATED (Using REST API):
1. **User Management**
   - Registration → `ApiUserRepository`
   - Login → `ApiUserRepository`
   - Profile updates → `ApiUserRepository`
   - All user operations go through API

### ⚠️ BACKEND READY, APP NOT USING YET:
2. **Workout Sessions**
   - Backend API: ✅ Complete
   - Android App: ❌ Still using local RoomDB only
   - **Action needed:** Update WorkoutRepository to use NetworkRepository

3. **Goals**
   - Backend API: ✅ Complete
   - Android App: ❌ Still using local RoomDB only
   - **Action needed:** Create GoalsRepository using NetworkRepository

4. **Nutrition Tracking**
   - Backend API: ✅ Complete
   - Android App: ❌ Still using local RoomDB only
   - **Action needed:** Update NutritionRepository to use NetworkRepository

5. **Progress Tracking**
   - Backend API: ✅ Complete
   - Android App: ❌ Still using local RoomDB only
   - **Action needed:** Create ProgressRepository using NetworkRepository

---

## 🔒 Security Verification Checklist

### ✅ JWT Token Verification
- Every protected endpoint uses `verifyToken` middleware
- Token must be present in `Authorization: Bearer <token>` header
- Token verified using Firebase Admin SDK

### ✅ User Data Isolation
- All Firestore queries filtered by `userId` from JWT token
- Users CANNOT access other users' data
- 403 Forbidden returned if userId mismatch

### ✅ Data Ownership
- **Users Collection:** Document ID = userId
- **Workouts:** Filtered by `where('userId', '==', userId)`
- **Goals:** Filtered by `where('userId', '==', userId)`
- **Nutrition:** Filtered by `where('userId', '==', userId)`
- **Progress:** Filtered by `where('userId', '==', userId)`

### ✅ Protected Fields
- `userId` cannot be changed via API
- `email` cannot be changed (security)
- `createdAt` timestamps protected
- `id` fields protected from updates

---

## 📝 Summary

### What's Being Stored in Firebase:
1. **User Profiles** - Personal info, fitness goals, nutrition goals
2. **Workout Sessions** - Exercise tracking with duration, calories, distance
3. **Goals** - Fitness goals with progress tracking
4. **Nutrition Entries** - Meal logging with macros
5. **Progress Tracking** - Body measurements over time

### User-Specific Security:
- ✅ **100% User-Specific** - Every query filtered by authenticated userId
- ✅ **JWT Authentication** - All requests require valid Firebase ID token
- ✅ **Ownership Verification** - Users can only access/modify their own data
- ✅ **Protected Fields** - Critical fields like userId, email cannot be changed

### API Integration Progress:
- ✅ **Users:** FULLY INTEGRATED
- ⚠️ **Workouts, Goals, Nutrition, Progress:** Backend ready, app needs integration

---

## 🚀 Next Steps to Complete Full API Integration

To make ALL app data go through the API (not just users):

1. **Update WorkoutRepository** to use API calls from NetworkRepository
2. **Update NutritionRepository** to use API calls from NetworkRepository
3. **Create/Update GoalsRepository** to use API calls from NetworkRepository
4. **Create/Update ProgressRepository** to use API calls from NetworkRepository
5. **Test all features** end-to-end with API
6. **Deploy backend** to Railway for production use

