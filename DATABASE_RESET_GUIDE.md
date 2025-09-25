# Database Reset Guide

## Problem
You're encountering the error: `Could not create entity object` from ObjectBox. This happens when:
- New fields are added to existing ObjectBox entities
- Existing database records don't have the new fields
- ObjectBox can't deserialize the old records

## Solution
The app now includes a built-in database reset functionality to handle schema changes.

## How to Fix the Error

### Method 1: Use the Clear Database Button (Recommended)
1. Open the AI Assistant app
2. Go to the PDF Management screen
3. Look for the red trash icon (🗑️) in the header
4. Tap the "Clear Database" button
5. Wait for the operation to complete
6. The app will refresh and show an empty document list
7. You can now upload new documents with the file storage feature

### Method 2: Clear App Data (Alternative)
1. Go to Android Settings
2. Navigate to Apps > AI Assistant
3. Tap on Storage
4. Tap "Clear Data" or "Clear Storage"
5. Reopen the app
6. Upload new documents

### Method 3: Uninstall and Reinstall (Last Resort)
1. Uninstall the AI Assistant app
2. Reinstall from the development environment
3. Upload new documents

## What This Fixes
- Resolves "Could not create entity object" errors
- Allows the new `localFilePath` field to work properly
- Enables the local file storage feature
- Provides a clean slate for development

## What You'll Lose
- All previously uploaded PDFs and their extracted text
- All chat history and search results
- All vector embeddings

## What You'll Gain
- Working local file storage functionality
- Ability to view original files
- Clean database with proper schema
- No more ObjectBox errors

## After Reset
1. Upload new documents - they will be stored locally
2. Use the "View Original" button to open files with external apps
3. All new uploads will have the `localFilePath` field properly set
4. The app will work without ObjectBox errors

## Prevention
For future development:
- Use the Clear Database button when making schema changes
- Test with fresh data after adding new fields
- Consider implementing proper database migrations for production

## Technical Details
The reset functionality:
- Clears all `PdfEntity` records
- Clears all `TextChunkEntity` records
- Refreshes the UI automatically
- Provides loading states and error handling
- Logs the operation for debugging
