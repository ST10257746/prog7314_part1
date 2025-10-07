const { auth } = require('../config/firebase');

/**
 * Middleware to verify Firebase ID token
 * Extracts token from Authorization header and verifies it
 */
const verifyToken = async (req, res, next) => {
  try {
    const authHeader = req.headers.authorization;
    
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return res.status(401).json({ 
        error: 'Unauthorized', 
        message: 'No token provided' 
      });
    }
    
    const token = authHeader.split('Bearer ')[1];
    
    // Verify the Firebase ID token
    const decodedToken = await auth.verifyIdToken(token);
    
    // Attach user info to request
    req.user = {
      uid: decodedToken.uid,
      email: decodedToken.email
    };
    
    next();
  } catch (error) {
    console.error('Token verification error:', error);
    return res.status(401).json({ 
      error: 'Unauthorized', 
      message: 'Invalid or expired token' 
    });
  }
};

/**
 * Optional auth middleware - continues even if no token
 */
const optionalAuth = async (req, res, next) => {
  try {
    const authHeader = req.headers.authorization;
    
    if (authHeader && authHeader.startsWith('Bearer ')) {
      const token = authHeader.split('Bearer ')[1];
      const decodedToken = await auth.verifyIdToken(token);
      
      req.user = {
        uid: decodedToken.uid,
        email: decodedToken.email
      };
    }
    
    next();
  } catch (error) {
    // Continue without auth
    next();
  }
};

module.exports = { verifyToken, optionalAuth };

