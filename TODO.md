# MIYU-Kotlin Porting TODOs
*A concise and detailed list of features from the React Native reference app that have not yet been implemented in the Kotlin port.*

### 1. Reader AI Assistant & Integrations
- [ ] **AI Configuration Modal** (`ReaderAiConfigModal`): UI to configure LLM providers (OpenAI, Gemini, local models) and API keys.
- [ ] **AI Side Panel** (`ReaderAiPanel`): Sliding panel in the reader to chat with the AI about the current book, ask for summaries, character descriptions, or complex translations.
- [ ] **AI Provider Icons** (`ReaderAiProviderIcon`): UI component for provider selection.

### 2. Cloud Sync & Authentication
- [ ] **Supabase Integration**: Set up the Supabase Kotlin SDK to match the `supabase-setup.sql` schema from the RN project.
- [ ] **Authentication Screens**: Implement Auth callback, email/password login, and user profile management (equivalent to `app/auth/confirm.tsx`).
- [ ] **Cloud Drive Sync**: Implement cloud backup/restore for books, annotations, and reading progress (`app/auth/drive.tsx`).

### 3. Advanced Dictionary Management
- [ ] **Offline Dictionary Parsing Logic**: The current `DictionaryLookupBottomSheet` is mocked. Real parsing logic for StarDict or similar formats needs to be written.
- [ ] **Dictionary Library Manager** (`DictionaryLibraryModal`): UI to download, import, prioritize, and delete offline dictionary packages.

### 4. Terms & Vocabulary Community Features
*(While the base `TermsScreen` exists, the advanced flashcard/vocabulary tools are missing)*
- [ ] **Community Term Groups** (`CommunityTermGroupsModal`): Ability to share and discover vocabulary lists created by other users via the cloud.
- [ ] **Term Group Export/Import** (`TermGroupExportModal`): Export vocabulary to Anki or CSV formats.
- [ ] **Term Group Details & Editing** (`TermGroupDetail`, `AddTermModal`): Deep management of flashcard groups, tags, and definitions.

### 5. Native C++ EPUB Search Engine
- [ ] **C++ Search Implementation**: The current Kotlin `SearchInBookBottomSheet` relies on simulated placeholder data. The `EpubEngineBridge` JNI layer must be updated to traverse the EPUB spine and run efficient regex/text searches across the raw HTML nodes, returning accurate `matchStart` and `matchEnd` indices.

### 6. Miscellaneous UI & Polish
- [ ] **Theme Creator** (`ThemeCreator`): Allow users to define custom reading themes (background color, text color, accent color) and save them to `UserPreferences`.
- [ ] **Book Loading Animation** (`BookLoadingAnimation`): Add the specialized loading spinner/animation from the RN app while the C++ engine parses large EPUBs.
