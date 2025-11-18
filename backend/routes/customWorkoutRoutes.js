const express = require('express');
const router = express.Router();
const admin = require('firebase-admin');
const db = admin.firestore();
const { verifyToken } = require('../middleware/auth');
const { sendNotification } = require('../utils/notifications');

/**
 * GET /api/custom-workouts/:userId
 * Get all custom workouts for a user
 */
router.get('/:userId', verifyToken, async (req, res) => {
    try {
        const { userId } = req.params;
        
        // Verify the requester is accessing their own workouts
        if (req.user.uid !== userId) {
            return res.status(403).json({ 
                error: 'Forbidden',
                message: 'You can only access your own workouts'
            });
        }

        const snapshot = await db.collection('customWorkouts')
            .where('createdBy', '==', userId)
            .orderBy('createdAt', 'desc')
            .get();

        const workouts = [];
        snapshot.forEach(doc => {
            workouts.push({
                id: doc.id,
                ...doc.data()
            });
        });

        res.status(200).json({
            count: workouts.length,
            workouts
        });

    } catch (error) {
        console.error('Get custom workouts error:', error);
        res.status(500).json({ 
            error: 'Failed to fetch custom workouts',
            message: error.message 
        });
    }
});

/**
 * POST /api/custom-workouts
 * Create a new custom workout template
 */
router.post('/', verifyToken, async (req, res) => {
    try {
        const userId = req.user.uid;
        const { 
            name, 
            description,
            category,
            difficulty,
            durationMinutes,
            estimatedCalories,
            exerciseCount,
            thumbnailUrl,
            exercises
        } = req.body;

        // Validation
        if (!name || !category || !difficulty) {
            return res.status(400).json({ 
                error: 'Missing required fields',
                required: ['name', 'category', 'difficulty']
            });
        }

        const workoutData = {
            name,
            description: description || '',
            category,
            difficulty,
            durationMinutes: durationMinutes || 30,
            estimatedCalories: estimatedCalories || 200,
            exerciseCount: exerciseCount || 0,
            thumbnailUrl: thumbnailUrl || null,
            rating: 0.0,
            isCustom: true,
            createdBy: userId,
            createdAt: Date.now(),
            exercises: exercises || []  // ✅ Save exercises array
        };

        const docRef = await db.collection('customWorkouts').add(workoutData);

        // Send notification
        try {
            await sendNotification(
                userId,
                'Workout logged!',
                `Great job! You've logged "${name}". Keep up the amazing work!`,
                { type: 'WORKOUT_LOGGED', workoutId: docRef.id }
            );
        } catch (notifyError) {
            console.error('Failed to send workout notification:', notifyError);
            // Don't fail workout creation if notification fails
        }

        res.status(201).json({
            message: 'Custom workout created successfully',
            workout: {
                id: docRef.id,
                ...workoutData
            }
        });

    } catch (error) {
        console.error('Create custom workout error:', error);
        res.status(500).json({ 
            error: 'Failed to create custom workout',
            message: error.message 
        });
    }
});

/**
 * PUT /api/custom-workouts/:workoutId
 * Update a custom workout template
 */
router.put('/:workoutId', verifyToken, async (req, res) => {
    try {
        const { workoutId } = req.params;
        const userId = req.user.uid;

        // Verify workout exists and belongs to user
        const doc = await db.collection('customWorkouts').doc(workoutId).get();

        if (!doc.exists) {
            return res.status(404).json({ 
                error: 'Workout not found' 
            });
        }

        if (doc.data().createdBy !== userId) {
            return res.status(403).json({ 
                error: 'Forbidden',
                message: 'You can only update your own workouts' 
            });
        }

        const updateData = {
            ...req.body,
            updatedAt: Date.now()
        };

        // Remove fields that shouldn't be updated
        delete updateData.createdBy;
        delete updateData.createdAt;
        delete updateData.id;
        delete updateData.isCustom;

        await db.collection('customWorkouts').doc(workoutId).update(updateData);

        const updatedDoc = await db.collection('customWorkouts').doc(workoutId).get();

        res.json({
            message: 'Custom workout updated successfully',
            workout: {
                id: updatedDoc.id,
                ...updatedDoc.data()
            }
        });

    } catch (error) {
        console.error('Update custom workout error:', error);
        res.status(500).json({ 
            error: 'Failed to update custom workout',
            message: error.message 
        });
    }
});

/**
 * DELETE /api/custom-workouts/:workoutId
 * Delete a custom workout template
 */
router.delete('/:workoutId', verifyToken, async (req, res) => {
    try {
        const { workoutId } = req.params;
        const userId = req.user.uid;

        // Verify workout exists and belongs to user
        const doc = await db.collection('customWorkouts').doc(workoutId).get();

        if (!doc.exists) {
            return res.status(404).json({ 
                error: 'Workout not found' 
            });
        }

        if (doc.data().createdBy !== userId) {
            return res.status(403).json({ 
                error: 'Forbidden',
                message: 'You can only delete your own workouts' 
            });
        }

        await db.collection('customWorkouts').doc(workoutId).delete();

        res.json({
            message: 'Custom workout deleted successfully'
        });

    } catch (error) {
        console.error('Delete custom workout error:', error);
        res.status(500).json({ 
            error: 'Failed to delete custom workout',
            message: error.message 
        });
    }
});

module.exports = router;

