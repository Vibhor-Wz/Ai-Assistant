# GetImg AI Service Usage Guide

This guide explains how to use the GetImg AI service for image-to-image transformations in your Android app.

## Overview

The `GetImgAiService` class provides image editing capabilities using GetImg AI's Stable Diffusion XL model. It allows users to upload an image and provide a text prompt to transform the image according to their requirements.

## Setup

### 1. API Key Configuration

Before using the service, you need to set your GetImg AI API key:

```kotlin
// In your Activity or Fragment
val chatViewModel = ChatViewModel(application)
chatViewModel.setGetImgApiKey("your_getimg_ai_api_key_here")
```

### 2. Service Initialization

The service is automatically initialized when you create a `ChatViewModel` instance:

```kotlin
class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val getImgAiService = GetImgAiService(application)
    // ... other code
}
```

## Usage in Chat

### Basic Image Editing

Users can upload an image and request edits by typing messages like:

- "Edit this image to make it look like a cartoon"
- "Change the background to a beach scene"
- "Make this image more artistic"
- "Convert this to anime style"

### Using GetImg AI Specifically

To use GetImg AI instead of the default Firebase AI, users can include keywords in their message:

- "Edit this image with GetImg AI to make it look like a painting"
- "Use stable diffusion to change the background"
- "Apply GetImg AI to make this more realistic"

### Multiple Variations

Users can request multiple variations by including keywords like:

- "Create variations of this image"
- "Show me different styles using GetImg AI"
- "Generate multiple versions"

## API Reference

### GetImgAiService Methods

#### `editImage(imageData: ByteArray, prompt: String, ...)`

Main method for image editing.

**Parameters:**
- `imageData`: Original image as byte array
- `prompt`: Text prompt describing the desired transformation
- `strength`: Strength of transformation (0.0 to 1.0, default: 0.8)
- `guidanceScale`: Guidance scale (1.0 to 20.0, default: 7.5)
- `numInferenceSteps`: Number of inference steps (1 to 50, default: 20)
- `negativePrompt`: Optional negative prompt to avoid certain elements

**Returns:** `Bitmap?` - Generated edited image or null if failed

#### `generateMultipleImageVariations(imageData: ByteArray, prompts: List<String>, ...)`

Generate multiple variations of an image.

**Parameters:**
- `imageData`: Original image as byte array
- `prompts`: List of prompts for different variations
- `strength`: Strength of transformation for all variations

**Returns:** `List<Bitmap>` - List of generated image variations

#### `quickImageEdit(imageData: ByteArray, style: String)`

Convenience method for quick edits with predefined styles.

**Parameters:**
- `imageData`: Original image as byte array
- `style`: Style of edit (enhanced, cartoon, realistic, artistic, vintage, modern, anime, oil_painting)

**Returns:** `Bitmap?` - Generated edited image or null if failed

#### `saveImageToStorage(bitmap: Bitmap, fileName: String)`

Save generated image to device storage.

**Parameters:**
- `bitmap`: Generated image as Bitmap
- `fileName`: Name for the saved file (without extension)

**Returns:** `String?` - File path of the saved image or null if failed

### ChatViewModel Methods

#### `setGetImgApiKey(apiKey: String)`

Set the GetImg AI API key.

#### `isGetImgServiceReady(): Boolean`

Check if the GetImg AI service is ready.

## Example Usage

### Basic Image Editing

```kotlin
// In your Activity or Fragment
val chatViewModel = ChatViewModel(application)

// Set API key
chatViewModel.setGetImgApiKey("your_api_key_here")

// User uploads an image and types: "Edit this image to make it look like a cartoon"
// The ChatViewModel will automatically detect this as an image edit request
// and use GetImg AI if the message contains GetImg keywords
```

### Direct Service Usage

```kotlin
val getImgService = GetImgAiService(context)
getImgService.setApiKey("your_api_key_here")

// Load image from file
val imageFile = File("path/to/image.jpg")
val imageBytes = imageFile.readBytes()

// Edit image
val editedBitmap = getImgService.editImage(
    imageData = imageBytes,
    prompt = "Convert this image to anime style",
    strength = 0.8f,
    guidanceScale = 7.5f
)

// Save result
if (editedBitmap != null) {
    val savedPath = getImgService.saveImageToStorage(editedBitmap, "edited_image")
    Log.d("GetImg", "Image saved to: $savedPath")
}
```

### Quick Style Edits

```kotlin
val getImgService = GetImgAiService(context)
getImgService.setApiKey("your_api_key_here")

val imageBytes = imageFile.readBytes()

// Apply different styles
val cartoonBitmap = getImgService.quickImageEdit(imageBytes, "cartoon")
val artisticBitmap = getImgService.quickImageEdit(imageBytes, "artistic")
val animeBitmap = getImgService.quickImageEdit(imageBytes, "anime")
```

## Supported Styles

The `quickImageEdit` method supports the following styles:

- `enhanced` - General enhancement
- `cartoon` - Cartoon style with vibrant colors
- `realistic` - More realistic with enhanced details
- `artistic` - Artistic painting style
- `vintage` - Vintage filter with sepia tones
- `modern` - Modern, sleek look
- `anime` - Anime style with vibrant colors
- `oil_painting` - Oil painting with rich textures

## Error Handling

The service includes comprehensive error handling:

- API key validation
- Network request retries (up to 3 attempts)
- Image format validation
- Base64 conversion error handling
- Response parsing error handling

## Requirements

1. **API Key**: You need a valid GetImg AI API key
2. **Internet Connection**: Required for API calls
3. **Image Format**: Supports common image formats (JPEG, PNG, etc.)
4. **Android Permissions**: Storage permission for saving images

## API Endpoint

The service uses the GetImg AI endpoint:
```
POST https://api.getimg.ai/v1/stable-diffusion/image-to-image
```

## Request Body Format

```json
{
    "image": "base64_encoded_image",
    "prompt": "text_prompt_describing_transformation",
    "model": "stable-diffusion-xl",
    "controlnet": "none",
    "strength": 0.8,
    "guidance_scale": 7.5,
    "num_inference_steps": 20,
    "negative_prompt": "optional_negative_prompt"
}
```

## Response Format

The API returns a JSON response with the generated image as base64:

```json
{
    "image": "base64_encoded_generated_image"
}
```

## Troubleshooting

### Common Issues

1. **API Key Not Set**: Make sure to call `setGetImgApiKey()` before using the service
2. **Network Errors**: Check internet connection and API key validity
3. **Image Conversion Errors**: Ensure the image file is valid and not corrupted
4. **Rate Limiting**: The service includes delays between requests to avoid rate limiting

### Debug Logging

The service uses the tag `GETIMG_AI_DEBUG` for detailed logging. Enable debug logging to troubleshoot issues:

```kotlin
// In your Application class or MainActivity
if (BuildConfig.DEBUG) {
    Log.d("GETIMG_AI_DEBUG", "Debug logging enabled")
}
```

## Integration Notes

- The service is integrated into the existing `ChatViewModel`
- Users can choose between Firebase AI (default) and GetImg AI by including keywords in their messages
- Generated images are automatically saved to the device's external storage
- The service maintains the same interface as other AI services in the app

