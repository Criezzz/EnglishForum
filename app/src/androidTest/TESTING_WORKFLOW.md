# Testing Workflow & Best Practices

## 📋 Overview

This document contains workflow guidelines and lessons learned from implementing instrumented tests for the EnglishForum Android app. It should be updated as tests are fixed and new patterns emerge.

**⚠️ IMPORTANT**: If tests build successfully and pass, update this file with the lessons learned from that session.

---

## 🎯 Core Principles

### 1. **Always Use `useUnmergedTree = true`**

**Problem**: Compose test framework searches in merged tree by default, but bottom sheets, dialogs, and overlays are in unmerged tree.

**Solution**: Always add `useUnmergedTree = true` to `onNodeWithTag()` and `onAllNodesWithTag()`:

```kotlin
// ✅ CORRECT
composeTestRule.onNodeWithTag("create_post_next_button", useUnmergedTree = true)
    .performClick()

// ❌ WRONG - will fail for bottom sheets/dialogs
composeTestRule.onNodeWithTag("create_post_next_button")
    .performClick()
```

**When to use**: 
- Bottom sheets (ModalBottomSheet)
- Dialogs (AlertDialog, confirmation dialogs)
- Overlays
- Any composable that might be in a separate composition tree

---

### 2. **Test Tags Must Be Added to All Interactive Components**

**Problem**: Tests fail with "Expected exactly '1' node but could not find any node" because UI elements don't have test tags.

**Solution**: Add `Modifier.testTag()` to:
- All TextFields/OutlinedTextFields
- All Buttons
- All IconButtons
- All clickable elements
- Error message Text composables
- Loading indicators
- Empty state messages

**Example**:
```kotlin
OutlinedTextField(
    value = title,
    onValueChange = onTitleChange,
    modifier = Modifier
        .fillMaxWidth()
        .testTag("create_post_title_field"), // ✅ REQUIRED
    // ...
)
```

**Key Lesson from Login/Register/ForgotPassword**: 
- Don't rely on `onNodeWithText()` with string resources - they can change or have encoding issues
- Always use test tags for reliable test identification

---

### 3. **Bottom Sheet State Management in Tests**

**Problem**: Bottom sheets don't automatically disappear from tree when dismissed, causing tests to fail when checking for absence.

**Solution**: Wrap bottom sheet in state-controlled conditional rendering:

```kotlin
composeTestRule.setContent {
    EnglishForumTheme {
        CompositionLocalProvider(LocalAppContainer provides appContainer) {
            // QUAN TRỌNG: state điều khiển việc hiện/ẩn sheet
            var showSheet = remember { mutableStateOf(true) }
            
            if (showSheet.value) {
                CreatePostBottomSheet(
                    onDismiss = { showSheet.value = false },
                    onNavigateToPostDetail = { _ ->
                        showSheet.value = false
                    }
                )
            }
        }
    }
}
```

**Why**: When ViewModel sets `successPostId`, `LaunchedEffect` calls `onDismiss()`, state changes to `false`, and the sheet composable disappears from the tree → all nodes (including buttons) are removed.

---

### 4. **Multi-Step Wizard Flow Testing**

**Problem**: Multi-step flows (like CreatePostBottomSheet with 4 steps) require clicking Next multiple times, not a single submit button.

**Solution**: Understand the actual UI flow:
- Step 0 → 1: Click Next
- Step 1 → 2: Click Next (after filling form)
- Step 2 → 3: Click Next (skip optional steps)
- Step 3 → Submit: Click Next at final step (calls `onSubmit()`)

**Example**:
```kotlin
// STEP 0 -> 1: từ chọn tag sang nhập nội dung
composeTestRule.onNodeWithTag("create_post_next_button", useUnmergedTree = true)
    .performClick()

// Nhập data...

// STEP 1 -> 2: sang màn chọn ảnh
composeTestRule.onNodeWithTag("create_post_next_button", useUnmergedTree = true)
    .performClick()

// STEP 2 -> 3: sang màn preview
composeTestRule.onNodeWithTag("create_post_next_button", useUnmergedTree = true)
    .performClick()

// STEP 3: nút Next bây giờ là "Đăng bài" -> submit
composeTestRule.onNodeWithTag("create_post_next_button", useUnmergedTree = true)
    .performClick()
```

**Key Insight**: The same button (`create_post_next_button`) is reused across steps. At the final step, clicking it calls `onSubmit()`, not a separate `create_post_submit_button`.

---

### 5. **Test Tags Must Match Across Different Composable Variants**

**Problem**: Same UI component used in different contexts (e.g., `Step2ContentInput` vs `CreateFormContent`) might not have test tags in all variants.

**Solution**: Ensure test tags are added to ALL variants of the same component:

```kotlin
// In Step2ContentInput (used in bottom sheet)
OutlinedTextField(
    modifier = Modifier
        .fillMaxWidth()
        .testTag("create_post_title_field"), // ✅ MUST HAVE
    // ...
)

// In CreateFormContent (used in full screen)
OutlinedTextField(
    modifier = Modifier
        .fillMaxWidth()
        .testTag("create_post_title_field"), // ✅ MUST HAVE
    // ...
)
```

**Lesson Learned**: When adding test tags, check all places where the component is used, not just one variant.

---

### 6. **Minimize `waitUntil` Usage**

**Problem**: Overusing `waitUntil` makes tests slow and fragile.

**Solution**: 
- Only use `waitUntil` for **async operations** (API calls, state changes, navigation)
- For synchronous UI rendering, use `assertExists()` directly
- Don't use `waitUntil` just to wait for UI to appear - Compose test framework handles that

**Good**:
```kotlin
// Wait for async operation (post submission)
composeTestRule.waitUntil(timeoutMillis = 5000) {
    composeTestRule.onAllNodesWithTag("create_post_next_button", useUnmergedTree = true)
        .fetchSemanticsNodes().isEmpty()
}
```

**Bad**:
```kotlin
// Don't wait for UI that should be immediately available
composeTestRule.waitUntil(timeoutMillis = 3000) {
    composeTestRule.onAllNodesWithTag("create_post_title_field", useUnmergedTree = true)
        .fetchSemanticsNodes().isNotEmpty()
}
// Instead, just use:
composeTestRule.onNodeWithTag("create_post_title_field", useUnmergedTree = true)
    .assertExists()
```

---

### 7. **ViewModel State Checking in Tests**

**Problem**: Tests need to wait for ViewModel state changes, but `runOnUiThread` in `waitUntil` can cause timeouts.

**Solution**: Use `runOnIdle` instead of `runOnUiThread`:

```kotlin
private fun waitUntilState(
    timeoutMillis: Long = 5_000,
    condition: () -> Boolean
) {
    composeTestRule.waitUntil(timeoutMillis) {
        var result = false
        composeTestRule.runOnIdle { // ✅ Use runOnIdle, not runOnUiThread
            result = condition()
        }
        result
    }
}
```

**Why**: `runOnIdle` ensures UI updates are processed before checking the condition, preventing `ComposeTimeoutException`.

---

### 8. **Helper Functions for Common Patterns**

**Problem**: Repeated code for OTP verification, form filling, etc.

**Solution**: Create reusable helper functions:

```kotlin
// Helper: Request OTP and wait for state update
private fun requestOtp(contact: String) {
    composeTestRule.onNodeWithTag("forgot_password_contact_field", useUnmergedTree = true)
        .performTextInput(contact)
    composeTestRule.onNodeWithTag("forgot_password_send_otp_button", useUnmergedTree = true)
        .performClick()
    
    waitUntilState(timeoutMillis = 5000) {
        viewModel.uiState.isOtpRequested || viewModel.uiState.errorMessage != null
    }
}

// Helper: Verify OTP (auto-verify when 6 digits entered)
private fun verifyOtpFull(otp: String) {
    // Wait for OTP field to appear
    composeTestRule.waitUntil(timeoutMillis = 5000) {
        composeTestRule.onAllNodesWithTag("forgot_password_otp_field", useUnmergedTree = true)
            .fetchSemanticsNodes().isNotEmpty()
    }
    
    composeTestRule.onNodeWithTag("forgot_password_otp_field", useUnmergedTree = true)
        .performTextInput(otp)
    
    waitUntilState(timeoutMillis = 5000) {
        viewModel.uiState.isOtpVerified || viewModel.uiState.otpErrorMessage != null
    }
}
```

---

## 🔧 Common Patterns

### Pattern 1: Testing Multi-Step Forms

```kotlin
@Test
fun testMultiStepForm() {
    setupScreen()
    
    // Step 0 → 1
    composeTestRule.onNodeWithTag("next_button", useUnmergedTree = true)
        .performClick()
    
    // Fill step 1
    composeTestRule.onNodeWithTag("field_1", useUnmergedTree = true)
        .performTextInput("value1")
    
    // Step 1 → 2
    composeTestRule.onNodeWithTag("next_button", useUnmergedTree = true)
        .performClick()
    
    // Continue...
}
```

### Pattern 2: Testing Async Operations

```kotlin
@Test
fun testAsyncOperation() {
    setupScreen()
    
    // Trigger action
    composeTestRule.onNodeWithTag("submit_button", useUnmergedTree = true)
        .performClick()
    
    // Wait for async completion
    composeTestRule.waitUntil(timeoutMillis = 5000) {
        // Check for success state (e.g., sheet dismissed, navigation occurred)
        composeTestRule.onAllNodesWithTag("submit_button", useUnmergedTree = true)
            .fetchSemanticsNodes().isEmpty()
    }
}
```

### Pattern 3: Testing Validation

```kotlin
@Test
fun testValidation() {
    setupScreen()
    
    // Leave required field empty
    // (Don't fill it)
    
    // Try to proceed
    composeTestRule.onNodeWithTag("next_button", useUnmergedTree = true)
        .assertIsNotEnabled() // ✅ Button should be disabled
    
    // Or check error message
    composeTestRule.onNodeWithTag("error_message", useUnmergedTree = true)
        .assertExists()
}
```

---

## ⚠️ Common Pitfalls

### 1. **Forgetting `useUnmergedTree = true`**
- **Symptom**: Tests pass locally but fail in CI, or fail for bottom sheets/dialogs
- **Fix**: Always add `useUnmergedTree = true`

### 2. **Missing Test Tags**
- **Symptom**: "Expected exactly '1' node but could not find any node"
- **Fix**: Add `Modifier.testTag()` to all interactive components

### 3. **Incorrect Flow Understanding**
- **Symptom**: Tests click wrong buttons or miss steps
- **Fix**: Understand the actual UI flow (check the composable code)

### 4. **Overusing `waitUntil`**
- **Symptom**: Tests are slow or timeout unnecessarily
- **Fix**: Only use for async operations

### 5. **State Management Issues**
- **Symptom**: Bottom sheets don't disappear, tests can't verify dismissal
- **Fix**: Wrap in state-controlled conditional rendering

---

## 📋 Pending Test Cases to Implement

### Comment Tests (10 test cases)

**Status**: Test file deleted, needs re-implementation  
**Location**: `app/src/androidTest/java/com/example/englishforum/feature/postdetail/CommentTest.kt`

**Test Cases with Inputs (from conversation history)**:

1. **TC01 - Bình luận bài viết**
   - Input: `comment = "Impressive broski"`
   - Expected: Comment appears in comment section. Post comment count increases by 1.

2. **TC02 - Sửa bình luận**
   - Input: Create comment `"Impressive broski"`, then edit to `"Impressive broski v2"`
   - Expected: Comment content changes. Success snackbar shown.

3. **TC03 - Xóa bình luận**
   - Input: Create comment `"Impressive broski"`, then delete
   - Expected: Confirmation dialog appears. Comment disappears from list. Success snackbar shown. Post comment count decreases by 1.

4. **TC04 - Sửa bình luận khi mất kết nối mạng** (SKIP - offline test)
   - Input: Create comment `"Impressive broski"`, disconnect network, edit to `"Impressive broski v2"`
   - Expected: Connection error snackbar. Comment unchanged.

5. **TC05 - Xóa bình luận khi mất kết nối mạng** (SKIP - offline test)
   - Input: Create comment `"Impressive broski"`, disconnect network, delete
   - Expected: Connection error snackbar. Comment unchanged.

6. **TC06 - Bình luận bài viết khi mất kết nối mạng** (SKIP - offline test)
   - Input: `comment = "Impressive broski"` (offline)
   - Expected: Connection error snackbar. Comment doesn't appear. Comment count unchanged.

7. **TC07 - Phản hồi bình luận**
   - Input: `reply = "reply test"`
   - Expected: Reply appears below the selected comment.

8. **TC08 - Phản hồi bình luận khi mất kết nối mạng** (SKIP - offline test)
   - Input: `reply = "reply test"` (offline)
   - Expected: Connection error snackbar. Reply doesn't appear. Reply count unchanged.

9. **TC09 - Phản hồi trống**
   - Input: `reply = ""`
   - Expected: Send button hidden.

10. **TC10 - Bình luận trống**
    - Input: `comment = ""`
    - Expected: Send button hidden.

**Test Tags Required**:
- `comment_reply_button_${comment.id}`
- `comment_edit_button_${comment.id}`
- `comment_delete_button_${comment.id}`
- `edit_comment_input`
- `edit_comment_confirm_button`
- `delete_comment_confirm_button`
- `comment_upvote_button_${comment.id}`
- `comment_downvote_button_${comment.id}`
- `comment_vote_count_${comment.id}`

**Notes**: 
- Use `PostDetailRouteForTest` helper composable to wrap `PostDetailScreen` with fake dependencies
- Use `useUnmergedTree = true` for all node interactions
- Comment input field and send button need test tags

---

### Vote Tests (12 test cases)

**Status**: Test file deleted, needs implementation  
**Location**: `app/src/androidTest/java/com/example/englishforum/feature/vote/VoteTest.kt`

**Test Cases (6 for Post, 6 for Comment)**:

**Post Vote Tests**:

1. **TC01 - Upvote post (no previous vote)**
   - Input: Click upvote on post with `voteState = NONE`, `voteCount = 0`
   - Expected: `voteState = UPVOTED`, `voteCount = 1`

2. **TC02 - Downvote post (no previous vote)**
   - Input: Click downvote on post with `voteState = NONE`, `voteCount = 0`
   - Expected: `voteState = DOWNVOTED`, `voteCount = -1`

3. **TC03 - Upvote post (previously downvoted)**
   - Input: Click upvote on post with `voteState = DOWNVOTED`, `voteCount = -1`
   - Expected: `voteState = UPVOTED`, `voteCount = 1` (change from -1 to +1 = +2 delta)

4. **TC04 - Downvote post (previously upvoted)**
   - Input: Click downvote on post with `voteState = UPVOTED`, `voteCount = 1`
   - Expected: `voteState = DOWNVOTED`, `voteCount = -1` (change from +1 to -1 = -2 delta)

5. **TC05 - Remove upvote (toggle off)**
   - Input: Click upvote again on post with `voteState = UPVOTED`, `voteCount = 1`
   - Expected: `voteState = NONE`, `voteCount = 0`

6. **TC06 - Remove downvote (toggle off)**
   - Input: Click downvote again on post with `voteState = DOWNVOTED`, `voteCount = -1`
   - Expected: `voteState = NONE`, `voteCount = 0`

**Comment Vote Tests**:

7. **TC07 - Upvote comment (no previous vote)**
   - Input: Click upvote on comment with `voteState = NONE`, `voteCount = 0`
   - Expected: `voteState = UPVOTED`, `voteCount = 1`

8. **TC08 - Downvote comment (no previous vote)**
   - Input: Click downvote on comment with `voteState = NONE`, `voteCount = 0`
   - Expected: `voteState = DOWNVOTED`, `voteCount = -1`

9. **TC09 - Upvote comment (previously downvoted)**
   - Input: Click upvote on comment with `voteState = DOWNVOTED`, `voteCount = -1`
   - Expected: `voteState = UPVOTED`, `voteCount = 1`

10. **TC10 - Downvote comment (previously upvoted)**
    - Input: Click downvote on comment with `voteState = UPVOTED`, `voteCount = 1`
    - Expected: `voteState = DOWNVOTED`, `voteCount = -1`

11. **TC11 - Remove upvote (toggle off)**
    - Input: Click upvote again on comment with `voteState = UPVOTED`, `voteCount = 1`
    - Expected: `voteState = NONE`, `voteCount = 0`

12. **TC12 - Remove downvote (toggle off)**
    - Input: Click downvote again on comment with `voteState = DOWNVOTED`, `voteCount = -1`
    - Expected: `voteState = NONE`, `voteCount = 0`

**Test Tags Required**:
- Post votes: `upvote_button_${postId}`, `downvote_button_${postId}`, `vote_count_${postId}`
- Comment votes: `comment_upvote_button_${comment.id}`, `comment_downvote_button_${comment.id}`, `comment_vote_count_${comment.id}`

**Notes**:
- Use `ForumContentCard` with `testId = postId` for post votes (already added in PostDetailScreen)
- Use `useUnmergedTree = true` for all node interactions
- Vote count can be retrieved using: `.config.getOrElse(SemanticsProperties.Text) { emptyList() }.firstOrNull()?.text?.toIntOrNull()`
- Use `PostDetailRouteForTest` helper composable similar to Comment tests

---

### Create Post Tests - Remaining Cases

**Status**: 6 test cases implemented (TC01-TC04, TC09, TC12), 8 remaining  
**Location**: `app/src/androidTest/java/com/example/englishforum/feature/create/CreatePostTest.kt`

**Remaining Test Cases**:

5. **TC05 - Add 5 valid images**
   - Input: Add 5 images to post
   - Expected: All 5 images added successfully, can proceed

6. **TC06 - Exceed image limit (6 images)**
   - Input: Try to add 6th image
   - Expected: Add button disabled

7. **TC07 - Invalid image format** (SKIP - requires image picker mocking)
   - Input: Try to add .heic/.tiff/.webp file
   - Expected: Error message shown

8. **TC08 - Image size exceeded (>5MB)** (SKIP - requires image picker mocking)
   - Input: Try to add image > 5MB
   - Expected: Error message shown

10. **TC10 - Post while offline** (SKIP - offline test)
    - Input: Create post offline
    - Expected: Connection error shown

11. **TC11 - Post after reconnect** (SKIP - offline test)
    - Input: Create post, disconnect, reconnect, retry
    - Expected: Post created successfully

13. **TC13 - Form state preserved when not posted**
    - Input: Fill form, navigate away without posting
    - Expected: Form state preserved when returning

**Notes**: 
- Image picker tests (TC05-TC08) require mocking `ActivityResultContracts.GetContent()`
- Offline tests (TC10-TC11) require network state mocking
- TC13 may require navigation state management

---

### Edit Post Tests - Remaining Cases

**Status**: 3 basic test cases implemented, 11 remaining  
**Location**: `app/src/androidTest/java/com/example/englishforum/feature/postedit/EditPostTest.kt`

**Remaining Test Cases** (from functional spec - 14 total):

4. **TC04 - Edit post with image changes**
   - Input: Edit post, add/remove images
   - Expected: Images updated correctly

5-14. **Other cases** (image handling, validation, offline, etc.)
   - Similar to Create Post test cases
   - Need to check functional spec for exact inputs

**Notes**: Edit Post uses same `CreateScreen` component, so test tags are already available.

---

## 📝 Notes for Future Sessions

### Current Status (Last Updated: 2024-12-XX)

- ✅ Login tests: Working with test tags and `useUnmergedTree`
- ✅ Register tests: Working with test tags and `useUnmergedTree`
- ✅ Forgot Password tests: Working with `waitUntilState` helper and `useUnmergedTree`
- ✅ Create Post tests: Build successful - 6 test cases implemented (TC01-TC04, TC09, TC12)
- ✅ View Post tests: Build successful - 7 test cases implemented (needs verification)
- ✅ Delete Post tests: Build successful - 4 test cases implemented (needs verification)
- ✅ Notification tests: Build successful - 4 test cases implemented (needs verification)
- ✅ Edit Post tests: Build successful - 3 basic test cases implemented
- ⚠️ Comment tests: File deleted, needs re-implementation (10 test cases)
- ⚠️ Vote tests: File deleted, needs implementation (12 test cases)
- ⚠️ Other test files: Need review for `useUnmergedTree` usage

### Lessons Learned from Create Post Tests

1. **Multi-step wizard requires understanding actual flow**: The CreatePostBottomSheet has 4 steps (tag selection → content → images → preview), and the same `create_post_next_button` is reused. At step 3, clicking Next calls `onSubmit()`, not a separate submit button.

2. **Test tags must be added to ALL variants**: `Step2ContentInput` (used in bottom sheet) initially didn't have test tags, causing tests to fail. Always check all places where a component is used.

3. **Bottom sheet state management is critical**: Wrapping the sheet in state-controlled conditional rendering (`if (showSheet.value)`) ensures the sheet actually disappears from the tree when dismissed, allowing tests to verify dismissal.

4. **Image picker testing is complex**: Tests for adding/removing images require mocking `ActivityResultContracts.GetContent()`, which is non-trivial. Basic tests verify UI structure instead.

### Next Steps

1. ✅ Build successful - tests compile
2. ⏳ Run all tests and verify they pass
3. ⏳ **PRIORITY**: Re-implement Comment tests (10 test cases) - see "Pending Test Cases" section
4. ⏳ **PRIORITY**: Implement Vote tests (12 test cases) - see "Pending Test Cases" section
5. ⏳ Complete remaining Create Post test cases (TC05-TC08, TC10-TC11, TC13)
6. ⏳ Complete remaining Edit Post test cases (TC04-TC14)
7. ⏳ Update this document with any new patterns discovered during test execution
8. ⏳ Add test tags to any remaining UI components if needed
9. ⏳ Review all test files for `useUnmergedTree = true` usage

---

## 🔄 Update Log

- **2024-12-XX**: Initial document created
- **2024-12-XX**: Build successful - Create Post tests compile. Added lessons learned about multi-step wizards, test tag variants, and bottom sheet state management.

---

**Remember**: This document should be updated whenever tests are fixed or new patterns are discovered. If a build succeeds, document what was learned!


    - Input: `comment = ""`
    - Expected: Nút gửi bị ẩn đi
    - Test tags needed: `comment_send_button` (should be disabled/hidden)

**Key Requirements**:
- Use `PostDetailRouteForTest` helper composable (similar to VoteTest pattern)
- Use `useUnmergedTree = true` for all node interactions
- Read `FakePostDetailRepository` and `FakePostStore` to understand mock data structure
- Test tags already added to `PostDetailScreen.kt`:
  - `comment_reply_button_{comment.id}`
  - `comment_edit_button_{comment.id}`
  - `comment_delete_button_{comment.id}`
  - `comment_upvote_button_{comment.id}`
  - `comment_downvote_button_{comment.id}`
  - `comment_vote_count_{comment.id}`
  - `edit_comment_input`
  - `edit_comment_confirm_button`
  - `delete_comment_confirm_button`

---

### ⏳ Vote Tests (12 test cases) - NEED TO RE-IMPLEMENT

**File**: `app/src/androidTest/java/com/example/englishforum/feature/vote/VoteTest.kt`

**Status**: File was deleted, needs to be re-created with proper setup.

**Test Cases (from conversation history)**:

**Post Voting (6 test cases)**:

1. **TC01 - Upvote post**
   - Input: Click upvote button on post
   - Expected: Vote count increases, button shows selected state
   - Test tags: `upvote_button_{postId}`, `vote_count_{postId}`

2. **TC02 - Downvote post**
   - Input: Click downvote button on post
   - Expected: Vote count decreases, button shows selected state
   - Test tags: `downvote_button_{postId}`, `vote_count_{postId}`

3. **TC03 - Remove upvote**
   - Input: Click upvote button again (already upvoted)
   - Expected: Vote count decreases, button shows unselected state
   - Test tags: `upvote_button_{postId}`, `vote_count_{postId}`

4. **TC04 - Switch from upvote to downvote**
   - Input: Post is upvoted, click downvote
   - Expected: Vote count decreases by 2 (from +1 to -1), downvote button selected
   - Test tags: `downvote_button_{postId}`, `vote_count_{postId}`

5. **TC05 - Switch from downvote to upvote**
   - Input: Post is downvoted, click upvote
   - Expected: Vote count increases by 2 (from -1 to +1), upvote button selected
   - Test tags: `upvote_button_{postId}`, `vote_count_{postId}`

6. **TC06 - Vote post offline** ⏭️ SKIP (offline test)

**Comment Voting (6 test cases)**:

7. **TC07 - Upvote comment**
   - Input: Click upvote button on comment
   - Expected: Vote count increases, button shows selected state
   - Test tags: `comment_upvote_button_{commentId}`, `comment_vote_count_{commentId}`

8. **TC08 - Downvote comment**
   - Input: Click downvote button on comment
   - Expected: Vote count decreases, button shows selected state
   - Test tags: `comment_downvote_button_{commentId}`, `comment_vote_count_{commentId}`

9. **TC09 - Remove upvote from comment**
   - Input: Click upvote button again (already upvoted)
   - Expected: Vote count decreases, button shows unselected state
   - Test tags: `comment_upvote_button_{commentId}`, `comment_vote_count_{commentId}`

10. **TC10 - Switch from upvote to downvote (comment)**
    - Input: Comment is upvoted, click downvote
    - Expected: Vote count decreases by 2, downvote button selected
    - Test tags: `comment_downvote_button_{commentId}`, `comment_vote_count_{commentId}`

11. **TC11 - Switch from downvote to upvote (comment)**
    - Input: Comment is downvoted, click upvote
    - Expected: Vote count increases by 2, upvote button selected
    - Test tags: `comment_upvote_button_{commentId}`, `comment_vote_count_{commentId}`

12. **TC12 - Vote comment offline** ⏭️ SKIP (offline test)

**Key Requirements**:
- Use `PostDetailRouteForTest` helper composable
- Use `useUnmergedTree = true` for all node interactions
- Read vote count from `SemanticsNode` using: `.config.getOrElse(SemanticsProperties.Text) { emptyList() }.firstOrNull()?.text?.toIntOrNull()`
- Test tags already added to `PostDetailScreen.kt` and `ForumContentCard.kt`

---

### ⏳ Create Post Tests - Remaining Cases

**File**: `app/src/androidTest/java/com/example/englishforum/feature/create/CreatePostTest.kt`

**Status**: 6 test cases implemented (TC01-TC04, TC09, TC12). Need to add remaining cases.

**Remaining Test Cases**:

5. **TC05 - Add 5 valid images**
   - Input: Add 5 images to post
   - Expected: All 5 images appear in preview, can proceed
   - Note: Requires image picker mocking (complex)

6. **TC06 - Exceed image limit (6 images)**
   - Input: Try to add 6th image
   - Expected: Add button disabled when 5 images reached
   - Test tags: `create_post_add_image_button` (check `assertIsNotEnabled()`)

7. **TC07 - Invalid image format** ⏭️ SKIP (requires image picker mocking)

8. **TC08 - Image size exceeded** ⏭️ SKIP (requires image picker mocking)

10. **TC10 - Post while offline** ⏭️ SKIP (offline test)

11. **TC11 - Post after reconnect** ⏭️ SKIP (offline test)

13. **TC13 - Form state preserved when not posted**
   - Input: Fill form, navigate away without posting
   - Expected: Form fields retain values when returning
   - Note: May require state preservation testing

---

### ⏳ View Post Tests - Fix Timeout Issues

**File**: `app/src/androidTest/java/com/example/englishforum/feature/postdetail/ViewPostTest.kt`

**Status**: Tests timeout - need to fix wait conditions.

**Issues**:
- Tests wait for `post_detail_single_image` but should wait for vote buttons first (more reliable)
- Need to add `testId = uiState.post.id` to `ForumContentCard` in `PostDetailScreen.kt` ✅ (DONE)
- Use vote buttons (`upvote_button_{postId}`) as primary wait condition instead of images

**Test Cases Status**:
- ✅ TC01: Fixed - now waits for vote buttons
- ✅ TC02: Fixed - now waits for vote buttons
- ✅ TC03: Fixed - now waits for vote buttons
- ⚠️ TC04: May need additional wait for fullscreen dialog
- ⚠️ TC05: May need adjustment
- ✅ TC06: Should work with not found message
- ⚠️ TC07: May need adjustment

---

## 📝 Notes for Future Sessions

### Current Status (Last Updated: 2024-12-XX)

- ✅ Login tests: Working with test tags and `useUnmergedTree`
- ✅ Register tests: Working with test tags and `useUnmergedTree`
- ✅ Forgot Password tests: Working with `waitUntilState` helper and `useUnmergedTree`
- ✅ Create Post tests: Build successful - 6 test cases implemented (TC01-TC04, TC09, TC12)
- ⚠️ View Post tests: Timeout issues - fixed wait conditions, need to verify
- ⏳ Comment tests: Need to re-implement (file was deleted)
- ⏳ Vote tests: Need to re-implement (file was deleted)
- ⚠️ Other test files: Need review for `useUnmergedTree` usage

### Lessons Learned from Create Post Tests

1. **Multi-step wizard requires understanding actual flow**: The CreatePostBottomSheet has 4 steps (tag selection → content → images → preview), and the same `create_post_next_button` is reused. At step 3, clicking Next calls `onSubmit()`, not a separate submit button.

2. **Test tags must be added to ALL variants**: `Step2ContentInput` (used in bottom sheet) initially didn't have test tags, causing tests to fail. Always check all places where a component is used.

3. **Bottom sheet state management is critical**: Wrapping the sheet in state-controlled conditional rendering (`if (showSheet.value)`) ensures the sheet actually disappears from the tree when dismissed, allowing tests to verify dismissal.

4. **Image picker testing is complex**: Tests for adding/removing images require mocking `ActivityResultContracts.GetContent()`, which is non-trivial. Basic tests verify UI structure instead.

### Next Steps

1. ✅ Build successful - tests compile
2. ⏳ Run all tests and verify they pass
3. ⏳ **PRIORITY**: Re-implement Comment tests (10 test cases) - see "Pending Test Cases" section
4. ⏳ **PRIORITY**: Implement Vote tests (12 test cases) - see "Pending Test Cases" section
5. ⏳ Complete remaining Create Post test cases (TC05-TC08, TC10-TC11, TC13)
6. ⏳ Complete remaining Edit Post test cases (TC04-TC14)
7. ⏳ Update this document with any new patterns discovered during test execution
8. ⏳ Add test tags to any remaining UI components if needed
9. ⏳ Review all test files for `useUnmergedTree = true` usage

---

## 🔄 Update Log

- **2024-12-XX**: Initial document created
- **2024-12-XX**: Build successful - Create Post tests compile. Added lessons learned about multi-step wizards, test tag variants, and bottom sheet state management.

---

**Remember**: This document should be updated whenever tests are fixed or new patterns are discovered. If a build succeeds, document what was learned!

