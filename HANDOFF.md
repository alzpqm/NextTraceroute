# NextTraceroute 接手檔

> **開工規則：每次開始修改前必須完整讀完本檔；每次結束前必須更新「目前工作」與「驗證紀錄」。**
> 不可只依賴聊天記憶，也不可把 PIN、簽署密碼、Token、真實姓名或真實電子郵件寫入專案、提交、建置產物或發佈資訊。

## 專案與 Git

- 工作目錄：以目前 Git repository 根目錄為準；不可把本機絕對路徑寫入可公開檔案。
- 上游：`origin` → `https://github.com/nxtrace/NextTraceroute.git`
- 維護 Fork：`fork` → `https://github.com/alzpqm/NextTraceroute.git`
- 目前開發分支：`codex/nexttrace-1.7.2-ui`
- 目前穩定版：`v0.2.2`，release commit `92eaca2`，GitHub Release：`https://github.com/alzpqm/NextTraceroute/releases/tag/v0.2.2`。
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
- 現行應用版本：`versionName = 0.2.2`、`versionCode = 20`
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
- 本機正式簽署金鑰的位置索引與鑰匙圈服務名稱已記錄在 `.git/info/nexttraceroute-private-handoff.md`；該檔僅存在本機且不會被提交。後續簽署前先讀該檔，不可重新搜尋或輸出金鑰位置與密碼。
- Fork 已設定正式簽署所需的四個加密 Actions Secrets；`.github/workflows/build.yml` 的「手動簽署建置」可直接產生與 0.2.1 相同憑證的 APK／AAB。API 37 平台的正確 SDK 套件 ID 是 `platforms;android-37.0`。
- 常用驗證：
  - `./gradlew testDebugUnitTest lintDebug assembleDebug`
  - `adb install -r app/build/outputs/apk/debug/app-debug.apk`
  - 測試輸入、清除、IME 動作、鍵盤開合、旋轉、深色模式與實際路由追蹤。

## 目前工作（2026-09-06）

- 首頁輸入框跳動已修正：移除浮動 label、固定 64 dp 高度、永久保留清除鍵槽位、單次更新輸入狀態、避免每次按鍵 `trim()`，並固定 Run 按鈕高度。
- API 37 UI hierarchy 已確認空白聚焦、第一字、完整文字與清空後，輸入框皆為 `[68,358][768,526]`，Run 文字皆為 `[905,416][970,469]`；第一字出現時版面 bounds 不變。
- Placeholder 已縮短為 `Domain, IP or URL`，避免一般手機寬度截斷。
- README 與隱私權政策已改為正體中文；舊商店徽章、舊聯絡方式、舊 FAQ 與舊 UI 截圖已移除，改用 API 37 新截圖。
- GitHub repository 描述、0.2.0 Release notes 與已淘汰預覽版 Release notes 已改為正體中文，Issues 已啟用。尚未建立新 Release。
- 原始碼與 LICENSE 保留 `surfaceocean` 著作權署名，但已移除 email；目前維護入口指向 `alzpqm/NextTraceroute`，並保留上游連結。
- 已加入 `scripts/privacy-scan.sh`；發佈來源前執行無參數模式，建置正式 APK/AAB 後執行 `--artifacts`。
- 0.2.1 正式版已完成建置、發佈與遠端核實；後續繼續研究 UI、設定頁面與未解 bugs，下一個公開版本必須高於 0.2.1。
- 0.2.2 已修正日／夜模式狀態錯誤：應用程式顏色固定來自當前 Material 3 `colorScheme`，舊版 `settings.json` 的 11 個顏色欄位會被忽略，重新儲存時也會自動移除；系統主題切換後不再被舊設定覆蓋。
- 設定頁已重構為 Material 3 區塊式版面，改用適當的 surface／on-surface 色階、垂直配置 Slider、外置輸入欄標籤與較低對比分隔線；已移除選色器、Dark／Light Preset 與 `compose-color-picker` 依賴。
- 已移植上游 0.1.7 的重複 IP 防崩潰修正，位址選擇清單改以去重後資料與穩定 key 顯示，不回退本分支較新的依賴、API 37 或版本基線。
- `.github/workflows/build.yml` 已改為僅能手動啟動的簽署建置，不再由 Release 事件觸發或覆蓋正式資產；工作流程只產生保留一天的暫存 artifact，正式發佈前仍須下載至本機重新核對。
- 0.2.2 正式版已發佈；APK、AAB 與 `SHA256SUMS.txt` 均已上傳，GitHub 遠端雜湊完成核對。後續公開版本必須高於 0.2.2。

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
- 2026-09-06：API 37 模擬器完成日／夜模式對照。全新設定下冷啟動可跟隨系統；在深色模式進入設定並按 Save，再切回日間模式及冷啟動，可穩定重現 App 背景仍為黑色、狀態列黑色圖示不可讀。根因位於 `MainActivity` 還原 11 組固定色與 `Settings` 無條件保存這些顏色；本輪僅診斷與記錄，未修改 UI 程式。
- 2026-09-06：0.2.2 修正後完成乾淨 `clean testDebugUnitTest lintDebug assembleDebug`，56 個 tasks 全部成功；lint 為 0 errors、5 個非阻擋更新／目錄整理警告。
- 2026-09-06：API 37 模擬器確認深色模式儲存設定後切回日間模式，首頁、狀態列與導覽列皆正確恢復淺色；重新產生的 `settings.json` 不含任何顏色欄位。
- 2026-09-06：API 37 模擬器完成設定頁頂部、DNS 與進階服務端的深色畫面檢查；大字級下輸入內容不再與標籤重疊，連續 Slider 不再顯示過密刻度。網域解析、IPv4／IPv6 位址選擇、開始追蹤及橫向旋轉皆未出現崩潰或 ANR。
- 2026-09-06：正式簽署工作流程 run `34016502792` 在提交 `92eaca2` 上成功完成 Android 37 安裝、單元測試、lint、APK／AAB 建置、產物隱私掃描與暫存上傳。
- 2026-09-06：0.2.2 正式 APK 可直接以 `adb install -r` 覆蓋 0.2.1；升級後為 versionCode 20、versionName 0.2.2、minSdk 26、targetSdk 37，Activity 冷啟動成功。0.2.1 與 0.2.2 憑證 SHA-256 完全相同，Subject 為 `CN=alzpqm, O=alzpqm`。
- 2026-09-06：0.2.2 發佈產物再次通過本機隱私掃描。APK SHA-256：`8dd786d5adbe407b26af16c6a9bd145cb266293b3d7ad69f1f13405b07d1e054`；AAB SHA-256：`ccb90862f679d2bacc38be0c7b438453058d3175c165b9bf139ee53f0ae58f9e`。
- 2026-09-06：遠端 v0.2.2 為正式版與 latest，作者為 `alzpqm`，tag 解析至 `92eaca2`；APK、AAB 與 SHA256SUMS 三個 Release assets 的遠端 digest 均與本機檔案一致，GitHub Secret Scanning 為 0 alerts。Release 發佈後沒有自動覆蓋資產的工作流程。
