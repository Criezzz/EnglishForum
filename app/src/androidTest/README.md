# Android Instrumented Tests

This directory contains instrumented tests that run on Android devices or emulators.

## Test Coverage

All tests are based on **functional test specifications** with real test data.

### Authentication Tests (`feature/auth/`)

#### **LoginScreenTest.kt** - 10 tests ✅
Based on Login functional test cases (TC1-TC6):
- ✅ TC1: Valid username and password → Success
- ✅ TC2: Valid email and password → Success  
- ✅ TC3: Non-existent account → Error message
- ✅ TC4: Wrong password → Error message
- ✅ TC5: Empty username → Login button disabled
- ✅ TC6: Empty password → Login button disabled
- UI components display
- Register link navigation
- Forgot password link
- Loading state

**Test Data:**
- Valid credentials: `testusername1` / `testusername1`
- Valid email: `cuong190105@gmail.com` / `testusername1`
- Invalid account: `abctusername1` / `asdfghjkl`

#### **RegisterScreenTest.kt** - 10 tests ✅
Based on Registration functional test cases (TC1-TC7, Phase 1):
- ✅ TC1: Valid inputs → Success, navigate to OTP screen
- ✅ TC2: Existing email → Error message
- ✅ TC3: Existing username → Error message
- ✅ TC4: Username < 8 chars → Error message
- ✅ TC5: Invalid email format → Error message
- ✅ TC6: Password < 8 chars → Error message (⚠️ server returns JSON)
- ✅ TC7: Password mismatch → Error message
- UI components display
- Loading state
- Input validation

**Test Data:**
- Valid: `testusername2` / `projectaccnt001@gmail.com` / `testusername2`
- Short username: `test2`
- Invalid email: `projectaccnt002,gmail.com`
- Password mismatch: `test0002` vs `test0005`

**Note:** Phase 2 (OTP verification TC8-TC10) tests are commented out - uncomment when implementing `EmailVerificationScreen`

#### **ForgotPasswordScreenTest.kt** - 14 tests ✅
Based on Forgot Password functional test cases (TC1-TC11, All 3 phases):

**Phase 1 - Request OTP (TC1-TC5):**
- ✅ TC1: Existing username → Send OTP success
- ✅ TC2: Existing email → Send OTP success
- ✅ TC3: Non-existent account → Error (⚠️ shows "User not found")
- ✅ TC4: Empty username → Send OTP button disabled
- ✅ TC5: Too many requests → Error message

**Phase 2 - Verify OTP (TC6-TC8):**
- ✅ TC6: Wrong OTP `000000` → Error message
- ✅ TC7: Resend OTP → Button disabled for 60s
- ✅ TC8: Correct OTP `330478` → Show password fields

**Phase 3 - Reset Password (TC9-TC11):**
- ✅ TC9: Short password `abc` → Error (⚠️ min 6 chars, not 8)
- ✅ TC10: Password mismatch → Error message
- ✅ TC11: Valid password match → Success, navigate to login

**Test Data:**
- Valid username: `testusername1`
- Valid email: `cuong190105@gmail.com`
- Wrong OTP: `000000`
- Correct OTP: `330478`
- Valid new password: `testusername1`

### Search Screen Tests (`feature/search/`)
- **SearchScreenTest.kt** - 9 tests
  - Search field display
  - Search query input
  - Empty state
  - Loading state
  - Post results display
  - User results display
  - No results message
  - Post click interaction
  - Clear button

### Comment Tests (`feature/postdetail/`)

#### **CommentTest.kt** - 13 tests ✅
Based on Comment functional test cases (TC1-TC10):
- ✅ TC1: Create comment → Success, count increases
- ✅ TC2: Edit comment → Content changes, success snackbar
- ✅ TC3: Delete comment → Confirmation dialog, success snackbar, count decreases
- ✅ TC4: Edit comment offline → Connection error
- ✅ TC5: Delete comment offline → Connection error
- ✅ TC6: Create comment offline → Connection error
- ✅ TC7: Reply to comment → Reply appears below comment
- ✅ TC8: Reply offline → Connection error
- ✅ TC9: Empty reply → Send button hidden
- ✅ TC10: Empty comment → Send button hidden
- Non-empty text shows send button
- Loading indicator
- Comment card interactions

**Test Data:**
- Comment text: `Impressive broski`
- Edited comment: `Impressive broski v2`
- Reply text: `reply test`


## Running Tests

### Using Gradle Tasks (UI)
1. Open **Gradle** panel in Android Studio
2. Navigate to: `app` → `Tasks` → `verification`
3. Double-click: **`jacocoAndroidTestReport`**

### Using Command Line
```bash
./gradlew jacocoAndroidTestReport
```

This will:
1. Run all instrumented tests on connected device/emulator
2. Generate JaCoCo coverage report
3. Output HTML report to: `build/reports/jacoco/androidTest/html/index.html`

### Prerequisites
- Android device or emulator must be connected
- USB Debugging enabled (for physical devices)
- Minimum API level: 28

## Test Statistics
- **Total Test Files**: 1 file
- **Total Test Cases**: 1 test (default example)
- **Functional Spec Coverage**: 
  - 📝 Login: 6 test cases (documented, not implemented)
  - 📝 Register: 10 test cases (documented, not implemented)
  - 📝 Forgot Password: 11 test cases (documented, not implemented)
  - 📝 Comment: 10 test cases (documented, not implemented)
  - 📝 Edit Post: 14 test cases (documented, not implemented)
  - 📝 Create Post: 14 test cases (documented, not implemented)
  - 📝 Search: 3 test cases (documented, not implemented)
  - 📝 Vote: 12 test cases (documented, not implemented)
  - 📝 View Post: 7 test cases (documented, not implemented)
  - 📝 View Notification: 4 test cases (documented, not implemented)
  - 📝 Delete Post: 5 test cases (documented, not implemented)
- **Overall Coverage**: 0/96 specs = **0% implemented**
- **Coverage Target**: All functional test specifications are documented but need implementation

**Note**: All test files were removed due to outdated component APIs. Tests need to be rewritten from scratch to match current codebase structure.

## Test Structure
```
app/src/androidTest/
├── java/com/example/englishforum/
│   └── ExampleInstrumentedTest.kt (1 test - default)
├── README.md (this file)
└── TEST_SUMMARY.md (detailed functional test case specifications)
```

**Total**: 1 file, 1 test

## Coverage Report Location
After running tests, open the coverage report:
```
build/reports/jacoco/androidTest/html/index.html
```

## Notes
- Tests use Jetpack Compose Testing library
- All tests are isolated and can run independently
- Tests verify UI behavior, not business logic (use unit tests for that)
- Test execution requires actual Android runtime (not JVM)

