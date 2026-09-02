# NextTraceroute 接手檔

> **開工規則：每次開始修改前必須完整讀完本檔；每次結束前必須更新「目前工作」與「驗證紀錄」。**
> 不可只依賴聊天記憶，也不可把 PIN、簽署密碼、Token、真實姓名或真實電子郵件寫入專案、提交、建置產物或發佈資訊。

## 專案與 Git

- 工作目錄：以目前 Git repository 根目錄為準；不可把本機絕對路徑寫入可公開檔案。
- 上游：`origin` → `https://github.com/nxtrace/NextTraceroute.git`
- 維護 Fork：`fork` → `https://github.com/alzpqm/NextTraceroute.git`
- 目前開發分支：`codex/nexttrace-1.7.2-ui`
- 目前穩定版：`v0.2.1`，release commit `750a517`，GitHub Release：`https://github.com/alzpqm/NextTraceroute/releases/tag/v0.2.1`。
- 發佈與提交只能使用 GitHub 帳號名稱，以及 GitHub 提供的 noreply email；不可出現真名或真實 email。
- 每次 push、tag 或 GitHub Release 前都必須完成隱私掃描。發現本機使用者名稱、絕對路徑、裝置序號、PIN、Token、密碼、私鑰、keystore、真實姓名或真實 email 時禁止發佈。
- 隱私掃描必須涵蓋工作樹、待推送 commits、tag/commit 作者資料、APK/AAB 簽章憑證與 Release 中繼資料；掃描結果須更新在本檔。
- 未經本輪明確要求，不建立 GitHub Release；正式版只能在完整檢查未發現阻擋問題後發佈。
- 不覆蓋或刪除不明的未提交檔案。2026-09-02 開工時存在兩個未追蹤目錄：
  - `app/src/main/res/drawable-v24/`
  - `app/src/main/res/mipmap-anydpi-v26/`
  先確認來源與用途，再決定是否納入版本控制。

## Android 與版本基線

- `compileSdk = 37`
- `targetSdk = 37`
- `minSdk = 26`（Android 8.0；舊於此版本不支援）
- Java / Kotlin JVM target：21
- 現行應用版本：`versionName = 0.2.1`、`versionCode = 19`
- 後續公開版本必須高於 0.2.1，不可重複既有版本號或造成比原版更舊的觀感。
- UI 基線：Jetpack Compose + Material 3，需兼顧 Android 17 / API 37 的設計與行為。

## NextTrace 後端

- 客戶端已對齊 NextTrace core/API `v1.7.2`。
- 使用 legacy v3 WebSocket / PoW API 流程；預設不需要使用者 API token。
- 不應重新加入會誤導一般使用者的 API token 設定，除非未來後端協定明確要求。

## 測試環境

- API 37 模擬器：`NextTraceroute_API_37`，ARM64、16K page size。
- Android 14 實機可透過 `adb devices` 在本機辨識；裝置序號與解鎖資訊是敏感資料，不得記錄在此檔或任何提交中。
- Debug application ID：`com.surfaceocean.nexttraceroute.debug`，可與正式版並存。
- macOS 命令列可能預設到 Java 8；建置時必須使用 Android Studio 內建 JBR 21，並設定 `ANDROID_HOME` 或 `ANDROID_SDK_ROOT`。不可把實際本機路徑寫進 repository。
- 常用驗證：
  - `./gradlew testDebugUnitTest lintDebug assembleDebug`
  - `adb install -r app/build/outputs/apk/debug/app-debug.apk`
  - 測試輸入、清除、IME 動作、鍵盤開合、旋轉、深色模式與實際路由追蹤。

## 目前工作（2026-09-02）

- 首頁輸入框跳動已修正：移除浮動 label、固定 64 dp 高度、永久保留清除鍵槽位、單次更新輸入狀態、避免每次按鍵 `trim()`，並固定 Run 按鈕高度。
- API 37 UI hierarchy 已確認空白聚焦、第一字、完整文字與清空後，輸入框皆為 `[68,358][768,526]`，Run 文字皆為 `[905,416][970,469]`；第一字出現時版面 bounds 不變。
- Placeholder 已縮短為 `Domain, IP or URL`，避免一般手機寬度截斷。
- README 與隱私權政策已改為正體中文；舊商店徽章、舊聯絡方式、舊 FAQ 與舊 UI 截圖已移除，改用 API 37 新截圖。
- GitHub repository 描述、0.2.0 Release notes 與已淘汰預覽版 Release notes 已改為正體中文，Issues 已啟用。尚未建立新 Release。
- 原始碼與 LICENSE 保留 `surfaceocean` 著作權署名，但已移除 email；目前維護入口指向 `alzpqm/NextTraceroute`，並保留上游連結。
- 已加入 `scripts/privacy-scan.sh`；發佈來源前執行無參數模式，建置正式 APK/AAB 後執行 `--artifacts`。
- 0.2.1 正式版已完成建置、發佈與遠端核實；後續繼續研究 UI、設定頁面與未解 bugs，下一個公開版本必須高於 0.2.1。

## 驗證紀錄

- `v0.2.0` 發佈前曾完成建置與 API 37 模擬器、Android 14 實機檢查；本輪 UI 修改後仍須重新驗證，不能沿用舊結果。
- 2026-09-02：最後文件與署名更新後，乾淨執行 `clean testDebugUnitTest lintDebug assembleDebug` 成功，56 個 tasks 全部完成。
- 2026-09-02：API 37 模擬器已驗證輸入、清除、IME Go、鍵盤開合、Stop 與實際追蹤；未發現崩潰或 ANR。實機曾短暫取得授權，但安裝時連線中斷，本輪實機回歸尚待補做。
- 2026-09-02：接手檔初稿曾含本機絕對路徑與裝置序號，但在提交前已移除；後續必須確認完整掃描結果為零阻擋項目。
- 2026-09-02：來源隱私掃描通過，GitHub Secret Scanning 為 0 alerts；既有 0.2.0 APK/AAB 解壓掃描通過，簽章 Subject 僅為 `CN=alzpqm, O=alzpqm`。
- 2026-09-02：工作目錄的同步機制曾產生檔名帶 ` 2` 的 Markdown、Gradle 與 class 副本。舊 Markdown 副本曾含已移除的 email，已在提交前刪除；class 副本曾造成 D8 duplicate class，執行 Gradle `clean` 後恢復。每次提交前都要檢查 `git status --untracked-files=all` 與名稱帶 ` 2` 的檔案。
- 2026-09-02：0.2.1／versionCode 19 已成功安裝到 API 37 模擬器與 Android 14 實機；實機確認套件版本並成功開啟首頁。
- 2026-09-02：0.2.1 Release APK/AAB 與 lintVital 建置成功（59 tasks）；APK manifest 為 application ID `com.surfaceocean.nexttraceroute`、target/compile SDK 37。
- 2026-09-02：0.2.1 產物隱私掃描通過，APK 簽章 Subject 為 `CN=alzpqm, O=alzpqm`。APK SHA-256：`e28a50ce66c56a45edc609fe4ffac556103ec6baecd169907e977f6ceb2a2164`；AAB SHA-256：`c3385873340f025629a81ba7dbfbdd94b8cfebb5f218374646a95b7bbb1693f0`。
- 2026-09-02：遠端 v0.2.1 為正式版與 latest，作者為 `alzpqm`，tag 解析至 `750a517`，兩個 Release assets 的遠端 digest 與上述雜湊一致，GitHub Secret Scanning 為 0 alerts。
- 2026-09-02：`.github/workflows/build.yml` 會在 Release 發佈後重複建置並以 `overwrite: true` 覆蓋已驗證資產。本次重複 run 已取消，遠端資產未被改動；下次發佈前必須修正或移除此流程。
