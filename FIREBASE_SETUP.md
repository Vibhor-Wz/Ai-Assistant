# Firebase AI Integration Setup Guide

This guide explains how to set up Firebase AI for the Vector Database Android application.

## Prerequisites

1. **Firebase Project**: You need a Firebase project with AI services enabled
2. **Google Cloud Project**: Firebase AI requires a Google Cloud project
3. **API Key**: You need a Firebase AI API key

## Setup Steps

### 1. Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click "Create a project" or use an existing project
3. Follow the setup wizard

### 2. Enable Required Services

In your Firebase project:

1. **Authentication**:
   - Go to Authentication > Sign-in method
   - Enable "Anonymous" sign-in method

2. **AI Services**:
   - Go to Extensions > Browse more extensions
   - Search for "Firebase AI" or "Generative AI"
   - Install the Firebase AI extension

### 3. Get Configuration Files

1. **google-services.json**:
   - Go to Project Settings > General
   - Under "Your apps", click "Add app" > Android
   - Enter package name: `com.bigcash.ai.vectordb`
   - Download `google-services.json`
   - Place it in the `app/` directory

2. **API Key**:
   - Go to Project Settings > Service accounts
   - Generate a new private key
   - Or use the Web API key from Project Settings > General

### 4. Update Configuration

1. **Replace google-services.json**:
   ```bash
   # Remove the template and add your actual file
   rm app/google-services.json.template
   # Add your downloaded google-services.json to app/
   ```

2. **Update API Key**:
   - Open `app/src/main/java/com/bigcash/ai/vectordb/service/FirebaseAiService.kt`
   - Replace `YOUR_FIREBASE_AI_API_KEY` with your actual API key:
   ```kotlin
   private const val API_KEY = "your-actual-api-key-here"
   ```

### 5. Enable Required APIs

In Google Cloud Console:

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Select your project
3. Go to "APIs & Services" > "Library"
4. Enable these APIs:
   - **Firebase Authentication API**
   - **Firebase Extensions API**
   - **Generative Language API** (for Gemini)

### 6. Configure Authentication Rules

In Firebase Console > Authentication > Settings:

1. Go to "Authorized domains"
2. Add your development domain if needed
3. Ensure anonymous authentication is enabled

## Testing the Setup

### 1. Build the Project

```bash
./gradlew assembleDebug
```

### 2. Check Logs

Look for these log messages:

```
VECTOR_DEBUG: 🔥 Firebase: Successfully initialized
VECTOR_DEBUG: 🔐 FirebaseAiService: Initializing Firebase Authentication
VECTOR_DEBUG: ✅ FirebaseAiService: Anonymous authentication successful
VECTOR_DEBUG: 🤖 FirebaseAiService: Starting AI content generation
```

### 3. Test File Upload

1. Upload a PDF or image file
2. Check logs for AI-generated content:
```
MLKitOutput: [Generated AI content will appear here]
```

## Troubleshooting

### Common Issues

1. **"Firebase not initialized"**:
   - Ensure `google-services.json` is in the correct location
   - Check that Google Services plugin is applied

2. **"Authentication failed"**:
   - Verify anonymous authentication is enabled
   - Check Firebase project settings

3. **"API key invalid"**:
   - Verify the API key is correct
   - Ensure the key has proper permissions

4. **"AI service unavailable"**:
   - Check if Generative Language API is enabled
   - Verify Firebase AI extension is installed

### Debug Steps

1. **Check Firebase Console**:
   - Look for authentication events
   - Check for any error logs

2. **Verify Dependencies**:
   ```bash
   ./gradlew dependencies | grep firebase
   ```

3. **Test Authentication**:
   - Check if anonymous users appear in Authentication > Users

## Security Considerations

1. **API Key Protection**:
   - Never commit API keys to version control
   - Use environment variables or secure storage
   - Consider using Firebase App Check for additional security

2. **Authentication**:
   - Anonymous authentication is suitable for demo purposes
   - For production, consider implementing proper user authentication

3. **Rate Limiting**:
   - Firebase AI has usage limits
   - Implement proper error handling for rate limit exceeded

## Production Deployment

1. **Replace API Key**:
   - Use a production API key
   - Store it securely (not in code)

2. **Enable App Check**:
   - Protect your Firebase resources
   - Prevent abuse and unauthorized usage

3. **Monitor Usage**:
   - Set up billing alerts
   - Monitor API usage in Google Cloud Console

## Support

- [Firebase Documentation](https://firebase.google.com/docs)
- [Firebase AI Documentation](https://firebase.google.com/docs/ai)
- [Google Cloud AI Documentation](https://cloud.google.com/ai)

## File Structure

```
app/
├── google-services.json          # Firebase configuration (add this)
├── google-services.json.template # Template file (remove after setup)
└── src/main/java/.../service/
    └── FirebaseAiService.kt      # Main AI service
```

## Next Steps

After successful setup:

1. Test with different file types (PDF, images, documents)
2. Implement proper error handling
3. Add user feedback for AI processing status
4. Consider implementing caching for AI responses
5. Add configuration for different AI models or parameters
