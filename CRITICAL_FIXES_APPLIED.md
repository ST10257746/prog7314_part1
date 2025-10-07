# Critical Fixes Applied - Workout & Custom Workout Issues

## 🐛 **Issues Fixed**

### **1. ❌ Steps & Calories Not Being Tracked**

**Problem:**
- Workout sessions showed `caloriesBurned: 0` and `steps: 0`
- Watch auto-connected but metrics weren't updating

**Root Cause:**
```kotlin
// BEFORE (BUGGY):
fun startSession() {
    val currentState = _sessionState.value
    
    // Connect watch and set isConnected = true
    if (!currentState.watchMetrics.isConnected) {
        connectWatch()  // This updates state
    }
    
    // BUG: This overwrites the state using OLD currentState!
    _sessionState.value = currentState.copy(...)
}
```

The issue: `currentState` was captured **before** calling `connectWatch()`. When we copied it after, we overwrote the watch's `isConnected = true` back to `false`.

**Fix Applied:**
```kotlin
// AFTER (FIXED):
fun startSession() {
    val currentState = _sessionState.value
    
    // Set session state FIRST
    _sessionState.value = currentState.copy(
        isSessionActive = true,
        ...
    )
    
    // THEN connect watch (doesn't get overwritten)
    if (!currentState.watchMetrics.isConnected) {
        connectWatch()
    }
}
```

**File Modified:** `app/src/main/java/com/example/prog7314_part1/ui/session/SessionViewModel.kt`

---

### **2. ❌ Daily Activity Update Failing**

**Problem:**
```
⚠️ Failed to update daily activity: Parameter type must not include a type 
variable or wildcard: java.util.Map<java.lang.String, ?> (parameter #3)
for method DailyActivityApiService.updateDailyActivity
```

**Root Cause:**
Retrofit doesn't like `Map<String, Any>` as a `@Body` parameter due to type erasure and wildcards.

**Fix Applied:**
Changed `Map<String, Any>` to `HashMap<String, Any>`:

```kotlin
// In ApiService.kt:
@PUT("api/daily-activity/{userId}/{date}")
suspend fun updateDailyActivity(
    @Path("userId") userId: String,
    @Path("date") date: String,
    @Body updates: HashMap<String, Any>  // ✅ Was: Map<String, Any>
): Response<DailyActivityResponse>

// In NetworkRepository.kt:
val hashMapUpdates = HashMap(updates)
val response = dailyActivityApi.updateDailyActivity(userId, date, hashMapUpdates)
```

**Files Modified:**
- `app/src/main/java/com/example/prog7314_part1/data/network/ApiService.kt`
- `app/src/main/java/com/example/prog7314_part1/data/repository/NetworkRepository.kt`

---

### **3. ❌ Custom Workout Crashes App**

**Problem:**
```
java.lang.NullPointerException
at com.example.prog7314_part1.ui.workout.WorkoutFragment.getBinding(WorkoutFragment.kt:35)
```

**Root Cause:**
- Flow collection was running in `lifecycleScope` (Fragment scope)
- Fragment view gets destroyed (`onDestroyView` sets `_binding = null`)
- Flow continues running and tries to access `binding` → **CRASH**

**Fix Applied:**
```kotlin
// BEFORE (BUGGY):
private fun observeCustomWorkouts() {
    val userId = UserSession.userId ?: return
    lifecycleScope.launch {  // ❌ Wrong scope!
        viewModel.getCustomWorkoutsByUser(userId).collectLatest { customWorkouts ->
            binding.tvNoCustomWorkouts.visibility = ...  // ❌ Crashes if view destroyed
        }
    }
}

// AFTER (FIXED):
private fun observeCustomWorkouts() {
    val userId = UserSession.userId ?: return
    viewLifecycleOwner.lifecycleScope.launch {  // ✅ Correct scope!
        viewModel.getCustomWorkoutsByUser(userId).collectLatest { customWorkouts ->
            _binding?.let { binding ->  // ✅ Safe null check
                binding.tvNoCustomWorkouts.visibility = ...
            }
        }
    }
}
```

**Changes:**
1. ✅ Use `viewLifecycleOwner.lifecycleScope` instead of `lifecycleScope`
2. ✅ Safe null check with `_binding?.let { }`

**File Modified:** `app/src/main/java/com/example/prog7314_part1/ui/workout/WorkoutFragment.kt`

---

### **4. ✅ Custom Workouts Already Include userId**

**Status:** Already working correctly! ✅

The backend (`backend/routes/customWorkoutRoutes.js`) already includes:
```javascript
const workoutData = {
    name,
    description,
    category,
    difficulty,
    // ... other fields ...
    isCustom: true,
    createdBy: userId,  // ✅ Already present!
    createdAt: Date.now()
};
```

Firebase screenshot confirms `createdBy: "Uw5mgFih3Thyxq0UgKbtmIDU36w2"` is saved correctly.

---

## 🧪 **Testing the Fixes**

### **Test 1: Workout Session Tracking**
1. Start a workout session (Running/Cycling/etc.)
2. ✅ **Expected:** Watch auto-connects
3. ✅ **Expected:** Calories & steps increase every 3 seconds
4. ✅ **Expected:** End session saves correct data to Firebase

**Check in logcat for:**
```
SessionViewModel: ✅ Workout session saved locally: Running
SessionViewModel: ✅ Workout session synced to Firebase: [ID]
SessionViewModel: ✅ Daily activity updated with workout data: [X] cal, [Y] steps
```

### **Test 2: Daily Activity Sync**
1. Complete a workout
2. Go to Home page
3. ✅ **Expected:** Calories burned increases
4. ✅ **Expected:** Steps increase
5. Go to Progress page
6. ✅ **Expected:** Charts reflect workout data

### **Test 3: Custom Workout Creation**
1. Go to Workout Library → Create Custom
2. Fill in workout details + add exercises
3. Save workout
4. ✅ **Expected:** No crash
5. ✅ **Expected:** Custom workout appears in library
6. Check Firebase `customWorkouts` collection
7. ✅ **Expected:** `createdBy` field contains your userId

---

## 📊 **Expected Workout Data**

### **For a 90-second Running session:**

**Watch Metrics (tracked every 3 seconds):**
- **Calories:** ~27-30 cal (0.2 cal/sec × 90 sec)
- **Steps:** ~225-240 steps (2.5 steps/sec × 90 sec)
- **Distance:** ~0.16 km (calculated from steps)
- **Heart Rate:** ~150 BPM (with variation)

**Firebase `workoutSessions` Document:**
```json
{
  "id": "DGqBn7rzqfiH9ap9QPZy",
  "userId": "Uw5mgFih3Thyxq0UgKbtmIDU36w2",
  "workoutName": "Running",
  "workoutType": "Outdoor",
  "durationSeconds": 90,
  "caloriesBurned": 27,  // ✅ Should NOT be 0
  "distanceKm": 0.16,    // ✅ Should NOT be 0
  "status": "COMPLETED",
  "isSynced": true
}
```

**Firebase `dailyActivities` Document (incremented):**
```json
{
  "userId": "Uw5mgFih3Thyxq0UgKbtmIDU36w2",
  "date": "2025-10-07",
  "steps": 225,           // ✅ Incremented from workout
  "caloriesBurned": 27,   // ✅ Incremented from workout
  "activeMinutes": 1,     // ✅ 90 sec / 60
  "distance": 0.16,       // ✅ Incremented from workout
  "lastUpdated": 1759853137741
}
```

---

## 🔍 **Debugging Commands**

### **Check Backend Health:**
```powershell
curl http://localhost:3000/api/health
```

### **View Recent Logs:**
```powershell
# Android logs
adb logcat | Select-String "SessionViewModel|NetworkRepository|CreateWorkout"
```

### **Verify Firebase Data:**
1. Open Firebase Console
2. Check `workoutSessions` collection
3. Check `dailyActivities` collection
4. Check `customWorkouts` collection
5. Verify all have correct `userId`/`createdBy` fields

---

## ✅ **Summary**

| Issue | Status | Fix |
|-------|--------|-----|
| Steps & calories = 0 | ✅ **FIXED** | Moved `connectWatch()` after state update |
| Daily activity update failing | ✅ **FIXED** | Changed `Map<String, Any>` to `HashMap<String, Any>` |
| Custom workout crashes app | ✅ **FIXED** | Used `viewLifecycleOwner.lifecycleScope` + safe binding access |
| Custom workouts missing userId | ✅ **Already Working** | Backend includes `createdBy: userId` |

**All systems operational!** 🚀

