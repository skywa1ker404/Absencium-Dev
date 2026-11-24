# Unused code scan (2026-11-24)

Method: every `fun` name that appears exactly once in the whole repository (its own definition). This finds dead code but also false positives: navigation screens (reached through generated destinations), Room DAO queries and type converters (called by Room), and interface or serialization hooks. The release build already strips unused code (R8), so removing dead code helps readability and maintenance, not the APK size.

## Likely dead, AI hub UI (composables nobody calls)
AssistantPicker, AssistantItem, MinimalFileButton (+Compact, Grouped), ReasoningLevelCard, SearchPickerButton, ReasoningPill, ToolUsePill, ActivityTimelineSheet, ToolCallDetails, UserMessageDropdown, ChatMessageAssistantAvatar, ChatMessageUserAvatar, ToolCallItem, CollapsibleTranslationText, LanguageSelectionDialog, AutoProviderIcon, DependentToggle, FloatingWindow, NumberInput, KeepScreenOn, MaterialIconPickerDialog, StickyHeader, TabAnimation, ExportDialog, WaveOriginPickerDialog, UpdatePill, PhantomLoadingTurn, SearchBarWithToggle, BuiltInToolsSettings, LocalHuggingFaceInstallFab, LocalLiteRtModelIdentitySheet, ModelPicker, OpenAICompatibilityModeSetting, ReasoningBehaviorEditor, DefaultTranslationModelSetting, SettingItem, AiMemoryModeCard, AiMemoryRow, AiMemorySettingsItem, AiComposerAddIcon, AiComposerDefaultActionContent, AiDrawerActionIcon.

## Likely dead pages (whole screens nothing opens)
AssistantRagMemorySubPage (the RAG memory page; its memory debugger is dead with it), AssistantLocalToolSubPage, AssistantSettingsSection / AssistantSettingsItem.

## Likely dead helpers
Hub: ChatUtil (count/delete/list chat files), FontFileManager (get/list/restore fonts), IconStorageManager (clear, count, size), EmojiUtils, OwnedFileStorage (import, delete), PermissionChecker (missing, location), AppShortcutManager (clear, circular icon), SecretKeyManager (Hugging Face token, STT secrets, import), SecureStore (clearAll, hasSecret, importSecret), ShareIntentResolver, MarkdownUtils.stripMarkdown, ChatService.translateMessage, BackupVM.mergeImportedProviders (left from the removed ChatBox/Cherry import), speech `fetchSttModels`.
App: ApiKeyVault addKey / removeKey / useKey / providersWithKey, SettingsRepository.clearAiConnection, TabPill, MarkdownToolbar, TemplateDialog, noteBadges, DetailLine, SyllabusViewModel importFile / setStatus, AddEntryViewModel dismissAiSetup / dismissAiSetup2 / showMessage, AiPermissions.isAllowed.

## Kept (false positives)
Navigation screens (About, Changelog, IntroTour, Licenses, AiHub, Profile, EditProfile, Settings pages, Study pages, Syllabus, TestLab, Trash, Vault, Main): reached through generated destinations. DAO queries and type converters: called by Room.
