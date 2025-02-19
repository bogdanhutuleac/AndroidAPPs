# Delivery Calculator 2

A modern Android application designed to help delivery drivers manage their deliveries, track earnings, and calculate their daily totals. Built with Jetpack Compose and following Material 3 design principles.

## Features

### Home Screen

- Parse and save delivery receipts from clipboard
- Display delivery details including:
  - Delivery addresses (clickable, opens in Google Maps)
  - Subtotal amounts
  - Paid/Unpaid status
- Edit or delete individual entries
- Automatic cleanup of old entries (7 days)

### Report Screen

- Calculate daily earnings
- Track working hours:
  - Configurable start and end times
  - Automatic calculation of total hours
  - Payment calculation (€5 per hour, rounded up)
- Display delivery totals:
  - Total number of deliveries
  - Unpaid delivery amounts
  - Paid delivery deductions (€3 per paid delivery)
- Extra amount field for additional deductions
- Final total calculation showing net earnings

## Technical Details

### Built With

- Kotlin
- Jetpack Compose
- Material 3
- Room Database
- Coroutines & Flow
- ViewModel
- WorkManager

### Architecture

- MVVM (Model-View-ViewModel) architecture
- Clean separation of concerns
- Reactive UI updates using StateFlow
- Persistent storage using Room

### Key Components

- `AppDatabase`: Room database for storing delivery entries
- `ReportViewModel`: Manages state and calculations for the report screen
- `ClipboardScreen`: Handles receipt parsing and entry management
- `ReportScreen`: Manages earnings calculations and display

## Setup

1. Clone the repository

```bash
git clone [repository-url]
```

2. Open in Android Studio

3. Build and run the application

```bash
./gradlew build
```

## Requirements

- Android Studio Arctic Fox or newer
- Minimum SDK: 24 (Android 7.0)
- Target SDK: 35
- Kotlin version: Compatible with your Android Studio version

## License

This project is licensed under the MIT License - see the LICENSE file for details
