# Delivery Calculator 2

A modern Android application designed to help delivery drivers manage their deliveries, track earnings, and calculate their daily totals. Built with Jetpack Compose and following Material 3 design principles.

## How to Use

### Scanning Receipts

1. Take a photo of the receipt using your phone's camera
2. Open Google Lens (or select the Lens option in your gallery)
3. Use the Lens option to scan the receipt
4. Select all the text from the scanned receipt
5. Copy the selected text to clipboard
6. Open the Delivery Calculator app
7. Click "Save Receipt from Clipboard" to import the receipt data

The app supports various receipt formats:

- Just Eat receipts
- JustEats receipts
- Shop receipts (San Marino)
- Deliveroo receipts
- Online receipts

### Managing Deliveries

1. Home Screen:

   - View all deliveries for the selected date
   - Each delivery card shows:
     - Delivery address (tap to open in Google Maps)
     - Subtotal amount
     - Payment status (Paid/Not Paid)
     - Phone number with direct call button (where available)
   - Edit subtotal by tapping the amount
   - Toggle payment status using the switch
   - Delete entries using the red delete button

2. Phone Number Handling:

   - Just Eat/JustEats: Uses "014832993" with masking/verification code
   - Shop Receipts: Uses actual phone number (adds "01" prefix if starts with "2")
   - Deliveroo: Uses provided phone number with access code
   - Online Receipts: Uses actual phone number

3. Address Handling:
   - All addresses are clickable and open in Google Maps
   - Different formats are supported for each receipt type
   - Special characters and formatting are cleaned automatically

### Calculating Earnings

1. Report Screen:

   - Select working hours:

     - Set start time (default 12:00)
     - Set end time
     - Hours are calculated automatically

   - Payment Calculations:

     - Hours payment: €5 per hour (rounded up)
     - Paid delivery deduction: €3 per paid delivery
     - Extra deductions: Add any additional amounts

   - View Totals:
     - Total unpaid deliveries amount
     - Number of paid/unpaid deliveries
     - Final earnings after all deductions

2. Daily Management:
   - Switch between dates using the date selector
   - Each date maintains separate:
     - Working hours
     - Delivery counts
     - Payment calculations
   - Automatic cleanup of entries older than 7 days

### Additional Features

- One-tap calling: Click phone icon to call customer
- Automatic formatting of phone numbers and codes
- Material 3 design with clean, modern interface
- Persistent storage of all delivery data
- Error handling for invalid receipt formats
- Automatic state saving and recovery

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
