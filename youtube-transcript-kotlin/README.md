# YouTube Transcript Kotlin

A Kotlin library for fetching YouTube video transcripts, similar to Python's [youtube-transcript-api](https://github.com/jdepoix/youtube-transcript-api).

## Features

- 🎯 **Simple API** - Easy to use, similar to Python library
- 🌍 **Multi-language Support** - Fetch transcripts in various languages
- 🔄 **Auto-generated & Manual** - Support for both auto-generated and manually created transcripts
- 🌐 **Translation Support** - Translate transcripts to different languages
- 📱 **Android Ready** - Built for Android with proper lifecycle management
- 🧪 **Well Tested** - Comprehensive test coverage
- 📚 **Well Documented** - Clear documentation and examples

## Installation

### Gradle

Add the dependency to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.youtubetranscript:youtube-transcript-kotlin:1.0.0")
}
```

### Maven

```xml
<dependency>
    <groupId>com.youtubetranscript</groupId>
    <artifactId>youtube-transcript-kotlin</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Quick Start

### Basic Usage

```kotlin
import com.youtubetranscript.YouTubeTranscriptApi
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    try {
        // Get transcript for a video
        val transcript = YouTubeTranscriptApi.getTranscript("dQw4w9WgXcQ")
        
        // Print transcript segments
        transcript.forEach { segment ->
            println("${segment.getFormattedTimestamp()}: ${segment.text}")
        }
    } catch (e: TranscriptException) {
        println("Error: ${e.message}")
    }
}
```

### Advanced Usage

```kotlin
import com.youtubetranscript.YouTubeTranscriptApi
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    try {
        // Get transcript with specific language preference
        val transcript = YouTubeTranscriptApi.getTranscript(
            videoId = "dQw4w9WgXcQ",
            languages = listOf("en", "es", "fr")
        )
        
        // Get available transcripts
        val transcriptList = YouTubeTranscriptApi.listTranscripts("dQw4w9WgXcQ")
        
        // Check available languages
        val languages = transcriptList.getAvailableLanguages()
        println("Available languages: $languages")
        
        // Get manually created transcript
        val manualTranscript = transcriptList.findManuallyCreatedTranscript(listOf("en"))
        
        // Translate transcript
        val translatedTranscript = manualTranscript.translate("es")
        val translatedSegments = translatedTranscript.fetch()
        
    } catch (e: TranscriptException) {
        println("Error: ${e.message}")
    }
}
```

### Android Usage

```kotlin
import com.youtubetranscript.YouTubeTranscriptApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Fetch transcript in background
        lifecycleScope.launch {
            try {
                val transcript = withContext(Dispatchers.IO) {
                    YouTubeTranscriptApi.getTranscript("dQw4w9WgXcQ")
                }
                
                // Update UI with transcript
                updateUI(transcript)
                
            } catch (e: TranscriptException) {
                // Handle error
                showError(e.message ?: "Unknown error")
            }
        }
    }
}
```

## API Reference

### YouTubeTranscriptApi

The main API class providing static methods for transcript operations.

#### Methods

- `getTranscript(videoId, languages, preserveFormatting)` - Get transcript for a video
- `listTranscripts(videoId)` - Get all available transcripts for a video
- `extractVideoId(url)` - Extract video ID from YouTube URL
- `hasTranscripts(videoId)` - Check if video has transcripts
- `getAvailableLanguages(videoId)` - Get available language codes

### TranscriptSegment

Represents a single segment of a transcript.

#### Properties

- `text: String` - The transcript text
- `start: Double` - Start timestamp in seconds
- `duration: Double` - Duration in seconds
- `end: Double` - End timestamp (computed)
- `speechDuration: Double` - Estimated speech duration

#### Methods

- `getFormattedTimestamp()` - Get formatted timestamp (MM:SS)
- `overlapsWith(other)` - Check if segments overlap

### Transcript

Represents a transcript for a specific language.

#### Properties

- `videoId: String` - YouTube video ID
- `language: String` - Language name
- `languageCode: String` - Language code
- `isGenerated: Boolean` - Whether auto-generated
- `isTranslatable: Boolean` - Whether can be translated

#### Methods

- `fetch(preserveFormatting)` - Fetch transcript segments
- `translate(languageCode)` - Translate to another language

### TranscriptList

Represents all available transcripts for a video.

#### Methods

- `findTranscript(languageCodes)` - Find transcript by language preference
- `findManuallyCreatedTranscript(languageCodes)` - Find manual transcript
- `findGeneratedTranscript(languageCodes)` - Find generated transcript
- `getAvailableLanguages()` - Get all available language codes
- `getManuallyCreatedLanguages()` - Get manual transcript languages
- `getGeneratedLanguages()` - Get generated transcript languages

## Supported URL Formats

The library supports various YouTube URL formats:

- `https://www.youtube.com/watch?v=VIDEO_ID`
- `https://youtu.be/VIDEO_ID`
- `https://www.youtube.com/embed/VIDEO_ID`
- `https://www.youtube.com/v/VIDEO_ID`
- `https://m.youtube.com/watch?v=VIDEO_ID`
- `https://www.youtube.com/live/VIDEO_ID`
- `https://www.youtube.com/shorts/VIDEO_ID`
- `VIDEO_ID` (direct video ID)

## Error Handling

The library provides specific exception types for different error scenarios:

- `TranscriptException` - Base exception class
- `VideoUnavailableException` - Video is unavailable
- `TranscriptsDisabledException` - Transcripts disabled for video
- `NoTranscriptFoundException` - No transcript for requested languages
- `AgeRestrictedException` - Video is age-restricted
- `IpBlockedException` - IP blocked by YouTube
- `RequestBlockedException` - Request was blocked
- `VideoUnplayableException` - Video is not playable
- `InvalidVideoIdException` - Invalid video ID provided

## Requirements

- Android API 21+ (Android 5.0+)
- Kotlin 1.8+
- OkHttp 4.12.0+
- Internet permission

## Dependencies

- `androidx.core:core-ktx`
- `org.jetbrains.kotlinx:kotlinx-coroutines-android`
- `com.squareup.okhttp3:okhttp`
- `org.json:json`

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Inspired by [youtube-transcript-api](https://github.com/jdepoix/youtube-transcript-api) for Python
- Built for the Android/Kotlin community

## Changelog

### 1.0.0
- Initial release
- Basic transcript fetching functionality
- Multi-language support
- Translation support
- Android integration
- Comprehensive test coverage
