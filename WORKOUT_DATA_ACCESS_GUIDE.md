# Workout Data Access Guide - Calories & Steps for All Pages

## ✅ **What's Now Available**

### **1. userId in Workout Sessions**
✅ **Already Working!** Every workout session saves with `userId`:

```json
{
  "id": "DGqBn7rzqfiH9ap9QPZy",
  "userId": "Uw5mgFih3Thyxq0UgKbtmIDU36w2",  // ✅ User-specific
  "workoutName": "Running",
  "caloriesBurned": 27,
  "distanceKm": 0.16,
  "durationSeconds": 90,
  ...
}
```

**Backend code** (`backend/routes/workoutRoutes.js`):
```javascript
const workoutData = {
    userId,  // ✅ From authenticated user (req.user.uid)
    workoutName,
    caloriesBurned: caloriesBurned || 0,
    distanceKm: distanceKm || 0,
    // ...
};
```

---

### **2. Calories & Steps Tracking**
✅ **Fixed!** The watch now properly tracks:
- ✅ **Calories**: 0.2 cal/sec for running (~27 cal for 90 sec)
- ✅ **Steps**: 2.5 steps/sec for running (~225 steps for 90 sec)
- ✅ **Distance**: Calculated from steps
- ✅ **Heart Rate**: ~150 BPM with variation

**What was fixed:**
- Watch auto-connects when session starts
- State doesn't overwrite watch connection
- Metrics update every 3 seconds

---

### **3. Data Synced to Two Places**

#### **A. Workout Sessions Collection** (individual workouts)
```
Firebase → workoutSessions/{sessionId}
```
Contains:
- `userId` ✅
- `caloriesBurned` ✅
- `distanceKm` ✅
- `durationSeconds` ✅
- `workoutName`, `workoutType`, etc.

#### **B. Daily Activity Collection** (aggregated daily totals)
```
Firebase → dailyActivities/{userId}_{date}
```
Contains:
- `steps` (incremented from workouts) ✅
- `caloriesBurned` (incremented from workouts) ✅
- `distance` (incremented from workouts) ✅
- `waterGlasses`, `activeMinutes`, etc.

---

## 📊 **How to Access Workout Data from Other Pages**

### **Method 1: Get Today's Workout Totals**
Use this in **Home** or **Progress** pages:

```kotlin
// In your ViewModel or Repository
val networkRepository = NetworkRepository(context)

lifecycleScope.launch {
    when (val result = networkRepository.getTodayWorkoutTotals()) {
        is Result.Success -> {
            val (calories, distance, workoutCount) = result.data
            
            // Update UI
            totalCaloriesBurnedToday = calories
            totalDistanceToday = distance
            totalWorkoutsToday = workoutCount
            
            Log.d("Home", "Today: $calories cal, $distance km, $workoutCount workouts")
        }
        is Result.Error -> {
            Log.e("Home", "Failed to fetch workout totals: ${result.message}")
        }
    }
}
```

**Returns:**
- **Calories**: Total calories burned from all workouts today
- **Distance**: Total distance covered today
- **Workout Count**: Number of completed workouts today

---

### **Method 2: Get Workout Stats for Date Range**
Use this in **Progress** page for weekly/monthly stats:

```kotlin
val networkRepository = NetworkRepository(context)

// Get this week's stats
val weekStart = getStartOfWeek()
val weekEnd = System.currentTimeMillis()

lifecycleScope.launch {
    when (val result = networkRepository.getWorkoutStats(weekStart, weekEnd)) {
        is Result.Success -> {
            val stats = result.data
            
            val totalCalories = (stats["totalCaloriesBurned"] as? Number)?.toInt() ?: 0
            val totalDistance = (stats["totalDistanceKm"] as? Number)?.toDouble() ?: 0.0
            val totalWorkouts = (stats["totalWorkouts"] as? Number)?.toInt() ?: 0
            val avgDuration = (stats["averageDurationMinutes"] as? Number)?.toInt() ?: 0
            
            // Update charts/UI
            updateWeeklyChart(totalCalories, totalDistance, totalWorkouts)
        }
        is Result.Error -> {
            Log.e("Progress", "Failed to fetch stats: ${result.message}")
        }
    }
}
```

---

### **Method 3: Get All Workout Sessions**
Use this to display individual workouts:

```kotlin
val networkRepository = NetworkRepository(context)

lifecycleScope.launch {
    when (val result = networkRepository.getWorkoutSessions(limit = 50)) {
        is Result.Success -> {
            val sessions = result.data
            
            sessions.forEach { session ->
                Log.d("Workouts", """
                    Workout: ${session.workoutName}
                    Calories: ${session.caloriesBurned}
                    Distance: ${session.distanceKm} km
                    Duration: ${session.durationSeconds / 60} min
                """.trimIndent())
            }
            
            // Display in RecyclerView
            workoutAdapter.submitList(sessions)
        }
        is Result.Error -> {
            Log.e("Workouts", "Failed to fetch sessions: ${result.message}")
        }
    }
}
```

---

### **Method 4: Access from Daily Activity**
The daily activity is **automatically updated** when workouts complete:

```kotlin
val dailyActivityRepository = DailyActivityRepository(context)

lifecycleScope.launch {
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    
    when (val result = networkRepository.getDailyActivity(userId, today)) {
        is Result.Success -> {
            val activity = result.data
            
            // These include workout data!
            val totalSteps = activity.steps  // ✅ Includes workout steps
            val totalCalories = activity.caloriesBurned  // ✅ Includes workout calories
            val totalDistance = activity.distance  // ✅ Includes workout distance
            
            Log.d("DailyActivity", "Total: $totalCalories cal, $totalSteps steps")
        }
    }
}
```

---

## 🎯 **Example: Home Page Implementation**

```kotlin
// HomeFragment.kt or HomeViewModel.kt
class HomeViewModel(private val context: Context) : ViewModel() {
    
    private val networkRepository = NetworkRepository(context)
    
    private val _homeStats = MutableLiveData<HomeStats>()
    val homeStats: LiveData<HomeStats> = _homeStats
    
    fun loadTodayStats() {
        viewModelScope.launch {
            // Get workout totals
            when (val workoutResult = networkRepository.getTodayWorkoutTotals()) {
                is Result.Success -> {
                    val (calories, distance, workoutCount) = workoutResult.data
                    
                    // Get daily activity for water, etc.
                    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    val userId = UserSession.userId ?: return@launch
                    
                    when (val activityResult = networkRepository.getDailyActivity(userId, today)) {
                        is Result.Success -> {
                            val activity = activityResult.data
                            
                            _homeStats.value = HomeStats(
                                totalCalories = calories + activity.caloriesBurned,  // Combined!
                                totalSteps = activity.steps,  // From daily activity
                                totalDistance = distance + activity.distance,  // Combined!
                                waterGlasses = activity.waterGlasses,
                                workoutsCompleted = workoutCount
                            )
                        }
                    }
                }
                is Result.Error -> {
                    Log.e("HomeViewModel", "Error loading stats: ${workoutResult.message}")
                }
            }
        }
    }
}

data class HomeStats(
    val totalCalories: Int,
    val totalSteps: Int,
    val totalDistance: Double,
    val waterGlasses: Int,
    val workoutsCompleted: Int
)
```

---

## 🔥 **Backend API Endpoints**

### **1. Get Workout Sessions**
```
GET /api/workouts?limit=50&status=COMPLETED
Authorization: Bearer {token}
```

**Response:**
```json
{
  "count": 2,
  "workouts": [
    {
      "id": "DGqBn7rzqfiH9ap9QPZy",
      "userId": "Uw5mgFih3Thyxq0UgKbtmIDU36w2",
      "workoutName": "Running",
      "caloriesBurned": 27,
      "distanceKm": 0.16,
      "durationSeconds": 90
    }
  ]
}
```

### **2. Get Workout Stats**
```
GET /api/workouts/stats/summary?startDate=1759844800000&endDate=1759931200000
Authorization: Bearer {token}
```

**Response:**
```json
{
  "summary": {
    "totalWorkouts": 5,
    "totalDurationMinutes": 45,
    "totalCaloriesBurned": 135,
    "totalDistanceKm": 2.5,
    "averageDurationMinutes": 9
  }
}
```

### **3. Get Daily Activity**
```
GET /api/daily-activity/{userId}/{date}
Authorization: Bearer {token}
```

**Response:**
```json
{
  "userId": "Uw5mgFih3Thyxq0UgKbtmIDU36w2",
  "date": "2025-10-07",
  "steps": 225,
  "caloriesBurned": 27,
  "waterGlasses": 3,
  "distance": 0.16,
  "activeMinutes": 2
}
```

---

## ✅ **Summary - What's Available**

| Data | Source | How to Access |
|------|--------|---------------|
| **Today's Workout Calories** | Workout Sessions | `networkRepository.getTodayWorkoutTotals()` |
| **Today's Workout Distance** | Workout Sessions | `networkRepository.getTodayWorkoutTotals()` |
| **Total Steps (inc. workouts)** | Daily Activity | `networkRepository.getDailyActivity(userId, date)` |
| **Water Intake** | Daily Activity | `networkRepository.getDailyActivity(userId, date)` |
| **Weekly Stats** | Workout Sessions | `networkRepository.getWorkoutStats(weekStart, weekEnd)` |
| **Individual Workouts** | Workout Sessions | `networkRepository.getWorkoutSessions(limit)` |

---

## 🧪 **Testing**

1. **Start a workout session** (Running, 90 seconds)
2. **Check logcat:**
   ```
   SessionViewModel: ✅ Workout session synced to Firebase: [ID]
   SessionViewModel: ✅ Daily activity updated with workout data: 27 cal, 225 steps
   ```
3. **Check Firebase Console:**
   - `workoutSessions` → Should have `caloriesBurned: 27`, `userId: [your-id]`
   - `dailyActivities` → Should increment `caloriesBurned` and `steps`

4. **Call from Home page:**
   ```kotlin
   networkRepository.getTodayWorkoutTotals()
   // Should return: Triple(27, 0.16, 1)
   ```

---

**Everything is now accessible across all pages!** 🎉

